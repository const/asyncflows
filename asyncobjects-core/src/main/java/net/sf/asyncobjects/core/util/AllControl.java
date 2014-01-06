package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The utility class with aAll operators.
 */
public final class AllControl {
    /**
     * Private constructor for utility class.
     */
    private AllControl() {
    }

    /**
     * The all operator. It allows to start two or more activities in interleaving way. The activities could finish
     * with different types.
     * <p/>
     * <pre>{@code
     * aAll(new ACallable<String>() {
     *   public Promise<String> call() throws Throwable {
     *     return aValue("The answer");
     *   }
     * }).andLast(new ACallable<Integer>() {
     *   public Promise<Integer> call() throws Throwable {
     *     return aLater(new ACallable<Integer>() {
     *       public Promise<Integer> call() throws Throwable {
     *         return aValue(42);
     *       }
     *     });
     *   }
     * });}</pre>
     *
     * @param start the start
     * @param <T>   the initial type
     * @return the builder for the all operator
     */
    public static <T> AllBuilder<T> aAll(final ACallable<T> start) {
        return new AllBuilder<T>(start);
    }

    /**
     * The builder for all.
     *
     * @param <T1> the first action type
     */
    public static final class AllBuilder<T1> {
        /**
         * The first action.
         */
        private final ACallable<T1> action;

        /**
         * The constructor.
         *
         * @param action the first action
         */
        public AllBuilder(final ACallable<T1> action) {
            this.action = action;
        }

        /**
         * Add new branch.
         *
         * @param otherAction other action
         * @param <T2>        the other type
         * @return the builder for the tuple
         */
        public <T2> AllBuilder2<T1, T2> and(final ACallable<T2> otherAction) {
            return new AllBuilder2<T1, T2>(action, otherAction);
        }

        /**
         * Add new branch.
         *
         * @param otherAction other action
         * @param <T2>        the second type
         * @return the promise for the tuple
         */
        public <T2> Promise<Tuple2<T1, T2>> andLast(final ACallable<T2> otherAction) {
            return and(otherAction).finish();
        }

        /**
         * The builder for the next action.
         *
         * @param <T1> the first type
         * @param <T2> the second type
         */
        public static final class AllBuilder2<T1, T2> {
            /**
             * The first action.
             */
            private final ACallable<T1> action1;
            /**
             * The second action.
             */
            private final ACallable<T2> action2;

            /**
             * The constructor.
             *
             * @param action1 the first action
             * @param action2 the second action
             */
            public AllBuilder2(final ACallable<T1> action1, final ACallable<T2> action2) {
                this.action1 = action1;
                this.action2 = action2;
            }

            /**
             * @return start executing the bodies and resolve to the tuple
             */
            public Promise<Tuple2<T1, T2>> finish() {
                final Promise<T1> p1 = aNow(action1);
                final Promise<T2> p2 = aNow(action2);
                final Promise<Tuple2<T1, T2>> rc = new Promise<Tuple2<T1, T2>>();
                final AResolver<Tuple2<T1, T2>> resolver = rc.resolver();
                p1.listen(new AResolver<T1>() {
                    @Override
                    public void resolve(final Outcome<T1> resolution1) throws Throwable {
                        p2.listen(new AResolver<T2>() {
                            @Override
                            public void resolve(final Outcome<T2> resolution2) throws Throwable {
                                if (!resolution1.isSuccess()) {
                                    notifyFailure(resolver, resolution1.failure());
                                } else if (!resolution2.isSuccess()) {
                                    notifyFailure(resolver, resolution2.failure());
                                } else {
                                    notifySuccess(resolver, Tuple2.of(resolution1.value(), resolution2.value()));
                                }
                            }
                        });
                    }
                });
                return rc;
            }

            /**
             * Add new branch.
             *
             * @param otherAction other action
             * @param <T3>        the other type
             * @return the builder for the tuple
             */
            public <T3> AllBuilder3<T1, T2, T3> and(final ACallable<T3> otherAction) {
                return new AllBuilder3<T1, T2, T3>(action1, action2, otherAction);
            }

            /**
             * Unzip values.
             *
             * @param function the function to unzip to
             * @param <R>      the result type
             * @return the promise for result.
             */
            public <R> Promise<R> unzip(final AFunction2<R, T1, T2> function) {
                return finish().map(FunctionUtil.uncurry2(function));
            }

