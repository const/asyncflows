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

package org.asyncflows.io.file.nio;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.file.AFileFactory;
import org.asyncflows.io.file.AFileFactoryProxyFactory;
import org.asyncflows.io.file.ARandomAccessFile;
import org.asyncflows.io.file.FileOpenException;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.asyncflows.core.CoreFlows.aValue;

/**
 * Factory for asynchronous files.
 */
public class FileFactory implements AFileFactory, NeedsExport<AFileFactory> {
    /**
     * Default write options
     */
    private static final Set<OpenOption> DEFAULT_WRITE_OPTIONS = Stream.of(
            StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
    ).collect(Collectors.toSet());
    /**
     * Default append options
     */
    private static final Set<OpenOption> DEFAULT_APPEND_OPTIONS = Stream.of(
            StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND
    ).collect(Collectors.toSet());
    /**
     * The executor service
     */
    private ExecutorService executorService = Vats.DAEMON_EXECUTOR;

    /**
     * Set executor service.
     *
     * @param executorService the service
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public Promise<ARandomAccessFile> openRandomAccess(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
        return aValue(openFile(path, options, attrs).export());
    }

    @Override
    public Promise<ARandomAccessFile> openRandomAccess(String path, String mode) {
        Set<OpenOption> options;
        Objects.requireNonNull(mode);
        switch (mode) {
            case "r":
                options = Collections.singleton(StandardOpenOption.READ);
                break;
            case "rw":
                options = Stream.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE).collect(Collectors.toSet());
                break;
            case "rws":
                options = Stream.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE, StandardOpenOption.SYNC).collect(Collectors.toSet());
                break;
            case "rwd":
                options = Stream.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE, StandardOpenOption.DSYNC).collect(Collectors.toSet());
                break;
            default:
                throw new FileOpenException("Invalid open mode '" + mode + "' for file " + path);
        }
        return openRandomAccess(new File(path).toPath(), options);
    }

    private RandomAccessFile openFile(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
        try {
            final AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, options, executorService, attrs);
            return new RandomAccessFile(channel, executorService);
        } catch (Exception ex) {
            throw new FileOpenException("Opening " + path.toString(), ex);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Promise<AInput<ByteBuffer>> openInput(Path path, Set<? extends OpenOption> options) {
        Set<OpenOption> o;
        if (options == null || options.isEmpty()) {
            o = Collections.singleton(StandardOpenOption.READ);
        } else if (!options.contains(StandardOpenOption.READ)) {
            o = new HashSet<>(options);
            o.add(StandardOpenOption.READ);
        } else {
            o = (Set<OpenOption>) options;
        }
        return aValue(new FileInput(openFile(path, o)).export());
    }

    /**
     * Open input.
     *
     * @param path the path
     * @return the promise for input
     */
    @Override
    public Promise<AInput<ByteBuffer>> openInput(String path) {
        return openInput(new File(path).toPath(), null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Promise<AOutput<ByteBuffer>> openOutput(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
        Set<OpenOption> o;
        if (options == null || options.isEmpty()) {
            o = DEFAULT_WRITE_OPTIONS;
        } else if (Stream.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND,
                StandardOpenOption.CREATE, StandardOpenOption.CREATE_NEW).noneMatch(options::contains)) {
            o = new HashSet<>(options);
            o.addAll(DEFAULT_WRITE_OPTIONS);
        } else {
            o = (Set<OpenOption>) options;
        }
        boolean append = false;
        if (o.contains(StandardOpenOption.APPEND)) {
            o = new HashSet<>(o);
            o.remove(StandardOpenOption.APPEND);
            append = true;
        }
        final RandomAccessFile file = openFile(path, o, attrs);
        return aValue(new FileOutput(file, append).export());
    }

    @Override
    public Promise<AOutput<ByteBuffer>> openOutput(String path, boolean append) {
        return openOutput(new File(path).toPath(), append ? DEFAULT_APPEND_OPTIONS : DEFAULT_WRITE_OPTIONS);
    }

    @Override
    public AFileFactory export(Vat vat) {
        return AFileFactoryProxyFactory.createProxy(vat, this);
    }
}
