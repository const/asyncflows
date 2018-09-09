package org.asyncflows.io.net; // NOPMD

import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.util.CoreFlowsResource.closeResource;

/**
 * Export utilities for the socket.
 */
public final class SocketExportUtil { // NOPMD

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
        return export(vat, vat, serverSocket);
    }

    /**
     * Export server socket.
     *
     * @param vat          the vat
     * @param acceptVat    the vat for accept operation
     * @param serverSocket the server socket
     * @return the exported server socket
     */
    public static AServerSocket export(final Vat vat, final Vat acceptVat, final AServerSocket serverSocket) {
        return new AServerSocket() {
            @Override
            public Promise<SocketAddress> bind(final SocketAddress address, final int backlog) {
                return aLater(vat, () -> serverSocket.bind(address, backlog));
            }

            @Override
            public Promise<SocketAddress> bind(final SocketAddress address) {
                return aLater(vat, () -> serverSocket.bind(address));
            }

            @Override
            public Promise<Void> setDefaultOptions(final SocketOptions options) {
                return aLater(vat, () -> serverSocket.setDefaultOptions(options));
            }

            @Override
            public Promise<SocketAddress> getLocalSocketAddress() {
                return aLater(vat, serverSocket::getLocalSocketAddress);
            }

            @Override
            public Promise<ASocket> accept() {
                return aLater(acceptVat, serverSocket::accept);
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
        return export(vat, vat, socket);
    }

    /**
     * Export a socket.
     *
     * @param vat      the vat
     * @param closeVat the vat on which socket is closed
     * @param socket   the socket
     * @return the exported socket
     */
    public static ASocket export(final Vat vat, final Vat closeVat, final ASocket socket) {
        return new ASocket() {
            @Override
            public Promise<Void> setOptions(final SocketOptions options) {
                return aLater(vat, () -> socket.setOptions(options));
            }

            @Override
            public Promise<Void> connect(final SocketAddress address) {
                return aLater(vat, () -> socket.connect(address));
            }

            @Override
            public Promise<SocketAddress> getRemoteAddress() {
                return aLater(vat, socket::getRemoteAddress);
            }

            @Override
            public Promise<SocketAddress> getLocalAddress() {
                return aLater(vat, socket::getLocalAddress);
            }

            @Override
            public Promise<AInput<ByteBuffer>> getInput() {
                return aLater(vat, socket::getInput);
            }

            @Override
            public Promise<AOutput<ByteBuffer>> getOutput() {
                return aLater(vat, socket::getOutput);
            }

            @Override
            public Promise<Void> close() {
                return closeResource(closeVat, socket);
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
                return aLater(vat, factory::makeSocket);
            }

            @Override
            public Promise<AServerSocket> makeServerSocket() {
                return aLater(vat, factory::makeServerSocket);
            }

            @Override
            public Promise<ADatagramSocket> makeDatagramSocket() {
                return aLater(vat, factory::makeDatagramSocket);
            }
        };
    }

    /**
     * A single vat version.
     *
     * @param vat    the vat
     * @param socket the socket to wrap
     * @return a socket wrapped into the proxy.
     */
    public static ADatagramSocket export(final Vat vat, final ADatagramSocket socket) {
        return export(vat, vat, vat, socket);
    }

    /**
     * A three vat version of the datagram socket that uses different vats for the operations.
     * It is needed to wrap blocking version of the datagram socket.
     *
     * @param controlVat the control vat
     * @param receiveVat the receive operation vat
     * @param sendVat    the send operation vat
     * @param socket     the socket to wrap
     * @return a socket wrapped into the proxy.
     */
    public static ADatagramSocket export(final Vat controlVat, final Vat receiveVat, final Vat sendVat, // NOPMD
                                         final ADatagramSocket socket) {
        return new ADatagramSocket() {
            @Override
            public Promise<Void> setOptions(final SocketOptions options) {
                return aLater(controlVat, () -> socket.setOptions(options));
            }

            @Override
            public Promise<Void> connect(final SocketAddress address) {
                return aLater(controlVat, () -> socket.connect(address));
            }

            @Override
            public Promise<Void> disconnect() {
                return aLater(controlVat, socket::disconnect);
            }

            @Override
            public Promise<SocketAddress> getRemoteAddress() {
                return aLater(controlVat, socket::getRemoteAddress);
            }

            @Override
            public Promise<SocketAddress> getLocalAddress() {
                return aLater(controlVat, socket::getLocalAddress);
            }

            @Override
            public Promise<SocketAddress> bind(final SocketAddress address) {
                return aLater(controlVat, () -> socket.bind(address));
            }

            @Override
            public Promise<Void> send(final ByteBuffer buffer) {
                return aLater(sendVat, () -> socket.send(buffer));
            }

            @Override
            public Promise<Void> send(final SocketAddress address, final ByteBuffer buffer) {
                return aLater(sendVat, () -> socket.send(address, buffer));
            }

            @Override
            public Promise<SocketAddress> receive(final ByteBuffer buffer) {
                return aLater(receiveVat, () -> socket.receive(buffer));
            }

            @Override
            public Promise<Void> close() {
                return aLater(controlVat, socket::close);
            }
        };
    }
}
