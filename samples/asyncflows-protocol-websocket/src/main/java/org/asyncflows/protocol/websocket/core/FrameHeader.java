/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.protocol.websocket.core;

import org.asyncflows.io.util.BinaryParsingUtil;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;

/**
 * The frame header.
 */
public final class FrameHeader {
    /**
     * The continuation frame.
     */
    public static final int OP_CONTINUATION = 0;
    /**
     * The text frame.
     */
    public static final int OP_TEXT = 1;
    /**
     * The binary frame.
     */
    public static final int OP_BINARY = 2;
    /**
     * The close frame.
     */
    public static final int OP_CLOSE = 8;
    /**
     * The ping frame.
     */
    public static final int OP_PING = 9;
    /**
     * The pong frame.
     */
    public static final int OP_PONG = 10;
    /**
     * Payload length is 2 bytes.
     */
    private static final int PAYLOAD_LENGTH_2 = 126;
    /**
     * Payload length is 8 bytes.
     */
    private static final int PAYLOAD_LENGTH_8 = 127;
    /**
     * The mask for op-code
     */
    private static final int OP_CODE_MASK = 0x0F;
    /**
     * The mask for final bit.
     */
    private static final int FINAL_BIT_MASK = 0x80;
    /**
     * The mask 'for mask presents' bit.
     */
    private static final int MASKED_BIT_MASK = 0x80;
    /**
     * The mask for initial length.
     */
    private static final int INITIAL_LENGTH_MASK = 0x7F;
    /**
     * Max 16-bit unsigned int.
     */
    public static final int MAX_UINT_16 = 0xFFFF;
    /**
     * Final fragment flag.
     */
    private boolean finalFragment;
    /**
     * The operation code.
     */
    private int opCode;
    /**
     * The payload length.
     */
    private long length;
    /**
     * The mask (4 byte array).
     */
    private byte[] mask;

    /**
     * @return true if this is a final fragment.
     */
    public boolean isFinalFragment() {
        return finalFragment;
    }

    /**
     * Set final fragment indicator.
     *
     * @param finalFragment the final fragment indicator.
     */
    public void setFinalFragment(final boolean finalFragment) {
        this.finalFragment = finalFragment;
    }

    /**
     * @return get the op code.
     */
    public int getOpCode() {
        return opCode;
    }

    /**
     * Set the op code.
     *
     * @param opCode the op code.
     */
    public void setOpCode(final int opCode) {
        this.opCode = opCode;
    }

    /**
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * Set length.
     *
     * @param length the length
     */
    public void setLength(final long length) {
        this.length = length;
    }

    /**
     * @return the payload mask
     */
    public byte[] getMask() {
        return mask;
    }

    /**
     * Set mask.
     *
     * @param mask the mask.
     */
    public void setMask(final byte[] mask) {
        this.mask = mask;
    }

    /**
     * @return get bytes assuming that length is taken from
     */
    private int getPostInitialSize() {
        int size = 0;
        if (length == PAYLOAD_LENGTH_2) {
            size += Short.BYTES;
        } else if (length == PAYLOAD_LENGTH_8) {
            size += Long.BYTES;
        }
        if (mask != null) {
            size += mask.length;
        }
        return size;
    }

    /**
     * @return get total length (works in fully initialized form).
     */
    private int getTotalLength() {
        int rc = Short.BYTES;
        if (length > MAX_UINT_16) {
            rc += Long.BYTES;
        } else if (length >= PAYLOAD_LENGTH_2) {
            rc += Short.BYTES;
        }
        if (mask != null) {
            rc += mask.length;
        }
        return rc;
    }

    /**
     * Write header.
     *
     * @param output the output to write to
     * @return the promise the resolves when header is put to buffer (it might need to free it)
     */
    public Promise<Void> write(final ByteGeneratorContext output) {
        return output.ensureAvailable(getTotalLength()).thenFlatGet(() -> {
            final ByteBuffer buffer = output.buffer();
            assert buffer.order() == ByteOrder.BIG_ENDIAN;
            int b = opCode;
            if (isFinalFragment()) {
                b = b | FINAL_BIT_MASK;
            }
            buffer.put((byte) b);
            if (length > MAX_UINT_16) {
                b = PAYLOAD_LENGTH_8;
            } else if (length >= PAYLOAD_LENGTH_2) {
                b = PAYLOAD_LENGTH_2;
            }
            if (mask != null) {
                b = b | MASKED_BIT_MASK;
            }
            buffer.put((byte) b);
            if (length > MAX_UINT_16) {
                buffer.putLong(length);
            } else if (length >= PAYLOAD_LENGTH_2) {
                buffer.putShort((short) length);
            }
            return aVoid();
        });
    }

    /**
     * Read frame header.
     *
     * @param input the context
     * @return the promise for the header.
     */
    public static Promise<FrameHeader> read(final ByteParserContext input) {
        final FrameHeader rc = new FrameHeader();
        final int START = 0;
        final int INITIAL = 1;
        int state = START;
        if (input.buffer().remaining() >= Short.BYTES) {
            parseInitial(rc, input);
            state = INITIAL;
            if (input.buffer().remaining() >= rc.getPostInitialSize()) {
                parsePostInitial(rc, input);
                return aValue(rc);
            }
        }
        final int savedState = state;
        return aSeqUntilValue(new ASupplier<Maybe<FrameHeader>>() {
            private int loopState = savedState;

            @Override
            public Promise<Maybe<FrameHeader>> get() throws Exception {
                if (loopState == START) {
                    return input.ensureAvailable(Short.BYTES).thenFlatGet(() -> {
                        parseInitial(rc, input);
                        loopState = INITIAL;
                        return aMaybeEmpty();
                    });
                } else if (loopState == INITIAL) {
                    return input.ensureAvailable(rc.getPostInitialSize()).thenFlatGet(() -> {
                        parseInitial(rc, input);
                        return aMaybeValue(rc);
                    });
                } else {
                    throw new IllegalStateException("Unknown state " + loopState);
                }
            }
        });
    }


    /**
     * Parse initial two bytes.
     *
     * @param rc    the target object
     * @param input the input
     */
    private static void parseInitial(final FrameHeader rc, final ByteParserContext input) {
        int b1 = input.buffer().get();
        if ((b1 & FINAL_BIT_MASK) != 0) {
            rc.setFinalFragment(true);
        }
        rc.setOpCode(b1 & OP_CODE_MASK);
        int b2 = input.buffer().get();
        if ((b2 & FINAL_BIT_MASK) != 0) {
            rc.setMask(new byte[Integer.BYTES]);
        }
        rc.setLength(b2 & INITIAL_LENGTH_MASK);
    }

    /**
     * Parse additional information if needed.
     *
     * @param rc    the target object
     * @param input the input
     */
    private static void parsePostInitial(final FrameHeader rc, final ByteParserContext input) {
        final ByteBuffer buffer = input.buffer();
        assert buffer.order() == ByteOrder.BIG_ENDIAN;
        if (rc.length == PAYLOAD_LENGTH_2) {
            rc.length = ((int) buffer.getShort()) & MAX_UINT_16;
        } else if (rc.length == PAYLOAD_LENGTH_8) {
            rc.length = BinaryParsingUtil.getLong(input);
        }
        if (rc.mask != null) {
            buffer.get(rc.mask);
        }
    }
}
