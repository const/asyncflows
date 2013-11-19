package net.sf.asyncobjects.nio.net;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResource;

/**
 * Export utilities for the socket.
 */
public final class SocketExportUtil {
    /**
     * Private constructor for utility class.
     */
    private SocketExportUtil() {
    }

    /**
     * Export server socket.
     *
     * @param vat          the vat
     * @param serverSocket the server socket
     * @return the exported server socket
     */
    public static AServerSocket export(final Vat vat, final AServerSocket serverSocket) {
        return new AServerSocket() {
            @Override
            public Promise<Void> bind(final SocketAddress address, final int backlog) {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return serverSocket.bind(address, backlog);
                    }
                });
            }

            @Override
            public Promise<Void> bind(final SocketAddress address) {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return serverSocket.bind(address);
                    }
                });
            }

            @Override
            public Promise<Void> setDefaultOptions(final SocketOptions options) {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return serverSocket.setDefaultOptions(options);
                    }
                });
            }

            @Override
            public Promise<SocketAddress> getLocalSocketAddress() {
                return aLater(vat, new ACallable<SocketAddress>() {
                    @Override
                    public Promise<SocketAddress> call() throws Throwable {
                        return serverSocket.getLocalSocketAddress();
                    }
                });
            }

            @Override
            public Promise<ASocket> accept() {
                return aLater(vat, new ACallable<ASocket>() {
                    @Override
                    public Promise<ASocket> call() throws Throwable {
                        return serverSocket.accept();
                    }
                });
            }

            @Override
            public Promise<Void> close() {
                return closeResource(vat, serverSocket);
            }
        };
    }

    /**
     * Export a socket.
     *
     * @param vat    the vat
     * @param socket the socket
     * @return the exported socket
     */
    public static ASocket export(final Vat vat, final ASocket socket) {
        return new ASocket() {
            @Override
            public Promise<Void> setOptions(final SocketOptions options) {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return socket.setOptions(options);
                    }
                });
            }

            @Override
            public Promise<Void> connect(final SocketAddress address) {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return socket.connect(address);
                    }
                });
            }

            @Override
            public Promise<SocketAddress> getRemoteAddress() {
                return aLater(vat, new ACallable<SocketAddress>() {
                    @Override
                    public Promise<SocketAddress> call() throws Throwable {
                        return socket.getRemoteAddress();
                    }
                });
            }

            @Override
            public Promise<AInput<ByteBuffer>> getInput() {
                return aLater(vat, new ACallable<AInput<ByteBuffer>>() {
                    @Override
                    public Promise<AInput<ByteBuffer>> call() throws Throwable {
                        return socket.getInput();
                    }
                });
            }

            @Override
            public Promise<AOutput<ByteBuffer>> getOutput() {
                return aLater(vat, new ACallable<AOutput<ByteBuffer>>() {
                    @Override
                    public Promise<AOutput<ByteBuffer>> call() throws Throwable {
                        return socket.getOutput();
                    }
                });
            }

            @Override
            public Promise<Void> close() {
                return closeResource(vat, socket);
            }
        };
    }

    /**
     * Export a socket factory.
     *
     * @param vat     the vat
     * @param factory the factory
     * @return the exported factory
     */
    public static ASocketFactory export(final Vat vat, final ASocketFactory factory) {
        return new ASocketFactory() {
            @Override
            public Promise<ASocket> makeSocket() {
                return aLater(vat, new ACallable<ASocket>() {
                    @Override
                    public Promise<ASocket> call() throws Throwable {
                        return factory.makeSocket();
                    }
                });
            }

            @Override
            public Promise<AServerSocket> makeServerSocket() {
                return aLater(vat, new ACallable<AServerSocket>() {
                    @Override
                    public Promise<AServerSocket> call() throws Throwable {
                        return factory.makeServerSocket();
                    }
                });
            }
        };
    }
}