            /**
             * @return the value of the first branch (ignore all others)
             */
            public Promise<T1> selectValue1() {
                return finish().map(new AFunction<T1, Tuple2<T1, T2>>() {
                    @Override
                    public Promise<T1> apply(final Tuple2<T1, T2> value) throws Throwable {
                        return aValue(value.getValue1());
                    }
                });
            }

            /**
             * @return the value of the second branch (ignore all others)
             */
            public Promise<T2> selectValue2() {
                return finish().map(new AFunction<T2, Tuple2<T1, T2>>() {
                    @Override
                    public Promise<T2> apply(final Tuple2<T1, T2> value) throws Throwable {
                        return aValue(value.getValue2());
                    }
                });
            }

            /**
             * Add new branch and finish it.
             *
             * @param otherAction other action
             * @param <T3>        the third type
             * @return the promise for the tuple
             */
            public <T3> Promise<Tuple3<T1, T2, T3>> andLast(final ACallable<T3> otherAction) {
                return and(otherAction).finish();
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
            /**
             * The first action.
             */
            private final ACallable<T1> action1;
            /**
             * The second action.
             */
            private final ACallable<T2> action2;
            /**
             * The third action.
             */
            private final ACallable<T3> action3;

            /**
             * The constructor.
             *
             * @param action1 the first action
             * @param action2 the second action
             * @param action3 the third action
             */
            public AllBuilder3(final ACallable<T1> action1, final ACallable<T2> action2, final ACallable<T3> action3) {
                this.action1 = action1;
                this.action2 = action2;
                this.action3 = action3;
            }

            /**
             * Finish building the all construct.
             *
             * @return the promise for the next action.
             */
            public Promise<Tuple3<T1, T2, T3>> finish() {
                final Promise<T1> p1 = aNow(action1);
                final Promise<T2> p2 = aNow(action2);
                final Promise<T3> p3 = aNow(action3);
                final Promise<Tuple3<T1, T2, T3>> rc = new Promise<Tuple3<T1, T2, T3>>();
                final AResolver<Tuple3<T1, T2, T3>> resolver = rc.resolver();
                p1.listen(new AResolver<T1>() {
                    @Override
                    public void resolve(final Outcome<T1> resolution1) throws Throwable {
                        p2.listen(new AResolver<T2>() {
                            @Override
                            public void resolve(final Outcome<T2> resolution2) throws Throwable {
                                p3.listen(new AResolver<T3>() {
                                    @Override
                                    public void resolve(final Outcome<T3> resolution3) throws Throwable {
                                        if (!resolution1.isSuccess()) {
                                            notifyFailure(resolver, resolution1.failure());
                                        } else if (!resolution2.isSuccess()) {
                                            notifyFailure(resolver, resolution2.failure());
                                        } else if (!resolution3.isSuccess()) {
                                            notifyFailure(resolver, resolution3.failure());
                                        } else {
                                            notifySuccess(resolver, new Tuple3<T1, T2, T3>(resolution1.value(),
                                                    resolution2.value(), resolution3.value()));
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
                return rc;
            }

            /**
             * Unzip values.
             *
             * @param function the function to unzip to
             * @param <R>      the result type
             * @return the promise for result.
             */
            public <R> Promise<R> unzip(final AFunction3<R, T1, T2, T3> function) {
                return finish().map(FunctionUtil.uncurry3(function));
            }

            /**
             * @return the value of the first branch (ignore all others)
             */
            public Promise<T1> selectValue1() {
                return finish().map(new AFunction<T1, Tuple3<T1, T2, T3>>() {
                    @Override
                    public Promise<T1> apply(final Tuple3<T1, T2, T3> value) throws Throwable {
                        return aValue(value.getValue1());
                    }
                });
            }

            /**
             * @return the value of the second branch (ignore all others)
             */
            public Promise<T2> selectValue2() {
                return finish().map(new AFunction<T2, Tuple3<T1, T2, T3>>() {
                    @Override
                    public Promise<T2> apply(final Tuple3<T1, T2, T3> value) throws Throwable {
                        return aValue(value.getValue2());
                    }
                });
            }

            /**
             * @return the value of the third branch (ignore all others)
             */
            public Promise<T3> selectValue3() {
                return finish().map(new AFunction<T3, Tuple3<T1, T2, T3>>() {
                    @Override
                    public Promise<T3> apply(final Tuple3<T1, T2, T3> value) throws Throwable {
                        return aValue(value.getValue3());
                    }
                });
            }

        }
    }
}
