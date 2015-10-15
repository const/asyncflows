package net.sf.asyncobjects.nio.sockets.selector;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.net.ADatagramSocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.blocking.BlockingSocketUtil;
import net.sf.asyncobjects.nio.net.selector.SelectorVatUtil;
import net.sf.asyncobjects.nio.util.ByteIOUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqMaybeLoop;
import static org.junit.Assert.assertEquals;

/**
 * The test for datagram sockets.
 */
public class DatagramSocketTest {

    @Test
    public void testSelector() throws Throwable {
        final int count = SelectorVatUtil.runThrowable(createTestAction());
        assertEquals(2, count);
    }

    /**
     * @return an action that runs test in any vat
     */
    private AFunction<Integer, ASocketFactory> createTestAction() {
        return socketFactory -> aTry(
                socketFactory.makeDatagramSocket()
        ).andOther(
                socketFactory.makeDatagramSocket()
        ).run((server, client) -> server.bind(new InetSocketAddress("localhost", 0)).map(
                        serverSocketAddress -> aAll(
                                () -> echoServer(server)
                        ).and(
                                () -> runClient(client, serverSocketAddress)
                        ).selectValue1()
                )
        );
    }

    @Test
    @Ignore
    public void testBlocking() throws Throwable {
        final int count = BlockingSocketUtil.runThrowable(createTestAction());
        assertEquals(2, count);
    }

    /**
     * Run client.
     *
     * @param client              the client socket
     * @param serverSocketAddress the socket address for the client.
     * @return when script finishes
     */
    private Promise<Void> runClient(final ADatagramSocket client, final SocketAddress serverSocketAddress) {
        final ByteBuffer buffer = ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
        final ACallable<SocketAddress> receiveAction = () -> {
            buffer.clear();
            return client.receive(buffer);
        };
        return client.bind(new InetSocketAddress("localhost", 0)).map(value -> {
            buffer.clear();
            ByteIOUtil.putLatin1(buffer, "Test Data 1");
            buffer.flip();
            return client.send(serverSocketAddress, buffer);
        }).thenDo(receiveAction).map(value -> {
            assertEquals(serverSocketAddress, value);
            buffer.flip();
            assertEquals("Test Data 1", ByteIOUtil.getLatin1(buffer));
            return client.connect(serverSocketAddress);
        }).thenDo(() -> {
            buffer.clear();
            ByteIOUtil.putLatin1(buffer, "Test Data 2");
            buffer.flip();
            return client.send(buffer);
        }).thenDo(receiveAction).map(value -> {
            assertEquals(serverSocketAddress, value);
            buffer.flip();
            assertEquals("Test Data 2", ByteIOUtil.getLatin1(buffer));
            return client.disconnect();
        }).mapOutcome(value -> {
            buffer.clear();
            ByteIOUtil.putLatin1(buffer, "Q");
            buffer.flip();
            return client.send(serverSocketAddress, buffer).thenPromise(Promise.forOutcome(value));
        });
    }

    /**
     * The echo server that gets datagrams until datagram with single byte 'Q' is received.
     *
     * @param server the server
     * @return the amount of datagrams until "Q"
     */
    private Promise<Integer> echoServer(final ADatagramSocket server) {
        final ByteBuffer buffer = ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
        final int[] count = new int[1];
        return aSeqMaybeLoop(() -> server.receive(buffer).map(value -> {
            buffer.flip();
            if (buffer.remaining() == 1 && buffer.get(0) == 'Q') {
                return aMaybeValue(count[0]);
            } else {
                return server.send(value, buffer).thenDo(() -> {
                    count[0]++;
                    buffer.clear();
                    return aMaybeEmpty();
                });
            }
        }));
    }
}
