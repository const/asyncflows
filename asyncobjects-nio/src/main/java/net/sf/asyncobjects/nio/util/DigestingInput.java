package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;

/**
 * The input that forwards requests further and digests the input.
 */
public class DigestingInput extends AbstractDigestingStream<AInput<ByteBuffer>>
        implements AInput<ByteBuffer>, ExportsSelf<AInput<ByteBuffer>> {
    /**
     * The request queue.
     */
    private final RequestQueue requests = new RequestQueue();

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped  the underlying object
     * @param resolver the resolver for digest
     * @param digest   the message digest
     */
    protected DigestingInput(final AInput<ByteBuffer> wrapped, final AResolver<byte[]> resolver,
                             final MessageDigest digest) {
        super(wrapped, resolver, digest);
    }

    /**
     * Create a digest factory that digests input using the algorithm specified as the next call.
     *
     * @param input    the input
     * @param resolver the resolver
     * @return the digested input factory
     */
    public static DigestFactory<AInput<ByteBuffer>> digestInput(final AInput<ByteBuffer> input,
                                                                final AResolver<byte[]> resolver) {
        return new DigestFactory<AInput<ByteBuffer>>(input, resolver) {
            @Override
            protected AInput<ByteBuffer> make(final AInput<ByteBuffer> digestedStream,
                                              final AResolver<byte[]> resolver,
                                              final MessageDigest digest) {
                return new DigestingInput(digestedStream, resolver, digest).export();
            }
        };
    }

    /**
     * Digest the input and discard.
     *
     * @param input  the input stream
     * @param digest the digest name
     * @return the digest
     */
    public static Promise<byte[]> digestAndDiscardInput(final AInput<ByteBuffer> input, final String digest) {
        final Promise<byte[]> rc = new Promise<byte[]>();
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        return IOUtil.BYTE.discard(digestInput(input, rc.resolver()).using(digest), buffer)
                .thenDo(promiseCallable(rc));
    }

    @Override
    public Promise<Integer> read(final ByteBuffer buffer) {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        final int positionBeforeRead = buffer.position();
        final Promise<Integer> read = aNow(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                return wrapped.read(buffer);
            }
        });
        return requests.run(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                return read.observe(outcomeChecker()).map(new AFunction<Integer, Integer>() {
                    @Override
                    public Promise<Integer> apply(final Integer value) throws Throwable {
                        if (value < 0) {
                            finishDigesting();
                        } else {
                            updateDigest(buffer, positionBeforeRead);
                        }
                        return aValue(value);
                    }
                });
            }
        });
    }

    @Override
    public AInput<ByteBuffer> export() {
        return export(Vat.current());
    }

    @Override
    public AInput<ByteBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
