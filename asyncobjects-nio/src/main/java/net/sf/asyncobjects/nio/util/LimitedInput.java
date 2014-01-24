package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.Buffer;

import static net.sf.asyncobjects.nio.IOUtil.isEof;

/**
 * The stream that is limited by the specified size, the {@link #close()} does not close underlying stream.
 *
 * @param <B> the buffer type
 */
public class LimitedInput<B extends Buffer>
        extends CloseableInvalidatingBase implements AInput<B>, ExportsSelf<AInput<B>> {
    /**
     * Read requests.
     */
    private final RequestQueue reads = new RequestQueue();
    /**
     * the input stream.
     */
    private final AInput<B> input;
    /**
     * The limit.
     */
    private final long limit;
    /**
     * The read amount.
     */
    private long readAmount;

    /**
     * The constructor for the stream.
     *
     * @param input the input stream
     * @param limit the limit
     */
    protected LimitedInput(final AInput<B> input, final long limit) {
        this.input = input;
        this.limit = limit;
    }

    /**
     * Return limited stream.
     *
     * @param input the stream to limit
     * @param limit the limit
     * @param <B>   the buffer type
     * @return the limited stream
     */
    public static <B extends Buffer> AInput<B> limit(final AInput<B> input, final long limit) {
        return new LimitedInput<B>(input, limit).export();
    }

    @Override
    public Promise<Integer> read(final B buffer) {
        return reads.run(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                ensureValidAndOpen();
                if (readAmount > limit) {
                    throw new IllegalStateException("Stream has read too much!");
                }
                if (readAmount == limit) {
                    return IOUtil.EOF_PROMISE;
                }
                final int savedLimit = buffer.limit();
                if (limit - readAmount < buffer.remaining()) {
                    buffer.limit(savedLimit - buffer.remaining() + (int) (limit - readAmount));
                }
                return input.read(buffer).observe(new AResolver<Integer>() {
                    @Override
                    public void resolve(final Outcome<Integer> resolution) throws Throwable {
                        if (resolution.isSuccess() && !isEof(resolution.value())) {
                            readAmount += resolution.value();
                        }
                        buffer.limit(savedLimit);
                    }
                });
            }
        }).observe(outcomeChecker());
    }

    @Override
    public AInput<B> export() {
        return export(Vat.current());
    }

    @Override
    public AInput<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
