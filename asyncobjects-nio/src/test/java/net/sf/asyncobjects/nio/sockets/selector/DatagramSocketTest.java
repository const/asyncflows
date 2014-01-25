package net.sf.asyncobjects.nio.sockets.selector;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.AFunction2;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.net.ADatagramSocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.blocking.BlockingSocketUtil;
import net.sf.asyncobjects.nio.net.selector.SelectorVatUtil;
import net.sf.asyncobjects.nio.util.ByteIOUtil;
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
        return new AFunction<Integer, ASocketFactory>() {
            @Override
            public Promise<Integer> apply(final ASocketFactory socketFactory) throws Throwable {
                return aTry(socketFactory.makeDatagramSocket()).andOther(socketFactory.makeDatagramSocket()).run(
                        new AFunction2<Integer, ADatagramSocket, ADatagramSocket>() {
                            @Override
                            public Promise<Integer> apply(final ADatagramSocket server, final ADatagramSocket client)
                                    throws Throwable {
                                return server.bind(new InetSocketAddress("localhost", 0)).map(
                                        new AFunction<Integer, SocketAddress>() {
                                            @Override
                                            public Promise<Integer> apply(final SocketAddress serverSocketAddress)
                                                    throws Throwable {
                                                return aAll(new ACallable<Integer>() {
                                                    @Override
                                                    public Promise<Integer> call() throws Throwable {
                                                        return echoServer(server);
                                                    }
                                                }).and(new ACallable<Void>() {
                                                    @Override
                                                    public Promise<Void> call() throws Throwable {
                                                        return runClient(client, serverSocketAddress);
                                                    }
                                                }).selectValue1();
                                            }
                                        });
                            }
                        });
            }
        };
    }

    @Test
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
        final ACallable<SocketAddress> receiveAction = new ACallable<SocketAddress>() {
            @Override
            public Promise<SocketAddress> call() throws Throwable {
                buffer.clear();
                return client.receive(buffer);
            }
        };
        return client.bind(new InetSocketAddress("localhost", 0)).map(new AFunction<Void, SocketAddress>() {
            @Override
            public Promise<Void> apply(final SocketAddress value) throws Throwable {
                buffer.clear();
                ByteIOUtil.putLatin1(buffer, "Test Data 1");
                buffer.flip();
                return client.send(serverSocketAddress, buffer);
            }
        }).thenDo(receiveAction).map(new AFunction<Void, SocketAddress>() {
            @Override
            public Promise<Void> apply(final SocketAddress value) throws Throwable {
                assertEquals(serverSocketAddress, value);
                buffer.flip();
                assertEquals("Test Data 1", ByteIOUtil.getLatin1(buffer));
                return client.connect(serverSocketAddress);
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                buffer.clear();
                ByteIOUtil.putLatin1(buffer, "Test Data 2");
                buffer.flip();
                return client.send(buffer);
            }
        }).thenDo(receiveAction).map(new AFunction<Void, SocketAddress>() {
            @Override
            public Promise<Void> apply(final SocketAddress value) throws Throwable {
                assertEquals(serverSocketAddress, value);
                buffer.flip();
                assertEquals("Test Data 2", ByteIOUtil.getLatin1(buffer));
                return client.disconnect();
            }
        }).mapOutcome(new AFunction<Void, Outcome<Void>>() {
            @Override
            public Promise<Void> apply(final Outcome<Void> value) throws Throwable {
                buffer.clear();
                ByteIOUtil.putLatin1(buffer, "Q");
                buffer.flip();
                return client.send(serverSocketAddress, buffer).thenPromise(Promise.forOutcome(value));
            }
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
        return aSeqMaybeLoop(new ACallable<Maybe<Integer>>() {
            @Override
            public Promise<Maybe<Integer>> call() throws Throwable {
                return server.receive(buffer).map(new AFunction<Maybe<Integer>, SocketAddress>() {
                    @Override
                    public Promise<Maybe<Integer>> apply(final SocketAddress value) throws Throwable {
                        buffer.flip();
                        if (buffer.remaining() == 1 && buffer.get(0) == 'Q') {
                            return aMaybeValue(count[0]);
                        } else {
                            return server.send(value, buffer).thenDo(new ACallable<Maybe<Integer>>() {
                                @Override
                                public Promise<Maybe<Integer>> call() throws Throwable {
                                    count[0]++;
                                    buffer.clear();
                                    return aMaybeEmpty();
                                }
                            });
                        }
                    }
                });
            }
        });
    }
}
