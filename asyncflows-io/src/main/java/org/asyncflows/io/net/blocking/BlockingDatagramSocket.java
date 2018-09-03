package org.asyncflows.io.net.blocking;

import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.SocketExportUtil;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.net.SocketUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.core.util.NeedsExport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The blocking datagram socket.
 */
public class BlockingDatagramSocket implements ADatagramSocket, NeedsExport<ADatagramSocket> {
    /**
     * The socket.
     */
    private final DatagramSocket socket;

    /**
     * The constructor.
     *
     * @throws SocketException if socket could not be created.
     */
    public BlockingDatagramSocket() throws SocketException {
        this(new DatagramSocket(null));
    }

    /**
     * The constructor from datagram socket.
     *
     * @param socket a socket
     */
    public BlockingDatagramSocket(final DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public Promise<Void> setOptions(final SocketOptions options) {
        try {
            SocketUtil.applyOptions(socket, options);
            return aVoid();
        } catch (SocketException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> connect(final SocketAddress address) {
        try {
            socket.connect(address);
            return aVoid();
        } catch (SocketException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> disconnect() {
        socket.disconnect();
        return aVoid();
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return aValue(socket.getRemoteSocketAddress());
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return aValue(socket.getLocalSocketAddress());
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        try {
            socket.bind(address);
            return getLocalAddress();
        } catch (SocketException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> send(final ByteBuffer buffer) {
        final byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        try {
            socket.send(new DatagramPacket(data, 0, data.length));
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> send(final SocketAddress address, final ByteBuffer buffer) {
        final byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        try {
            socket.send(new DatagramPacket(data, 0, data.length, address));
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<SocketAddress> receive(final ByteBuffer buffer) {
        final byte[] data;
        final int offset;
        final int length;
        if (buffer.hasArray()) {
            data = buffer.array();
            offset = buffer.arrayOffset() + buffer.position();
            length = buffer.remaining();
        } else {
            data = new byte[buffer.remaining()];
            offset = 0;
            length = data.length;
        }
        final DatagramPacket datagramPacket = new DatagramPacket(data, offset, length);
        try {
            socket.receive(datagramPacket);
            if (buffer.hasArray()) {
                buffer.position(buffer.position() + datagramPacket.getLength());
            } else {
                buffer.put(data, offset, datagramPacket.getLength());
            }
            return aValue(datagramPacket.getSocketAddress());
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> close() {
        socket.close();
        return aVoid();
    }

    @Override
    public ADatagramSocket export() {
        return SocketExportUtil.export(Vats.daemonVat(), Vats.daemonVat(), Vats.daemonVat(), this);
    }

    @Override
    public ADatagramSocket export(final Vat vat) {
        return export();
    }
}
