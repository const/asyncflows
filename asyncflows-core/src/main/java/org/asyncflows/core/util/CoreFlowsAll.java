package org.asyncflows.core.util;

import org.asyncflows.core.AsyncContext;
import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.data.Tuple3;
import org.asyncflows.core.data.Tuple4;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AFunction2;
import org.asyncflows.core.function.AFunction3;
import org.asyncflows.core.function.AFunction4;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ARunner;
import org.asyncflows.core.function.ASupplier;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aResolver;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * Asynchronous control utilities.
 */
public class CoreFlowsAll {
    /**
     * The daemon thread runner.
     */
    private static final ARunner DAEMON_RUNNER = new ARunner() {
        @Override
        public <A> Promise<A> start(ASupplier<A> t) {
            return aLater(t, Vats.daemonVat());
        }
    };

    /**
     * Private constructor for utility class.
     */
    private CoreFlowsAll() {
    }

    /**
     * The all operator. It allows to start two or more activities in interleaving way. The activities could finish
     * with different types.
     * <pre>
     * {@code aAll(()->aValue("The answer is ")).and(()->aLater(()->aValue(42)))}
     * </pre>
     *
     * @param start the start
     * @param <T>   the initial type
     * @return the builder for the all operator
     */
    public static <T> AllBuilder<T> aAll(final ASupplier<T> start) {
        return aPar(start, CoreFlows::aNow);
    }

    /**
     * The all operator. It allows to start two or more activities in interleaving way. The activities could finish
     * with different types.
     *
     * <pre>
     * {@code aPar(()->aValue("The answer is ")).and(()->aLater(()->aValue(42)), runner)}
     * </pre>
     *
     * @param start      the start
     * @param bodyRunner the runner for the body
     * @param <T>        the initial type
     * @return the builder for the all operator
     */
    public static <T> AllBuilder<T> aPar(final ASupplier<T> start, ARunner bodyRunner) {
        return AsyncContext.withDefaultContext((runner, executor) ->
                new AllBuilder<>(new AllContext(executor, runner, bodyRunner), start));
    }

    /**
     * The all operator. It allows to start two or more activities in interleaving way. The activities could finish
     * with different types. The method runs bodies on individual daemon vats.
     *
     * <pre>
     * {@code aPar(()->aValue("The answer is ")).and(()->aLater(()->aValue(42)), runner)}
     * </pre>
     *
     * @param start the start
     * @param <T>   the initial type
     * @return the builder for the all operator
     */
    public static <T> AllBuilder<T> aPar(final ASupplier<T> start) {
        return aPar(start, DAEMON_RUNNER);
    }


