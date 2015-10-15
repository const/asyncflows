package net.sf.asyncobjects.protocol.http.client.core;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.net.ADatagramSocket;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.SocketExportUtil;
import net.sf.asyncobjects.nio.net.SocketOptions;
import net.sf.asyncobjects.protocol.http.client.AHttpClient;
import net.sf.asyncobjects.protocol.http.client.AHttpRequest;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.HttpURIUtil;
import net.sf.asyncobjects.protocol.http.common.Scope;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;

/**
 * The socket factory.
 */
public class HttpConnectSocketFactory implements ASocketFactory, ExportsSelf<ASocketFactory> {
    /**
     * The http client (note that it should be closed, after the factory is not used anymore).
     */
    private final AHttpClient client;
    /**
     * The proxy host.
     */
    private final String proxyHost;

    /**
     * The constructor.
     *
     * @param client    the client to use
     * @param proxyHost the proxy host
     */
    public HttpConnectSocketFactory(final AHttpClient client, final String proxyHost) {
        this.client = client;
        this.proxyHost = proxyHost;
    }

    /**
     * @return the promise for a plain socket
     */
    @Override
    public Promise<ASocket> makeSocket() {
        return aValue(new ConnectSocket().export());
    }

    @Override
    public Promise<AServerSocket> makeServerSocket() {
        throw new UnsupportedOperationException("The operation is not supported");
    }

    @Override
    public Promise<ADatagramSocket> makeDatagramSocket() {
        throw new UnsupportedOperationException("The operation is not supported");
    }

    @Override
    public ASocketFactory export() {
        return export(Vat.current());
    }

    @Override
    public ASocketFactory export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }


    /**
     * The connect operation.
     */
    private class ConnectSocket extends CloseableInvalidatingBase implements ASocket, ExportsSelf<ASocket> {
        /**
         * The connect scope.
         */
        private final Scope scope = new Scope();
        /**
         * The http request to use.
         */
        private AHttpRequest httpRequest;
        /**
         * The input for the socket.
         */
        private AInput<ByteBuffer> input;
        /**
         * The output for the socket.
         */
        private AOutput<ByteBuffer> output;
        /**
         * The remote address.
         */
        private SocketAddress remoteAddress;

        @Override
        public Promise<Void> setOptions(final SocketOptions options) {
            // ignore this method.
            return aVoid();
        }

        @Override
        public Promise<Void> connect(final SocketAddress address) {
            if (address == null) {
                return aFailure(new ConnectException("Address must not be null"));
            }
            if (remoteAddress != null) {
                return aFailure(new ConnectException("Connect could be called only once"));
            }
            remoteAddress = address;
            return aSeq(client::newRequest).map(request -> {
                httpRequest = request;
                scope.set(AHttpRequest.CONNECTION_HOST, proxyHost);
                return httpRequest.request(scope,
                        HttpMethodUtil.CONNECT,
                        new URI("http://" + HttpURIUtil.getHost(address)),
                        new HttpHeaders(),
                        AHttpRequest.NO_CONTENT);
            }).map(ACloseable::close).thenDo(() -> {
                // TODO Http client and server options (NO_WAIT)
                // TODO get local address from the scope
                //noinspection Convert2MethodRef
                return httpRequest.getResponse();
            }).map(response -> {
                if (!HttpStatusUtil.isSuccess(response.getStatusCode())) {
                    throw new ConnectException("Unable to execute request: "
                            + response.getStatusCode() + " " + response.getReason());
                }
                if (response.getSwitchedChannel() == null) {
                    throw new ConnectException("No switch protocol happened.");
                }
                return aAll(
                        () -> response.getSwitchedChannel().getInput()
                ).and(
                        () -> response.getSwitchedChannel().getOutput()
                ).unzip((channelInput, channelOutput) -> {
                    input = channelInput;
                    output = channelOutput;
                    return aVoid();
                });
            }).failedLast(value -> {
                if (value instanceof SocketException) {
                    return aFailure(value);
                }
                final ConnectException exception = new ConnectException("Failed to connect: " + value.getMessage());
                exception.initCause(value);
                return aFailure(exception);
            });
        }

        @Override
        public Promise<SocketAddress> getRemoteAddress() {
            if (remoteAddress == null) {
                return aFailure(new SocketException("Socket not connected"));
            }
            return aValue(remoteAddress);
        }

        @Override
        public Promise<SocketAddress> getLocalAddress() {
            if (httpRequest == null) {
                return aFailure(new SocketException("Socket not connected"));
            }
            return httpRequest.getLocalAddress();
        }

        @Override
        public Promise<AInput<ByteBuffer>> getInput() {
            if (input == null) {
                return aFailure(new SocketException("Socket not connected"));
            }
            return aValue(input);
        }

        @Override
        public Promise<AOutput<ByteBuffer>> getOutput() {
            if (output == null) {
                return aFailure(new SocketException("Socket not connected"));
            }
            return aValue(output);
        }

        @Override
        protected Promise<Void> closeAction() {
            if (httpRequest != null) {
                return httpRequest.close();
            } else {
                return aVoid();
            }
        }

        @Override
        public ASocket export() {
            return export(Vat.current());
        }

        @Override
        public ASocket export(final Vat vat) {
            return SocketExportUtil.export(vat, this);
        }
    }
}
