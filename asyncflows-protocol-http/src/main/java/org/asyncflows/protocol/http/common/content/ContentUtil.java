package org.asyncflows.protocol.http.common.content; // NOPMD

import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.io.util.DeflateOutput;
import org.asyncflows.io.util.GZipInput;
import org.asyncflows.io.util.GZipOutput;
import org.asyncflows.io.util.InflateInput;
import org.asyncflows.protocol.http.HttpException;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.HttpVersionUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.TransferEncoding;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.vats.Vats.defaultVat;

/**
 * The content utilities.
 */
public final class ContentUtil {
    /**
     * The chunked encoding.
     */
    public static final String CHUNKED_ENCODING = "chunked";
    /**
     * The deflate encoding.
     */
    public static final String DEFLATE_ENCODING = "deflate";
    /**
     * The gzip encoding.
     */
    public static final String GZIP_ENCODING = "gzip";
    /**
     * The gzip encoding.
     */
    public static final String X_GZIP_ENCODING = "x-gzip";

    /**
     * The private constructor for the utility class.
     */
    private ContentUtil() {
        // do nothing
    }

    /**
     * Get message input for the message (request and response). The key sections are:
     * <ul>
     * <li><a href="http://tools.ietf.org/search/rfc7230#section-3.3">RFC 7320: 3.3. Message Body</a></li>
     * <li><a href="http://tools.ietf.org/search/rfc7231#section-4.3.8">RFC 7321: 4.3.8. TRACE</a></li>
     * </ul>
     * Just not to be surprised: GET, HEAD, DELETE, OPTIONS could have input message bodies as by RFC 7321.
     *
     * @param method            the http method (always non-null)
     * @param statusCode        the status code (non-null for response and null for request)
     * @param output            the output
     * @param stateTracker      the state tracker
     * @param trailersProvider  the provider for trailers
     * @param listener          the listener for finish event or null if it should not be tracked (for the statistics)
     * @param transferEncodings the transfer encodings to apply
     * @param contentLength     the content length
     * @return the corresponding message input or null if there is no message body.
     * @throws HttpException                    if there is a generic validation problem
     * @throws UnknownTransferEncodingException if unknown encoding is detected
     */
    // CHECKSTYLE:OFF
    public static StreamInfo<AOutput<ByteBuffer>> getOutput(final String method, final Integer statusCode,
                                                            final ByteGeneratorContext output,
                                                            final AResolver<OutputState> stateTracker,
                                                            final ASupplier<HttpHeaders> trailersProvider,
                                                            final Consumer<StreamFinishedEvent> listener,
                                                            final List<TransferEncoding> transferEncodings,
                                                            final Long contentLength) {
        // CHECKSTYLE:ON
        final boolean isRequest = statusCode == null;
        if (!transferEncodings.isEmpty() && contentLength != null) {
            throw new HttpException("Both Transfer-Encoding and Content-Length specified");
        } else if (!isRequest && HttpMethodUtil.isHead(method)) {
            return new StreamInfo<>(
                    export(listener, new ContentLengthOutput(output, stateTracker, 0L)),
                    contentLength, false, transferEncodings);
        } else if (isNoContent(method, statusCode, isRequest, transferEncodings, contentLength)) {
            return new StreamInfo<>(
                    export(listener, new ContentLengthOutput(output, stateTracker, 0L)),
                    null, false, Collections.<TransferEncoding>emptyList());
        } else if (!transferEncodings.isEmpty()) {
            return createEncodedOutputStream(output, stateTracker, trailersProvider, listener,
                    isRequest, transferEncodings);
        } else if (contentLength != null) {
            return new StreamInfo<>(
                    export(listener, new ContentLengthOutput(output, stateTracker, contentLength)),
                    contentLength, false, Collections.<TransferEncoding>emptyList());
        } else {
            return new StreamInfo<>(
                    export(listener, new RestOfStreamOutput(output, stateTracker)),
                    null, true, Collections.<TransferEncoding>emptyList());
        }
    }

    /**
     * Export the stream and add counting if needed.
     *
     * @param listener the listener to add
     * @param stream   the stream
     * @return the exported stream
     */
    private static AOutput<ByteBuffer> export(final Consumer<StreamFinishedEvent> listener,
                                              final AOutput<ByteBuffer> stream) {
        return NIOExportUtil.export(defaultVat(), CountingOutput.countIfNeeded(stream, listener));
    }