    /**
     * Iterate using iterator.
     *
     * @param iterator  the iterator
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector to gather results
     * @param <T>       the element type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aParForCollect(Iterator<T> iterator, AFunction<T, R> body,
                                                         Collector<R, I, C> collector) {
        return aParForCollect(iterator, body, collector, DAEMON_RUNNER);
    }


    /**
     * Iterate using iterable.
     *
     * @param iterable  the iterable
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector to gather results
     * @param <T>       the element type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aParForCollect(Iterable<T> iterable, AFunction<T, R> body,
                                                         Collector<R, I, C> collector) {
        return aParForCollect(iterable.iterator(), body, collector);
    }

    /**
     * Iterate using stream.
     *
     * @param stream    the stream
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector to gather results
     * @param <T>       the element type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aParForCollect(Stream<T> stream, AFunction<T, R> body,
                                                         Collector<R, I, C> collector) {
        return aParForCollect(stream.iterator(), body, collector);
    }


    /**
     * Iterate using iterator.
     *
     * @param iterator  the iterator
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector to gather results
     * @param <T>       the element type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aAllForCollect(Iterator<T> iterator, AFunction<T, R> body,
                                                         Collector<R, I, C> collector) {
        return aParForCollect(iterator, body, collector, CoreFlows::aNow);
    }


    /**
     * Iterate using iterable.
     *
     * @param iterable  the iterable
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector to gather results
     * @param <T>       the element type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aAllForCollect(Iterable<T> iterable, AFunction<T, R> body,
                                                         Collector<R, I, C> collector) {
        return aAllForCollect(iterable.iterator(), body, collector);
    }

    /**
     * Iterate using stream.
     *
     * @param stream    the stream
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector to gather results
     * @param <T>       the element type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aAllForCollect(Stream<T> stream, AFunction<T, R> body,
                                                         Collector<R, I, C> collector) {
        return aAllForCollect(stream.iterator(), body, collector);
    }


    /**
     * Iterate using iterator.
     *
     * @param iterator   the iterator
     * @param body       the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector  the collector to gather results
     * @param bodyRunner the runner for body
     * @param <T>        the element type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aParForCollect(Iterator<T> iterator, AFunction<T, R> body,
                                                         Collector<R, I, C> collector, ARunner bodyRunner) {
        return AsyncContext.withDefaultContext((runner, executor) -> runner.start(() -> aResolver(resolver -> {
            // TODO use more optimal strategies basing on java.util.stream.Collector.Characteristics
            final I accumulator = collector.supplier().get();
            final AFunction2<Promise<R>, Promise<Void>, Void> merge = (bodyPromise, cyclePromise) ->
                    cyclePromise.flatMapOutcome(po -> bodyPromise.flatMapOutcome(to -> {
                        if (po.isFailure()) {
                            if (to.isFailure()) {
                                po.failure().addSuppressed(to.failure());
                            }
                            return aFailure(po.failure());
                        } else {
                            if (to.isFailure()) {
                                return aFailure(to.failure());
                            }
                            collector.accumulator().accept(accumulator, to.value());
                            return aVoid();
                        }
                    }));
            Promise<Void> mergeNext = aVoid();
            while (true) {
                try {
                    if (iterator.hasNext()) {
                        T value = iterator.next();
                        mergeNext = merge.apply(bodyRunner.start(() -> body.apply(value)), mergeNext);
                    } else {
                        break;
                    }
                } catch (Throwable t) {
                    final Promise<Void> previous = mergeNext;
                    mergeNext = aNow(() -> merge.apply(CoreFlows.<R>aFailure(t), previous));
                    break;
                }
            }
            mergeNext.thenGet(() -> collector.finisher().apply(accumulator)).listenSync(resolver);
        })));
    }


    /**
     * Check if there are failures among outcomes, and notify resolver in that case.
     *
     * @param resolver the resolver
     * @param outcomes the outcomes
     * @param <T>      the type
     * @return true if some failures are detected
     */
    private static <T> boolean notifyIfFailed(AResolver<T> resolver, Outcome<?>... outcomes) {
        Throwable result = null;
        for (Outcome<?> outcome : outcomes) {
            if (outcome.isFailure()) {
                if (result == null) {
                    result = outcome.failure();
                } else {
                    if(result != outcome.failure()) {
                        result.addSuppressed(outcome.failure());
                    }
                }
            }
        }
        if (result != null) {
            Outcome.notifyFailure(resolver, result);
            return true;
        } else {
            return false;
        }

    }

    /**
     * The builder for all.
     *
     * @param <T1> the first action type
     */
    public static final class AllBuilder<T1> {
        /**
         * The context.
         */
        private final AllContext context;
        /**
         * The first action.
         */
        private final ASupplier<T1> action;


        /**
         * The constructor.
         *
         * @param context the context.
         * @param action  the first action
         */
        private AllBuilder(AllContext context, final ASupplier<T1> action) {
            this.context = context;
            this.action = action;
        }

        /**
         * Add new branch.
         *
         * @param otherAction other action
         * @param <T2>        the other type
         * @return the builder for the tuple
         */
        public <T2> AllBuilder2<T1, T2> and(final ASupplier<T2> otherAction) {
            return new AllBuilder2<>(context, action, otherAction);
        }

        /**
         * Add new branch.
         *
         * @param otherAction other action
         * @param <T2>        the second type
         * @return the promise for the tuple
         */
        public <T2> Promise<Tuple2<T1, T2>> andLast(final ASupplier<T2> otherAction) {
            return and(otherAction).toTuple();
        }

        /**
         * The builder for the next action.
         *
         * @param <T1> the first type
         * @param <T2> the second type
         */
        public static final class AllBuilder2<T1, T2> {
            /**
             * The context.
             */
            private final AllContext context;
            /**
             * The first action.
             */
            private final ASupplier<T1> action1;
            /**
             * The second action.
             */
            private final ASupplier<T2> action2;

            /**
             * The constructor.
             *
             * @param context the context
             * @param action1 the first action
             * @param action2 the second action
             */
            private AllBuilder2(final AllContext context, final ASupplier<T1> action1, final ASupplier<T2> action2) {
                this.context = context;
                this.action1 = action1;
                this.action2 = action2;
            }

