/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

package org.asyncflows.protocol.http.common;

/**
 * Status utility class.
 */
public final class HttpStatusUtil {
    /**
     * The minimal status code.
     */
    public static final int MIN_STATUS_CODE = 100;
    /**
     * The maximum status code.
     */
    public static final int MAX_STATUS_CODE = 599;
    /**
     * RFC2616 HTTP Status Code Section 10.1.1: Continue (100).
     */
    public static final int CONTINUE = 100;
    /**
     * RFC2616 HTTP Status Code Section 10.1.2: Switching Protocols (101).
     */
    public static final int SWITCHING_PROTOCOLS = 101;
    /**
     * The processing code from WebDav (RFC 2518).
     */
    public static final int WEBDAV_PROCESSING = 102;
    /**
     * RFC2616 HTTP Status Code Section 10.2.1: OK (200).
     */
    public static final int OK = 200;
    /**
     * RFC2616 HTTP Status Code Section 10.2.2: Created (201).
     */
    public static final int CREATED = 201;
    /**
     * RFC2616 HTTP Status Code Section 10.2.3: Accepted (202).
     */
    public static final int ACCEPTED = 202;
    /**
     * RFC2616 HTTP Status Code Section 10.2.4: Non-Authoritative Information (203).
     */
    public static final int NON_AUTHORITATIVE_INFORMATION = 203;
    /**
     * RFC2616 HTTP Status Code Section 10.2.5: No Content (204).
     */
    public static final int NO_CONTENT = 204;
    /**
     * RFC2616 HTTP Status Code Section 10.2.6: Reset Content (205).
     */
    public static final int RESET_CONTENT = 205;
    /**
     * RFC2616 HTTP Status Code Section 10.2.7: Partial Content (206).
     */
    public static final int PARTIAL_CONTENT = 206;
    /**
     * Multi-Status (WebDAV; RFC 4918).
     */
    public static final int WEBDAV_MULTI_STATUS = 207;
    /**
     * Already Reported (WebDAV; RFC 5842).
     */
    public static final int WEBDAV_ALREADY_REPORTED = 208;
    /**
     * IM Used (RFC 3229).
     */
    public static final int IM_USED = 226;
    /**
     * RFC2616 HTTP Status Code Section 10.3.1: Multiple Choices (300).
     */
    public static final int MULTIPLE_CHOICES = 300;
    /**
     * RFC2616 HTTP Status Code Section 10.3.2: Moved Permanently (301).
     */
    public static final int MOVED_PERMANENTLY = 301;
    /**
     * RFC2616 HTTP Status Code Section 10.3.3: Found (302).
     */
    public static final int FOUND = 302;
    /**
     * RFC2616 HTTP Status Code Section 10.3.4: See Other (303).
     */
    public static final int SEE_OTHER = 303;
    /**
     * RFC2616 HTTP Status Code Section 10.3.5: Not Modified (304).
     */
    public static final int NOT_MODIFIED = 304;
    /**
     * RFC2616 HTTP Status Code Section 10.3.6: Use Proxy (305).
     */
    public static final int USE_PROXY = 305;
    /**
     * RFC2616 HTTP Status Code Section 10.3.8: Temporary Redirect (307).
     */
    public static final int TEMPORARY_REDIRECT = 307;
    /**
     * 308 Permanent Redirect (RFC 7238).
     */
    public static final int PERMANENT_REDIRECT = 308;
    /**
     * RFC2616 HTTP Status Code Section 10.4.1: Bad Request (400).
     */
    public static final int BAD_REQUEST = 400;
    /**
     * RFC2616 HTTP Status Code Section 10.4.2: Unauthorized (401).
     */
    public static final int UNAUTHORIZED = 401;
    /**
     * RFC2616 HTTP Status Code Section 10.4.3: Payment Required (402).
     */
    public static final int PAYMENT_REQUIRED = 402;
    /**
     * RFC2616 HTTP Status Code Section 10.4.4: Forbidden (403).
     */
    public static final int FORBIDDEN = 403;
    /**
     * RFC2616 HTTP Status Code Section 10.4.5: Not Found (404).
     */
    public static final int NOT_FOUND = 404;
    /**
     * RFC2616 HTTP Status Code Section 10.4.6: Method Not Allowed (405).
     */
    public static final int METHOD_NOT_ALLOWED = 405;
    /**
     * RFC2616 HTTP Status Code Section 10.4.7: Not Acceptable (406).
     */
    public static final int NOT_ACCEPTABLE = 406;
    /**
     * RFC2616 HTTP Status Code Section 10.4.8: Proxy Authentication Required (407).
     */
    public static final int PROXY_AUTHENTICATION_REQUIRED = 407;
    /**
     * RFC2616 HTTP Status Code Section 10.4.9: Request Time-out (408).
     */
    public static final int REQUEST_TIMEOUT = 408;
    /**
     * RFC2616 HTTP Status Code Section 10.4.10: Conflict (409).
     */
    public static final int CONFLICT = 409;
    /**
     * RFC2616 HTTP Status Code Section 10.4.11: Gone (410).
     */
    public static final int GONE = 410;
    /**
     * RFC2616 HTTP Status Code Section 10.4.12: Length Required (411).
     */
    public static final int LENGTH_REQUIRED = 411;
    /**
     * RFC2616 HTTP Status Code Section 10.4.13: Precondition Failed (412).
     */
    public static final int PRECONDITION_FAILED = 412;
    /**
     * RFC2616 HTTP Status Code Section 10.4.14: Request Entity Too Large (413).
     */
    public static final int REQUEST_ENTITY_TOO_LARGE = 413;
    /**
     * RFC2616 HTTP Status Code Section 10.4.15: Request-URI Too Large (414).
     */
    public static final int REQUEST_URI_TOO_LARGE = 414;
    /**
     * RFC2616 HTTP Status Code Section 10.4.16: Unsupported Media Type (415).
     */
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    /**
     * RFC2616 HTTP Status Code Section 10.4.17: Requested range not satisfiable (416).
     */
    public static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    /**
     * RFC2616 HTTP Status Code Section 10.4.18: Expectation Failed (417).
     */
    public static final int EXPECTATION_FAILED = 417;
    /**
     * RFC7231 <a href="http://tools.ietf.org/search/rfc7231#section-6.5.15">Upgrade required status</a>.
     */
    public static final int UPGRADE_REQUIRED = 426;
    /**
     * RFC2616 HTTP Status Code Section 10.5.1: Internal Server Error (500).
     */
    public static final int INTERNAL_SERVER_ERROR = 500;
    /**
     * RFC2616 HTTP Status Code Section 10.5.2: Not Implemented (501).
     */
    public static final int NOT_IMPLEMENTED = 501;
    /**
     * RFC2616 HTTP Status Code Section 10.5.3: Bad Gateway (502).
     */
    public static final int BAD_GATEWAY = 502;
    /**
     * RFC2616 HTTP Status Code Section 10.5.4: Service Unavailable (503).
     */
    public static final int SERVICE_UNAVAILABLE = 503;
    /**
     * RFC2616 HTTP Status Code Section 10.5.5: Gateway Time-out (504).
     */
    public static final int GATEWAY_TIMEOUT = 504;
    /**
     * RFC2616 HTTP Status Code Section 10.5.6: HTTP Version not supported (505).
     */
    public static final int HTTP_VERSION_NOT_SUPPORTED = 505;
    /**
     * Size of range of one class.
     */
    public static final int CLASS_SIZE = 100;

