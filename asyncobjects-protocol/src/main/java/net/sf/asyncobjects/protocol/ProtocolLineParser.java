package net.sf.asyncobjects.protocol;

/**
 * The line parser for the line based protocols.
 */
public class ProtocolLineParser {
    /**
     * The value.
     */
    protected final String text;
    /**
     * The position within header.
     */
    protected int position;

    /**
     * The constructor.
     *
     * @param text the text to parse
     */
    public ProtocolLineParser(final String text) {
        this.text = text;
    }

    /**
     * Check if the character is whitespace.
     *
     * @param c the character to check
     * @return true if the whitespace
     */
    public static boolean isWhitespace(final char c) {
        return c == ' ' || c == '\t';
    }


    /**
     * Check if character is alphabetic character.
     *
     * @param c the the character to check
     * @return true if alpha
     */
    public static boolean isAlpha(final char c) {
        return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z';
    }

    /**
     * Check if character is a digit.
     *
     * @param c the character to check
     * @return true if digit
     */
    public static boolean isDigit(final char c) {
        return '0' <= c && c <= '9';
    }


    /**
     * Check if character is a digit.
     *
     * @param c the character to check
     * @return true if digit
     */
    public static boolean isHexDigit(final char c) {
        return isDigit(c) || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F';
    }

    /**
     * Check if character is visible ASCII character.
     *
     * @param c the character
     * @return true if visible character
     */
    public static boolean isVChar(final char c) {
        return '!' <= c && c <= '~';
    }

    /**
     * @return true if there are more characters
     */
    public boolean hasNext() {
        return position < text.length();
    }

    /**
     * @return the next character
     */
    public char la() {
        return text.charAt(position);
    }

    /**
     * @return consume and return the character
     */
    public char consume() {
        return text.charAt(position++);
    }

    /**
     * Consume the specified character.
     *
     * @param c the character to consume
     */
    public void consume(final char c) {
        if (!hasNext() || la() != c) {
            throw new ProtocolException("Expecting character " + c + " at  " + position + ": " + text);
        }
        consume();
    }


    /**
     * Parse optional whitespace.
     */
    public void ows() {
        while (hasNext() && isWhitespace(la())) {
            consume();
        }
    }

    /**
     * Required whitespace is expected.
     */
    public void rws() {
        if (!hasNext() || !isWhitespace(consume())) {
            throw new ProtocolException("Missing required whitespace at " + (position - 1) + ": " + text);
        }
        ows();
    }

    /**
     * Try character.
     *
     * @param c the character to try
     * @return if character matched
     */
    public boolean tryChar(final char c) {
        if (hasNext() && la() == c) {
            consume();
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the position of the parser
     */
    public int position() {
        return position;
    }

    /**
     * @return the text
     */
    public String text() {
        return text;
    }


    /**
     * @return read hex number or fail
     */
    public String hexNumber() {
        final int s = position();
        while (hasNext() && isHexDigit(la())) {
            consume();
        }
        if (s == position()) {
            throw new ProtocolException("Hex digit is expected at " + s + " in string " + text);
        }
        return text.substring(s, position);
    }

    /**
     * @return the text from the current position until whitespace (or the end of the string).
     */
    public String untilWhiteSpace() {
        final int s = position;
        while (hasNext() && !isWhitespace(la())) {
            consume();
        }
        return text.substring(s, position);
    }

    /**
     * @return the rest of the text
     */
    public String rest() {
        return text.substring(position);
    }
}
