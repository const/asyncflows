package net.sf.asyncobjects.protocol.http.common;

import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;

import java.net.URI;

/**
 * The HTTP request message. It is used to accumulate information about the HTTP request on client or server side.
 */
public class HttpRequestMessage extends HttpMessageBase {
    /**
     * The request target.
     */
    private String requestTarget;
    /**
     * The request method.
     */
    private String method;
    /**
     * The effective URL of the request.
     */
    private URI effectiveUri;

    /**
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set HTTP method.
     *
     * @param method the method
     */
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * @return the request target (as it is specified in the request)
     */
    public String getRequestTarget() {
        return requestTarget;
    }

    /**
     * Set the request target (as it is specified in the request).
     *
     * @param requestTarget the new value
     */
    public void setRequestTarget(final String requestTarget) {
        this.requestTarget = requestTarget;
    }

    /**
     * @return get the effective URL.
     */
    public URI getEffectiveUri() {
        return effectiveUri;
    }

    /**
     * Set the effective URL.
     *
     * @param effectiveUri the effective URL.
     */
    public void setEffectiveUri(final URI effectiveUri) {
        this.effectiveUri = effectiveUri;
    }

    /**
     * @return the content length (or null if undefined)
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * Set the content length.
     *
     * @param contentLength the content length
     */
    public void setContentLength(final Long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return true, if continuation is expected.
     */
    public boolean expectsContinue() {
        if (HttpVersionUtil.isHttp11(getVersion())) {
            for (final String expect : getHeaders().getHeaders(HttpHeadersUtil.EXPECT_HEADER)) {
                if (expect.equalsIgnoreCase(HttpHeadersUtil.EXPECT_CONTINUE)) {
                    return true;
                }
            }
        }
        return false;
    }


}
