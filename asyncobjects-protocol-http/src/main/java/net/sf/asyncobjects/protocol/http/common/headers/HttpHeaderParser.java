package net.sf.asyncobjects.protocol.http.common.headers;

import net.sf.asyncobjects.protocol.ProtocolLineParser;

/**
 * The parser for the commons elements for the HTTP headers. Note that syntax is slightly different from mime,
 * for example token definition does not include "/" in HTTP headers. So it is parsed using other algorithms.
 */
public class HttpHeaderParser extends ProtocolLineParser {

    /**
     * The constructor.
     *
     * @param text the text to parse
     */
    public HttpHeaderParser(final String text) {
        super(text);
    }

    /**
     * Check is the character is obsolete text.
     *
     * @param c the character to check
     * @return true if the obsolete text
     */
    public static boolean isObsText(final char c) {
        return '\u0080' <= c && c <= '\u00FF';
    }

    /**
     * Check if the character is token character.
     *
     * @param c the character to check
     * @return true if the token character
     */
    public static boolean isToken(final char c) {
        switch (c) {
            case '!':
            case '#':
            case '$':
            case '%':
            case '&':
            case '\'':
            case '*':
            case '+':
            case '-':
            case '.':
            case '^':
            case '_':
            case '`':
            case '|':
            case '~':
                return true;
            default:
                return isDigit(c) || isAlpha(c);
        }
    }

    /**
     * @return try parsing token.
     */
    public String tryToken() {
        final int p = position;
        while (hasNext() && isToken(la())) {
            consume();
        }
        return p != position ? text.substring(p, position) : null;
    }

    /**
     * @return try parsing quoted string.
     */
    public String tryString() {
        if (la() != '"') {
            return null;
        }
        final int p = position; // NOPMD
        consume();
        while (hasNext() && la() != '"') {
            final char c = consume();
            if (c == '\\') {
                if (hasNext()) {
                    final char q = consume();
                    if (!isObsText(q) && !isWhitespace(q) && !isVChar(q)) {
                        throw new HttpHeaderParserException("Unexpected quoted character in the header ("
                                + (position - 1) + "): " + text);
                    }
                } else {
                    throw new HttpHeaderParserException("EOF on escape in header: " + text);
                }
            }
            if (!isObsText(c) && !isWhitespace(c) && !isVChar(c)) {
                throw new HttpHeaderParserException("Unexpected character in the header string ("
                        + (position - 1) + "): " + text);
            }
        }
        if (!hasNext()) {
            throw new HttpHeaderParserException("Unterminated string in the header: " + text);
        }
        consume();
        return text.substring(p, position);
    }

    /**
     * @return the parsed token
     */
    public String token() {
        final String name = tryToken();
        if (name == null) {
            throw new HttpHeaderParserException("token is expected at " + position + ":" + text);
        }
        return name;
    }

    /**
     * @return parse token or string
     */
    public String tokenOrString() {
        String value = tryToken();
        if (value == null) {
            value = tryString();
            if (value == null) {
                throw new HttpHeaderParserException("token or string are expected at " + position + ":" + text);
            }
        }
        return value;
    }
}