    /**
     * Create encoded output.
     *
     * @param output            the output
     * @param stateTracker      the state tracker
     * @param trailersProvider  the provider for trailers
     * @param listener          the listener for finish event or null if it should not be tracked (for the statistics)
     * @param isRequest         true if it is a request stream
     * @param transferEncodings the list of encodings
     * @return the corresponding message input or null if there is no message body.
     * @throws HttpException                    if there is a generic validation problem
     * @throws UnknownTransferEncodingException if unknown encoding is detected
     */
    private static StreamInfo<AOutput<ByteBuffer>> createEncodedOutputStream(
            final ByteGeneratorContext output,
            final AResolver<OutputState> stateTracker,
            final ASupplier<HttpHeaders> trailersProvider,
            final Consumer<StreamFinishedEvent> listener,
            final boolean isRequest,
            final List<TransferEncoding> transferEncodings) {
        final TransferEncoding last = transferEncodings.get(transferEncodings.size() - 1);
        boolean restOfTheStream;
        AOutput<ByteBuffer> current;
        int i = transferEncodings.size() - 1;
        if (CHUNKED_ENCODING.equalsIgnoreCase(last.getName())) {
            ensureNoParameters(last);
            current = new ChunkedOutput(output, stateTracker, trailersProvider);
            restOfTheStream = false;
            i--;
        } else {
            if (isRequest) {
                throw new HttpException("The chunked encoding must be last for the request.");
            }
            current = new RestOfStreamOutput(output, stateTracker);
            restOfTheStream = true;
        }
        while (i > 0) {
            final TransferEncoding currentEncoding = transferEncodings.get(i--);
            final String encoding = currentEncoding.getName();
            if (CHUNKED_ENCODING.equalsIgnoreCase(encoding)) {
                throw new HttpException("The chunked encoding must happen only once.");
            } else if (GZIP_ENCODING.equalsIgnoreCase(encoding) || X_GZIP_ENCODING.equalsIgnoreCase(encoding)) {
                ensureNoParameters(currentEncoding);
                current = new GZipOutput(current, IOUtil.BYTE.writeBuffer(output.buffer().capacity()), null); // NOPMD
            } else if (DEFLATE_ENCODING.equalsIgnoreCase(encoding)) {
                ensureNoParameters(currentEncoding);
                current = new DeflateOutput(new Deflater(), current, // NOPMD
                        IOUtil.BYTE.writeBuffer(output.buffer().capacity()));
            } else {
                throw new UnknownTransferEncodingException("The unsupported encoding: " + currentEncoding);
            }
        }
        current = export(listener, current);
        return new StreamInfo<>(current, null, restOfTheStream, transferEncodings);
    }


    /**
     * Get message input for the message (request and response). The key sections are:
     * <ul>
     * <li><a href="http://tools.ietf.org/search/rfc7230#section-3.3">RFC 7320: 3.3. Message Body</a></li>
     * <li><a href="http://tools.ietf.org/search/rfc7231#section-4.3.8">RFC 7321: 4.3.8. TRACE</a></li>
     * </ul>
     * Just not to be surprised: GET, HEAD, DELETE, OPTIONS could have input message bodies as by RFC 7321.
     *
     * @param method            the http method (always non-null)
     * @param statusCode        the status code (non-null for response and null for request)
     * @param input             the input
     * @param stateTracker      the state tracker
     * @param trailersResolver  the resolver for trailers
     * @param listener          the listener for finish event or null if it should not be tracked (for the statistics)
     * @param transferEncodings the transfer encodings to apply
     * @param contentLength     the content length
     * @return the corresponding message input or null if there is no message body.
     * @throws HttpException                    if there is a generic validation problem
     * @throws UnknownTransferEncodingException if unknown encoding is detected
     */
    // CHECKSTYLE:OFF
    public static StreamInfo<AInput<ByteBuffer>> getInput(final String method, final Integer statusCode,
                                                          final ByteParserContext input,
                                                          final AResolver<InputState> stateTracker,
                                                          final AResolver<HttpHeaders> trailersResolver,
                                                          final Consumer<StreamFinishedEvent> listener,
                                                          final List<TransferEncoding> transferEncodings,
                                                          final Long contentLength) {
        // CHECKSTYLE:ON
        final boolean isRequest = statusCode == null;
        if (!transferEncodings.isEmpty() && contentLength != null) {
            throw new HttpException("Both Transfer-Encoding and Content-Length specified");
        } else if (!isRequest && HttpMethodUtil.isHead(method)) {
            trailersWouldNotHappen(trailersResolver);
            return new StreamInfo<>(
                    export(listener, new ContentLengthInput(stateTracker, input, 0L)),
                    contentLength, false, transferEncodings);
        } else if (isNoContent(method, statusCode, isRequest, transferEncodings, contentLength)) {
            trailersWouldNotHappen(trailersResolver);
            return new StreamInfo<>(
                    export(listener, new ContentLengthInput(stateTracker, input, 0L)),
                    null, false, Collections.<TransferEncoding>emptyList());
        } else if (!transferEncodings.isEmpty()) {
            return createEncodedInputStream(input, stateTracker, trailersResolver, listener,
                    isRequest, transferEncodings);
        } else if (contentLength != null) {
            trailersWouldNotHappen(trailersResolver);
            return new StreamInfo<>(
                    export(listener, new ContentLengthInput(stateTracker, input, contentLength)),
                    contentLength, false, Collections.<TransferEncoding>emptyList());
        } else {
            trailersWouldNotHappen(trailersResolver);
            return new StreamInfo<>(
                    export(listener, new RestOfStreamInput(input, stateTracker)),
                    null, true, Collections.<TransferEncoding>emptyList());
        }
    }

