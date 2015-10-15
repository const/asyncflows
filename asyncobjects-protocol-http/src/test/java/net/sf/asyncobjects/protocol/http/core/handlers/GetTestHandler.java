package net.sf.asyncobjects.protocol.http.core.handlers;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.util.ByteIOUtil;
import net.sf.asyncobjects.protocol.LineUtil;
import net.sf.asyncobjects.protocol.http.HttpStatusException;
import net.sf.asyncobjects.protocol.http.common.HttpLimits;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.HttpURIUtil;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;
import net.sf.asyncobjects.protocol.http.server.HttpHandlerBase;
import net.sf.asyncobjects.protocol.http.server.util.ResponseUtil;

import java.util.Map;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;

/**
 * The handler that produces the specified amount of the random data.
 */
public class GetTestHandler extends HttpHandlerBase {
    @Override
    public Promise<Void> handle(final HttpExchange exchange) { // NOPMD
        try {
            final Map<String, String> parameters = HttpURIUtil.getQueryParameters(exchange.getRequestUri());
            final long length;
            final String lengthText = parameters.get("length");
            if (LineUtil.isBlank(lengthText)) {
                return ResponseUtil.redirect(exchange, "/chargen.txt?length=1024");
            } else {
                try {
                    length = Long.parseLong(lengthText);
                } catch (NumberFormatException ex) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Bad length format", ex);
                }
                if (length < 0) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "length must be positive");
                }
            }
            final Integer chunkSize;
            final String chunkText = parameters.get("chunk");
            if (LineUtil.isBlank(chunkText)) {
                chunkSize = null;
            } else {
                try {
                    chunkSize = Integer.parseInt(chunkText);
                } catch (NumberFormatException ex) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Bad chunk size format", ex);
                }
                if (chunkSize < 0) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Chunk size must be positive");
                }
            }
            final HttpHeaders headers = new HttpHeaders();
            headers.setHeader(HttpHeadersUtil.CONTENT_TYPE_HEADER, HttpHeadersUtil.CONTENT_TYPE_TEXT_UTF8);
            return aTry(exchange.respond(HttpStatusUtil.OK, headers, chunkSize == null ? length : null)).run(
                    output -> {
                        int bufferSize;
                        if (chunkSize != null) {
                            bufferSize = chunkSize;
                        } else {
                            bufferSize = (int) Math.min(HttpLimits.DEFAULT_BUFFER_SIZE, length / 3 + 1);
                        }
                        return ByteIOUtil.charGen(output, length, bufferSize);
                    }
            );
        } catch (Exception ex) { // NOPMD
            return aFailure(ex);
        }
    }
}
