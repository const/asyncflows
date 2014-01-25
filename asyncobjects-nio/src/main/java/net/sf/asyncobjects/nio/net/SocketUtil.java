package net.sf.asyncobjects.nio.net;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.ResourceUtil;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.IOUtil;

import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;

/**
 * Socket utilities.
 */
public final class SocketUtil {
    /**
     * The private constructor for utility class.
     */
    private SocketUtil() {
    }

    /**
     * Apply socket options to the {@link java.net.Socket}.
     *
     * @param socket  the socket
     * @param options the options.
     * @throws SocketException if the option could not be set
     */
    public static void applyOptions(final Socket socket, final SocketOptions options) throws SocketException { // NOPMD
        final Boolean tpcNoDelay = options.getTpcNoDelay();
        if (tpcNoDelay != null) {
            socket.setTcpNoDelay(tpcNoDelay);
        }
        final Boolean keepAlive = options.getKeepAlive();
        if (keepAlive != null) {
            socket.setKeepAlive(keepAlive);
        }
        final Tuple2<Boolean, Integer> linger = options.getLinger();
        if (linger != null) {
            if (linger.getValue1() == null) {
                throw new IllegalArgumentException("The 'on' component of SO_LINGER is null");
            }
            if (linger.getValue1() && linger.getValue2() == null) {
                throw new IllegalArgumentException("The 'linger' component of SO_LINGER is null");
            }
            socket.setSoLinger(linger.getValue1(), linger.getValue1() ? linger.getValue2() : 0);
        }
        final Boolean oobInline = options.getOobInline();
        if (oobInline != null) {
            socket.setOOBInline(oobInline);
        }
        final Integer receiveBufferSize = options.getReceiveBufferSize();
        if (receiveBufferSize != null) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }
        final Integer sendBufferSize = options.getSendBufferSize();
        if (sendBufferSize != null) {
            socket.setSendBufferSize(sendBufferSize);
        }
        final Integer timeout = options.getTimeout();
        if (timeout != null) {
            socket.setSoTimeout(timeout);
        }
        final Integer trafficClass = options.getTrafficClass();
        if (trafficClass != null) {
            socket.setTrafficClass(trafficClass);
        }
    }

    /**
     * Apply socket options to the {@link java.net.DatagramSocket}.
     *
     * @param socket  the socket
     * @param options the options.
     * @throws SocketException if the option could not be set
     */
    public static void applyOptions(final DatagramSocket socket,
                                    final SocketOptions options) throws SocketException { // NOPMD
        final Integer receiveBufferSize = options.getReceiveBufferSize();
        if (receiveBufferSize != null) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }
        final Integer sendBufferSize = options.getSendBufferSize();
        if (sendBufferSize != null) {
            socket.setSendBufferSize(sendBufferSize);
        }
        final Integer timeout = options.getTimeout();
        if (timeout != null) {
            socket.setSoTimeout(timeout);
        }
        final Integer trafficClass = options.getTrafficClass();
        if (trafficClass != null) {
            socket.setTrafficClass(trafficClass);
        }
        final Boolean broadcast = options.getBroadcast();
        if (broadcast != null) {
            socket.setBroadcast(broadcast);
        }
        final Boolean reuseAddress = options.getReuseAddress();
        if (reuseAddress != null) {
            socket.setReuseAddress(reuseAddress);
        }
    }

    /**
     * Try channel.
     *
     * @param socket a socket
     * @return a try operation on the socket
     */
    public static ResourceUtil.Try3<ASocket, AInput<ByteBuffer>, AOutput<ByteBuffer>> aTrySocket(
            final ACallable<ASocket> socket) {
        return IOUtil.BYTE.tryChannel(socket);
    }

    /**
     * Try channel.
     *
     * @param socket a socket
     * @return a try operation on the socket
     */
    public static ResourceUtil.Try3<ASocket, AInput<ByteBuffer>, AOutput<ByteBuffer>> aTrySocket(
            final Promise<ASocket> socket) {
        return aTrySocket(promiseCallable(socket));
    }
}
