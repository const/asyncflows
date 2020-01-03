/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.protocol.http.client.core;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.util.SimpleChannel;
import org.asyncflows.protocol.http.HttpException;
import org.asyncflows.protocol.http.client.AHttpRequest;
import org.asyncflows.protocol.http.client.AHttpRequestProxyFactory;
import org.asyncflows.protocol.http.client.HttpRequestUtil;
import org.asyncflows.protocol.http.client.HttpResponse;
import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.HttpRequestMessage;
import org.asyncflows.protocol.http.common.HttpResponseMessage;
import org.asyncflows.protocol.http.common.HttpRuntimeUtil;
import org.asyncflows.protocol.http.common.HttpScopeUtil;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.HttpVersionUtil;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.content.ContentUtil;
import org.asyncflows.protocol.http.common.content.InputState;
import org.asyncflows.protocol.http.common.content.OutputState;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.common.headers.TransferEncoding;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.function.AsyncFunctionUtil.constantSupplier;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * The client action. Note that the action does not supports pipelining directly.
 */
public class HttpClientAction extends CloseableInvalidatingBase implements AHttpRequest, NeedsExport<AHttpRequest> {
    // TODO enable pipelining (output released event, input released event)
    /**
     * The connection to use.
     */
    private final HttpClientConnection connection;
    /**
     * The request message.
     */
    private final HttpRequestMessage requestMessage = new HttpRequestMessage();
    /**
     * The response message.
     */
    private final HttpResponseMessage responseMessage = new HttpResponseMessage();
    /**
     * The input trailers.
     */
    private final Promise<HttpHeaders> inputTrailers = new Promise<>();
    /**
     * A promise that resolves when processing is finished.
     */
    private final Promise<Boolean> finished = new Promise<>();
    /**
     * The current state of input stream.
     */
    private InputState inputState;
    /**
     * The input stream information.
     */
    private ContentUtil.StreamInfo<AInput<ByteBuffer>> inputStream;
    /**
     * The current state of output stream.
     */
    private OutputState outputState;
    /**
     * The output stream information.
     */
    private ContentUtil.StreamInfo<AOutput<ByteBuffer>> outputStream;
    /**
     * If true, requests can continue.
     */
    private boolean canContinue = true;
    /**
     * The scope for the request.
     */
    private Scope scope;
    /**
     * True if request started.
     */
    private boolean requestStarted;
    /**
     * True if response started.
     */
    private boolean responseStarted;
    /**
     * True if protocol was switched.
     */
    private boolean switchedProtocol;

    /**
     * The constructor.
     *
     * @param connection the connection
     */
    public HttpClientAction(final HttpClientConnection connection) {
        this.connection = connection;
    }

    /**
     * @return if the request finished.
     */
    public Promise<Boolean> finish() {
        return finished;
    }

    @Override
    protected Promise<Void> closeAction() {
        return aSeq(
                () -> aAll(() -> {
                    if (inputStream != null) {
                        return inputStream.getStream().close();
                    } else {
                        canContinue = false;
                        return aVoid();
                    }
                }).andLast(() -> {
                    if (outputStream != null) {
                        return outputStream.getStream().close();
                    } else {
                        canContinue = false;
                        return aVoid();
                    }
                }).toVoid()
        ).finallyDo(() -> {
            if (!isValid()) {
                canContinue = false;
            }
            if (inputState != InputState.CLOSED || outputState != OutputState.CLOSED) {
                canContinue = false;
            }
            notifySuccess(finished.resolver(), canContinue);
            return aVoid();
        });
    }

    /**
     * Check if auto-close operation should be initiated.
     */
    private void checkAutoClose() {
        if (isValid()) {
            if (inputState == null || outputState == null) {
                return;
            }
            switch (inputState) {
                case ERROR:
                case CLOSED:
                case CLOSED_BEFORE_EOF:
                case EOF_NO_TRAILERS:
                    break;
                default:
                    return;
            }
            switch (outputState) {
                case ERROR:
                case CLOSED:
                case CLOSED_LAST:
                    break;
                default:
                    return;
            }
        }
        close();
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return connection.getRemoteAddress();
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return connection.getLocalAddress();
    }

