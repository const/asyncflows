package org.asyncflows.protocol.websocket.core;

import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.io.text.EncoderOutput;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.CharIOUtil;
import org.asyncflows.protocol.websocket.AWebSocketOutput;
import org.asyncflows.protocol.websocket.WebSocketMessage;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.util.CoreFlowsResource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Random;
import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.asyncflows.core.util.CoreFlowsResource.closeResourceAction;

/**
 * The implementation of web socket output message stream.
 */
public class WebSocketOutput extends CloseableInvalidatingBase implements AWebSocketOutput {
    // TODO ping/pong
    /**
     * The mask provider (it initialize mask with random data).
     */
    private final Consumer<byte[]> maskProvider;
    /**
     * The output to write.
     */
    private final ByteGeneratorContext output;
    /**
     * The mask bytes.
     */
    private final byte[] maskBytes;
    /**
     * The request queue to be used.
     */
    private final RequestQueue requests = new RequestQueue();

    /**
     * The constructor.
     *
     * @param output       the output
     * @param maskProvider the provider the mask
     */
    public WebSocketOutput(final ByteGeneratorContext output, final Consumer<byte[]> maskProvider) {
        output.buffer().order(ByteOrder.BIG_ENDIAN);
        this.maskProvider = maskProvider;
        if (maskProvider != null) {
            maskBytes = new byte[Integer.BYTES];
        } else {
            maskBytes = null;
        }
        this.output = output;
    }

    /**
     * The constructor.
     *
     * @param output the output
     */
    public WebSocketOutput(final ByteGeneratorContext output, final Random maskProvider) {
        this(output, maskProvider::nextBytes);
    }

    /**
     * Send short binary message (buffer should not be used until method promise finishes).
     *
     * @param message the message to send.
     * @return when stream is ready for the next message.
     */
    @Override
    public Promise<Void> send(final ByteBuffer message) {
        return requests.run(() -> sendFrame(message, true, FrameHeader.OP_BINARY));
    }

    /**
     * Send binary data frame.
     *
     * @param message       the message.
     * @param finalFragment the final fragment flag
     * @param opCode        the operation code
     * @return true whe frame is sent downstream
     */
    private Promise<Void> sendFrame(final ByteBuffer message, final boolean finalFragment, final int opCode) {
        final FrameHeader header = getFrameHeader(finalFragment, opCode, message.remaining());
        return header.write(output)
                .thenFlatGet(() -> new DataWriter().write(message))
                .thenFlatGet(output::send).toVoid().listen(outcomeChecker());
    }

    /**
     * Get frame header.
     *
     * @param finalFragment the final fragment flag
     * @param opCode        the op code
     * @param length        the length
     * @return the header
     */
    private FrameHeader getFrameHeader(final boolean finalFragment, final int opCode, final long length) {
        final FrameHeader header = new FrameHeader();
        header.setFinalFragment(finalFragment);
        header.setOpCode(opCode);
        header.setLength(length);
        if (maskBytes != null) {
            header.setMask(maskBytes);
            maskProvider.accept(maskBytes);
        }
        return header;
    }

    /**
     * Send short text message (buffer should not be used until method promise finishes).
     *
     * @param message the message to send.
     * @return when stream is ready of the next message.
     */
    @Override
    public Promise<Void> send(final CharBuffer message) {
        return null;
    }

    @Override
    public Promise<Void> send(final WebSocketMessage message) {
        switch (message.getType()) {
            case BINARY:
                return send(message.getBinary());
            case BINARY_STREAM:
                return CoreFlowsResource.aTryResource(message.getBinaryStream())
                        .andOther(startBinary(message.getLength()))
                        .run((value1, value2) ->
                                IOUtil.BYTE.copy(value1, value2, false, IOUtil.BYTE.writeBuffer()).toVoid());
            case TEXT:
                return send(message.getText());
            case TEXT_STREAM:
                return CoreFlowsResource.aTryResource(message.getTextStream())
                        .andOther(startText(message.getLength()))
                        .run((value1, value2) ->
                                IOUtil.CHAR.copy(value1, value2, false, IOUtil.CHAR.writeBuffer()).toVoid());
            default:
                throw new IllegalArgumentException("Unsupported message type: " + message.getType());
        }
    }

