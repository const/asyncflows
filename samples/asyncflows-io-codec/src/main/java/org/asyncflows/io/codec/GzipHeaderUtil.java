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

package org.asyncflows.io.codec;

import org.asyncflows.io.util.GZipHeader;

import static org.asyncflows.io.codec.BinaryReader.readByte;
import static org.asyncflows.io.codec.BinaryReader.readSequence;
import static org.asyncflows.io.codec.BinaryReader.readUint16;
import static org.asyncflows.io.codec.BinaryReader.readerField;
import static org.asyncflows.io.codec.BinaryReader.withCRC32;

public class GzipHeaderUtil {

    private GzipHeaderUtil() {
    }

    public static BinaryReader.ReaderAction<GZipHeader> readGZip() {
        return withCRC32(
                readSequence(GZipHeader.class,
                        readerField(readUint16(), (z, magic) -> {
                                    if (magic != GZipHeader.ID) {
                                        throw new BinaryFormatException(String.format("Not a GZip format: %04x", magic));
                                    }
                                }
                        )
                ).then(
                        readerField(readByte(), GZipHeader::setCompressionMethod)
                ).then(
                        readerField(readByte(), GZipHeader::setFlags)
                ).end()
        );
    }
}
