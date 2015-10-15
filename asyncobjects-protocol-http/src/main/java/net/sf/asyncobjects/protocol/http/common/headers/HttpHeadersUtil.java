package net.sf.asyncobjects.protocol.http.common.headers;

import net.sf.asyncobjects.protocol.LineUtil;
import net.sf.asyncobjects.protocol.http.HttpException;
import net.sf.asyncobjects.protocol.http.common.HttpVersionUtil;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import static net.sf.asyncobjects.protocol.LineUtil.HT;
import static net.sf.asyncobjects.protocol.LineUtil.SPACE;

/**
 * The utilities related to generating and parsing data for HTTP headers.
 */
public final class HttpHeadersUtil {
    /**
     * The library description string in the form project.name/project.version.
     * If project information failed to be read, an empty string is returned.
     * This is a format that is expected for User-Agent and Server headers.
     */
    public static final String LIBRARY_DESCRIPTION = findLibraryDescription();
    /**
     * The UTC time zone (used for date formatting).
     */
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    /**
     * The allow header.
     */
    public static final String ALLOW_HEADER = "allow";
    /**
     * The connection header.
     */
    public static final String CONNECTION_HEADER = "connection";
    /**
     * The connection close value.
     */
    public static final String CONNECTION_CLOSE_VALUE = "close";
    /**
     * The connection keep-alive.
     */
    public static final String CONNECTION_KEEP_ALIVE_VALUE = "keep-alive";
    /**
     * The content length header.
     */
    public static final String CONTENT_LENGTH_HEADER = "content-length";
    /**
     * The content type.
     */
    public static final String CONTENT_TYPE_HEADER = "content-type";
    /**
     * The content type: HTML, UTF-8 charset (typical for generated html content).
     */
    public static final String CONTENT_TYPE_HTML_UTF8 = "text/html; charset=UTF-8";
    /**
     * The content type: Text, UTF-8 charset (typical for generated text content).
     */
    public static final String CONTENT_TYPE_TEXT_UTF8 = "text/plain; charset=UTF-8";
    /**
     * The content encoding.
     */
    public static final String CONTENT_ENCODING_HEADER = "content-encoding";
    /**
     * The connection header.
     */
    public static final String DATE_HEADER = "date";
    /**
     * The expect header name.
     */
    public static final String EXPECT_HEADER = "expect";
    /**
     * The continue expectation.
     */
    public static final String EXPECT_CONTINUE = "101-continue";
    /**
     * The host header name.
     */
    public static final String HOST_HEADER = "host";
    /**
     * The location header.
     */
    public static final String LOCATION_HEADER = "location";
    /**
     * The connection header.
     */
    public static final String SERVER_HEADER = "server";
    /**
     * Allowed transfer encoding value.
     */
    public static final String TE_HEADER = "te";
    /**
     * Trailers value for te field.
     */
    public static final String TE_TRAILERS_VALUE = "trailers";
    /**
     * The trailer header.
     */
    public static final String TRAILER_HEADER = "trailer";
    /**
     * The transfer encoding header.
     */
    public static final String TRANSFER_ENCODING_HEADER = "transfer-encoding";
    /**
     * Upgrade header.
     */
    public static final String UPGRADE_HEADER = "upgrade";
    /**
     * The user agent header.
     */
    public static final String USER_AGENT_HEADER = "user-agent";
    /**
     * Blacklist of trailer names.
     */
    public static final Set<String> TRAILERS_BLACKLIST = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // TODO make use of blacklist on client and server.
            // TODO add more headers
            CONNECTION_HEADER,
            CONTENT_ENCODING_HEADER,
            CONTENT_LENGTH_HEADER,
            CONTENT_TYPE_HEADER,
            DATE_HEADER,
            EXPECT_HEADER,
            LOCATION_HEADER,
            SERVER_HEADER,
            TE_HEADER,
            TRANSFER_ENCODING_HEADER,
            UPGRADE_HEADER,
            USER_AGENT_HEADER
    )));

    /**
     * The private constructor for utility class.
     */
    private HttpHeadersUtil() {
        // do nothing
    }

    /**
     * Format the date according to the HTTP format.
     *
     * @param date the date to format
     * @return the date
     */
    public static String formatDate(final Date date) {
        final SimpleDateFormat format = getHttpDateFormat();
        return format.format(date);
    }

    /**
     * @return the date format used to generate dates for HTTP.
     */
    private static SimpleDateFormat getHttpDateFormat() {
        final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(UTC);
        return format;
    }

    /**
     * @return the library description string, empty string if there is a problem with loading resources.
     */
    private static String findLibraryDescription() {
        final InputStream in = HttpException.class.getResourceAsStream("common/version.properties");
        if (in == null) {
            return "";
        }
        try {
            try {
                final Properties properties = new Properties();
                properties.load(in);
                final String name = properties.getProperty("library.name");
                final String version = properties.getProperty("library.version");
                final String description = name + "/" + version;
                // handle the case when variables are not replaced in the resources.
                return description.indexOf('{') == -1 ? "" : description;
            } finally {
                in.close();
            }
        } catch (Exception ex) { // NOPMD
            return "";
        }
    }

    /**
     * Get content length from headers. The method throws different exception if values could not be parsed.
     *
     * @param headers the headers
     * @return the the content length if it presents and could be parsed.
     */
    public static Long getContentLength(final HttpHeaders headers) {
        Long current = null;
        for (final String header : headers.getCommaSeparatedValues(CONTENT_LENGTH_HEADER)) {
            final long value = Long.parseLong(header);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid value: " + header);
            }
            if (current != null) {
                if (current != value) {
                    throw new IllegalArgumentException("Non matching values: " + current + " != " + value);
                }
            } else {
                current = value;
            }
        }
        return current;
    }

    /**
     * List of values (using toString()).
     *
     * @param values the values
     * @param <T>    the value type
     * @return the string value
     */
    public static <T> String listValue(final List<T> values) {
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return "";
        }
        boolean first = true;
        final StringBuilder builder = new StringBuilder();
        for (final T value : values) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    /**
     * Check if the message is the last message on the HTTP connection.
     *
     * @param httpVersion the version of HTTP protocol
     * @param headers     the http headers
     * @return true if this exchange is the last one basing on connection header
     */
    public static boolean isLastExchange(final String httpVersion, final HttpHeaders headers) {
        if (HttpVersionUtil.isHttp10(httpVersion)) {
            return hasHeaderValue(headers, CONNECTION_HEADER, CONNECTION_KEEP_ALIVE_VALUE);
        } else if (HttpVersionUtil.isHttp11(httpVersion)) {
            return !hasHeaderValue(headers, CONNECTION_HEADER, CONNECTION_CLOSE_VALUE);
        } else {
            throw new HttpException("Unsupported http version: " + httpVersion);
        }
    }

    /**
     * Check header value (ignoring the case).
     *
     * @param headers the header to check
     * @param header  the header name
     * @param value   the header value that is checked for the presence (ignoring the case)
     * @return true if value presents
     */
    public static boolean hasHeaderValue(final HttpHeaders headers, final String header, final String value) {
        for (final String headerValue : headers.getCommaSeparatedValues(header)) {
            if (headerValue.trim().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set connection close or keep-alive header if needed.
     *
     * @param headers     the headers to update
     * @param version     the http version
     * @param lastMessage true, if this message is last on the stream
     */
    public static void setLastMessageHeader(final HttpHeaders headers, final String version,
                                            final boolean lastMessage) {
        if (lastMessage) {
            if (HttpVersionUtil.isHttp11(version)) {
                headers.setHeader(CONNECTION_HEADER, CONNECTION_CLOSE_VALUE);
            }
        } else {
            if (HttpVersionUtil.isHttp10(version)) {
                headers.setHeader(CONNECTION_HEADER, CONNECTION_KEEP_ALIVE_VALUE);
            }
        }
    }

    /**
     * Append name of the header. The name is appended in the classic HTTP 1.x style.
     *
     * @param b    the builder
     * @param name the name to append
     */
    public static void appendCapitalizedName(final StringBuilder b, final String name) {
        boolean capital = true;
        for (int i = 0; i < name.length(); i++) {
            final char ch = name.charAt(i);
            if (capital) {
                b.append(Character.toUpperCase(ch));
                capital = false;
            } else {
                b.append(Character.toLowerCase(ch));
            }
            if (ch == '-') {
                capital = true;
            }
        }
    }

    /**
     * Get capitalized name.
     *
     * @param name the name to convert
     * @return the name
     */
    public static String capitalizedName(final String name) {
        final StringBuilder b = new StringBuilder(name.length());
        appendCapitalizedName(b, name);
        return b.toString();
    }

    /**
     * Set headers that specify how the message body is transferred.
     *
     * @param headers       the headers
     * @param encodingList  the list of encodings
     * @param contentLength the content length
     */
    public static void setMessageBodyHeaders(final HttpHeaders headers,
                                             final List<TransferEncoding> encodingList, final Long contentLength) {
        if (contentLength != null) {
            headers.setHeader(CONTENT_LENGTH_HEADER, contentLength.toString());
        } else if (!encodingList.isEmpty()) {
            headers.setHeader(TRANSFER_ENCODING_HEADER, TransferEncoding.toString(encodingList));
        }
    }

    /**
     * Normalize name (convert to lowercase).
     *
     * @param name the name to normalize
     * @return the normalized name
     */
    public static String normalizeName(final String name) {
        return name.toLowerCase(Locale.US);
    }

    /**
     * Check if the character is MIME LWSP.
     *
     * @param c the character to check
     * @return true if LWSP character
     */
    public static boolean isLWSP(final char c) {
        return c == HT || c == SPACE;
    }

    /**
     * Check if characters is a valid field name character.
     *
     * @param ch the character to check
     * @return true if character is a valid field name character.
     */
    public static boolean isFieldNameChar(final char ch) {
        return ch > SPACE && ch < (int) LineUtil.DEL && ch != ':';
    }
}
