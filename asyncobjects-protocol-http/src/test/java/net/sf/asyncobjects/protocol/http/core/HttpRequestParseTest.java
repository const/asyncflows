package net.sf.asyncobjects.protocol.http.core;

import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.adapters.Adapters;
import net.sf.asyncobjects.nio.util.ByteParserContext;
import net.sf.asyncobjects.nio.util.CharIOUtil;
import net.sf.asyncobjects.protocol.http.HttpStatusException;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.HttpRequestMessage;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.server.core.HttpServerMessageUtil;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        doAsync(() -> {
            final AInput<ByteBuffer> input = Adapters.getByteArrayInput(request.getBytes(CharIOUtil.ISO8859_1));
            final ByteParserContext parserContext = new ByteParserContext(input);
            return HttpServerMessageUtil.parseRequestMessage(parserContext, requestMessage);
        });
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
