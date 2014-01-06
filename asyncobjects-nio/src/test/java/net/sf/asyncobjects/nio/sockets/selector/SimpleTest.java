package net.sf.asyncobjects.nio.sockets.selector;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.core.util.AFunction3;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.selector.SelectorVatUtil;
import net.sf.asyncobjects.nio.util.AbstractDigestingStream;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.nio.net.SocketUtil.aTrySocket;
import static net.sf.asyncobjects.nio.util.DigestingInput.digestAndDiscardInput;
import static net.sf.asyncobjects.nio.util.DigestingOutput.digestOutput;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * The simple test
 */
public class SimpleTest {
    @Test
    public void test() throws Throwable {
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result = SelectorVatUtil.runThrowable(new AFunction<Tuple2<Long, Tuple3<byte[], byte[], Long>>, ASocketFactory>() {
            @Override
            public Promise<Tuple2<Long, Tuple3<byte[], byte[], Long>>> apply(final ASocketFactory socketFactory) throws Throwable {
                return aTry(socketFactory.makeServerSocket()).run(new AFunction<Tuple2<Long, Tuple3<byte[], byte[], Long>>, AServerSocket>() {
                    @Override
                    public Promise<Tuple2<Long, Tuple3<byte[], byte[], Long>>> apply(final AServerSocket serverSocket) throws Throwable {
                        return aSeq(new ACallable<SocketAddress>() {
                            @Override
                            public Promise<SocketAddress> call() throws Throwable {
                                return serverSocket.bind(new InetSocketAddress(0)).thenDo(new ACallable<SocketAddress>() {
                                    @Override
                                    public Promise<SocketAddress> call() throws Throwable {
                                        return serverSocket.getLocalSocketAddress();
                                    }
                                });
                            }
                        }).mapLast(new AFunction<Tuple2<Long, Tuple3<byte[], byte[], Long>>, SocketAddress>() {
                            @Override
                            public Promise<Tuple2<Long, Tuple3<byte[], byte[], Long>>> apply(final SocketAddress socketAddress) throws Throwable {
                                return aAll(new ACallable<Long>() {
                                    @Override
                                    public Promise<Long> call() throws Throwable {
                                        return aTrySocket(serverSocket.accept()).run(new AFunction3<Long, ASocket, AInput<ByteBuffer>, AOutput<ByteBuffer>>() {
                                            @Override
                                            public Promise<Long> apply(final ASocket socket, final AInput<ByteBuffer> input, final AOutput<ByteBuffer> output) throws Throwable {
                                                return IOUtil.BYTE.copy(input, output, false, ByteBuffer.allocate(1024));
                                            }
                                        });
                                    }
                                }).andLast(new ACallable<Tuple3<byte[], byte[], Long>>() {
                                    @Override
                                    public Promise<Tuple3<byte[], byte[], Long>> call() throws Throwable {
                                        return aTry(socketFactory.makeSocket()).run(new AFunction<Tuple3<byte[], byte[], Long>, ASocket>() {
                                            @Override
                                            public Promise<Tuple3<byte[], byte[], Long>> apply(final ASocket socket) throws Throwable {
                                                final Random rnd = new Random();
                                                final long length = rnd.nextInt(10240) + 1024;

                                                final SocketAddress connectAddress = new InetSocketAddress("localhost", ((InetSocketAddress) socketAddress).getPort());
                                                return socket.connect(connectAddress).thenDo(new ACallable<Tuple3<byte[], byte[], Long>>() {
                                                    @Override
                                                    public Promise<Tuple3<byte[], byte[], Long>> call() throws Throwable {
                                                        return aAll(new ACallable<byte[]>() {
                                                            @Override
                                                            public Promise<byte[]> call() throws Throwable {
                                                                final Promise<byte[]> digest = new Promise<byte[]>();
                                                                return aTry(new ACallable<AOutput<ByteBuffer>>() {
                                                                    @Override
                                                                    public Promise<AOutput<ByteBuffer>> call() throws Throwable {
                                                                        return socket.getOutput().map(new AFunction<AOutput<ByteBuffer>, AOutput<ByteBuffer>>() {
                                                                            @Override
                                                                            public Promise<AOutput<ByteBuffer>> apply(final AOutput<ByteBuffer> output) throws Throwable {
                                                                                return aValue(digestOutput(output, digest.resolver()).md5());
                                                                            }
                                                                        });
                                                                    }
                                                                }).run(new AFunction<Void, AOutput<ByteBuffer>>() {
                                                                    @Override
                                                                    public Promise<Void> apply(final AOutput<ByteBuffer> output) throws Throwable {
                                                                        byte[] data = new byte[(int) length];
                                                                        rnd.nextBytes(data);
                                                                        return output.write(ByteBuffer.wrap(data));
                                                                    }
                                                                }).thenDo(promiseCallable(digest));
                                                            }
                                                        }).and(new ACallable<byte[]>() {
                                                            @Override
                                                            public Promise<byte[]> call() throws Throwable {
                                                                return aTry(socket.getInput()).run(new AFunction<byte[], AInput<ByteBuffer>>() {
                                                                    @Override
                                                                    public Promise<byte[]> apply(final AInput<ByteBuffer> value) throws Throwable {
                                                                        return digestAndDiscardInput(value, AbstractDigestingStream.MD5);
                                                                    }
                                                                });
                                                            }
                                                        }).andLast(constantCallable(length));
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertEquals(result.getValue1(), result.getValue2().getValue3());
        assertArrayEquals(result.getValue2().getValue1(), result.getValue2().getValue2());
    }
}
