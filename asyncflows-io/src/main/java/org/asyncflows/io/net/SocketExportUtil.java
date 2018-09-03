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
                return aLater(() -> serverSocket.bind(address, backlog), vat);
            }

            @Override
            public Promise<SocketAddress> bind(final SocketAddress address) {
                return aLater(() -> serverSocket.bind(address), vat);
            }

            @Override
            public Promise<Void> setDefaultOptions(final SocketOptions options) {
                return aLater(() -> serverSocket.setDefaultOptions(options), vat);
            }

            @Override
            public Promise<SocketAddress> getLocalSocketAddress() {
                return aLater(serverSocket::getLocalSocketAddress, vat);
            }

            @Override
            public Promise<ASocket> accept() {
                return aLater(serverSocket::accept, acceptVat);
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
                return aLater(() -> socket.setOptions(options), vat);
            }

            @Override
            public Promise<Void> connect(final SocketAddress address) {
                return aLater(() -> socket.connect(address), vat);
            }

            @Override
            public Promise<SocketAddress> getRemoteAddress() {
                return aLater(socket::getRemoteAddress, vat);
            }

            @Override
            public Promise<SocketAddress> getLocalAddress() {
                return aLater(socket::getLocalAddress, vat);
            }

            @Override
            public Promise<AInput<ByteBuffer>> getInput() {
                return aLater(socket::getInput, vat);
            }

            @Override
            public Promise<AOutput<ByteBuffer>> getOutput() {
                return aLater(socket::getOutput, vat);
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
                return aLater(factory::makeSocket, vat);
            }

            @Override
            public Promise<AServerSocket> makeServerSocket() {
                return aLater(factory::makeServerSocket, vat);
            }

            @Override
            public Promise<ADatagramSocket> makeDatagramSocket() {
                return aLater(factory::makeDatagramSocket, vat);
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
                return aLater(() -> socket.setOptions(options), controlVat);
            }

            @Override
            public Promise<Void> connect(final SocketAddress address) {
                return aLater(() -> socket.connect(address), controlVat);
            }

            @Override
            public Promise<Void> disconnect() {
                return aLater(socket::disconnect, controlVat);
            }

            @Override
            public Promise<SocketAddress> getRemoteAddress() {
                return aLater(socket::getRemoteAddress, controlVat);
            }

            @Override
            public Promise<SocketAddress> getLocalAddress() {
                return aLater(socket::getLocalAddress, controlVat);
            }

            @Override
            public Promise<SocketAddress> bind(final SocketAddress address) {
                return aLater(() -> socket.bind(address), controlVat);
            }

            @Override
            public Promise<Void> send(final ByteBuffer buffer) {
                return aLater(() -> socket.send(buffer), sendVat);
            }

            @Override
            public Promise<Void> send(final SocketAddress address, final ByteBuffer buffer) {
                return aLater(() -> socket.send(address, buffer), sendVat);
            }

            @Override
            public Promise<SocketAddress> receive(final ByteBuffer buffer) {
                return aLater(() -> socket.receive(buffer), receiveVat);
            }

            @Override
            public Promise<Void> close() {
                return aLater(socket::close, controlVat);
            }
        };
    }
}
