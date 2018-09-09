package org.asyncflows.io.util;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.RequestQueue;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static org.asyncflows.io.IOUtil.isEof;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;

/**
 * The input that forwards requests further and digests the input.
 */
public class DigestingInput extends AbstractDigestingStream<AInput<ByteBuffer>>
        implements AInput<ByteBuffer>, NeedsExport<AInput<ByteBuffer>> {
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
        final Promise<byte[]> rc = new Promise<>();
        final ByteBuffer buffer = ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
        return IOUtil.BYTE.discard(digestInput(input, rc.resolver()).using(digest), buffer).thenPromise(rc);
    }

    @Override
    public Promise<Integer> read(final ByteBuffer buffer) {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        final int positionBeforeRead = buffer.position();
        final Promise<Integer> read = aNow(() -> wrapped.read(buffer));
        return requests.run(() -> read.listen(outcomeChecker()).flatMap(value -> {
            if (isEof(value)) {
                finishDigesting();
            } else {
                updateDigest(buffer, positionBeforeRead);
            }
            return aValue(value);
        }));
    }

    @Override
    public AInput<ByteBuffer> export(final Vat vat) {
        return IOExportUtil.export(vat, this);
    }
}
