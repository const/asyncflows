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

package org.asyncflows.io.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.CRC32;


/**
 * The binary reader is intended for reading binary data asynchronously. It is mostly oriented
 * for reading simple protocol packets with shallow nesting.
 */
public class BinaryReader {


    /**
     * Read sequence.
     *
     * @param type the type (used only to set type constraint).
     * @param s    the fist action to read model
     * @param <M>  the model
     * @return the reader builder.
     */
    @SuppressWarnings("squid:S1172")
    public static <M> ReadSequenceBuilder<M> readSequence(Class<M> type, final ReaderAction<M> s) {
        return new ReadSequenceBuilder<>(s);
    }

    /**
     * Read sequence.
     *
     * @param s   the first action
     * @param <M> the model type
     * @return the reader builder.
     */
    public static <M> ReadSequenceBuilder<M> readSequence(final ReaderAction<M> s) {
        return new ReadSequenceBuilder<>(s);
    }

    /**
     * Read optional block.
     *
     * @param predicate       the predicate that checks if block should be read.
     * @param actionASupplier the action supplier.
     * @param <M>             the model type
     * @return the reader action
     */
    @SuppressWarnings("squid:S5411")
    public static <M> ReaderAction<M> readOptional(final Predicate<M> predicate, final Supplier<ReaderAction<M>> actionASupplier) {
        return new ReaderAction<M>() {
            Boolean needsExecution;
            ReaderAction<M> action;

            @Override
            public int read(ReaderContext context, M model) {
                if (needsExecution == null) {
                    needsExecution = predicate.test(model);
                    if (needsExecution) {
                        action = actionASupplier.get();
                    }
                }
                if (needsExecution) {
                    return action.read(context, model);
                } else {
                    needsExecution = null;
                    action = null;
                    return 0;
                }
            }
        };
    }

    /**
     * Do technical action that modifies model or context and does not need any data.
     *
     * @param consumer the consumer
     * @param <M>      the model
     * @return the reader action
     */
    public static <M> ReaderAction<M> technical(BiConsumer<ReaderContext, M> consumer) {
        return (context, model) -> {
            consumer.accept(context, model);
            return 0;
        };
    }

    /**
     * Read data into field.
     *
     * @param supplierReader the reader for value
     * @param merge          the merge operation
     * @param <M>            the model type
     * @param <S>            the value type
     * @return the reader action.
     */
    public static <M, S> ReaderAction<M> readerField(SupplierReader<M, S> supplierReader, BiConsumer<M, S> merge) {
        return (context, model) -> supplierReader.read(context, model, merge);
    }

    /**
     * Reader for unsigned int 16 (big endian).
     *
     * @param <M> the model type
     * @return the reader
     */
    public static <M> SupplierReader<M, Integer> readUint16() {
        return (context, model, output) -> {
            if (context.buffer().remaining() < 2) {
                return 2;
            }
            int i = (context.getOctet() << 8) | context.getOctet();
            output.accept(model, i);
            return 0;
        };
    }

    /**
     * Reader for signed int 32 (big endian).
     *
     * @param <M> the model type
     * @return the reader
     */
    public static <M> SupplierReader<M, Integer> readInt32() {
        return (context, model, output) -> {
            if (context.buffer().remaining() < 2) {
                return 2;
            }
            int i = ((((((context.getOctet() << 8) | context.getOctet()) << 8) | context.getOctet()) << 8) | context.getOctet());
            output.accept(model, i);
            return 0;
        };
    }


    /**
     * Reader for byte.
     *
     * @param <M> the value
     * @return the reader
     */
    public static <M> SupplierReader<M, Byte> readByte() {
        return (context, model, output) -> {
            if (context.buffer().remaining() < 1) {
                return 1;
            }
            int i = context.getOctet();
            output.accept(model, (byte) i);
            return 0;
        };
    }