            /**
             * Map result.
             *
             * @param function the mapper function
             * @param <R>      the result
             * @return the promise for result
             */
            public <R> Promise<R> map(AFunction2<T1, T2, R> function) {
                AllContext ctx = this.context;
                return ctx.getOperatorRunner().start(() -> {
                    final Promise<T1> p1 = ctx.getBodyRunner().start(action1);
                    final Promise<T2> p2 = ctx.getBodyRunner().start(action2);
                    return aResolver(resolver -> {
                        p1.listenSync(r1 -> p2.listen(r2 -> {
                            if (notifyIfFailed(resolver, r1, r2)) {
                                return;
                            }
                            aNow(() -> function.apply(r1.value(), r2.value())).listenSync(resolver);
                        }, ctx.getExecutor()));
                    });
                });
            }


            /**
             * @return start executing the bodies and resolve to the tuple
             */
            public Promise<Tuple2<T1, T2>> toTuple() {
                return map((t1, t2) -> aValue(Tuple2.of(t1, t2)));
            }

            /**
             * Add new branch.
             *
             * @param otherAction other action
             * @param <T3>        the other type
             * @return the builder for the tuple
             */
            public <T3> AllBuilder3<T1, T2, T3> and(final ASupplier<T3> otherAction) {
                return new AllBuilder3<>(context, action1, action2, otherAction);
            }

            /**
             * @return the value of the first branch (ignore all others)
             */
            public Promise<T1> selectValue1() {
                return map((t1, t2) -> aValue(t1));
            }

            /**
             * @return the value of the second branch (ignore all others)
             */
            public Promise<T2> selectValue2() {
                return map((t1, t2) -> aValue(t2));
            }

            /**
             * Add new branch and finish it.
             *
             * @param otherAction other action
             * @param <T3>        the third type
             * @return the promise for the tuple
             */
            public <T3> Promise<Tuple3<T1, T2, T3>> andLast(final ASupplier<T3> otherAction) {
                return and(otherAction).toTuple();
            }
        }

        /**
         * The builder for the next action.
         *
         * @param <T1> the first type
         * @param <T2> the second type
         * @param <T3> the third type
         */
        public static final class AllBuilder3<T1, T2, T3> {
            private final AllContext context;
            /**
             * The first action.
             */
            private final ASupplier<T1> action1;
            /**
             * The second action.
             */
            private final ASupplier<T2> action2;
            /**
             * The third action.
             */
            private final ASupplier<T3> action3;

            /**
             * The constructor.
             *
             * @param context the context
             * @param action1 the first action
             * @param action2 the second action
             * @param action3 the third action
             */
            private AllBuilder3(final AllContext context, final ASupplier<T1> action1, final ASupplier<T2> action2, final ASupplier<T3> action3) {
                this.context = context;
                this.action1 = action1;
                this.action2 = action2;
                this.action3 = action3;
            }

            /**
             * Map result.
             *
             * @param function the mapper function
             * @param <R>      the result
             * @return the promise for result
             */
            public <R> Promise<R> map(AFunction3<T1, T2, T3, R> function) {
                AllContext ctx = this.context;
                return ctx.getOperatorRunner().start(() -> {
                    final Promise<T1> p1 = ctx.getBodyRunner().start(action1);
                    final Promise<T2> p2 = ctx.getBodyRunner().start(action2);
                    final Promise<T3> p3 = ctx.getBodyRunner().start(action3);
                    return aResolver(resolver -> {
                        p1.listenSync(r1 -> p2.listenSync(r2 -> p3.listen(r3 -> {
                            if (notifyIfFailed(resolver, r1, r2, r3)) {
                                return;
                            }
                            aNow(() -> function.apply(r1.value(), r2.value(), r3.value())).listenSync(resolver);
                        }, ctx.getExecutor())));
                    });
                });
            }


            /**
             * @return start executing the bodies and resolve to the tuple
             */
            public Promise<Tuple3<T1, T2, T3>> toTuple() {
                return map((t1, t2, t3) -> aValue(Tuple3.of(t1, t2, t3)));
            }

            /**
             * Add new branch.
             *
             * @param otherAction other action
             * @param <T4>        the other type
             * @return the builder for the tuple
             */
            public <T4> AllBuilder4<T1, T2, T3, T4> and(final ASupplier<T4> otherAction) {
                return new AllBuilder4<>(context, action1, action2, action3, otherAction);
            }

            /**
             * @return the value of the first branch (ignore all others)
             */
            public Promise<T1> selectValue1() {
                return map((t1, t2, t3) -> aValue(t1));
            }

            /**
             * @return the value of the second branch (ignore all others)
             */
            public Promise<T2> selectValue2() {
                return map((t1, t2, t3) -> aValue(t2));
            }

            /**
             * @return the value of the third branch (ignore all others)
             */
            public Promise<T3> selectValue3() {
                return map((t1, t2, t3) -> aValue(t3));
            }

