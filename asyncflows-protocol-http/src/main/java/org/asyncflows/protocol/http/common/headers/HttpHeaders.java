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

package org.asyncflows.protocol.http.common.headers; // NOPMD

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.ProtocolException;
import org.asyncflows.protocol.ProtocolLimitExceededException;
import org.asyncflows.protocol.ProtocolStreamTruncatedException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.asyncflows.protocol.LineUtil.CR;
import static org.asyncflows.protocol.LineUtil.CRLF;
import static org.asyncflows.protocol.LineUtil.LF;
import static org.asyncflows.protocol.LineUtil.MAX_ISO_8859_1;
import static org.asyncflows.protocol.LineUtil.isBlank;
import static org.asyncflows.protocol.LineUtil.writeLatin1;

/**
 * The HTTP headers structure. The structure is keeping headers in the normalized form.
 * It also provide some utility methods that check or setup headers.
 * <ul>
 * <li>The header names happen in the order they are encountered in the stream.</li>
 * <li>The new lines are removed from the header values.</li>
 * <li>The header names are converted to lowercase.</li>
 * </ul>
 * The headers are mutable structure, so it should be passed around carefully. In
 * the typical request workflow it is passed as a token, so when one component
 * processing it, other already forgotten about it, or copy constructor
 * {@link #HttpHeaders(HttpHeaders)} could be used in cases when there is a risk
 * of modification.
 */
public final class HttpHeaders {
    /**
     * The collection of the headers.
     */
    private final LinkedHashMap<String, List<String>> headers; // NOPMD

    /**
     * The private constructor.
     *
     * @param headers the headers.
     */
    private HttpHeaders(final LinkedHashMap<String, List<String>> headers) { // NOPMD
        this.headers = headers;
    }

    /**
     * The new header collection.
     */
    public HttpHeaders() {
        this(new LinkedHashMap<>());
    }

    /**
     * The new header collection.
     *
     * @param headers the headers
     */
    public HttpHeaders(final HttpHeaders headers) {
        this(copy(headers.headers));
    }

