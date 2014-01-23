package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;

/**
 * The resource related utilities.
 */
public final class ResourceUtil {
    /**
     * The private constructor for utility class.
     */
    private ResourceUtil() {
    }

    /**
     * Start building a try with resources.
     *
     * @param openBody the action that opens resource
     * @param <A>      the resource type
     * @return the opened resource
     */
    public static <A extends ACloseable> Try1<A> aTry(final ACallable<A> openBody) {
        return new Try1<A>(openBody);
    }

    /**
     * Start building a try with resources.
     *
     * @param resource the open resource
     * @param <A>      the resource type
     * @return the opened resource
     */
    public static <A extends ACloseable> Try1<A> aTry(final A resource) {
        return new Try1<A>(constantCallable(resource));
    }

    /**
     * Start building a try with resources.
     *
     * @param openPromise the action that opens resource
     * @param <A>         the resource type
     * @return the opened resource
     */
    public static <A extends ACloseable> Try1<A> aTry(final Promise<A> openPromise) {
        return new Try1<A>(promiseCallable(openPromise));
    }

    /**
     * Close the resource action.
     *
     * @param resourceCell the cell with resource
     * @param <A>          the resource type
     * @return the close resource action
     */
    private static <A extends ACloseable> ACallable<Void> closeResourceCellAction(final Cell<A> resourceCell) {
        return new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if (resourceCell.isEmpty()) {
                    return aVoid();
                } else {
                    return resourceCell.getValue().close();
                }
            }
        };
    }

    /**
     * Close object on the specified vat. This method is mostly used for proxy implementation.
     *
     * @param vat    the vat
     * @param stream the object to close
     * @return the promise that resolves when close is finished
     */
    public static Promise<Void> closeResource(final Vat vat, final ACloseable stream) {
        return aLater(vat, closeResourceAction(stream));
    }

    /**
     * Create an action that closes resource. This method is mostly used for utility classes that work
     * with closeable resources.
     *
     * @param stream the closeable object
     * @return the action that closes it
     */
    public static ACallable<Void> closeResourceAction(final ACloseable stream) {
        return new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return stream.close();
            }
        };
    }

    /**
     * Builder for aTry operation with one resource.
     *
     * @param <A> the resource type
     */
    public static final class Try1<A extends ACloseable> {
        /**
         * The open operation for the resource.
         */
        private final ACallable<A> openAction;

        /**
         * The constructor from operation.
         *
         * @param openAction the operation
         */
        public Try1(final ACallable<A> openAction) {
            this.openAction = openAction;
        }

        /**
         * Add new resource to open.
         *
         * @param otherOpen other open operation
         * @param <B>       the other resource type
         * @return the next builder
         */
        public <B extends ACloseable> Try2<A, B> andChain(final AFunction<B, A> otherOpen) {
            return new Try2<A, B>(this, otherOpen);
        }

        /**
         * Add independent resource.
         *
         * @param otherOpen the open operation
         * @param <B>       the resource type
         * @return the next builder
         */
        public <B extends ACloseable> Try2<A, B> andOther(final ACallable<B> otherOpen) {
            return andChain(CoreFunctionUtil.<B, A>discardArgument(otherOpen));
        }

        /**
         * Open and run body with resource, and close it after body finishes.
         *
         * @param body a body to run
         * @param <R>  a resource
         * @return the promise that finishes when body finishes and resource is closed
         */
        public <R> Promise<R> run(final AFunction<R, A> body) {
            final Cell<A> resource1 = new Cell<A>();
            return aSeq(openAction).map(new AFunction<R, A>() {
                @Override
                public Promise<R> apply(final A value) throws Throwable {
                    resource1.setValue(value);
                    return body.apply(value);
                }
            }).finallyDo(closeResourceCellAction(resource1));
        }
    }

    /**
     * The builder for two resource actions.
     *
     * @param <A> the first resource type
     * @param <B> the second resource type
     */
    public static final class Try2<A extends ACloseable, B extends ACloseable> {
        /**
         * The outer builder.
         */
        private final Try1<A> outer;
        /**
         * The open action for this level.
         */
        private final AFunction<B, A> openAction;

        /**
         * The constructor.
         *
         * @param outer      the outer builder
         * @param openAction the open action for this level
         */
        public Try2(final Try1<A> outer, final AFunction<B, A> openAction) {
            this.outer = outer;
            this.openAction = openAction;
        }

        /**
         * Add new resource to open.
         *
         * @param otherOpen other open operation
         * @param <C>       the other resource type
         * @return the next builder
         */
        public <C extends ACloseable> Try3<A, B, C> andChainBoth(final AFunction2<C, A, B> otherOpen) {
            return new Try3<A, B, C>(this, otherOpen);
        }

        /**
         * Add new resource to open (the case of {@link #andChainBoth(AFunction2)} that depends on
         * the second resource only.
         *
         * @param otherOpen other open operation
         * @param <C>       the other resource type
         * @return the next builder
         */
        public <C extends ACloseable> Try3<A, B, C> andChainSecond(final AFunction<C, B> otherOpen) {
            return andChainBoth(FunctionUtil.<C, A, B>useUseSecondArg(otherOpen));
        }

        /**
         * Add new resource to open (the case of {@link #andChainBoth(AFunction2)} that depends on
         * the first resource only.
         *
         * @param otherOpen other open operation
         * @param <C>       the other resource type
         * @return the next builder
         */
        public <C extends ACloseable> Try3<A, B, C> andChainFirst(final AFunction<C, A> otherOpen) {
            return andChainBoth(FunctionUtil.<C, A, B>useUseFirstArg(otherOpen));
        }

        /**
         * Run body with two open resources.
         *
         * @param body the body to run
         * @param <R>  the resource
         * @return the result of body execution
         */
        public <R> Promise<R> run(final AFunction2<R, A, B> body) {
            final Cell<B> resource = new Cell<B>();
            return outer.run(new AFunction<R, A>() {
                @Override
                public Promise<R> apply(final A value1) throws Throwable {
                    return aSeq(new ACallable<B>() {
                        @Override
                        public Promise<B> call() throws Throwable {
                            return openAction.apply(value1);
                        }
                    }).map(new AFunction<R, B>() {
                        @Override
                        public Promise<R> apply(final B value2) throws Throwable {
                            resource.setValue(value2);
                            return body.apply(value1, value2);
                        }
                    }).finallyDo(closeResourceCellAction(resource));
                }
            });
        }

        /**
         * Run body with two open resources.
         *
         * @param body the body to run
         * @param <R>  the resource
         * @return the result of body execution
         */
        public <R> Promise<R> runWithSecond(final AFunction<R, B> body) {
            return run(FunctionUtil.<R, A, B>useUseSecondArg(body));
        }
    }

    /**
     * The builder for two resource actions.
     *
     * @param <A> the first resource type
     * @param <B> the second resource type
     * @param <C> the second resource type
     */
    public static final class Try3<A extends ACloseable, B extends ACloseable, C extends ACloseable> {
        /**
         * The outer builder.
         */
        private final Try2<A, B> outer;
        /**
         * The open action for this level.
         */
        private final AFunction2<C, A, B> openAction;

        /**
         * The constructor.
         *
         * @param outer      the outer builder
         * @param openAction the open action for this level
         */
        public Try3(final Try2<A, B> outer, final AFunction2<C, A, B> openAction) {
            this.outer = outer;
            this.openAction = openAction;
        }

        /**
         * Run body with two open resources.
         *
         * @param body the body to run
         * @param <R>  the resource
         * @return the result of body execution
         */
        public <R> Promise<R> run(final AFunction3<R, A, B, C> body) {
            final Cell<C> resource = new Cell<C>();
            return outer.run(new AFunction2<R, A, B>() {
                @Override
                public Promise<R> apply(final A value1, final B value2) throws Throwable {
                    return aSeq(new ACallable<C>() {
                        @Override
                        public Promise<C> call() throws Throwable {
                            return openAction.apply(value1, value2);
                        }
                    }).map(new AFunction<R, C>() {
                        @Override
                        public Promise<R> apply(final C value3) throws Throwable {
                            resource.setValue(value3);
                            return body.apply(value1, value2, value3);
                        }
                    }).finallyDo(closeResourceCellAction(resource));
                }
            });
        }
    }
}
