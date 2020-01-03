/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.io.file;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.file.nio.FileFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for file factory.
 */
public class FileFactoryTest {
    private static final byte[] SMALL_DATA = "Small Data\n".getBytes(StandardCharsets.UTF_8);

    private static String getMavenTargetDirectory() {
        Class<FileFactoryTest> context = FileFactoryTest.class;
        String resourceName = context.getName().replaceAll("\\.", "/") + ".class";
        URL resource = context.getClassLoader().getResource(resourceName);
        if (resource == null || !"file".equalsIgnoreCase(resource.getProtocol())) {
            throw new IllegalStateException("Test assumes maven project structure");
        }
        String path = resource.getPath();
        String base = path.substring(0, path.length() - resourceName.length());
        return new File(base).getParent();
    }

    @Test
    public void testAppend() {
        String base = getMavenTargetDirectory() + "/test-standbox/fs-tests";
        new File(base).mkdirs();
        String file = base + "/append-file.txt";
        doAsync(() -> {
            AFileFactory files = new FileFactory().export();
            final ASupplier<Void> appendToFile = () -> aTry(files.openOutput(file, true)).run(
                    this::writeSmallData
            );
            return aSeq(
                    () -> aTry(files.openOutput(file)).run(t -> aVoid())
            ).thenDo(
                    appendToFile
            ).thenDoLast(
                    appendToFile
            );
        });
        assertEquals(SMALL_DATA.length * 2L, new File(file).length());
    }

    private Promise<Void> writeSmallData(AOutput<ByteBuffer> f) {
        return f.write(ByteBuffer.wrap(SMALL_DATA));
    }

    @Test
    public void testWrite() {
        String base = getMavenTargetDirectory() + "/test-standbox/fs-tests";
        new File(base).mkdirs();
        String file = base + "/write-file.txt";
        doAsync(() -> {
            AFileFactory files = new FileFactory().export();
            return aSeq(
                    () -> aTry(files.openOutput(file)).run(this::writeSmallData)
            ).thenDoLast(
                    () -> aTry(files.openOutput(file)).run(
                            f -> writeSmallData(f).thenGet(() -> writeSmallData(f)))
            );
        });
        assertEquals(SMALL_DATA.length * 2L, new File(file).length());
    }

}
