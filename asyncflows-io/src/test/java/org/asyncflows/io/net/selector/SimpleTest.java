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

package org.asyncflows.io.net.selector;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.data.Tuple3;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.async.AsyncSocketUtil;
import org.asyncflows.io.net.blocking.BlockingSocketUtil;
import org.asyncflows.io.util.AbstractDigestingStream;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.asyncflows.core.function.AsyncFunctionUtil.constantSupplier;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.io.net.SocketUtil.aTrySocket;
import static org.asyncflows.io.util.DigestingInput.digestAndDiscardInput;
import static org.asyncflows.io.util.DigestingOutput.generateDigested;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The simple test
 */
public class SimpleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTest.class);

    @Test
    public void testSelector() throws Throwable {
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result =
                SelectorVatUtil.doAsyncIoThrowable(this::checkSocketFactory);
        assertEquals(result.getValue1(), result.getValue2().getValue3());
        assertArrayEquals(result.getValue2().getValue1(), result.getValue2().getValue2());
    }

    @Test
    public void testBlocking() throws Throwable {
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result =
                BlockingSocketUtil.runThrowable(this::checkSocketFactory);
        assertEquals(result.getValue1(), result.getValue2().getValue3());
        assertArrayEquals(result.getValue2().getValue1(), result.getValue2().getValue2());
    }


    @Test
    public void testAsync() throws Throwable {
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result =
                AsyncSocketUtil.runThrowable(this::checkSocketFactory);
        assertEquals(result.getValue1(), result.getValue2().getValue3());
        assertArrayEquals(result.getValue2().getValue1(), result.getValue2().getValue2());
    }

    /**
     * Run smoke test for a socket factory.
     *
     * @param socketFactory a socket factory
     * @return the test result (stream size from server, (output digest, input digest, length).
     */
    private Promise<Tuple2<Long, Tuple3<byte[], byte[], Long>>> checkSocketFactory(final ASocketFactory socketFactory) {
        return aTry(socketFactory.makeServerSocket()).run(
                serverSocket -> serverSocket.bind(new InetSocketAddress(0)).flatMap(
                        socketAddress -> {
                            LOGGER.info("Server socket created: " + socketAddress);
                            return aAll(
                                    () -> aTrySocket(serverSocket.accept()).run(
                                            (socket, input, output) -> {
                                                LOGGER.info("Server: connection accepted");
                                                return IOUtil.BYTE.copy(input, output, false, ByteBuffer.allocate(1024));
                                            })
                                            .listen(o -> LOGGER.info("Server finished"))
                            ).andLast(
                                    () -> aTry(socketFactory.makeSocket()).run(
                                            socket -> digestingClient(socket, socketAddress))
                                            .listen(o -> LOGGER.info("Client finished"))
                            );
                        }
                )
        );
    }

    /**
     * The digesting client for the specified socket address.
     *
     * @param socket        the socket
     * @param socketAddress the socket address
     * @return output digest, input digest, and length
     */
    private Promise<Tuple3<byte[], byte[], Long>> digestingClient(final ASocket socket,
                                                                  final SocketAddress socketAddress) {
        final Random rnd = new Random();
        final long length = rnd.nextInt(10240) + 1024;

        final SocketAddress connectAddress = new InetSocketAddress("localhost",
                ((InetSocketAddress) socketAddress).getPort());
        return socket.connect(connectAddress).thenFlatGet(
                () -> aAll(
                        () -> generateDigested(socket.getOutput(),
                                AbstractDigestingStream.MD5,
                                output -> {
                                    final byte[] data = new byte[(int) length];
                                    rnd.nextBytes(data);
                                    return output.write(ByteBuffer.wrap(data));
                                }).listen(o -> LOGGER.info("Client: finished generating data"))
                ).and(
                        () -> aTry(socket.getInput()).run(
                                input -> digestAndDiscardInput(input, AbstractDigestingStream.MD5)
                        ).listen(o -> LOGGER.info("Client: finished reading data"))
                ).andLast(constantSupplier(length)));
    }

}