    @Override
    public Promise<AOutput<CharBuffer>> startText(final Long length) {
        final Promise<Void> voidPromise = new Promise<>();
        final Promise<AOutput<CharBuffer>> outputPromise = new Promise<>();
        requests.run(() -> {
            final AOutput<ByteBuffer> stream;
            if (length == null) {
                stream = new FrameOutputStream(voidPromise.resolver(), FrameHeader.OP_TEXT);
            } else {
                stream = new MessageOutputStream(voidPromise.resolver(), FrameHeader.OP_BINARY, length);
            }
            final AOutput<CharBuffer> utfStream = EncoderOutput.encode(stream, CharIOUtil.UTF8);
            notifySuccess(outputPromise.resolver(), utfStream);
            return voidPromise;
        });
        return outputPromise;
    }

    @Override
    public Promise<AOutput<ByteBuffer>> startBinary(final Long length) {
        final Promise<Void> voidPromise = new Promise<>();
        final Promise<AOutput<ByteBuffer>> outputPromise = new Promise<>();
        requests.run(() -> {
            final AOutput<ByteBuffer> stream;
            if (length == null) {
                stream = new FrameOutputStream(voidPromise.resolver(), FrameHeader.OP_BINARY).export();
            } else {
                stream = new MessageOutputStream(voidPromise.resolver(), FrameHeader.OP_BINARY, length).export();
            }
            notifySuccess(outputPromise.resolver(), stream);
            return voidPromise;
        });
        return outputPromise;
    }

    @Override
    protected Promise<Void> closeAction() {
        return requests.run(() -> sendFrame(ByteBuffer.allocate(0), true, FrameHeader.OP_CLOSE))
                .thenFlatGet(closeResourceAction(output.getOutput()));
    }


    /**
     * The writer of the data that masks written data.
     */
    private final class DataWriter {
        /**
         * The position within mask.
         */
        private int maskPosition;

        /**
         * Write the message.
         *
         * @param messagePart the message part
         * @return when message is written to output. Note that this class leaves message in the output and does
         * not sends it.
         */
        private Promise<Void> write(final ByteBuffer messagePart) {
            return aSeqWhile(() -> {
                if (!messagePart.hasRemaining()) {
                    return aFalse();
                }
                final ByteBuffer buffer = output.buffer();
                if (!buffer.hasRemaining()) {
                    return output.send();
                }
                final int size = Math.min(messagePart.remaining(), buffer.remaining());
                final byte[] data = buffer.array();
                final int position = buffer.position();
                final int offset = buffer.arrayOffset() + position;
                messagePart.get(data, offset, size);
                if (maskBytes != null) {
                    for (int i = 0; i < size; i++) {
                        data[offset + i] = (byte) (data[offset + i] ^ maskBytes[maskPosition]);
                    }
                    maskPosition = (maskPosition + 1) & (Integer.BYTES - 1);
                }
                buffer.position(position + size);
                if (messagePart.hasRemaining()) {
                    return output.send();
                } else {
                    return aFalse();
                }
            });
        }
    }

    /**
     * The output stream for single-frame message with known length in octets.
     */
    private final class MessageOutputStream extends CloseableInvalidatingBase
            implements AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
        /**
         * The finished resolver.
         */
        private final AResolver<Void> finished;
        /**
         * The operation code for the first frame.
         */
        private final int opCode;
        /**
         * The request queue.
         */
        private final RequestQueue streamRequests = new RequestQueue();
        /**
         * True if the first frame.
         */
        private boolean firstWrite;
        /**
         * Remaining amount to be written.
         */
        private long remaining;
        /**
         * The data writer.
         */
        private final DataWriter writer = new DataWriter();


        /**
         * The constructor.
         *
         * @param finished the finished resolver
         * @param opCode   the message code
         * @param length   the length
         */
        private MessageOutputStream(final AResolver<Void> finished, final int opCode, final long length) {
            this.finished = finished;
            this.opCode = opCode;
            this.remaining = length;


        }


