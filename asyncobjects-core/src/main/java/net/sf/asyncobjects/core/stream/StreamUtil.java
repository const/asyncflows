package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.data.Maybe;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.CoreFunctionUtil.booleanCallable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResourceAction;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * The stream utilities.
 */
public final class StreamUtil {
    /**
     * The private constructor for utility class.
     */
    private StreamUtil() {
    }

    /**
     * The producer for the stream.
     *
     * @param stream the stream.
     * @param <O>    the stream element type
     * @return the callable that invokes a read operation
     */
    public static <O> ACallable<Maybe<O>> producerFromStream(final AStream<O> stream) {
        return new ACallable<Maybe<O>>() {
            @Override
            public Promise<Maybe<O>> call() throws Throwable {
                return stream.next();
            }
        };
    }

    /**
     * Copy the stream to sink and then close the stream in any case.
     *
     * @param stream the stream to copy
     * @param sink   the target sink
     * @param <O>    the element type
     * @return the amount of elements copied
     */
    public static <O> Promise<Long> connect(final AStream<O> stream, final ASink<? super O> sink) {
        final long[] count = new long[1];
        final Cell<Outcome<Void>> stopped = new Cell<Outcome<Void>>();
        sink.finished().listen(new AResolver<Void>() {
            @Override
            public void resolve(final Outcome<Void> resolution) throws Throwable {
                stream.close();
                stopped.setValue(resolution);
            }
        });
        return aSeq(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aSeqLoop(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        if (!stopped.isEmpty()) {
                            return aFalse();
                        }
                        final Promise<Maybe<O>> next = aNow(StreamUtil.producerFromStream(stream));
                        return next.mapOutcome(new AFunction<Boolean, Outcome<Maybe<O>>>() {
                            @Override
                            public Promise<Boolean> apply(final Outcome<Maybe<O>> value) throws Throwable {
                                try {
                                    if (value.isSuccess()) {
                                        if (value.value().isEmpty()) {
                                            return sink.close().thenDo(booleanCallable(false));
                                        } else {
                                            if (!stopped.isEmpty()) {
                                                return aFalse();
                                            }
                                            count[0]++;
                                            return sink.put(value.value().value()).thenDo(booleanCallable(true));
                                        }
                                    } else {
                                        return sink.fail(value.failure()).thenDo(
                                                CoreFunctionUtil.<Boolean>failureCallable(value.failure()));
                                    }
                                } catch (Throwable problem) {
                                    return sink.fail(problem).thenDo(
                                            CoreFunctionUtil.<Boolean>failureCallable(problem));
                                }
                            }
                        });
                    }
                });
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return stopped.isEmpty() ? aVoid() : Promise.forOutcome(stopped.getValue());
            }
        }).thenDo(new ACallable<Long>() {
            @Override
            public Promise<Long> call() throws Throwable {
                return aSuccess(count[0]);
            }
        }).finallyDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aAll(closeResourceAction(sink)).andLast(closeResourceAction(stream)).toVoid();
            }
        });
    }

}