    /**
     * Implement typical checksum pattern where bock of data that need to be checked is followed by checksum.
     *
     * @param checksumProvider the provider of checksum instance
     * @param updateAction     the update action for checksum
     * @param body             the body
     * @param checksumReader   the reader of checksum in data
     * @param verifyChecksum   the verifier of checksum
     * @param <M>              the model type
     * @param <V>              the checksum value type
     * @param <C>              the checksum accumulator type
     * @return the reader action that implement checksums
     */
    public static <M, V, C> ReaderAction<M> withChecksum(Supplier<C> checksumProvider,
                                                         Function<C, ReaderListener> updateAction,
                                                         ReaderAction<M> body,
                                                         SupplierReader<M, V> checksumReader,
                                                         BiConsumer<C, V> verifyChecksum) {
        return new ReaderAction<M>() {
            private final C checksum = checksumProvider.get();
            private final ReaderListener listener = updateAction.apply(checksum);
            private ReaderAction<M> composed = BinaryReader
                    .<M>readSequence(technical((c, m) -> c.addListener(listener)))
                    .then(body)
                    .then(technical((c, m) -> c.removeListener(listener)))
                    .then(readerField(checksumReader, (m, v) -> verifyChecksum.accept(checksum, v)))
                    .end();

            @Override
            public int read(ReaderContext context, M model) {
                return composed.read(context, model);
            }
        };
    }

    /**
     * Read data hashed with CRC32.
     *
     * @param body the body
     * @param <M>  the model
     * @return the reader action
     */
    public static <M> ReaderAction<M> withCRC32(ReaderAction<M> body) {
        return withChecksum(CRC32::new, c -> c::update, body, readInt32(), (c, v) -> {
            long read = v & 0xFFFF_FFFFL;
            if (read != (c.getValue() & 0xFFFF_FFFFL)) {
                throw new BinaryFormatException(String.format("Checksums do not match: expected = %08x, actual = %08x", read, v));
            }
        });
    }


    /**
     * Read sub-state.
     *
     * @param subModel the creator for sub-model
     * @param reader   the reader
     * @param merge    the merge operation
     * @param <M>      the model
     * @param <S>      the sub model
     * @return the reader action
     */
    public static <M, S> ReaderAction<M> readMapped(final Function<M, S> subModel, Supplier<ReaderAction<S>> reader, BiConsumer<M, S> merge) {
        return new ReaderAction<M>() {
            private ReaderAction<S> subAction;
            private S subState;

            @Override
            public int read(ReaderContext context, M model) {
                if (subAction == null) {
                    subState = subModel.apply(model);
                    subAction = reader.get();
                }
                int rc = subAction.read(context, subState);
                if (rc > 0) {
                    return rc;
                }
                merge.accept(model, subState);
                subState = null;
                subAction = null;
                return 0;
            }
        };
    }

    /**
     * Read while condition is true
     *
     * @param condition condition
     * @param subModel  sub-model
     * @param reader    the reader
     * @param merge     the merge operation
     * @param <M>       the model type
     * @param <S>       the sub model type
     * @return the reader
     */
    public static <M, S> ReaderAction<M> readWhile(Predicate<M> condition, final Function<M, S> subModel, Supplier<ReaderAction<S>> reader, BiConsumer<M, S> merge) {
        return new ReaderAction<M>() {
            private ReaderAction<S> subAction;
            private S subState;

            @Override
            public int read(ReaderContext context, M model) {
                while (true) {
                    if (subAction == null) {
                        if (!condition.test(model)) {
                            return 0;
                        }
                        subState = subModel.apply(model);
                        subAction = reader.get();
                    }
                    int rc = subAction.read(context, subState);
                    if (rc > 0) {
                        return rc;
                    }
                    merge.accept(model, subState);
                    subState = null;
                    subAction = null;
                }
            }
        };
    }