        @Override
        protected void onInvalidation(final Throwable throwable) {
            notifyFailure(finished, throwable);
        }

        @Override
        public Promise<Void> write(final ByteBuffer buffer) {
            return streamRequests.run(
                    () -> aSeq(this::writeHeader).thenDoLast(() -> {
                        if (buffer.remaining() > remaining) {
                            throw new IllegalArgumentException("The " + remaining
                                    + " bytes could be written to message, and writing " + buffer.remaining());
                        }
                        remaining -= buffer.remaining();
                        return writer.write(buffer);
                    })
            );
        }

        /**
         * Write header.
         *
         * @return the promise to putting header to the buffer.
         */
        private Promise<Void> writeHeader() {
            if (firstWrite) {
                firstWrite = false;
                final FrameHeader frameHeader = getFrameHeader(true, opCode, remaining);
                return frameHeader.write(output);
            } else {
                return aVoid();
            }
        }

        @Override
        public Promise<Void> flush() {
            return streamRequests.run(() -> output.send().thenFlatGet(() -> output.getOutput().flush())
                    .listen(outcomeChecker()));
        }

        @Override
        protected Promise<Void> closeAction() {
            // TODO constant for zero-length buffer?
            return streamRequests.run(() -> {
                if (remaining != 0) {
                    throw new IllegalStateException("Closing while " + remaining + " bytes are not written");
                }
                return aSeq(() -> {
                    if (firstWrite) {
                        return writeHeader();
                    } else {
                        return aVoid();
                    }
                }).thenDoLast(output::send).toVoid().listen(outcomeChecker());
            }).listen(resolution -> {
                if (isValid()) {
                    finished.resolve(resolution);
                }
            });
        }

        @Override
        public AOutput<ByteBuffer> export(final Vat vat) {
            return NIOExportUtil.export(vat, this);
        }
    }

    /**
     * The output stream for frames.
     */
    private final class FrameOutputStream extends CloseableInvalidatingBase
            implements AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
        /**
         * The finished resolver.
         */
        private final AResolver<Void> finished;
        /**
         * The operation code for the first frame.
         */
        private final int firstFrameOpCode;
        /**
         * The request queue.
         */
        private final RequestQueue streamRequests = new RequestQueue();
        /**
         * True if the first frame.
         */
        private boolean firstFrame;

        /**
         * The constructor.
         *
         * @param finished         the finished event resolver
         * @param firstFrameOpCode the op code for the first frame
         */
        private FrameOutputStream(final AResolver<Void> finished, final int firstFrameOpCode) {
            this.finished = finished;
            this.firstFrameOpCode = firstFrameOpCode;
        }


        @Override
        protected void onInvalidation(final Throwable throwable) {
            notifyFailure(finished, throwable);
        }

        @Override
        public Promise<Void> write(final ByteBuffer buffer) {
            return streamRequests.run(() -> {
                int opCode;
                if (firstFrame) {
                    opCode = firstFrameOpCode;
                    firstFrame = false;
                } else {
                    opCode = FrameHeader.OP_CONTINUATION;
                }
                return sendFrame(buffer, false, opCode).listen(outcomeChecker());
            });
        }

        @Override
        public Promise<Void> flush() {
            return streamRequests.run(() -> output.send().thenFlatGet(() -> output.getOutput().flush())
                    .listen(outcomeChecker()));
        }

        @Override
        protected Promise<Void> closeAction() {
            // TODO constant for zero-length buffer?
            return streamRequests.run(() -> sendFrame(ByteBuffer.allocate(0), true, FrameHeader.OP_CONTINUATION)
                    .listen(outcomeChecker()))
                    .listen(resolution -> {
                        if (isValid()) {
                            finished.resolve(resolution);
                        }
                    });
        }

        @Override
        public AOutput<ByteBuffer> export() {
            return export(Vat.current());
        }

        @Override
        public AOutput<ByteBuffer> export(final Vat vat) {
            return NIOExportUtil.export(vat, this);
        }
    }

}