    @Override
    public Promise<AOutput<ByteBuffer>> request(final Scope requestScope, final String method, final URI uri,
                                                final HttpHeaders headers, final Long length) {
        if (requestStarted) {
            throw new HttpException("The request is already started");
        }
        requestStarted = true;
        this.scope = requestScope;
        try {
            ensureValidAndOpen();
            requestMessage.setMethod(method);
            requestMessage.setEffectiveUri(uri);
            requestMessage.setVersion(requestScope.get(HttpScopeUtil.FORCE_VERSION, HttpVersionUtil.HTTP_VERSION_1_1));
            requestMessage.setHeaders(new HttpHeaders(headers));
            final Boolean lastExchange = requestScope.get(HttpScopeUtil.LAST_EXCHANGE);
            if (lastExchange != null && lastExchange) {
                canContinue = false;
            }
            HttpClientMessageUtil.inferRequestTarget(requestMessage, connection.getHost());
            final List<TransferEncoding> encodings;
            final Long contentLength;
            if (length != null && length == HttpRequestUtil.NO_CONTENT) {
                contentLength = null;
                encodings = Collections.emptyList();
            } else {
                contentLength = length;
                encodings = ContentUtil.getTransferEncodings(requestMessage.getVersion(), length);
            }
            outputStream = ContentUtil.getOutput(requestMessage.getMethod(), null, connection.getOutput(),
                    outputTracker(), trailersProvider(encodings), null, encodings, contentLength);
            if (outputStream.isRestOfTheStream()) {
                canContinue = false;
            }
            HttpHeadersUtil.setMessageBodyHeaders(requestMessage.getHeaders(),
                    outputStream.getEncodingList(), outputStream.getContentLength());
            requestContinue();
            HttpHeadersUtil.setLastMessageHeader(requestMessage.getHeaders(),
                    requestMessage.getVersion(), !canContinue);
            requestMessage.getHeaders().setHeaderIfMissing(
                    HttpHeadersUtil.USER_AGENT_HEADER, connection.getUserAgent());
            return aSeq(
                    () -> HttpClientMessageUtil.writeRequestMessage(connection.getOutput(), requestMessage)
            ).thenDoLast(
                    () -> aValue(outputStream.getStream())).listen(outcomeChecker()
            );
        } catch (Throwable ex) {
            invalidate(ex);
            return aFailure(new HttpException("Failed to create request", ex));
        }
    }


    /**
     * Request continue if needed.
     */
    public void requestContinue() {
        if (HttpVersionUtil.isHttp11(requestMessage.getVersion())) {
            final HttpHeaders headers = requestMessage.getHeaders();
            if (scope.get(HttpRequestUtil.CONTINUE_LISTENER) != null) {
                headers.addHeader(HttpHeadersUtil.CONNECTION_HEADER, HttpHeadersUtil.EXPECT_HEADER);
                headers.setHeader(HttpHeadersUtil.EXPECT_HEADER, HttpHeadersUtil.EXPECT_CONTINUE);
            } else {
                headers.removeHeader(HttpHeadersUtil.EXPECT_HEADER);
            }
        } else {
            notifyContinue();
        }
    }

    /**
     * Get provider for the trailers.
     *
     * @param encodings encodings
     * @return the provider for trailers
     */
    private ASupplier<HttpHeaders> trailersProvider(final List<TransferEncoding> encodings) {
        return HttpScopeUtil.trailersProvider(scope, requestMessage.getHeaders(), encodings);
    }


    /**
     * @return the tracker for output state
     */
    private AResolver<OutputState> outputTracker() {
        return resolution -> {
            if (resolution.isFailure()) {
                outputState = OutputState.ERROR;
                invalidate(resolution.failure());
            } else {
                outputState = resolution.value();
            }
            if (switchedProtocol && (outputState == OutputState.CLOSED
                    || outputState == OutputState.CLOSED_LAST
                    || outputState == OutputState.ERROR)) {
                connection.getOutput().getOutput().close();
            }
            checkAutoClose();
        };
    }

    /**
     * @return the promise for HTTP response, when response type becomes finally known.
     */
    @Override
    public Promise<HttpResponse> getResponse() {
        if (responseStarted) {
            throw new HttpException("Response could be requested only once");
        }
        responseStarted = true;
        return aSeq(() -> {
            ensureValidAndOpen();
            return waitForResponse();
        }).thenDo(() -> {
            if (HttpStatusUtil.isSwitchProtocol(requestMessage.getMethod(), responseMessage.getStatusCode())) {
                return switchProtocol();
            } else {
                return normalResponse();
            }
        }).failedLast(HttpRuntimeUtil.toHttpException()).listen(outcomeChecker());
    }

