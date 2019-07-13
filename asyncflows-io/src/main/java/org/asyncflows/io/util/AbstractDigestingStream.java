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

package org.asyncflows.io.util;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.ChainedClosable;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * A base class for digesting stream.
 *
 * @param <S> the stream type
 */
public abstract class AbstractDigestingStream<S extends ACloseable> extends ChainedClosable<S> {
    /**
     * The MD5 digest name.
     */
    public static final String MD5 = "MD5";
    /**
     * The SHA-256 digest name.
     */
    public static final String SHA_256 = "SHA-256";
    /**
     * The SHA-1 digest name.
     */
    public static final String SHA_1 = "SHA-1";
    /**
     * The message digest to use.
     */
    private final MessageDigest digest;
    /**
     * The digest resolver.
     */
    private AResolver<byte[]> digestResolver;

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
            return using(SHA_1);
        }

        /**
         * @return stream digested using SHA-256
         */
        public final S sha256() {
            return using(SHA_256);
        }

        /**
         * @return stream digested using MD5
         */
        public final S md5() {
            return using(MD5);
        }
    }
}