    /**
     * The private constructor.
     */
    private HttpStatusUtil() {

    }


    /**
     * Check if the status code is valid one.
     *
     * @param statusCode the code to check.
     * @return the status.
     */
    public static boolean isValidStatus(final int statusCode) {
        return MIN_STATUS_CODE <= statusCode && statusCode <= MAX_STATUS_CODE;
    }

    /**
     * Check if the status is informational.
     *
     * @param statusCode the status code.
     * @return true if the informational status.
     */
    public static boolean isInformational(final int statusCode) {
        return statusCode / CLASS_SIZE == 1;
    }

    /**
     * Check if the status is success.
     *
     * @param statusCode the status code.
     * @return true if the informational status.
     */
    public static boolean isSuccess(final int statusCode) {
        return statusCode / CLASS_SIZE == OK / CLASS_SIZE;
    }

    /**
     * Check if the status is redirect.
     *
     * @param statusCode the status code.
     * @return true if the informational status.
     */
    public static boolean isRedirect(final int statusCode) {
        return statusCode / CLASS_SIZE == PERMANENT_REDIRECT / CLASS_SIZE;
    }

    /**
     * Check if the status is redirect.
     *
     * @param statusCode the status code.
     * @return true if the informational status.
     */
    public static boolean isClientError(final int statusCode) {
        return statusCode / CLASS_SIZE == BAD_REQUEST / CLASS_SIZE;
    }

