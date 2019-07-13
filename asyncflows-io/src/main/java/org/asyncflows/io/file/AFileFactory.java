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

package org.asyncflows.io.file;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;

import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

/**
 * File service.
 */
@Asynchronous
public interface AFileFactory {
    /**
     * Open file.
     *
     * @param path    the file
     * @param options the open options
     * @param attrs   the attributes
     * @return the promise for the file
     */
    Promise<ARandomAccessFile> openRandomAccess(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs);

    /**
     * Open file.
     *
     * @param path the path
     * @param mode the mode
     * @return the file
     */
    Promise<ARandomAccessFile> openRandomAccess(String path, String mode);

    /**
     * Open input.
     *
     * @param path    the path
     * @param options the open options
     * @return the promise for input
     */
    Promise<AInput<ByteBuffer>> openInput(Path path, Set<? extends OpenOption> options);

    /**
     * Open input.
     *
     * @param path the path
     * @return the promise for input
     */
    Promise<AInput<ByteBuffer>> openInput(String path);

    /**
     * Open output. Note: {@link java.nio.file.StandardOpenOption#APPEND} is supported via emulation,
     * and might fail if there is concurrent access to file.
     *
     * @param path    the path
     * @param options the open options
     * @return the promise for input
     */
    Promise<AOutput<ByteBuffer>> openOutput(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs);

    /**
     * Open output. Note: append flag is supported via emulation,
     * and might fail if there is concurrent access to file.
     *
     * @param path   the path
     * @param append if true, append mode
     * @return the promise for input
     */
    Promise<AOutput<ByteBuffer>> openOutput(String path, boolean append);

    /**
     * Open output.
     *
     * @param path the path
     * @return the promise for input
     */
    default Promise<AOutput<ByteBuffer>> openOutput(String path) {
        return openOutput(path, false);
    }
}
