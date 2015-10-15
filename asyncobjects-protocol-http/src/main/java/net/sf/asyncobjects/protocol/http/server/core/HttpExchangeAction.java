package net.sf.asyncobjects.protocol.http.server.core; // NOPMD

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.CoreExportUtil;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.AConsumer;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.ReflectionExporter;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.util.UtilExportUtil;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.util.SimpleChannel;
import net.sf.asyncobjects.protocol.http.HttpException;
import net.sf.asyncobjects.protocol.http.HttpStatusException;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.HttpRequestMessage;
import net.sf.asyncobjects.protocol.http.common.HttpResponseMessage;
import net.sf.asyncobjects.protocol.http.common.HttpScopeUtil;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.HttpURIUtil;
import net.sf.asyncobjects.protocol.http.common.HttpVersionUtil;
import net.sf.asyncobjects.protocol.http.common.Scope;
import net.sf.asyncobjects.protocol.http.common.content.ContentUtil;
import net.sf.asyncobjects.protocol.http.common.content.InputState;
import net.sf.asyncobjects.protocol.http.common.content.OutputState;
import net.sf.asyncobjects.protocol.http.common.content.StreamFinishedEvent;
import net.sf.asyncobjects.protocol.http.common.content.UnknownTransferEncodingException;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.common.headers.TransferEncoding;
import net.sf.asyncobjects.protocol.http.server.AHttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResourceAction;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;

/**
 * The handler for single HTTP exchange of the connection.
 * TODO handle TRACE (tricky)
 * TODO validate and filter headers (response)
 * TODO localhost checks for connect (only localhost could connect to localhost)
 */