    private Promise<Void> waitForResponse() {
        return aSeqWhile(
                () -> HttpClientMessageUtil.readResponseMessage(connection.getInput(), responseMessage).thenFlatGet(
                        () -> {
                            final int statusCode = responseMessage.getStatusCode();
                            if (statusCode == HttpStatusUtil.CONTINUE) {
                                notifyContinue();
                                return aTrue();
                            } else if (statusCode == HttpStatusUtil.SWITCHING_PROTOCOLS) {
                                return aFalse();
                            } else if (HttpStatusUtil.isInformational(statusCode)) {
                                return aTrue();
                            }
                            return aFalse();
                        }
                ));
    }

    private Promise<HttpResponse> normalResponse() {
        if (HttpHeadersUtil.isLastExchange(responseMessage.getVersion(), responseMessage.getHeaders())) {
            canContinue = false;
        }
        final Long contentLength = HttpHeadersUtil.getContentLength(responseMessage.getHeaders());
        final List<TransferEncoding> encodings = TransferEncoding.parse(
                responseMessage.getHeaders().getHeaders(HttpHeadersUtil.TRANSFER_ENCODING_HEADER));
        inputStream = ContentUtil.getInput(
                requestMessage.getMethod(), responseMessage.getStatusCode(),
                connection.getInput(),
                inputStateTracker(),
                inputTrailers.resolver(),
                null,
                encodings,
                contentLength);
        if (inputStream.isRestOfTheStream()) {
            canContinue = false;
        }
        return aValue(new HttpResponse(
                responseMessage.getStatusCode(),
                responseMessage.getStatusMessage(),
                responseMessage.getVersion(),
                new HttpHeaders(responseMessage.getHeaders()),
                inputStream.getStream(),
                null));
    }

    private Promise<HttpResponse> switchProtocol() {
        return aSeq(
                () -> outputStream.getStream().close()
        ).thenDoLast(() -> {
            if (outputState != OutputState.CLOSED) {
                throw new HttpException("Output stream must be closed before switching protocols: "
                        + outputState);
            }
            canContinue = false;
            outputState = null;
            inputStream = ContentUtil.getInput(
                    HttpMethodUtil.GET, HttpStatusUtil.OK, connection.getInput(),
                    inputStateTracker(), inputTrailers.resolver(), null,
                    Collections.emptyList(), null);
            outputStream = ContentUtil.getOutput(HttpMethodUtil.GET, HttpStatusUtil.OK,
                    connection.getOutput(),
                    outputTracker(),
                    constantSupplier(null),
                    null,
                    Collections.emptyList(), null);
            switchedProtocol = true;
            return aValue(new HttpResponse(
                    responseMessage.getStatusCode(),
                    responseMessage.getStatusMessage(),
                    responseMessage.getVersion(),
                    new HttpHeaders(responseMessage.getHeaders()),
                    null,
                    new SimpleChannel<>(inputStream.getStream(), outputStream.getStream()).export()));
        });
    }

    /**
     * @return the tracker for the input state.
     */
    private AResolver<InputState> inputStateTracker() {
        return resolution -> {
            if (resolution.isFailure()) {
                inputState = InputState.ERROR;
                invalidate(resolution.failure());
            } else {
                inputState = resolution.value();
            }
            if (switchedProtocol && (inputState == InputState.CLOSED
                    || inputState == InputState.CLOSED_BEFORE_EOF
                    || inputState == InputState.ERROR)) {
                connection.getInput().input().close();
            }
            checkAutoClose();
        };
    }

    /**
     * The invalidation callback.
     *
     * @param throwable the invalidation reason
     */
    @Override
    protected void onInvalidation(final Throwable throwable) {
        final AResolver<Void> resolver = scope.remove(HttpRequestUtil.CONTINUE_LISTENER);
        if (resolver != null) {
            notifyFailure(resolver, throwable);
        }
        checkAutoClose();
        super.onInvalidation(throwable);
    }

    /**
     * Notify continue request.
     */
    public void notifyContinue() {
        final AResolver<Void> resolver = scope.remove(HttpRequestUtil.CONTINUE_LISTENER);
        if (resolver != null) {
            notifySuccess(resolver, null);
        }
    }

    @Override
    public AHttpRequest export(Vat vat) {
        return AHttpRequestProxyFactory.createProxy(vat, this);
    }
}
