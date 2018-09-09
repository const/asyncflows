package org.asyncflows.core.util;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AFunction2;
import org.asyncflows.core.function.AFunction3;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.function.AsyncFunctionUtil;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.function.AsyncFunctionUtil.constantSupplier;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;
import static org.asyncflows.core.function.AsyncFunctionUtil.supplierToFunction;
import static org.asyncflows.core.function.AsyncFunctionUtil.useFirstArg;
import static org.asyncflows.core.function.AsyncFunctionUtil.useSecondArg;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;

/**
 * The resource related utilities.
 */
public final class CoreFlowsResource {
    /**
     * The private constructor for utility class.
     */
    private CoreFlowsResource() {
    }

    /**
     * Start building a try with resources.
     *
     * @param openBody the action that opens resource
     * @param <A>      the resource type
     * @return the opened resource
     */
    public static <A extends ACloseable> Try1<A> aTry(final ASupplier<A> openBody) {
        return new Try1<>(openBody);
    }

    /**
     * Start building a try with resources.
     *
     * @param resource the open resource
     * @param <A>      the resource type
     * @return the opened resource
     */
    public static <A extends ACloseable> Try1<A> aTryResource(final A resource) {
        return new Try1<>(constantSupplier(resource));
    }

    /**
     * Start building a try with resources.
     *
     * @param openPromise the action that opens resource
     * @param <A>         the resource type
     * @return the opened resource
     */
    public static <A extends ACloseable> Try1<A> aTry(final Promise<A> openPromise) {
        return new Try1<>(promiseSupplier(openPromise));
    }

    /**
     * Close the resource action.
     *
     * @param resourceCell the cell with resource
     * @param <A>          the resource type
     * @return the close resource action
     */
    private static <A extends ACloseable> ASupplier<Void> closeResourceCellAction(final Cell<A> resourceCell) {
        return () -> {
            if (resourceCell.isEmpty()) {
                return aVoid();
            } else {
                return resourceCell.getValue().close();
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
    public static ASupplier<Void> closeResourceAction(final ACloseable stream) {
        return stream::close;
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
        private final ASupplier<A> openAction;

        /**
         * The constructor from operation.
         *
         * @param openAction the operation
         */
        public Try1(final ASupplier<A> openAction) {
            this.openAction = openAction;
        }

        /**
         * Add new resource to open.
         *
         * @param otherOpen other open operation
         * @param <B>       the other resource type
         * @return the next builder
         */
        public <B extends ACloseable> Try2<A, B> andChain(final AFunction<A, B> otherOpen) {
            return new Try2<>(this, otherOpen);
        }

        /**
         * Add independent resource.
         *
         * @param otherOpen the open operation
         * @param <B>       the resource type
         * @return the next builder
         */
        public <B extends ACloseable> Try2<A, B> andOther(final ASupplier<B> otherOpen) {
            return andChain(supplierToFunction(otherOpen));
        }

        /**
         * Add independent resource.
         *
         * @param otherOpen the open operation
         * @param <B>       the resource type
         * @return the next builder
         */
        public <B extends ACloseable> Try2<A, B> andOther(final Promise<B> otherOpen) {
            return andOther(promiseSupplier(otherOpen));
        }


        /**
         * Open and run body with resource, and close it after body finishes.
         *
         * @param body a body to run
         * @param <R>  a resource
         * @return the promise that finishes when body finishes and resource is closed
         */
        public <R> Promise<R> run(final AFunction<A, R> body) {
            final Cell<A> resource1 = new Cell<>();
            return aSeq(openAction).map(value -> {
                resource1.setValue(value);
                return body.apply(value);
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
        private final AFunction<A, B> openAction;

        /**
         * The constructor.
         *
         * @param outer      the outer builder
         * @param openAction the open action for this level
         */
        public Try2(final Try1<A> outer, final AFunction<A, B> openAction) {
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
        public <C extends ACloseable> Try3<A, B, C> andChainBoth(final AFunction2<A, B, C> otherOpen) {
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
        public <C extends ACloseable> Try3<A, B, C> andChainSecond(final AFunction<B, C> otherOpen) {
            return andChainBoth(AsyncFunctionUtil.<A, B, C>useSecondArg(otherOpen));
        }

        /**
         * Add new resource to open (the case of {@link #andChainBoth(AFunction2)} that depends on
         * the first resource only.
         *
         * @param otherOpen other open operation
         * @param <C>       the other resource type
         * @return the next builder
         */
        public <C extends ACloseable> Try3<A, B, C> andChainFirst(final AFunction<A, C> otherOpen) {
            return andChainBoth(useFirstArg(otherOpen));
        }

        /**
         * Run body with two open resources.
         *
         * @param body the body to run
         * @param <R>  the resource
         * @return the result of body execution
         */
        public <R> Promise<R> run(final AFunction2<A, B, R> body) {
            final Cell<B> resource = new Cell<>();
            return outer.run(value1 -> aSeq(() -> openAction.apply(value1)).map(value2 -> {
                resource.setValue(value2);
                return body.apply(value1, value2);
            }).finallyDo(closeResourceCellAction(resource)));
        }

        /**
         * Run body with two open resources.
         *
         * @param body the body to run
         * @param <R>  the resource
         * @return the result of body execution
         */
        public <R> Promise<R> runWithSecond(final AFunction<B, R> body) {
            return run(useSecondArg(body));
        }
    }

    /**
     * The builder for two resource actions.
     *
     * @param <A> the first resource type
     * @param <B> the second resource type
     * @param <C> the third resource type
     */
    public static final class Try3<A extends ACloseable, B extends ACloseable, C extends ACloseable> {
        /**
         * The outer builder.
         */
        private final Try2<A, B> outer;
        /**
         * The open action for this level.
         */
        private final AFunction2<A, B, C> openAction;

        /**
         * The constructor.
         *
         * @param outer      the outer builder
         * @param openAction the open action for this level
         */
        public Try3(final Try2<A, B> outer, final AFunction2<A, B, C> openAction) {
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
        public <R> Promise<R> run(final AFunction3<A, B, C, R> body) {
            final Cell<C> resource = new Cell<>();
            return outer.run((value1, value2) ->
                    aSeq(() -> openAction.apply(value1, value2)
                    ).map(value3 -> {
                        resource.setValue(value3);
                        return body.apply(value1, value2, value3);
                    }).finallyDo(closeResourceCellAction(resource)));
        }
    }
}
