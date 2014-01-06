package net.sf.asyncobjects.nio.net.blocking;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.core.vats.Vats;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.adapters.InputStreamAdapter;
import net.sf.asyncobjects.nio.adapters.OutputStreamAdapter;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.SocketExportUtil;
import net.sf.asyncobjects.nio.net.SocketOptions;
import net.sf.asyncobjects.nio.net.SocketUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The blocking socket. Note that by default it exports on the daemon vat.
 */
public class BlockingSocket extends CloseableInvalidatingBase implements ASocket, ExportsSelf<ASocket> {
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
        return export(Vats.daemonVat());
    }

    @Override
    public ASocket export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
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
