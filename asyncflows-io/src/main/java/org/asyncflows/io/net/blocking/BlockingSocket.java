package org.asyncflows.io.net.blocking;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.adapters.InputStreamAdapter;
import org.asyncflows.io.adapters.OutputStreamAdapter;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketExportUtil;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.net.SocketUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.core.util.CloseableInvalidatingBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static org.asyncflows.core.AsyncControl.aFailure;
import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.AsyncControl.aVoid;

/**
 * The blocking socket. Note that by default it exports on the daemon vat.
 */
public class BlockingSocket extends CloseableInvalidatingBase implements ASocket, NeedsExport<ASocket> {
    /**
     * The socket to use.
     */
    private final Socket socket;
    /**
     * The input for the socket.
     */
    private AInput<ByteBuffer> input;
    /**
     * The output for the socket.
     */
    private AOutput<ByteBuffer> output;

    /**
     * The constructor.
     *
     * @param socket the socket to use
     */
    public BlockingSocket(final Socket socket) {
        this.socket = socket;
    }

    /**
     * The constructor from new socket.
     */
    public BlockingSocket() {
        this(new Socket());
    }

    @Override
    public Promise<Void> setOptions(final SocketOptions options) {
        try {
            SocketUtil.applyOptions(socket, options);
            return aVoid();
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> connect(final SocketAddress address) {
        try {
            socket.connect(address);
            return aVoid();
        } catch (Throwable e) {
            return aFailure(e);
        }
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
    public Promise<AInput<ByteBuffer>> getInput() {
        try {
            if (input == null) {
                input = new SocketInput(socket.getInputStream()).exportBlocking();
            }
            return aValue(input);
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<AOutput<ByteBuffer>> getOutput() {
        try {
            if (output == null) {
                output = new SocketOutput(socket.getOutputStream()).exportBlocking();
            }
            return aValue(output);
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            socket.close();
            return super.closeAction();
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public ASocket export() {
        return SocketExportUtil.export(Vats.daemonVat(), Vats.daemonVat(), this);
    }

    @Override
    public ASocket export(final Vat vat) {
        return export();
    }

    /**
     * The socket input stream.
     */
    private class SocketInput extends InputStreamAdapter {

        /**
         * The constructor.
         *
         * @param stream the input stream
         */
        public SocketInput(final InputStream stream) {
            super(stream);
        }

        @Override
        protected void closeStream(final InputStream streamToClose) throws IOException {
            socket.shutdownInput();
        }
    }

    /**
     * The socket input stream.
     */
    private class SocketOutput extends OutputStreamAdapter {
        /**
         * The constructor.
         *
         * @param stream the input stream
         */
        public SocketOutput(final OutputStream stream) {
            super(stream);
        }

        @Override
        protected void closeStream(final OutputStream streamToClose) throws IOException {
            socket.shutdownOutput();
        }
    }
}
