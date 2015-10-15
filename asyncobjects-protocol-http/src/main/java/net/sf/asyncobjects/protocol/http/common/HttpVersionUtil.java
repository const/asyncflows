package net.sf.asyncobjects.protocol.http.common;

/**
 * The HTTP version utilities.
 */
public final class HttpVersionUtil {
    /**
     * The HTTP version 1.0.
     */
    public static final String HTTP_VERSION_1_0 = "HTTP/1.0";
    /**
     * The HTTP version 1.1.
     */
    public static final String HTTP_VERSION_1_1 = "HTTP/1.1";

    /**
     * Private constructor for utility class.
     */
    private HttpVersionUtil() {
        // do nothing
    }

    /**
     * Check if the protocol is HTTP 1.1.
     *
     * @param version the version to check
     * @return true if http 1.1
     */
    public static boolean isHttp11(final String version) {
        return HTTP_VERSION_1_1.equals(version);
    }

    /**
     * Check if the protocol is HTTP 1.0.
     *
     * @param version the version to check
     * @return true if http 1.1
     */
    public static boolean isHttp10(final String version) {
        return HTTP_VERSION_1_0.equals(version);
    }
}