    /**
     * Read until the specified condition is true
     *
     * @param subModel  the creator for iteration model
     * @param reader    the reader of sub-model
     * @param merge     the merge operation
     * @param condition if true, the iteration is stopped
     * @param <M>       the model
     * @param <S>       the sub model
     * @return the result
     */
    public static <M, S> ReaderAction<M> readUntil(final Function<M, S> subModel, Supplier<ReaderAction<S>> reader, BiConsumer<M, S> merge, BiPredicate<M, S> condition) {
        return new ReaderAction<M>() {
            private ReaderAction<S> subAction;
            private S subState;

            @Override
            public int read(ReaderContext context, M model) {
                while (true) {
                    if (subAction == null) {
                        subState = subModel.apply(model);
                        subAction = reader.get();
                    }
                    int rc = subAction.read(context, subState);
                    if (rc > 0) {
                        return rc;
                    }
                    merge.accept(model, subState);
                    S s = subState;
                    subState = null;
                    subAction = null;
                    if (condition.test(model, s)) {
                        return 0;
                    }
                }
            }
        };
    }

    /**
     * The reader action. The action is assumed mutable. It is created in initital state,
     *
     * @param <R> the result
     */
    public interface ReaderAction<R> {
        /**
         * Read part of the stream. This method is repeatably invoked until it returns 0.
         * The returned value indicates minimal amount of bytes available in the buffer needed for method to continue.
         *
         * @param context the context
         * @param model   the model
         * @return 0 if reading is finished, positive amount if more bytes are needed.
         */
        int read(ReaderContext context, R model);
    }

    /**
     * The supplier reader.
     *
     * @param <M> the type of model
     * @param <S> the type of value supplied to model
     */
    public interface SupplierReader<M, S> {
        /**
         * Read operation.
         *
         * @param context the context
         * @param model   the model
         * @param output  the output
         * @return the
         */
        int read(ReaderContext context, M model, BiConsumer<M, S> output);
    }

    /**
     * The context.
     */
    public interface ReaderContext {
        /**
         * @return the buffer with data (backed by array)
         */
        ByteBuffer buffer();

        /**
         * @return get byte as int (unsigned)
         */
        default int getOctet() {
            return getByte() & 0xFF;
        }

        /**
         * @return the buffer byte
         */
        default byte getByte() {
            return buffer().get();
        }

        /**
         * Notify listener about data if there is any.
         */
        void checkpoint();

        /**
         * Add listener (forces checkpoint for previously registered listeners)
         *
         * @param listener the listener.
         */
        void addListener(ReaderListener listener);

        /**
         * Remove listener (forces checkpoint for previously registered listeners)
         *
         * @param listener the listener
         */
        void removeListener(ReaderListener listener);
    }


    /**
     * The listener is notified about all bytes read while is was active.
     * This is mostly used to calculate checksums.
     */
    public interface ReaderListener {
        /**
         * Indicate that bytes have been read.
         *
         * @param data   the data
         * @param start  the start of bytes
         * @param length the end of bytes
         */
        void bytesRead(byte[] data, int start, int length);
    }

    /**
     * The builder for the sequence.
     *
     * @param <M> the model type
     */
    public static class ReadSequenceBuilder<M> {
        /**
         * The list of actions.
         */
        private final ArrayList<ReaderAction<M>> actions = new ArrayList<>();

        /**
         * The sequence builder.
         *
         * @param first the first action
         */
        protected ReadSequenceBuilder(ReaderAction<M> first) {
            actions.add(first);
        }

        /**
         * Add action.
         *
         * @param next the next action
         * @return this builder
         */
        public ReadSequenceBuilder<M> then(ReaderAction<M> next) {
            actions.add(next);
            return this;
        }

        /**
         * End building process.
         *
         * @return the the reader action
         */
        public ReaderAction<M> end() {
            return new ReaderAction<M>() {
                private ReaderAction<M> current;
                private Iterator<ReaderAction<M>> iterator = actions.iterator();

                @Override
                public int read(ReaderContext context, M model) {
                    while (true) {
                        if (current == null) {
                            if (iterator.hasNext()) {
                                current = iterator.next();
                            } else {
                                return 0;
                            }
                        }
                        int need = current.read(context, model);
                        if (need > 0) {
                            return need;
                        } else {
                            current = null;
                        }
                    }
                }
            };
        }
    }
}