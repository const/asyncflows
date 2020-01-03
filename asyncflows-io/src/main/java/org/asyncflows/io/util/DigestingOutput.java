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

package org.asyncflows.io.util;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CoreFlowsResource;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.AOutputProxyFactory;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The digesting output.
 */
public class DigestingOutput extends AbstractDigestingStream<AOutput<ByteBuffer>>
        implements AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
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

    /**
     * Generate data for output in action, then close the stream producing the digest.
     *
     * @param output    the output to generate to
     * @param algorithm the algorithm
     * @param action    the action
     * @return the promise for digest
     */
    public static Promise<byte[]> generateDigested(final AOutput<ByteBuffer> output, final String algorithm,
                                                   final AFunction<AOutput<ByteBuffer>, Void> action) {
        final Promise<byte[]> result = new Promise<>();
        return CoreFlowsResource.aTryResource(
                digestOutput(output, result.resolver()).using(algorithm)
        ).run(
                action
        ).thenPromise(result);
    }

    /**
     * Generate digested.
     *
     * @param output    the output to generate to
     * @param algorithm the algorithm
     * @param action    the action
     * @return the promise for digest
     */
    public static Promise<byte[]> generateDigested(final Promise<AOutput<ByteBuffer>> output, final String algorithm,
                                                   final AFunction<AOutput<ByteBuffer>, Void> action) {
        return output.flatMap(output1 -> generateDigested(output1, algorithm, action));
    }

    @Override
    public Promise<Void> write(final ByteBuffer buffer) {
        final int positionBeforeWrite = buffer.position();
        final Promise<Void> write = aNow(() -> wrapped.write(buffer));
        return requests.run(() -> write.listen(outcomeChecker()).thenFlatGet(() -> {
            updateDigest(buffer, positionBeforeWrite);
            return aVoid();
        }));
    }

    @Override
    public Promise<Void> flush() {
        return wrapped.flush();
    }

    @Override
    public AOutput<ByteBuffer> export(final Vat vat) {
        return AOutputProxyFactory.createProxy(vat, this);
    }

}
