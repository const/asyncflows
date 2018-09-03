package org.asyncflows.protocol.http.common;

import org.asyncflows.protocol.LineUtil;

/**
 * The XML utils.
 */
public final class XmlUtil {
    /**
     * Private constructor for the utility class.
     */
    private XmlUtil() {
        // do nothing
    }

    /**
     * Escape XML.
     *
     * @param value the value to encode.
     * @return the encoded value
     */
    public static String escapeXml(final String value) {
        if (value == null) {
            return null;
        }
        final StringBuilder rc = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '<':
                    rc.append("&lt;");
                    break;
                case '>':
                    rc.append("&gt;");
                    break;
                case '\"':
                    rc.append("&quot;");
                    break;
                case '&':
                    rc.append("&amp;");
                    break;
                case '\'':
                    rc.append("&apos;");
                    break;
                default:
                    if (c == LineUtil.DEL || c < ' ' && !Character.isSpaceChar(c)) {
                        rc.append("&#").append((int) c).append(';');
                    } else {
                        rc.append(c);
                    }
                    break;
            }
        }
        return rc.toString();
    }
}
