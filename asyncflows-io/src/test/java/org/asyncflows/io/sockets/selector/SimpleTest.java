package org.asyncflows.io.sockets.selector;

import org.asyncflows.io.IOUtil;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.blocking.BlockingSocketUtil;
import org.asyncflows.io.net.selector.SelectorVatUtil;
import org.asyncflows.io.util.AbstractDigestingStream;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.data.Tuple3;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.asyncflows.io.net.SocketUtil.aTrySocket;
import static org.asyncflows.io.util.DigestingInput.digestAndDiscardInput;
import static org.asyncflows.io.util.DigestingOutput.generateDigested;
import static org.asyncflows.core.function.AsyncFunctionUtil.constantSupplier;
import static org.asyncflows.core.util.AsyncAllControl.aAll;
import static org.asyncflows.core.util.ResourceUtil.aTry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The simple test
 */
public class SimpleTest {
    @Test
    public void testSelector() throws Throwable {
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result =
                SelectorVatUtil.runThrowable(this::checkSocketFactory);
        assertEquals(result.getValue1(), result.getValue2().getValue3());
        assertArrayEquals(result.getValue2().getValue1(), result.getValue2().getValue2());
    }

    @Test
    @Disabled("sometimes hangs")
    public void testBlocking() throws Throwable {
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result =
                BlockingSocketUtil.runThrowable(this::checkSocketFactory);
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
                        socketAddress -> aAll(
                                () -> aTrySocket(serverSocket.accept()).run(
                                        (socket, input, output) ->
                                                IOUtil.BYTE.copy(input, output, false, ByteBuffer.allocate(1024)))
                        ).andLast(
                                () -> aTry(socketFactory.makeSocket()).run(
                                        socket -> digestingClient(socket, socketAddress))
                        )
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
                                })
                ).and(
                        () -> aTry(socket.getInput()).run(
                                input -> digestAndDiscardInput(input, AbstractDigestingStream.MD5)
                        )
                ).andLast(constantSupplier(length)));
    }

}
