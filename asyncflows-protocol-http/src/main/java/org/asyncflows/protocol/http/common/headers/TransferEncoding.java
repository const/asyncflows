/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.protocol.http.common.headers;

import org.asyncflows.protocol.http.common.content.ContentUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The transfer encoding element. This object represents transfer encoding and used
 * for TE and Transfer-Encoding headers.
 */
public class TransferEncoding {
    /**
     * The encoding name.
     */
    private final String name;
    /**
     * The encoding parameters.
     */
    private final Map<String, String> parameters;

    /**
     * The constructor.
     *
     * @param name       the name
     * @param parameters the parameters
     */
    public TransferEncoding(final String name, final Map<String, String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * The constructor.
     *
     * @param name the name
     */
    public TransferEncoding(final String name) {
        this(name, Collections.<String, String>emptyMap());
    }


    /**
     * Parse a single transfer encoding, it is expected to be on the list.
     *
     * @param p the parser to use
     * @return the parsed transfer encoding
     */
    private static TransferEncoding parseValue(final HttpHeaderParser p) {
        final String name = p.token();
        String key = null;
        String value = null;
        Map<String, String> parameters = null;
        p.ows();
        while (p.tryChar(';')) {
            if (key != null && parameters == null) {
                parameters = new LinkedHashMap<>(); // NOPMD
                parameters.put(key, value);
            }
            p.ows();
            key = p.token();
            p.ows();
            p.consume('=');
            p.ows();
            value = p.tokenOrString();
            p.ows();
            if (parameters != null) {
                parameters.put(key, value);
            }
        }
        if (parameters != null) {
            parameters = Collections.unmodifiableMap(parameters);
        } else if (key != null) {
            parameters = Collections.unmodifiableMap(Collections.singletonMap(key, value));
        } else {
            parameters = Collections.emptyMap();
        }
        return new TransferEncoding(name, parameters);
    }


    /**
     * Parse transfer encodings to the list.
     *
     * @param headerValues the header values
     * @return the parsed list
     */
    public static List<TransferEncoding> parse(final List<String> headerValues) {
        final List<TransferEncoding> rc = new ArrayList<>();
        for (final String value : headerValues) {
            final HttpHeaderParser p = new HttpHeaderParser(value); // NOPMD
            while (true) {
                do {
                    p.ows();
                } while (p.tryChar(','));
                if (p.hasNext()) {
                    final TransferEncoding encoding = parseValue(p);
                    rc.add(encoding);
                } else {
                    break;
                }
            }
        }
        return rc;
    }


    /**
     * Convert transfer encodings to the header value.
     *
     * @param encodings the encodings
     * @return the string representation.
     */
    public static String toString(final List<TransferEncoding> encodings) {
        return HttpHeadersUtil.listValue(encodings);
    }

    /**
     * Check if the list of transfer encoding is finally wrapped by chunked encodings.
     *
     * @param encodings the list of encodings
     * @return true if chunked is a final encoding.
     */
    public static boolean isChunked(final List<TransferEncoding> encodings) {
        return !encodings.isEmpty() && ContentUtil.CHUNKED_ENCODING.equals(
                encodings.get(encodings.size() - 1).getName());
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            sb.append(';').append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }
}