    /**
     * Indicate that trailers would not happen.
     *
     * @param trailersResolver the resolver for the trailers
     */
    private static void trailersWouldNotHappen(final AResolver<HttpHeaders> trailersResolver) {
        if (trailersResolver != null) {
            notifySuccess(trailersResolver, null);
        }
    }

    /**
     * Created encoded input (request and response).
     *
     * @param input             the input
     * @param stateTracker      the state tracker
     * @param trailersResolver  the resolver for trailers
     * @param listener          the listener for finish event or null if it should not be tracked (for the statistics)
     * @param isRequest         true if it is a request stream
     * @param transferEncodings the list of encodings
     * @return the corresponding message input or null if there is no message body.
     * @throws HttpException                    if there is a generic validation problem
     * @throws UnknownTransferEncodingException if unknown encoding is detected
     */
    private static StreamInfo<AInput<ByteBuffer>> createEncodedInputStream(
            final ByteParserContext input,
            final AResolver<InputState> stateTracker,
            final AResolver<HttpHeaders> trailersResolver,
            final Consumer<StreamFinishedEvent> listener,
            final boolean isRequest,
            final List<TransferEncoding> transferEncodings) {
        final TransferEncoding last = transferEncodings.get(transferEncodings.size() - 1);
        boolean restOfTheStream;
        AInput<ByteBuffer> current;
        int i = transferEncodings.size() - 1;
        if (CHUNKED_ENCODING.equalsIgnoreCase(last.getName())) {
            ensureNoParameters(last);
            current = new ChunkedInput(input, stateTracker, HttpLimits.MAX_HEADERS_SIZE, trailersResolver);
            restOfTheStream = false;
            i--;
        } else {
            if (isRequest) {
                throw new HttpException("The chunked encoding must be last for the request.");
            }
            current = new RestOfStreamInput(input, stateTracker);
            trailersWouldNotHappen(trailersResolver);
            restOfTheStream = true;
        }
        while (i > 0) {
            final TransferEncoding currentEncoding = transferEncodings.get(i--);
            final String encoding = currentEncoding.getName();
            if (CHUNKED_ENCODING.equalsIgnoreCase(encoding)) {
                throw new HttpException("The chunked encoding must happen only once.");
            } else if (GZIP_ENCODING.equalsIgnoreCase(encoding) || X_GZIP_ENCODING.equalsIgnoreCase(encoding)) {
                ensureNoParameters(currentEncoding);
                current = new GZipInput(current, IOUtil.BYTE.writeBuffer(input.buffer().capacity()), null); // NOPMD
            } else if (DEFLATE_ENCODING.equalsIgnoreCase(encoding)) {
                ensureNoParameters(currentEncoding);
                current = new InflateInput(new Inflater(), current, // NOPMD
                        IOUtil.BYTE.writeBuffer(input.buffer().capacity()));
            } else {
                throw new UnknownTransferEncodingException("The unsupported encoding: " + currentEncoding);
            }
        }
        current = export(listener, current);
        return new StreamInfo<>(current, null, restOfTheStream, transferEncodings);
    }

