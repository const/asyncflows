package net.sf.asyncobjects.protocol.http.common;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * The test for URL utilities.
 */
public class HttpURIUtilTest {

    @Test
    public void parametersParser() throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
        final Map<String, String> map = HttpURIUtil.getQueryParameters( // NOPMD
                new URI("http://localhost:58441/chargen.txt?length=1024&code=Test+0"));
        assertEquals(2, map.size());
        assertEquals("1024", map.get("length"));
        assertEquals("Test 0", map.get("code"));
    }
}
