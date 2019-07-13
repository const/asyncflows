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

package org.asyncflows.protocol.http.core;

import org.asyncflows.core.AsyncContext;
import org.asyncflows.core.AsyncExecutionException;
import org.asyncflows.io.AInput;
import org.asyncflows.io.adapters.blocking.Adapters;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.io.util.CharIOUtil;
import org.asyncflows.protocol.http.HttpStatusException;
import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.HttpRequestMessage;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.server.core.HttpServerMessageUtil;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for HTTP request parsing.
 */
public class HttpRequestParseTest {

    /**
     * Parse the specified request message.
     *
     * @param request the request message
     * @return the parsed message
     */
    private static HttpRequestMessage parseRequestMessage(final String request) {
        final HttpRequestMessage requestMessage = new HttpRequestMessage();
        requestMessage.setProtocol("http");
        requestMessage.setServerAddress("localhost:80");
        try {
            AsyncContext.doAsyncThrowable(() -> {
                final AInput<ByteBuffer> input = Adapters.getByteArrayInput(request.getBytes(CharIOUtil.ISO8859_1));
                final ByteParserContext parserContext = new ByteParserContext(input);
                return HttpServerMessageUtil.parseRequestMessage(parserContext, requestMessage);
            });
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable throwable) {
            throw new AsyncExecutionException(throwable);
        }
        return requestMessage;
    }

    @Test
    public void connectTest() {
        final String request = "CONNECT server.example.com:80 HTTP/1.1\r\n" +
                "Host: server.example.com:80\r\n" +
                "Proxy-Authorization: basic aGVsbG86d29ybGQ=\r\n\r\n";
        final HttpRequestMessage requestMessage = parseRequestMessage(request);
        assertEquals(HttpMethodUtil.CONNECT, requestMessage.getMethod());
        assertEquals("http://server.example.com:80", requestMessage.getEffectiveUri().toString());
        assertEquals("basic aGVsbG86d29ybGQ=", requestMessage.getHeaders().getHeaders("Proxy-Authorization").get(0));
    }

    @Test
    public void getTest() {
        final String request = "GET /hello.txt HTTP/1.1\r\n" +
                "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
                "Host: www.example.com\r\n" +
                "Accept-Language: en, mi\r\n\r\n";
        final HttpRequestMessage requestMessage = parseRequestMessage(request);
        assertEquals(HttpMethodUtil.GET, requestMessage.getMethod());
        assertEquals("http://www.example.com:80/hello.txt", requestMessage.getEffectiveUri().toString());
        assertEquals("en, mi", requestMessage.getHeaders().getHeaders("Accept-Language").get(0));
    }

    @Test
    public void failNonAscii() {
        final String request = "GET /hello\u0099.txt HTTP/1.1\r\n" +
                "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
                "Host: www.example.com\r\n" +
                "Accept-Language: en, mi\r\n\r\n";
        try {
            final HttpRequestMessage message = parseRequestMessage(request);
            fail("The parsing should have failed: " + message.getEffectiveUri());
        } catch (HttpStatusException ex) {
            assertEquals(HttpStatusUtil.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Test
    public void failSpaces() {
        final String request = "GET /hello. txt HTTP/1.1\r\n" +
                "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
                "Host: www.example.com\r\n" +
                "Accept-Language: en, mi\r\n\r\n";
        try {
            final HttpRequestMessage message = parseRequestMessage(request);
            fail("The parsing should have failed: " + message.getEffectiveUri());
        } catch (HttpStatusException ex) {
            assertEquals(HttpStatusUtil.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Test
    public void failUserInfoHost() {
        final String request = "GET /hello.txt HTTP/1.1\r\n" +
                "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
                "Host: user:pw@www.example.com\r\n" +
                "Accept-Language: en, mi\r\n\r\n";
        try {
            final HttpRequestMessage message = parseRequestMessage(request);
            fail("The parsing should have failed: " + message.getEffectiveUri());
        } catch (HttpStatusException ex) {
            assertEquals(HttpStatusUtil.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Test
    public void failUserInfoProxy() {
        final String request = "GET http://user:pw@www.example.com/hello.txt HTTP/1.1\r\n" +
                "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
                "Host: www.example.com\r\n" +
                "Accept-Language: en, mi\r\n\r\n";
        try {
            final HttpRequestMessage message = parseRequestMessage(request);
            fail("The parsing should have failed: " + message.getEffectiveUri());
        } catch (HttpStatusException ex) {
            assertEquals(HttpStatusUtil.BAD_REQUEST, ex.getStatusCode());
        }
    }

}