    /**
     * Export the stream and add counting if needed.
     *
     * @param listener the listener to add
     * @param stream   the stream
     * @return the exported stream
     */
    private static AInput<ByteBuffer> export(final Consumer<StreamFinishedEvent> listener,
                                             final AInput<ByteBuffer> stream) {
        return NIOExportUtil.export(defaultVat(), CountingInput.countIfNeeded(stream, listener));
    }


    /**
     * Check if message contains no content.
     *
     * @param method            the method
     * @param statusCode        the status code
     * @param isRequest         true if this is a request message
     * @param transferEncodings the list of transfer encodings
     * @param contentLength     the content length
     * @return true if there is no content
     */
    private static boolean isNoContent(final String method, final Integer statusCode,
                                       final boolean isRequest, final List<TransferEncoding> transferEncodings,
                                       final Long contentLength) {
        if (isRequest) {
            if (transferEncodings.isEmpty() && contentLength == null) {
                return true;
            }
            if ((contentLength == null || contentLength != 0L) && HttpMethodUtil.isTrace(method)) {
                throw new TraceMethodWithContentException("The TRACE method could not have content");
            }
        } else {
            // check no content statuses
            switch (statusCode) {
                case HttpStatusUtil.NO_CONTENT:
                case HttpStatusUtil.NOT_MODIFIED:
                    return true;
                default:
                    if (HttpStatusUtil.isInformational(statusCode)) {
                        return true;
                    }
                    if (HttpStatusUtil.isSuccess(statusCode) && HttpMethodUtil.isConnect(method)) {
                        return true;
                    }
            }
        }
        return false;
    }

    /**
     * Ensure that encoding does not have any parameters as per HTTP 1.1 specification.
     *
     * @param encoding the encoding
     */
    private static void ensureNoParameters(final TransferEncoding encoding) {
        if (!encoding.getParameters().isEmpty()) {
            throw new UnknownTransferEncodingException("The transfer encoding should not have parameters: " + encoding);
        }
    }

    /**
     * Get list transfer encodings basing on length and chunked.
     *
     * @param version the version
     * @param length  the length
     * @return the encoding list
     */
    public static List<TransferEncoding> getTransferEncodings(final String version, final Long length) {
        final List<TransferEncoding> encodings;
        if (length != null) {
            encodings = Collections.emptyList();
        } else {
            if (HttpVersionUtil.isHttp10(version)) {
                encodings = Collections.emptyList();
            } else {
                encodings = Collections.singletonList(new TransferEncoding(CHUNKED_ENCODING));
            }
        }
        return encodings;
    }


    /**
     * The information about detected stream.
     */
    public static class StreamInfo<S> {
        /**
         * The stream stream.
         */
        private final S stream;
        /**
         * The content length (if present and effective).
         */
        private final Long contentLength;
        /**
         * True if the stream lasts until end of the stream (no further messages are possible).
         */
        private final boolean restOfTheStream;
        /**
         * The list of encodings (empty if transfer encodings are not used).
         */
        private final List<TransferEncoding> encodingList;

        /**
         * The constructor.
         *
         * @param stream          the stream
         * @param contentLength   the content length (if present and effective)
         * @param restOfTheStream true if the stream lasts until end of the stream (no further messages are possible)
         * @param encodingList    the list of encodings (empty if the transfer encodings are not used)
         */
        public StreamInfo(final S stream, final Long contentLength, final boolean restOfTheStream,
                          final List<TransferEncoding> encodingList) {
            this.stream = stream;
            this.contentLength = contentLength;
            this.restOfTheStream = restOfTheStream;
            this.encodingList = encodingList;
        }

        /**
         * @return the stream
         */
        public S getStream() {
            return stream;
        }

        /**
         * @return the content length (if present and effective).
         */
        public Long getContentLength() {
            return contentLength;
        }

        /**
         * @return true if the stream lasts until end of the stream (no further messages are possible)
         */
        public boolean isRestOfTheStream() {
            return restOfTheStream;
        }

        /**
         * @return the list of encodings (empty if the transfer encodings are not used)
         */
        public List<TransferEncoding> getEncodingList() {
            return encodingList;
        }

        @Override
        public String toString() {
            return "StreamInfo{" + "stream=" + stream + ", contentLength=" + contentLength
                    + ", restOfTheStream=" + restOfTheStream + ", encodingList=" + encodingList + '}';
        }
    }
}
