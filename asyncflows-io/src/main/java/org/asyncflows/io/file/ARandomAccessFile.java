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
import org.asyncflows.core.function.ACloseable;

import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

/**
 * Asynchronous file channel that provides random access to the file.
 */
@Asynchronous
public interface ARandomAccessFile extends ACloseable {
    /**
     * @return the file size
     */
    Promise<Long> size();

    /**
     * Truncate file to the specified size.
     *
     * @param newSize new size
     * @return when complete
     */
    Promise<Void> truncate(long newSize);

    /**
     * Force the file to file system.
     *
     * @param metadata true if metadata needs to be updated as well
     * @return when completed
     */
    Promise<Void> force(boolean metadata);

    /**
     * Read data at position.
     *
     * @param buffer   the buffer
     * @param position the position
     * @return amount read
     */
    Promise<Integer> read(ByteBuffer buffer, long position);

    /**
     * Write data at position.
     *
     * @param buffer   the buffer
     * @param position the position
     * @return when complete
     */
    Promise<Void> write(ByteBuffer buffer, long position);

    /**
     * Lock file.
     *
     * @param position the position
     * @param size     the size
     * @param shared   if shared
     * @return the lock
     */
    Promise<FileLock> lock(long position, long size, boolean shared);

    /**
     * Lock file.
     *
     * @return the lock
     */
    Promise<FileLock> lock();

    /**
     * Try locking file.
     *
     * @param position the position
     * @param size     the size
     * @param shared   if shared
     * @return the lock or null
     */
    Promise<FileLock> tryLock(long position, long size, boolean shared);

    /**
     * Try locking file.
     *
     * @return the lock or null
     */
    Promise<FileLock> tryLock();
}
