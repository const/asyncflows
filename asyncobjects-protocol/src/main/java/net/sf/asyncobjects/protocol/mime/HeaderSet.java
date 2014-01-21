package net.sf.asyncobjects.protocol.mime;

import net.sf.asyncobjects.protocol.LineUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The builder for the headers structure.
 */
public class HeaderSet implements Iterable<Header> {
    /**
     * The set of headers.
     */
    private final List<Header> headers = new ArrayList<Header>();

    /**
     * Add header from raw ASCII text. The text must include terminating CRLF.
     *
     * @param rawText the raw text
     * @return the added header
     */
    public Header addHeader(final String rawText) {
        final Header h = Header.parse(rawText);
        headers.add(h);
        return h;
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }

    /**
     * @return saves headers to raw text
     */
    public String toText() {
        final StringBuilder b = new StringBuilder();
        for (final Header header : this) {
            b.append(header.getRaw());
        }
        b.append(LineUtil.CRLF);
        return b.toString();
    }
}