            /**
             * Add new branch and finish it.
             *
             * @param otherAction other action
             * @param <T4>        the forth type
             * @return the promise for the tuple
             */
            public <T4> Promise<Tuple4<T1, T2, T3, T4>> andLast(final ASupplier<T4> otherAction) {
                return and(otherAction).toTuple();
            }

        }
    }


    /**
     * The builder for the next action.
     *
     * @param <T1> the first type
     * @param <T2> the second type
     * @param <T3> the third type
     */
    public static final class AllBuilder4<T1, T2, T3, T4> {
        /**
         * The context.
         */
        private final AllContext context;
        /**
         * The first action.
         */
        private final ASupplier<T1> action1;
        /**
         * The second action.
         */
        private final ASupplier<T2> action2;
        /**
         * The third action.
         */
        private final ASupplier<T3> action3;
        /**
         * The forth action.
         */
        private final ASupplier<T4> action4;

        /**
         * The constructor.
         *
         * @param context the context
         * @param action1 the first action
         * @param action2 the second action
         * @param action3 the third action
         */
        private AllBuilder4(final AllContext context, final ASupplier<T1> action1, final ASupplier<T2> action2, final ASupplier<T3> action3, final ASupplier<T4> action4) {
            this.context = context;
            this.action1 = action1;
            this.action2 = action2;
            this.action3 = action3;
            this.action4 = action4;
        }

        /**
         * Map result.
         *
         * @param function the mapper function
         * @param <R>      the result
         * @return the promise for result
         */
        public <R> Promise<R> map(AFunction4<T1, T2, T3, T4, R> function) {
            AllContext ctx = this.context;
            return ctx.getOperatorRunner().start(() -> {
                final Promise<T1> p1 = ctx.getBodyRunner().start(action1);
                final Promise<T2> p2 = ctx.getBodyRunner().start(action2);
                final Promise<T3> p3 = ctx.getBodyRunner().start(action3);
                final Promise<T4> p4 = ctx.getBodyRunner().start(action4);
                return aResolver(resolver -> {
                    p1.listenSync(r1 -> p2.listenSync(r2 -> p3.listenSync(r3 -> p4.listen(r4 -> {
                        if (notifyIfFailed(resolver, r1, r2, r3, r4)) {
                            return;
                        }
                        aNow(() -> function.apply(r1.value(), r2.value(), r3.value(), r4.value())).listenSync(resolver);
                    }, ctx.getExecutor()))));
                });
            });
        }


        /**
         * @return start executing the bodies and resolve to the tuple
         */
        public Promise<Tuple4<T1, T2, T3, T4>> toTuple() {
            return map((t1, t2, t3, t4) -> aValue(Tuple4.of(t1, t2, t3, t4)));
        }

        /**
         * @return the value of the first branch (ignore all others)
         */
        public Promise<T1> selectValue1() {
            return map((t1, t2, t3, t4) -> aValue(t1));
        }

        /**
         * @return the value of the second branch (ignore all others)
         */
        public Promise<T2> selectValue2() {
            return map((t1, t2, t3, t4) -> aValue(t2));
        }

        /**
         * @return the value of the third branch (ignore all others)
         */
        public Promise<T3> selectValue3() {
            return map((t1, t2, t3, t4) -> aValue(t3));
        }

        /**
         * @return the value of the forth branch (ignore all others)
         */
        public Promise<T4> selectValue4() {
            return map((t1, t2, t3, t4) -> aValue(t4));
        }
    }

    /**
     * The context for all operator.
     */
    public static class AllContext {
        /**
         * Executor.
         */
        private final Executor executor;
        /**
         * Operator runner.
         */
        private final ARunner operatorRunner;
        /**
         * Body runner.
         */
        private final ARunner bodyRunner;

        /**
         * Constructor.
         *
         * @param executor       the executor.
         * @param operatorRunner the operator runner
         * @param bodyRunner     the body runner
         */
        public AllContext(Executor executor, ARunner operatorRunner, ARunner bodyRunner) {
            this.executor = executor;
            this.operatorRunner = operatorRunner;
            this.bodyRunner = bodyRunner;
        }

        /**
         * @return the executor.
         */
        public Executor getExecutor() {
            return executor;
        }

        /**
         * @return the runner for operator (including map block)
         */
        public ARunner getOperatorRunner() {
            return operatorRunner;
        }

        /**
         * @return the runner for bodies for operator
         */
        public ARunner getBodyRunner() {
            return bodyRunner;
        }
    }
}
