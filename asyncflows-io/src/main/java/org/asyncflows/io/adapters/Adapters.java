package org.asyncflows.io.adapters;

import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.core.function.AResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The standard adapters.
 */
public final class Adapters {
    /**
     * The lock for stream creation.
     */
    private static final Object LOCK = new Object();
    /**
     * The standard input stream.
     */
    private static AInput<ByteBuffer> stdin;
    /**
     * The standard input stream.
     */
    private static AOutput<ByteBuffer> stdout;
    /**
     * The standard error stream.
     */
    private static AOutput<ByteBuffer> stderr;

    /**
     * The private constructor for utility class.
     */
    private Adapters() {
    }

    /**
     * @return the standard input stream for the application
     */
    public static AInput<ByteBuffer> getStandardInput() {
        synchronized (LOCK) {
            if (stdin == null) {
                stdin = new InputStreamAdapter(System.in).exportBlocking();
            }
            return stdin;
        }
    }

    /**
     * @return the standard output stream for the application
     */
    public static AOutput<ByteBuffer> getStandardOutput() {
        synchronized (LOCK) {
            if (stdout == null) {
                stdout = new OutputStreamAdapter(System.out).exportBlocking();
            }
            return stdout;
        }
    }

    /**
     * @return the standard output stream for the application
     */
    public static AOutput<ByteBuffer> getStandardError() {
        synchronized (LOCK) {
            if (stderr == null) {
                stderr = new OutputStreamAdapter(System.err).exportBlocking();
            }
            return stderr;
        }
    }

    /**
     * Byte array output stream that is a usual wrapper over {@link ByteArrayOutputStream} except that it notifies
     * a resolver with an resulting array when stream is closed. The stream is exported to the current vat.
     *
     * @param arrayResolver the resolver that resolves with resulting array when steam is closed successfully.
     * @return the output stream.
     */
    public static AOutput<ByteBuffer> getByteArrayOutput(final AResolver<byte[]> arrayResolver) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStreamAdapter adapter = new OutputStreamAdapter(out) {
            @Override
            protected void closeStream(final OutputStream streamToClose) throws IOException {
                notifySuccess(arrayResolver, out.toByteArray());
                super.closeStream(streamToClose);
            }
        };
        return adapter.export();
    }

    /**
     * Byte array input stream.
     *
     * @param input the input stream
     * @return the input over byte array.
     */
    public static AInput<ByteBuffer> getByteArrayInput(final byte[] input) {
        return new InputStreamAdapter(new ByteArrayInputStream(input)).export();
    }

    /**
     * The adapter for string output over {@link StringWriter}.
     *
     * @param stringResolver the resolver that resolves to a string on stream close
     * @return the text output
     */
    public static AOutput<CharBuffer> getStringOutput(final AResolver<String> stringResolver) {
        final StringWriter writer = new StringWriter();
        final WriterAdapter adapter = new WriterAdapter(writer) {
            @Override
            protected void closeStream(final Writer streamToClose) throws IOException {
                notifySuccess(stringResolver, writer.toString());
                super.closeStream(streamToClose);
            }
        };
        return adapter.export();
    }

    /**
     * Get input over string.
     *
     * @param input the input
     * @return the exported input
     */
    public static AInput<CharBuffer> getStringInput(final String input) {
        return new ReaderAdapter(new StringReader(input)).export();
    }

    /**
     * Get resource as stream.
     *
     * @param contextClass the context class
     * @param name         the resource name
     * @return the resource stream
     * @throws FileNotFoundException if resource is not found
     */
    public static AInput<ByteBuffer> getResource(final Class<?> contextClass, final String name)
            throws FileNotFoundException {
        final InputStream stream = contextClass.getResourceAsStream(name);
        if (stream == null) {
            throw new FileNotFoundException("Resource not found: " + name);
        }
        return new InputStreamAdapter(stream).export();
    }
}
