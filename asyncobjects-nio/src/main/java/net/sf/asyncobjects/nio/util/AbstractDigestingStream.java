package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.ChainedClosable;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * A base class for digesting stream.
 *
 * @param <S> the stream type
 */
public abstract class AbstractDigestingStream<S extends ACloseable> extends ChainedClosable<S> {
    /**
     * The digest resolver.
     */
    private AResolver<byte[]> digestResolver;
    /**
     * The message digest to use.
     */
    private final MessageDigest digest;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped        the underlying object
     * @param digestResolver the digest resolver
     * @param digest         the message digest
     */
    protected AbstractDigestingStream(final S wrapped, final AResolver<byte[]> digestResolver,
                                      final MessageDigest digest) {
        super(wrapped);
        this.digestResolver = digestResolver;
        this.digest = digest;
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        if (digestResolver != null) {
            notifyFailure(digestResolver, throwable);
        }
    }

    /**
     * Update digest according to the change in the position with buffer and revert buffer back to its original state.
     *
     * @param data             the data to update
     * @param previousPosition the previous position
     */
    protected final void updateDigest(final ByteBuffer data, final int previousPosition) {
        final int position = data.position();
        final int limit = data.limit();
        data.limit(position);
        data.position(previousPosition);
        digest.update(data);
        data.position(position);
        data.limit(limit);
    }

    @Override
    protected Promise<Void> beforeClose() {
        finishDigesting();
        return super.beforeClose();
    }

    /**
     * Finish digesting.
     */
    protected final void finishDigesting() {
        if (digestResolver != null) {
            notifySuccess(digestResolver, digest.digest());
            digestResolver = null;
        }
    }

    /**
     * The digest factory that allows selecting a specific digest type.
     *
     * @param <S> the stream type
     */
    public abstract static class DigestFactory<S extends ACloseable> {
        /**
         * The digest resolver.
         */
        private final AResolver<byte[]> digestResolver;
        /**
         * The stream.
         */
        private final S stream;

        /**
         * The digest factory.
         *
         * @param stream         the stream
         * @param digestResolver the resolver
         */
        protected DigestFactory(final S stream, final AResolver<byte[]> digestResolver) {
            this.digestResolver = digestResolver;
            this.stream = stream;
        }

        /**
         * Create the exported stream.
         *
         * @param digestedStream the underlying object
         * @param resolver       the digest resolver
         * @param digest         the message digest
         * @return the exported stream
         */
        protected abstract S make(S digestedStream, AResolver<byte[]> resolver, MessageDigest digest);

        /**
         * Digest stream using the specified message digest.
         *
         * @param digest the digest to use
         * @return the resulting stream.
         */
        public final S using(final MessageDigest digest) {
            return make(stream, digestResolver, digest);
        }

        /**
         * Digest stream using the specified message digest.
         *
         * @param digest the digest algorithm to use
         * @return the resulting stream.
         */
        public final S using(final String digest) {
            try {
                return make(stream, digestResolver, MessageDigest.getInstance(digest));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("Failed to create digest: " + digest, e);
            }
        }

        /**
         * @return stream digested using SHA-1
         */
        public final S sha1() {
            return using("SHA-1");
        }

        /**
         * @return stream digested using SHA-256
         */
        public final S sha256() {
            return using("SHA-256");
        }

        /**
         * @return stream digested using MD5
         */
        public final S md5() {
            return using("SHA-256");
        }
    }
}
