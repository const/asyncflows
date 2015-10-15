package net.sf.asyncobjects.protocol.http.server.core;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.annotations.Internal;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;

import java.nio.ByteBuffer;

/**
 * <p>The response interface for {@link HttpExchangeContext}.
 * It provides a set of functions that allow answering HTTP requests in the different ways.
 * Not all HTTP versions support all response options. Most of unsupported features are just
 * ignored or they cause a failure.</p>
 * <p>The instance is specific to a particular request.</p>
 */
@Internal
public interface AHttpResponse extends ACloseable {
    /**
     * Perform intermediate response with 10x code. This is a method that is invoked before
     * primary response. If primary response is already generated, the method does nothing.
     * The method is ignored and immediately returns on HTTP 2.0.
     *
     * @param status  the status code
     * @param reason  the status line
     * @param headers the header set
     * @return a promise for moment when the response is written
     */
    Promise<Void> intermediateResponse(int status, String reason, HttpHeaders headers);

    /**
     * Get response body according to provided message and content mode. The content mode
     * will adjust headers accordingly. The headers will be also adjusted by server, by adding
     * required fields and by applying content length and encodings. The transfer encodings are guessed
     * by server according to configuration options and the request.
     *
     * @param status  the status code
     * @param reason  the reason for status code (if null, the standard text is used),
     *                it is ignored for HTTP 2.0.
     * @param headers the http headers
     * @param length  the content length (null if it is not yet known)
     * @return a promise for output. The promise could return null, if the response type
     * is not supposed to have an output. For example, it could happen in the case of responding
     * HEAD requests, or when response is generated for 204 or 304 responses.
     */
    Promise<AOutput<ByteBuffer>> respond(int status, String reason, HttpHeaders headers, Long length);

    /**
     * Switch protocol with the specified headers. Note that this is used for both responding to connect request
     * and handling the upgrade requests. The CONNECT respond with 2xx response, and upgrade with 101. Note,
     * the input should be read completely, before getting access to the channel input.
     *
     * @param status  the status code
     * @param reason  the reason for status code (if null, the standard text is used),
     *                it is ignored for HTTP 2.0.
     * @param headers the http headers
     * @return a channel for the switched protocol
     */
    Promise<AChannel<ByteBuffer>> switchProtocol(int status, String reason, HttpHeaders headers);


    /**
     * The method {@link ACloseable#close()} aborts request and close connection in the case
     * of HTTP 1.1 or HTTP 1.0 if response has been started. If both input and output are closed,
     * the method has no effect.  If response has not yet been started, the method just
     * answers with 500 error.
     * <p>
     * {@inheritDoc}
     */
    Promise<Void> close();
}
