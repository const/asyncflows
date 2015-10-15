package net.sf.asyncobjects.protocol.http.client;

import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;

import java.nio.ByteBuffer;

/**
 * The response object that represents a response returned from the service.
 */
public class HttpResponse {
    /**
     * The status code.
     */
    private final int statusCode;
    /**
     * The status reason string.
     */
    private final String reason;
    /**
     * The HTTP version.
     */
    private final String version;
    /**
     * The HTTP headers.
     */
    private final HttpHeaders headers;
    /**
     * The response stream (might be null if switched protocol).
     */
    private final AInput<ByteBuffer> responseStream;
    /**
     * The channel in case if stream switched protocol, otherwise null.
     */
    private final AChannel<ByteBuffer> switchedChannel;


    /**
     * The constructor.
     *
     * @param statusCode      the status code
     * @param reason          the status reason
     * @param version         the response version
     * @param headers         the response headers
     * @param responseStream  the response stream
     * @param switchedChannel the response channel in the case of
     */
    public HttpResponse(final int statusCode, final String reason, final String version, final HttpHeaders headers,
                        final AInput<ByteBuffer> responseStream, final AChannel<ByteBuffer> switchedChannel) {
        this.statusCode = statusCode;
        this.reason = reason;
        this.version = version;
        this.headers = headers;
        this.responseStream = responseStream;
        this.switchedChannel = switchedChannel;
    }

    /**
     * @return http status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return http status reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return http status version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return http version
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * @return http response stream (if protocol switched, null)
     */
    public AInput<ByteBuffer> getResponseStream() {
        return responseStream;
    }

    /**
     * @return the switched channel
     */
    public AChannel<ByteBuffer> getSwitchedChannel() {
        return switchedChannel;
    }
}
