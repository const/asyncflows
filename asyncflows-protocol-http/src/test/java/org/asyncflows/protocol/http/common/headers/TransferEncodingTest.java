package org.asyncflows.protocol.http.common.headers;

import org.asyncflows.protocol.http.common.content.ContentUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for transfer encodings.
 */
public class TransferEncodingTest {
    @Test
    public void testParseSingle() {
        final List<TransferEncoding> parsed0 = TransferEncoding.parse(Collections.singletonList(
                ContentUtil.CHUNKED_ENCODING));
        assertEquals(1, parsed0.size());
        assertEquals(ContentUtil.CHUNKED_ENCODING, parsed0.get(0).getName());
        final List<TransferEncoding> parsed1 = TransferEncoding.parse(Collections.singletonList(
                "\t" + ContentUtil.CHUNKED_ENCODING + " "));
        assertEquals(1, parsed1.size());
        assertEquals(ContentUtil.CHUNKED_ENCODING, parsed1.get(0).getName());
        final List<TransferEncoding> parsed2 = TransferEncoding.parse(Arrays.asList(
                ContentUtil.CHUNKED_ENCODING, ContentUtil.GZIP_ENCODING + ", " + ContentUtil.DEFLATE_ENCODING));
        assertEquals(3, parsed2.size());
        assertEquals(ContentUtil.CHUNKED_ENCODING, parsed2.get(0).getName());
        assertEquals(ContentUtil.GZIP_ENCODING, parsed2.get(1).getName());
        assertEquals(ContentUtil.DEFLATE_ENCODING, parsed2.get(2).getName());
    }
}
