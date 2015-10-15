package net.sf.asyncobjects.nio.sockets.ssl;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.selector.SelectorVatUtil;
import net.sf.asyncobjects.nio.net.ssl.SSLSocketFactory;
import net.sf.asyncobjects.nio.util.AbstractDigestingStream;
import net.sf.asyncobjects.nio.util.LimitedInput;
import org.junit.Test;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static net.sf.asyncobjects.nio.net.SocketUtil.aTrySocket;
import static net.sf.asyncobjects.nio.util.DigestingInput.digestAndDiscardInput;
import static net.sf.asyncobjects.nio.util.DigestingOutput.digestOutput;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * The test for SSL socket
 */
public class SSLSocketTest {
    /**
     * The smoke test for SSLSocket
     *
     * @throws Throwable in case of the problem
     */
    @Test
    public void smokeTest() throws Throwable { // NOPMD
        final SSLTestData sslTestData = new SSLTestData();
        final Random rnd = new Random();
        final long length = rnd.nextInt(10240) + 1024;
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result = SelectorVatUtil.runThrowable(rawSocketFactory -> { // NOPMD
            SSLSocketFactory sslFactory = new SSLSocketFactory();
            sslFactory.setSocketFactory(rawSocketFactory);
            sslFactory.setClientEngineFactory(value -> {
                final InetSocketAddress inetSocketAddress = (InetSocketAddress) value;
                SSLEngine rc = sslTestData.getSslClientContext().createSSLEngine(inetSocketAddress.getHostName(),
                        inetSocketAddress.getPort());
                rc.setUseClientMode(true);
                return aValue(rc);
            });
            sslFactory.setServerEngineFactory(value -> {
                final SSLEngine rc = sslTestData.getSslServerContext().createSSLEngine();
                rc.setUseClientMode(false);
                return aValue(rc);
            });

            final ASocketFactory socketFactory = sslFactory.export();
            return aTry(socketFactory.makeServerSocket()).run(
                    serverSocket -> serverSocket.bind(new InetSocketAddress(0)).map(
                            socketAddress -> aAll(
                                    () -> aTrySocket(serverSocket.accept()).run(
                                            (socket, input, output) -> IOUtil.BYTE.copy(
                                                    LimitedInput.limit(input, length), output, false,
                                                    ByteBuffer.allocate(1024))
                                    )
                            ).andLast(
                                    () -> aTry(socketFactory.makeSocket()).run(
                                            socket -> digestingClient(socket, socketAddress, rnd, length))
                            )
                    )
            );
        });
        assertEquals(result.getValue1(), result.getValue2().getValue3());
        assertArrayEquals(result.getValue2().getValue1(), result.getValue2().getValue2());
    }

    /**
     * The client that generates data
     *
     * @param socket        the socket
     * @param socketAddress the socket address
     * @param rnd           the random number generator
     * @param length        the data length
     * @return digests and data length
     */
    private Promise<Tuple3<byte[], byte[], Long>> digestingClient(final ASocket socket,
                                                                  final SocketAddress socketAddress,
                                                                  final Random rnd, final long length) {
        final int port = ((InetSocketAddress) socketAddress).getPort();
        final SocketAddress connectAddress = new InetSocketAddress("localhost", port);
        return socket.connect(connectAddress).thenDo(() -> aAll(() -> {
            final Promise<byte[]> digest = new Promise<>();
            return aTry(() -> {
                return socket.getOutput().map(
                        socketOutput -> aValue(digestOutput(socketOutput, digest.resolver()).md5()));
            }).run(output -> {
                final byte[] data = new byte[(int) length];
                rnd.nextBytes(data);
                return output.write(ByteBuffer.wrap(data));
            }).thenPromise(digest);
        }).and(
                () -> aTry(socket.getInput()).run(
                        socketInput -> digestAndDiscardInput(
                                LimitedInput.limit(socketInput, length),
                                AbstractDigestingStream.MD5)
                )
        ).andLast(constantCallable(length)));
    }
}
