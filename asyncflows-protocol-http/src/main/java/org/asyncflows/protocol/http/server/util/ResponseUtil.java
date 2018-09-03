package org.asyncflows.protocol.http.server.util;

import org.asyncflows.io.AInput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.CharIOUtil;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.core.Promise;

import java.nio.ByteBuffer;

import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;

/**
 * The utility class that contains helper methods for responding.
 */
public final class ResponseUtil {
    /**
     * The private constructor for utility class.
     */
    private ResponseUtil() {
        // do nothing
    }

    /**
     * Do a short reply with the specified html text, the text is converted to an array of bytes encoded as UTF-8.
     * Use this method only for the short replies, for which creating an array would not cause a problem.
     *
     * @param exchange the message exchange
     * @param code     the code to reply with
     * @param reason   the reason text (ignored for HTML 2.0)
     * @param text     the body text
     * @return when replied
     */
    public static Promise<Void> shortReply(final HttpExchange exchange, final int code, final String reason,
                                           final String text) {
        return shortReply(exchange, code, reason, text, new HttpHeaders());
    }

    /**
     * Do a short reply with the specified html text, the text is converted to an array of bytes encoded as UTF-8.
     * Use this method only for the short replies, for which creating an array would not cause a problem.
     *
     * @param exchange the message exchange
     * @param code     the code to reply with
     * @param reason   the reason text (ignored for HTML 2.0)
     * @param text     the body text
     * @param headers  the headers
     * @return when replied
     */
    public static Promise<Void> shortReply(final HttpExchange exchange, final int code, final String reason,
                                           final String text, final HttpHeaders headers) {
        final byte[] data = text.getBytes(CharIOUtil.UTF8);
        headers.setHeader(HttpHeadersUtil.CONTENT_TYPE_HEADER, HttpHeadersUtil.CONTENT_TYPE_HTML_UTF8);
        return aTry(exchange.respond(code, reason, headers, (long) data.length)).run(
                output -> output.write(ByteBuffer.wrap(data)));
    }

    /**
     * Do a short reply with the specified html text, the text is converted to an array of bytes encoded as UTF-8.
     * Use this method only for the short replies, for which creating an array would not cause a problem.
     *
     * @param exchange the message exchange
     * @param code     the code to reply with
     * @param text     the body text
     * @param headers  the headers
     * @return when replied
     */
    public static Promise<Void> shortReply(final HttpExchange exchange, final int code,
                                           final String text, final HttpHeaders headers) {
        return shortReply(exchange, code, null, text, headers);
    }

    /**
     * Redirect to the page (relative path).
     *
     * @param exchange the exchange context
     * @param path     the relative path
     * @return when redirect finishes.
     */
    public static Promise<Void> redirect(final HttpExchange exchange, final String path) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setHeader(HttpHeadersUtil.LOCATION_HEADER, exchange.getRequestUri().resolve(path).toString());
        return exchange.respond(HttpStatusUtil.TEMPORARY_REDIRECT, null, headers, 0L).toVoid();
    }

    /**
     * Discard input message content and close the stream.
     *
     * @param input the input
     * @return when finishes
     */
    public static Promise<Void> discardAndClose(final AInput<ByteBuffer> input) {
        return aSeq(
                () -> IOUtil.BYTE.discard(input, ByteBuffer.allocate(HttpLimits.DEFAULT_BUFFER_SIZE)).toVoid()
        ).finallyDo(
                input::close
        );
    }
}