    /**
     * Check if the status is redirect.
     *
     * @param statusCode the status code.
     * @return true if the informational status.
     */
    public static boolean isServerError(final int statusCode) {
        return statusCode / CLASS_SIZE == INTERNAL_SERVER_ERROR / CLASS_SIZE;
    }

    /**
     * Get text if it is missing.
     *
     * @param code the status code
     * @param text the text
     * @return if text is not null return supplied text or defaults to null
     */
    public static String getText(final int code, final String text) {
        if (text == null) {
            final String defaultText = getDefaultText(code);
            if (defaultText != null) {
                return defaultText;
            } else if (isInformational(code)) {
                return "Unknown informational code";
            } else if (isSuccess(code)) {
                return "Unknown success code";
            } else if (isRedirect(code)) {
                return "Unknown redirect code";
            } else if (isClientError(code)) {
                return "Unknown client error";
            } else if (isServerError(code)) {
                return "Unknown server error";
            } else {
                return "Unknown code type";
            }
        } else {
            return text;
        }
    }

    /**
     * Get default text for the code.
     *
     * @param code the code
     * @return the default text or null if code does not have a corresponding RFC
     */
    @SuppressWarnings("squid:S1479")
    public static String getDefaultText(final int code) {
        switch (code) {
            case CONTINUE:
                return "Continue";
            case SWITCHING_PROTOCOLS:
                return "Switching Protocols";
            case OK:
                return "OK";
            case CREATED:
                return "Created";
            case ACCEPTED:
                return "Accepted";
            case NON_AUTHORITATIVE_INFORMATION:
                return "Non-Authoritative Information";
            case NO_CONTENT:
                return "No Content";
            case RESET_CONTENT:
                return "Reset Content";
            case PARTIAL_CONTENT:
                return "Partial Content";
            case MULTIPLE_CHOICES:
                return "Multiple Choices";
            case MOVED_PERMANENTLY:
                return "Moved Permanently";
            case FOUND:
                return "Found";
            case SEE_OTHER:
                return "See Other";
            case NOT_MODIFIED:
                return "Not Modified";
            case USE_PROXY:
                return "Use Proxy";
            case TEMPORARY_REDIRECT:
                return "Temporary Redirect";
            case BAD_REQUEST:
                return "Bad Request";
            case UNAUTHORIZED:
                return "Unauthorized";
            case PAYMENT_REQUIRED:
                return "Payment Required";
            case FORBIDDEN:
                return "Forbidden";
            case NOT_FOUND:
                return "Not Found";
            case METHOD_NOT_ALLOWED:
                return "Method Not Allowed";
            case NOT_ACCEPTABLE:
                return "Not Acceptable";
            case PROXY_AUTHENTICATION_REQUIRED:
                return "Proxy Authentication Required";
            case REQUEST_TIMEOUT:
                return "Request Time-out";
            case CONFLICT:
                return "Conflict";
            case GONE:
                return "Gone";
            case LENGTH_REQUIRED:
                return "Length Required";
            case PRECONDITION_FAILED:
                return "Precondition Failed";
            case REQUEST_ENTITY_TOO_LARGE:
                return "Request Entity Too Large";
            case REQUEST_URI_TOO_LARGE:
                return "Request-URI Too Large";
            case UNSUPPORTED_MEDIA_TYPE:
                return "Unsupported Media Type";
            case REQUESTED_RANGE_NOT_SATISFIABLE:
                return "Requested range not satisfiable";
            case EXPECTATION_FAILED:
                return "Expectation Failed";
            case INTERNAL_SERVER_ERROR:
                return "Internal Server Error";
            case NOT_IMPLEMENTED:
                return "Not Implemented";
            case BAD_GATEWAY:
                return "Bad Gateway";
            case SERVICE_UNAVAILABLE:
                return "Service Unavailable";
            case GATEWAY_TIMEOUT:
                return "Gateway Time-out";
            case HTTP_VERSION_NOT_SUPPORTED:
                return "HTTP Version not supported";
            case WEBDAV_PROCESSING:
                return "Processing";
            case WEBDAV_MULTI_STATUS:
                return "Multi-Status";
            case WEBDAV_ALREADY_REPORTED:
                return "Already Reported";
            default:
                return null;
        }
    }

    /**
     * Check if status indicates switching protocols.
     *
     * @param method the method.
     * @param status the status.
     * @return true if status indicating switching protocols.
     */
    public static boolean isSwitchProtocol(final String method, final int status) {
        return SWITCHING_PROTOCOLS == status
                || HttpMethodUtil.isConnect(method) && isSuccess(status);
    }
}
