package net.sf.asyncobjects.protocol.mime;

/**
 * The MIME header.
 */
public class Header {
    /**
     * The raw header text.
     */
    private final String raw;
    /**
     * The header name.
     */
    private final String name;
    /**
     * The header value.
     */
    private final String value;

    /**
     * The constructor.
     *
     * @param raw   the raw header text
     * @param name  the header name
     * @param value the header value
     */
    public Header(final String raw, final String name, final String value) {
        this.raw = raw;
        this.name = name;
        this.value = value;
    }

    /**
     * The constructor.
     *
     * @param name  the header name
     * @param value the header value
     */
    public Header(final String name, final String value) {
        this(name.trim() + ": " + value.trim() + "\r\n", name, value);
    }

    /**
     * The ASCII text of the header.
     *
     * @param raw the raw header text
     * @return the header text
     */
    public static Header parse(final String raw) {
        final int i = raw.indexOf(':');
        final String name;
        final String value;
        if (i > 0) {
            name = raw.substring(0, i).trim();
            value = raw.substring(i + 1).trim();
        } else {
            name = raw.trim();
            value = "";
        }
        return new Header(raw, name, value);
    }

    /**
     * @return the header name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the header value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return get raw header representation
     */
    public String getRaw() {
        return raw;
    }
}