class HttpExchangeAction extends CloseableInvalidatingBase
        implements AHttpResponse, ExportsSelf<AHttpResponse> {
    /**
     * Fake event indicating that stream was not created.
     */
    public static final Callable<StreamFinishedEvent> NOT_CREATED = () -> {
        //CHECKSTYLE:OFF
        final long time = System.currentTimeMillis();
        //CHECKSTYLE:ON
        return new StreamFinishedEvent(time, time, 0, null);
    };
    /**
     * Key for statistics.
     */
    private static final Scope.Key<StreamFinishedEvent> CLIENT_TO_SERVER
            = new Scope.Key<>(HttpExchangeAction.class, "clientToServer", NOT_CREATED);
    /**
     * Key for statistics.
     */
    private static final Scope.Key<StreamFinishedEvent> SERVER_TO_CLIENT
            = new Scope.Key<>(HttpExchangeAction.class, "serverToClient", NOT_CREATED);
    /**
     * Key for statistics.
     */
    private static final Scope.Key<StreamFinishedEvent> CLIENT_TO_SERVER_SWITCHED
            = new Scope.Key<>(HttpExchangeAction.class, "clientToServerSwitched");
    /**
     * Key for statistics.
     */
    private static final Scope.Key<StreamFinishedEvent> SERVER_TO_CLIENT_SWITCHED
            = new Scope.Key<>(HttpExchangeAction.class, "serverToClientSwitched");
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpExchangeAction.class);
    /**
     * The server connection.
     */
    private final HttpServerConnection connection;
    /**
     * The exchange id.
     */
    private final long exchangeId;
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
     * Responses.
     */
    private final RequestQueue responses = new RequestQueue();
    /**
     * Check if can continue processing messages after handling this exchange.
     */
    private boolean canContinue = true;
    /**
     * The state of input.
     */
    private InputState inputState;
    /**
     * The state of output.
     */
    private OutputState outputState;
    /**
     * True if the response message has started to be written.
     */
    private boolean responseStarted;
    /**
     * The input for the handler.
     */
    private ContentUtil.StreamInfo<AInput<ByteBuffer>> inputInfo;
    /**
     * The output for the handler.
     */
    private ContentUtil.StreamInfo<AOutput<ByteBuffer>> outputInfo;
    /**
     * Exchange context.
     */
    private HttpExchangeContext exchangeContext;
    /**
     * True if switched protocol.
     */
    private boolean switchedProtocol;


    /**
     * The constructor.
     *
     * @param connection the connection
     * @param exchangeId the exchange id within connection
     */
    public HttpExchangeAction(final HttpServerConnection connection, final long exchangeId) {
        this.connection = connection;
        this.exchangeId = exchangeId;
        final String address = HttpURIUtil.getHost(connection.getLocalAddress());
        requestMessage.setProtocol(connection.getProtocol());
        requestMessage.setServerAddress(address);
        responseMessage.setProtocol(connection.getProtocol());
        responseMessage.setServerAddress(address);
    }

    /**
     * Handle a single request/response.
     *
     * @return resolves to true if the next request response could be handled on this this connection.
     */
    public Promise<Boolean> handle() {
        return aSeq(
                () -> HttpServerMessageUtil.parseRequestMessage(connection.getInput(), requestMessage)
        ).failed(value -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse request " + id() + ": " + requestMessage.getMethod()
                        + " " + requestMessage.getRequestTarget() + " " + requestMessage.getVersion()
                        + "\n" + requestMessage.getHeaders().toString(), value);
            }
            return aFailure(value);
        }).map(
                hasMessage -> hasMessage ? processRequest() : aFalse()
        ).failed(exception -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to process request " + id(), exception);
            }
            // if handler failed to respond properly, the exception is thrown to indicate bad response.
            if (!responseStarted) {
                if (exchangeContext == null) {
                    exchangeContext = createContext(null);
                }
                // TODO old service could be still alive (revoke proxy?)
                //noinspection ThrowableResultOfMethodCallIgnored
                exchangeContext.getExchangeScope().set(HttpServer.BAD_REQUEST_PROBLEM, exception);
                canContinue = false;
                return connection.getServer().getBadRequestHandler().handle(exchangeContext)
                        .thenValue(false);
            } else {
                return aFalse();
            }
        }).finallyDo(closeResourceAction(this));
    }

    /**
     * @return the id
     */
    private String id() {
        return connection.getConnectionId() + "." + exchangeId;
    }

    /**
     * Process received request.
     *
     * @return true if can continue.
     */
    private Promise<Boolean> processRequest() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting processing " + id() + ": " + requestMessage.getMethod()
                    + " " + requestMessage.getRequestTarget()
                    + " " + requestMessage.getVersion() + "\n" + requestMessage.getHeaders().toString());
        }
        responseMessage.setVersion(requestMessage.getVersion());
        canContinue = canContinue
                && HttpHeadersUtil.isLastExchange(requestMessage.getVersion(), requestMessage.getHeaders());
        final List<TransferEncoding> transferEncodings = TransferEncoding.parse(
                requestMessage.getHeaders().getHeaders(HttpHeadersUtil.TRANSFER_ENCODING_HEADER));
        final Long contentLength = HttpHeadersUtil.getContentLength(requestMessage.getHeaders());
        try {
            inputInfo = ContentUtil.getInput(
                    requestMessage.getMethod(), null, connection.getInput(),
                    inputStateTracker(), inputTrailers.resolver(), countTo(CLIENT_TO_SERVER),
                    transferEncodings, contentLength);
        } catch (UnknownTransferEncodingException ex) {
            throw new HttpStatusException(HttpStatusUtil.NOT_IMPLEMENTED, ex.getMessage(), ex);
        } catch (HttpException ex) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, ex.getMessage(), ex);
        }
        canContinue = canContinue && !inputInfo.isRestOfTheStream();
        requestMessage.setContentLength(inputInfo.getContentLength());
        final AInput<ByteBuffer> stream = inputInfo.getStream();
        final AHttpHandler handler = connection.getServer().getHandler();
        exchangeContext = createContext(stream);
        return handler.handle(exchangeContext).thenDo(() -> {
            if (outputInfo == null) {
                throw new HttpStatusException(HttpStatusUtil.INTERNAL_SERVER_ERROR,
                        "Handler did not started reply" + id());
            }
            return close().thenDo(() -> {
                if (inputState != InputState.CLOSED || outputState != OutputState.CLOSED) {
                    canContinue = false;
                }
                return aValue(canContinue);
            });
        });
    }

    /**
     * Create counting listener.
     *
     * @param key the listener key
     * @return the listener
     */
    private AConsumer<StreamFinishedEvent> countTo(final Scope.Key<StreamFinishedEvent> key) {
        return UtilExportUtil.export(new AConsumer<StreamFinishedEvent>() {
            @Override
            public void accept(final StreamFinishedEvent event) {
                if (exchangeContext != null) {
                    exchangeContext.getExchangeScope().set(key, event);
                }
            }
        });
    }

    /**
     * Create wrapper for the stream.
     *
     * @param stream the stream to use
     * @return the context object
     */
    private HttpExchangeContext createContext(final AInput<ByteBuffer> stream) {
        final HttpExchangeContext context = new HttpExchangeContext(connection.getServer().getServerScope(),
                requestMessage.getMethod(),
                requestMessage.getHeaders(),
                stream,
                CoreExportUtil.export(promiseCallable(inputTrailers)),
                export(),
                requestMessage.getEffectiveUri(),
                connection.getLocalAddress(),
                connection.getRemoteAddress(),
                requestMessage.getContentLength());
        if (connection.getSslSession() != null) {
            context.getExchangeScope().set(HttpScopeUtil.SSL_SESSION, connection.getSslSession());
        }
        return context;
    }

    /**
     * @return create tracker for the input state.
     */
    private AResolver<InputState> inputStateTracker() {
        return resolution -> {
            if (resolution.isFailure()) {
                inputState = InputState.ERROR;
                invalidate(resolution.failure());
            } else {
                if (inputState == InputState.IDLE && requestMessage.expectsContinue()) {
                    intermediateResponse(HttpStatusUtil.CONTINUE, null, new HttpHeaders()).listen(outcomeChecker());
                }
                inputState = resolution.value();
            }
            if (switchedProtocol && (inputState == InputState.CLOSED
                    || inputState == InputState.CLOSED_BEFORE_EOF
                    || inputState == InputState.ERROR)) {
                connection.getInput().input().close();
            }
        };
    }

    @Override
    public Promise<Void> intermediateResponse(final int status, final String reason, final HttpHeaders headers) {
        if (!HttpStatusUtil.isInformational(status)) {
            throw new IllegalArgumentException("Only 1xx statues are allowed " + id());
        }
        if (HttpStatusUtil.SWITCHING_PROTOCOLS == status) {
            throw new HttpException(
                    "Switch protocol status should be used only with switchProtocol(...) method " + id());
        }
        return responses.run(() -> {
            if (responseStarted) {
                return aVoid();
            } else {
                // Intermediate responses never have a content.
                final HttpResponseMessage intermediate = new HttpResponseMessage();
                initResponseMessage(intermediate, status, reason, headers);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Intermediate response started " + id() + ": "
                            + intermediate.getVersion()
                            + intermediate.getStatusCode()
                            + " " + intermediate.getStatusMessage()
                            + "\n" + intermediate.getHeaders());
                }
                return HttpServerMessageUtil.writeResponseMessage(connection.getOutput(), intermediate);
            }
        });
    }

    /**
     * Initialize response message with the specified properties. For version processing see
     * (<a href="http://tools.ietf.org/search/rfc7230#section-2.6">RFC 7230</a>).
     *
     * @param message the message
     * @param status  the status
     * @param reason  the reason for status code (if null, the standard text is used), it is ignored for HTTP 2.0.
     * @param headers the message headers
     */
    private void initResponseMessage(final HttpResponseMessage message,
                                     final int status, final String reason,
                                     final HttpHeaders headers) {
        message.setProtocol(requestMessage.getProtocol());
        String requestVersion = requestMessage.getVersion();
        // TODO check on the server if the version should be actually 1.0 depending on some condition from the client.
        if (HttpVersionUtil.isHttp10(requestVersion) || HttpVersionUtil.isHttp11(requestVersion)) {
            // Set HTTP/1.1 for responses according to RFC 7230.
            requestVersion = HttpVersionUtil.HTTP_VERSION_1_1;
        } else {
            // TODO support HTTP/2.0
            throw new HttpException("Unsupported response version");
        }
        message.setVersion(requestVersion);
        message.setServerAddress(requestMessage.getServerAddress());
        message.setHeaders(headers);
        message.setStatusCode(status);
        message.setStatusMessage(HttpStatusUtil.getText(message.getStatusCode(), reason));
        enrichReplyHeaders(message);
    }

    /**
     * Enrich message with server-specific headers. The method also removes all hop-to-hop headers.
     *
     * @param message the message
     */
    private void enrichReplyHeaders(final HttpResponseMessage message) {
        final HttpHeaders headers = message.getHeaders();
        // todo remove only some connection values
        // headers.removeHeader(HttpHeadersUtil.CONNECTION_HEADER);
        headers.removeHeader(HttpHeadersUtil.CONTENT_LENGTH_HEADER);
        // todo trailers indication?
        headers.removeHeader(HttpHeadersUtil.TRANSFER_ENCODING_HEADER);
        headers.setHeader(HttpHeadersUtil.SERVER_HEADER, connection.getServer().getServerDescription());
        headers.setHeader(HttpHeadersUtil.DATE_HEADER, HttpHeadersUtil.formatDate(new Date()));
    }

    @Override
    public Promise<AOutput<ByteBuffer>> respond(final int status, final String reason,
                                                final HttpHeaders headers, final Long length) {
        return responses.run(() -> {
            if (HttpStatusUtil.isSwitchProtocol(requestMessage.getMethod(), status)) {
                throw new HttpException("User switchProtocol(...) for switching protocol");
            }
            if (HttpStatusUtil.isInformational(status)) {
                throw new HttpException("Use intermediateResponse(...) for informational responses");
            }
            ensureResponseNotStarted();
            initResponseMessage(responseMessage, status, reason, headers);
            // TODO TE (gzip, deflate) encode response if asks
            final List<TransferEncoding> encodings = ContentUtil.getTransferEncodings(
                    responseMessage.getVersion(), length);
            outputInfo = ContentUtil.getOutput(requestMessage.getMethod(), status, connection.getOutput(),
                    outputTracker(),
                    trailersProvider(encodings), countTo(SERVER_TO_CLIENT), encodings, length);
            if (outputInfo.isRestOfTheStream()) {
                canContinue = false;
            }
            HttpHeadersUtil.setMessageBodyHeaders(headers,
                    outputInfo.getEncodingList(), outputInfo.getContentLength());
            responseStarted = true;
            canContinue &= !exchangeContext.getExchangeScope().get(HttpScopeUtil.LAST_EXCHANGE);
            HttpHeadersUtil.setLastMessageHeader(headers, responseMessage.getVersion(), !canContinue);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Response started " + id() + ": "
                        + responseMessage.getVersion()
                        + " " + responseMessage.getStatusCode()
                        + " " + responseMessage.getStatusMessage()
                        + "\n" + responseMessage.getHeaders());
            }
            return HttpServerMessageUtil.writeResponseMessage(connection.getOutput(), responseMessage).thenDo(
                    () -> aValue(outputInfo.getStream())
            );
        });
    }

    /**
     * Get provider for the trailers.
     *
     * @param encodings encodings
     * @return the provider for trailers
     */
    private ACallable<HttpHeaders> trailersProvider(final List<TransferEncoding> encodings) {

        final List<TransferEncoding> te = TransferEncoding.parse(
                requestMessage.getHeaders().getHeaders(HttpHeadersUtil.TE_HEADER));
        boolean acceptsTrailers = false;
        for (final TransferEncoding encoding : te) {
            if (HttpHeadersUtil.TE_TRAILERS_VALUE.equalsIgnoreCase(encoding.getName())) {
                acceptsTrailers = true;
                break;
            }
        }
        if (!acceptsTrailers) {
            return constantCallable(null);
        }
        final ACallable<HttpHeaders> provider = HttpScopeUtil.trailersProvider(exchangeContext.getExchangeScope(),
                responseMessage.getHeaders(), encodings);
        return CoreExportUtil.export(() -> {
            return provider.call().observe(resolution -> {
                if (resolution.isFailure()) {
                    return;
                }
                if (LOG.isDebugEnabled()) {
                    final HttpHeaders trailers = resolution.value();
                    final List<String> declared = responseMessage.getHeaders().getCommaSeparatedValues(
                            HttpHeadersUtil.TRAILER_HEADER);
                    final Set<String> actual = new LinkedHashSet<>(trailers.getNames());
                    for (final String h : declared) {
                        actual.remove(HttpHeadersUtil.normalizeName(h));
                    }
                    if (!actual.isEmpty()) {
                        LOG.debug("Undeclared trailers headers on exchange " + id() + ": " + actual);
                    }
                }
            });
        });
    }

    /**
     * Ensure that response is not started yet.
     *
     * @throws HttpException if response started
     */
    private void ensureResponseNotStarted() {
        if (responseStarted) {
            throw new HttpException("The response has been already started on this request. " + id());
        }
    }

    /**
     * @return the tracker for the output stream
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
        };
    }

    @Override
    public Promise<AChannel<ByteBuffer>> switchProtocol(final int status, final String reason, // NOPMD
                                                        final HttpHeaders headers) {
        return responses.run(() -> {
            if (HttpStatusUtil.SWITCHING_PROTOCOLS != status
                    && !(HttpMethodUtil.isConnect(requestMessage.getMethod())
                    && HttpStatusUtil.isSuccess(status))) {
                throw new HttpException("Not a valid request/response for switching protocols: "
                        + requestMessage.getMethod() + " -> " + status);
            }
            if (inputState != InputState.EOF_NO_TRAILERS
                    && inputState != InputState.CLOSED
                    && inputState != InputState.TRAILERS_READ) {
                throw new HttpException("Read all input before switching protocols : " + inputState);
            }
            ensureResponseNotStarted();
            responseStarted = true;
            canContinue = false;
            initResponseMessage(responseMessage, status, reason, headers);
            return aSeq(() -> {
                final AInput<ByteBuffer> stream = inputInfo.getStream();
                final boolean closed = inputState == InputState.CLOSED;
                inputInfo = null;
                inputState = null;
                return closed ? aVoid() : stream.close();
            }).thenDo(() -> {
                if (exchangeContext.getExchangeScope().get(HttpServer.SWITCH_NO_REPLY)) {
                    // This is for the case of switching to HTTP 2.0.
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Switching protocols " + id() + " without reply message");
                    }
                    return aVoid();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Switching protocols " + id() + ": "
                                + responseMessage.getVersion()
                                + " " + responseMessage.getStatusCode()
                                + " " + responseMessage.getStatusMessage()
                                + "\n" + responseMessage.getHeaders());
                    }
                    return HttpServerMessageUtil.writeResponseMessage(connection.getOutput(), responseMessage);
                }
            }).thenDoLast(() -> {
                // there is a minor hack here since it imitates rest of the stream response for the get request.
                // It does what is needed. Possibly more optimized version will be used later.
                inputInfo = ContentUtil.getInput(
                        HttpMethodUtil.GET, HttpStatusUtil.OK, connection.getInput(),
                        inputStateTracker(), null, countTo(CLIENT_TO_SERVER_SWITCHED),
                        Collections.<TransferEncoding>emptyList(), null);
                outputInfo = ContentUtil.getOutput(HttpMethodUtil.GET, HttpStatusUtil.OK,
                        connection.getOutput(),
                        outputTracker(),
                        constantCallable((HttpHeaders) null),
                        countTo(SERVER_TO_CLIENT_SWITCHED),
                        Collections.<TransferEncoding>emptyList(), null);
                switchedProtocol = true;
                return aValue(new SimpleChannel<>(
                        inputInfo.getStream(), outputInfo.getStream()).export());
            });
        });
    }


    @Override
    public Promise<Void> closeAction() {
        return aSeq(
                () -> aAll(() -> {
                    if (inputInfo != null) {
                        return inputInfo.getStream().close();
                    } else {
                        return aVoid();
                    }
                }).andLast(() -> {
                    if (outputInfo != null) {
                        return outputInfo.getStream().close();
                    } else if (requestMessage.getMethod() != null) {
                        throw new HttpStatusException(HttpStatusUtil.INTERNAL_SERVER_ERROR,
                                "Handler did not started reply");
                    } else {
                        return aVoid();
                    }
                }).toVoid()
        ).thenDo(() -> {
            if (inputState != InputState.CLOSED || outputState != OutputState.CLOSED) {
                canContinue = false;
            }
            return aVoid();
        }).finallyDo(() -> {
            if (exchangeContext != null) {
                final ExchangeFinishedEvent event = new ExchangeFinishedEvent(
                        id(), exchangeContext.getRemoteAddress(),
                        exchangeContext.getLocalAddress(),
                        requestMessage.getMethod(),
                        requestMessage.getEffectiveUri(),
                        requestMessage.getVersion(),
                        exchangeContext.getExchangeScope().get(CLIENT_TO_SERVER),
                        exchangeContext.getExchangeScope().get(CLIENT_TO_SERVER_SWITCHED),
                        responseMessage.getStatusCode(),
                        responseMessage.getStatusMessage(),
                        exchangeContext.getExchangeScope().get(SERVER_TO_CLIENT),
                        exchangeContext.getExchangeScope().get(SERVER_TO_CLIENT_SWITCHED),
                        exchangeContext.getExchangeScope().get(ExchangeFinishedEvent.REMOTE),
                        exchangeContext.getExchangeScope().get(ExchangeFinishedEvent.SERVER_TO_REMOTE),
                        exchangeContext.getExchangeScope().get(ExchangeFinishedEvent.REMOTE_TO_SERVER));
                connection.getServer().fireExchangeFinished(event);
            }
            return aVoid();
        });
    }

    @Override
    public AHttpResponse export() {
        return export(Vat.current());
    }

    @Override
    public AHttpResponse export(final Vat vat) {
        return ReflectionExporter.export(vat, this);
    }
}
