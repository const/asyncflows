package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The digesting output.
 */
public class DigestingOutput extends AbstractDigestingStream<AOutput<ByteBuffer>>
        implements AOutput<ByteBuffer>, ExportsSelf<AOutput<ByteBuffer>> {
    /**
     * The request queue.
     */
    private final RequestQueue requests = new RequestQueue();

    /**
     * The constructor from the underlying stream.
     *
     * @param wrapped        the underlying object
     * @param digestResolver the digest resolver
     * @param digest         the message digest
     */
    protected DigestingOutput(final AOutput<ByteBuffer> wrapped, final AResolver<byte[]> digestResolver,
                              final MessageDigest digest) {
        super(wrapped, digestResolver, digest);
    }

    @Override
    public Promise<Void> write(final ByteBuffer buffer) {
        final int positionBeforeWrite = buffer.position();
        final Promise<Void> write = aNow(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return wrapped.write(buffer);
            }
        });
        return requests.run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return write.observe(outcomeChecker()).then(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        updateDigest(buffer, positionBeforeWrite);
                        return aVoid();
                    }
                });
            }
        });
    }

    @Override
    public Promise<Void> flush() {
        return wrapped.flush();
    }

    @Override
    public AOutput<ByteBuffer> export() {
        return export(Vat.current());
    }

    @Override
    public AOutput<ByteBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }

    /**
     * Create a digest factory that digests output using the algorithm specified as the next call.
     *
     * @param output   the output
     * @param resolver the resolver
     * @return the digested output factory
     */
    public static DigestFactory<AOutput<ByteBuffer>> digestOutput(final AOutput<ByteBuffer> output,
                                                                  final AResolver<byte[]> resolver) {
        return new DigestFactory<AOutput<ByteBuffer>>(output, resolver) {
            @Override
            protected AOutput<ByteBuffer> make(final AOutput<ByteBuffer> digestedStream,
                                               final AResolver<byte[]> resolver,
                                               final MessageDigest digest) {
                return new DigestingOutput(digestedStream, resolver, digest).export();
            }
        };
    }

}
