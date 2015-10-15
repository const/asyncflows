package net.sf.asyncobjects.protocol.http.common;

import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;

/**
 * Base type for the http messages.
 */
public class HttpMessageBase {
    /**
     * The content length.
     */
    protected Long contentLength;
    /**
     * The protocol for the message. It is different from the scheme,
     */
    private String protocol;
    /**
     * The server address for the connection.
     */
    private String serverAddress;
    /**
     * The request method.
     */
    private String version;
    /**
     * The headers.
     */
    private HttpHeaders headers;

    /**
     * @return the HTTP version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set version.
     *
     * @param version the version
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * @return the HTTP headers
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * Set headers.
     *
     * @param headers the headers
     */
    public void setHeaders(final HttpHeaders headers) {
        this.headers = headers;
    }

    /**
     * @return get the message protocol.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set message underlying protocol.
     *
     * @param protocol the message protocol.
     */
    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the server address for the connection.
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Set server address for the connection.
     *
     * @param serverAddress the server address
     */
    public void setServerAddress(final String serverAddress) {
        this.serverAddress = serverAddress;
    }
}