    /**
     * Copy headers.
     *
     * @param headers the header map to copy
     * @return the copied headers
     */
    private static LinkedHashMap<String, List<String>> copy(final Map<String, List<String>> headers) { // NOPMD
        final LinkedHashMap<String, List<String>> map = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            map.put(entry.getKey(), new ArrayList<>(entry.getValue())); // NOPMD
        }
        return map;
    }

    /**
     * Read MIME headers.
     *
     * @param input the head input
     * @param limit the limit for the total header size
     * @return the headers
     */
    public static Promise<HttpHeaders> readHeaders(final ByteParserContext input, final int limit) { // NOPMD
        final HttpHeaders headers = new HttpHeaders();
        return aSeqWhile(new ASupplier<Boolean>() {
            private static final int LINE_START = 0;
            private static final int NAME = 1;
            private static final int VALUE = 3;
            private static final int VALUE_AFTER_CR = 4;
            private static final int END_BEFORE_CR = 5;
            private static final int END_AFTER_LF = 6;
            private static final int LINE_SKIP = 7;
            private static final int LINE_SKIP_AFTER_CR = 8;

            private final StringBuilder current = new StringBuilder(); // NOPMD
            private int size;
            private int state = LINE_START;
            private String name;

            @Override
            public Promise<Boolean> get() { // NOPMD
                if (!input.hasRemaining()) {
                    if (input.isEofSeen()) {
                        throw new ProtocolStreamTruncatedException("EOF before headers ends");
                    } else {
                        return input.readMore();
                    }
                }
                final ByteBuffer buffer = input.buffer();
                do {
                    if (size + 1 >= limit) {
                        throw new ProtocolLimitExceededException("The headers total size is more than " + limit);
                    }
                    final char c = (char) (buffer.get() & MAX_ISO_8859_1);
                    size++;
                    if (state == LINE_START) {
                        if (HttpHeadersUtil.isLWSP(c)) {
                            if (name == null) {
                                state = LINE_SKIP;
                            } else {
                                state = VALUE;
                            }
                        } else {
                            if (name != null) {
                                headers.addHeader(name, current.toString().trim());
                                name = null;
                                current.setLength(0);
                            }
                            if (c == CR) {
                                state = END_BEFORE_CR;
                            } else {
                                // the next MIME header
                                current.setLength(0);
                                state = NAME;
                            }
                        }
                    }
                    current.append(c);
                    switch (state) {
                        case NAME:
                            if (!HttpHeadersUtil.isFieldNameChar(c)) {
                                if (c == ':') {
                                    current.setLength(current.length() - 1);
                                    name = current.toString();
                                    if (name.length() == 0) {
                                        throw new ProtocolException("Empty header is encountered");
                                    }
                                    current.setLength(0);
                                    state = VALUE;
                                } else {
                                    throw new ProtocolException("Invalid header name character is encountered: "
                                            + (int) c);
                                }
                            }
                            break;
                        case VALUE:
                            if (c == CR) {
                                state = VALUE_AFTER_CR;
                            }
                            break;
                        case VALUE_AFTER_CR:
                            if (c == LF) {
                                current.setLength(current.length() - 2);
                                state = LINE_START;
                            } else {
                                throw new ProtocolException("CR must be followed by LF in header: " + name);
                            }
                            break;
                        case END_BEFORE_CR:
                            state = END_AFTER_LF;
                            break;
                        case END_AFTER_LF:
                            if (c == LF) {
                                return aFalse();
                            } else {
                                throw new ProtocolException("CR must be followed by LF in the end of headers");
                            }
                        case LINE_SKIP:
                            if (c == CR) {
                                state = LINE_SKIP_AFTER_CR;
                            }
                            break;
                        case LINE_SKIP_AFTER_CR:
                            if (c != LF) {
                                throw new ProtocolException("CR must be followed by LF for skipped lines");
                            }
                            current.setLength(0);
                            state = LINE_START;
                            break;
                        default:
                            throw new IllegalStateException("Invalid state: " + state);
                    }
                } while (buffer.hasRemaining());
                return input.readMore();
            }
        }).thenValue(headers);
    }

    /**
     * Create headers from map (map is copied).
     *
     * @param headers the headers
     * @return the headers object
     */
    public static HttpHeaders fromMap(final Map<String, List<String>> headers) {
        final LinkedHashMap<String, List<String>> map = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null) {
                map.put(HttpHeadersUtil.normalizeName(entry.getKey()), new ArrayList<>(entry.getValue())); // NOPMD
            }
        }
        return new HttpHeaders(map);
    }

    /**
     * Return list of header values for the specified name.
     *
     * @param name the name
     * @return the list of headers, if header does not exists, an empty list is returned. The resulting list
     * is unmodifiable, but it might change after update operations.
     */
    public List<String> getHeaders(final String name) {
        final List<String> list = headers.get(HttpHeadersUtil.normalizeName(name));
        if (list == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(list);
        }
    }

    /**
     * @return the field names
     */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    /**
     * Add the header. If header exists, a new value will be added to the list.
     *
     * @param name  the header name
     * @param value the header value
     */
    public void addHeader(final String name, final String value) {
        final List<String> list = getHeadersList(name);
        list.add(value);
    }

    /**
     * Get the mutable header list and create it if it does not exists.
     *
     * @param name the list name
     * @return the list value
     */
    private List<String> getHeadersList(final String name) {
        final String normalizedName = HttpHeadersUtil.normalizeName(name);
        return headers.computeIfAbsent(normalizedName, k -> new LinkedList<>());
    }

    /**
     * Get headers that have multiple token values separated by comma (no parameters or other things).
     *
     * @param name the name of the header
     * @return comma separated values or multiple headers
     */
    public List<String> getCommaSeparatedValues(final String name) {
        final List<String> rc = new ArrayList<>();
        for (final String header : getHeaders(name)) {
            if (header.indexOf(',') != -1) {
                for (final String value : header.split(",")) {
                    if (!isBlank(value)) {
                        rc.add(value.trim());
                    }
                }
            } else if (!isBlank(header)) {
                rc.add(header.trim());
            }
        }
        return rc;
    }

    /**
     * Set the header. If header exists, all old values will be removed, and a new value will be added.
     *
     * @param name  the header name
     * @param value the header value
     */
    public void setHeader(final String name, final String value) {
        final List<String> list = getHeadersList(name);
        if (!list.isEmpty()) {
            list.clear();
        }
        list.add(value);
    }

    /**
     * Set collection of headers.
     *
     * @param name   the header name
     * @param values the header values
     */
    public void setHeaders(final String name, final Collection<String> values) {
        final List<String> list = getHeadersList(name);
        if (!list.isEmpty()) {
            list.clear();
        }
        list.addAll(values);
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        for (final Map.Entry<String, List<String>> header : headers.entrySet()) {
            for (final String value : header.getValue()) {
                HttpHeadersUtil.appendCapitalizedName(b, header.getKey());
                b.append(':').append(' ').append(value).append(CR).append(LF);
            }
        }
        b.append(CRLF);
        return b.toString();
    }

    /**
     * Write headers to the stream.
     *
     * @param context the context to write to
     * @return when adding to the context finishes (data might be still in the buffer)
     */
    public Promise<Void> write(final ByteGeneratorContext context) {
        return writeLatin1(context, toString());
    }

    /**
     * Remove header if it exists.
     *
     * @param name the header name
     */
    public void removeHeader(final String name) {
        headers.remove(name);
    }

    /**
     * Set header to be the first on header list. This is used for the "Host:" header that should be
     * the first on the list. It is supposed that this method will be called only once. Note, that this
     * is an expensive operation, since it recreates a map anew.
     *
     * @param header the header name
     * @param value  the header value.
     */
    public void setFirstHeader(final String header, final String value) {
        removeHeader(header);
        final LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>(headers);
        final LinkedList<String> list = new LinkedList<>();
        list.add(value);
        headers.clear();
        headers.put(header, list);
        headers.putAll(copy);
    }

    /**
     * Set header value if missing.
     *
     * @param name  the header
     * @param value the value
     */
    public void setHeaderIfMissing(final String name, final String value) {
        final List<String> list = getHeadersList(name);
        if (list.isEmpty()) {
            list.add(value);
        }
    }
}
