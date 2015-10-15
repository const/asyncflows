package net.sf.asyncobjects.nio.sockets.selector;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.blocking.BlockingSocketUtil;
import net.sf.asyncobjects.nio.net.selector.SelectorVatUtil;
import net.sf.asyncobjects.nio.util.AbstractDigestingStream;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static net.sf.asyncobjects.nio.net.SocketUtil.aTrySocket;
import static net.sf.asyncobjects.nio.util.DigestingInput.digestAndDiscardInput;
import static net.sf.asyncobjects.nio.util.DigestingOutput.generateDigested;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
    @Ignore(value = "sometimes hangs")
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
                serverSocket -> serverSocket.bind(new InetSocketAddress(0)).map(
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
        return socket.connect(connectAddress).thenDo(
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
                ).andLast(constantCallable(length)));
    }

}
