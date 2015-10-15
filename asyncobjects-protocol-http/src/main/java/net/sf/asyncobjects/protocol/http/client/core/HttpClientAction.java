package net.sf.asyncobjects.protocol.http.client.core; //NOPMD

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.ReflectionExporter;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.util.SimpleChannel;
import net.sf.asyncobjects.protocol.http.HttpException;
import net.sf.asyncobjects.protocol.http.client.AHttpRequest;
import net.sf.asyncobjects.protocol.http.client.HttpResponse;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.HttpRequestMessage;
import net.sf.asyncobjects.protocol.http.common.HttpResponseMessage;
import net.sf.asyncobjects.protocol.http.common.HttpRuntimeUtil;
import net.sf.asyncobjects.protocol.http.common.HttpScopeUtil;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.HttpVersionUtil;
import net.sf.asyncobjects.protocol.http.common.Scope;
import net.sf.asyncobjects.protocol.http.common.content.ContentUtil;
import net.sf.asyncobjects.protocol.http.common.content.InputState;
import net.sf.asyncobjects.protocol.http.common.content.OutputState;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.common.headers.TransferEncoding;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * The client action. Note that the action does not supports pipelining directly.
 */
public class HttpClientAction extends CloseableInvalidatingBase implements AHttpRequest, ExportsSelf<AHttpRequest> {
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
            if (requestScope.get(HttpScopeUtil.LAST_EXCHANGE)) {
                canContinue = false;
            }
            HttpClientMessageUtil.inferRequestTarget(requestMessage, connection.getHost());
            final List<TransferEncoding> encodings;
            final Long contentLength;
            if (length != null && length == NO_CONTENT) {
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
                    () -> aValue(outputStream.getStream())).observe(outcomeChecker()
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
            if (scope.get(AHttpRequest.CONTINUE_LISTENER) != null) {
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
    private ACallable<HttpHeaders> trailersProvider(final List<TransferEncoding> encodings) {
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
            return aSeqLoop(
                    () -> HttpClientMessageUtil.readResponseMessage(connection.getInput(), responseMessage).thenDo(
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
        }).thenDo(() -> {
            if (HttpStatusUtil.isSwitchProtocol(requestMessage.getMethod(), responseMessage.getStatusCode())) {
                return outputStream.getStream().close();
            } else {
                return aVoid();
            }
        }).thenDo(() -> {
            if (HttpStatusUtil.isSwitchProtocol(requestMessage.getMethod(), responseMessage.getStatusCode())) {
                if (outputState != OutputState.CLOSED) {
                    throw new HttpException("Output stream must be closed before switching protocols: "
                            + outputState);
                }
                canContinue = false;
                outputState = null;
                inputStream = ContentUtil.getInput(
                        HttpMethodUtil.GET, HttpStatusUtil.OK, connection.getInput(),
                        inputStateTracker(), inputTrailers.resolver(), null,
                        Collections.<TransferEncoding>emptyList(), null);
                outputStream = ContentUtil.getOutput(HttpMethodUtil.GET, HttpStatusUtil.OK,
                        connection.getOutput(),
                        outputTracker(),
                        constantCallable((HttpHeaders) null),
                        null,
                        Collections.<TransferEncoding>emptyList(), null);
                switchedProtocol = true;
                return aValue(new HttpResponse(
                        responseMessage.getStatusCode(),
                        responseMessage.getStatusMessage(),
                        responseMessage.getVersion(),
                        new HttpHeaders(responseMessage.getHeaders()),
                        null,
                        new SimpleChannel<>(inputStream.getStream(), outputStream.getStream()).export()));
            } else {
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
        }).failedLast(HttpRuntimeUtil.<HttpResponse>toHttpException()).observe(outcomeChecker());
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
        final AResolver<Void> resolver = scope.remove(AHttpRequest.CONTINUE_LISTENER);
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
        final AResolver<Void> resolver = scope.remove(AHttpRequest.CONTINUE_LISTENER);
        if (resolver != null) {
            notifySuccess(resolver, null);
        }
    }

    @Override
    public AHttpRequest export() {
        return export(Vat.current());
    }

    @Override
    public AHttpRequest export(final Vat vat) {
        return ReflectionExporter.export(vat, this);
    }
}
