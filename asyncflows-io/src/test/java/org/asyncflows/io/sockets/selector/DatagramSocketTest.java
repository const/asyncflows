package org.asyncflows.io.sockets.selector;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.blocking.BlockingSocketUtil;
import org.asyncflows.io.net.selector.SelectorVatUtil;
import org.asyncflows.io.util.ByteIOUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.ASupplier;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static org.asyncflows.core.AsyncControl.aMaybeEmpty;
import static org.asyncflows.core.AsyncControl.aMaybeValue;
import static org.asyncflows.core.AsyncControl.aOutcome;
import static org.asyncflows.core.util.AsyncAllControl.aAll;
import static org.asyncflows.core.util.AsyncSeqControl.aSeqUntilValue;
import static org.asyncflows.core.util.ResourceUtil.aTry;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private AFunction<ASocketFactory, Integer> createTestAction() {
        return socketFactory -> aTry(
                socketFactory.makeDatagramSocket()
        ).andOther(
                socketFactory.makeDatagramSocket()
        ).run((server, client) -> server.bind(new InetSocketAddress("localhost", 0)).flatMap(
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
        final ASupplier<SocketAddress> receiveAction = () -> {
            buffer.clear();
            return client.receive(buffer);
        };
        return client.bind(new InetSocketAddress("localhost", 0)).flatMap(value -> {
            buffer.clear();
            ByteIOUtil.putLatin1(buffer, "Test Data 1");
            buffer.flip();
            return client.send(serverSocketAddress, buffer);
        }).thenFlatGet(receiveAction).flatMap(value -> {
            assertEquals(serverSocketAddress, value);
            buffer.flip();
            assertEquals("Test Data 1", ByteIOUtil.getLatin1(buffer));
            return client.connect(serverSocketAddress);
        }).thenFlatGet(() -> {
            buffer.clear();
            ByteIOUtil.putLatin1(buffer, "Test Data 2");
            buffer.flip();
            return client.send(buffer);
        }).thenFlatGet(receiveAction).flatMap(value -> {
            assertEquals(serverSocketAddress, value);
            buffer.flip();
            assertEquals("Test Data 2", ByteIOUtil.getLatin1(buffer));
            return client.disconnect();
        }).flatMapOutcome(value -> {
            buffer.clear();
            ByteIOUtil.putLatin1(buffer, "Q");
            buffer.flip();
            return client.send(serverSocketAddress, buffer).thenPromise(aOutcome(value));
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
        return aSeqUntilValue(() -> server.receive(buffer).flatMap(value -> {
            buffer.flip();
            if (buffer.remaining() == 1 && buffer.get(0) == 'Q') {
                return aMaybeValue(count[0]);
            } else {
                return server.send(value, buffer).thenFlatGet(() -> {
                    count[0]++;
                    buffer.clear();
                    return aMaybeEmpty();
                });
            }
        }));
    }
}
