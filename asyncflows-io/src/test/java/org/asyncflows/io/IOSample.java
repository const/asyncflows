package org.asyncflows.io;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.FailFast;
import org.asyncflows.core.util.SimpleQueue;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.FailFast.failFast;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.asyncflows.io.IOUtil.isEof;

public class IOSample {

    /**
     * Copy operation stream operation. The stream is copied until EOF on input is reached.
     *
     * @param input  the input stream
     * @param output the output stream
     * @return the amount of bytes read then written (bytes already in the buffer are not counted)
     */
    public static Promise<Long> copy(final AInput<ByteBuffer> input, final AOutput<ByteBuffer> output, int bufferSize) {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        final long[] result = new long[1];
        return aSeqWhile(
                () -> input.read(buffer).flatMap(value -> {
                    if (isEof(value)) {
                        return aFalse();
                    } else {
                        result[0] += +value;
                        buffer.flip();
                        return output.write(buffer).thenFlatGet(() -> {
                            buffer.compact();
                            return aTrue();
                        });
                    }
                })
        ).thenGet(() -> result[0]);
    }

    /**
     * Copy operation stream operation. The stream is copied until EOF on input is reached.
     *
     * @param input  the input stream
     * @param output the output stream
     * @return the amount of bytes read then written (bytes already in the buffer are not counted)
     */
    public static Promise<Long> copy(final AInput<ByteBuffer> input, final AOutput<ByteBuffer> output, int bufferSize, int buffers) {
        final SimpleQueue<ByteBuffer> readQueue = new SimpleQueue<>();
        final SimpleQueue<ByteBuffer> writeQueue = new SimpleQueue<>();
        final FailFast failFast = failFast();
        for (int i = 0; i < buffers; i++) {
            readQueue.put(ByteBuffer.allocate(bufferSize));
        }
        final long[] result = new long[1];
        return aAll(
                () -> aSeqWhile(
                        () -> failFast.run(readQueue::take).flatMap(
                                b -> failFast.run(() -> input.read(b)).flatMap(c -> {
                            if (isEof(c)) {
                                writeQueue.put(null);
                                return aFalse();
                            } else {
                                result[0] += c;
                                writeQueue.put(b);
                                return aTrue();
                            }
                        }))
                )
        ).and(
                () -> aSeqWhile(
                        () -> failFast.run(writeQueue::take).flatMap(b -> {
                            if(b == null) {
                                return aFalse();
                            } else {
                                b.flip();
                                return failFast.run(() -> output.write(b)).thenGet(() -> {
                                    b.compact();
                                    readQueue.put(b);
                                    return true;
                                });
                            }
                        })
                )
        ).map((a, b) -> aValue(result[0]));
    }


}
