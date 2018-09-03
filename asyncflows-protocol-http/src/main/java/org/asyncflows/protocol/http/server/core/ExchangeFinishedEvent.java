package org.asyncflows.protocol.http.server.core;

import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.content.StreamFinishedEvent;

import java.net.SocketAddress;
import java.net.URI;

/**
 * The exchange finished event (this event is used for reporting).
 */
public final class ExchangeFinishedEvent {
    /**
     * Key used to report remote address.
     */
    public static final Scope.Key<String> REMOTE = new Scope.Key<>(ExchangeFinishedEvent.class, "remote");
    /**
     * The key used to report bytes transferred to remote server.
     */
    public static final Scope.Key<StreamFinishedEvent> REMOTE_TO_SERVER
            = new Scope.Key<>(ExchangeFinishedEvent.class, "remoteToServer");
    /**
     * The key used to report bytes transferred from remote server.
     */
    public static final Scope.Key<StreamFinishedEvent> SERVER_TO_REMOTE
            = new Scope.Key<>(ExchangeFinishedEvent.class, "serverToRemote");
    /**
     * The request id on the server.
     */
    private final String id;
    /**
     * The client socket address.
     */
    private final SocketAddress clientAddress;
    /**
     * The server socket address.
     */
    private final SocketAddress serverAddress;
    /**
     * The method.
     */
    private final String method;
    /**
     * The effective request URI.
     */
    private final URI request;
    /**
     * The HTTP version.
     */
    private final String version;
    /**
     * The amount transferred from client to server.
     */
    private final StreamFinishedEvent clientToServer;
    /**
     * The amount transferred from client to server after switching.
     */
    private final StreamFinishedEvent clientToServerSwitched;
    /**
     * The response status.
     */
    private final Integer status;
    /**
     * The status message.
     */
    private final String statusMessage;
    /**
     * The server to client stream.
     */
    private final StreamFinishedEvent serverToClient;
    /**
     * The client to server stream.
     */
    private final StreamFinishedEvent serverToClientSwitched;
    /**
     * If remote reported, the remote name.
     */
    private final String remote;
    /**
     * The server to remote events.
     */
    private final StreamFinishedEvent serverToRemote;
    /**
     * The remote to server event.
     */
    private final StreamFinishedEvent remoteToServer;

    /**
     * The constructor.
     *
     * @param id                     the request id
     * @param clientAddress          the client address.
     * @param serverAddress          the server address.
     * @param method                 the request method
     * @param request                the request effective URL
     * @param version                the request version
     * @param clientToServer         the bytes sent from client to server
     * @param clientToServerSwitched the bytes sent from client to server after switching protocol
     * @param status                 the request status
     * @param statusMessage          the status message
     * @param serverToClient         the bytes from server to client
     * @param serverToClientSwitched the bytes from server to client after switch
     * @param remote                 the remote name
     * @param serverToRemote         the amount sent from server to remote
     * @param remoteToServer         the amount set from remote to server
     */
    // CHECKSTYLE:OFF
    public ExchangeFinishedEvent(final String id, final SocketAddress clientAddress, // NOPMD
                                 final SocketAddress serverAddress,
                                 final String method, final URI request, final String version,
                                 final StreamFinishedEvent clientToServer,
                                 final StreamFinishedEvent clientToServerSwitched,
                                 final Integer status, final String statusMessage,
                                 final StreamFinishedEvent serverToClient,
                                 final StreamFinishedEvent serverToClientSwitched,
                                 final String remote,
                                 final StreamFinishedEvent serverToRemote, final StreamFinishedEvent remoteToServer) {
        // CHECKSTYLE:ON
        this.id = id;
        this.clientAddress = clientAddress;
        this.serverAddress = serverAddress;
        this.method = method;
        this.request = request;
        this.version = version;
        this.clientToServer = clientToServer;
        this.clientToServerSwitched = clientToServerSwitched;
        this.status = status;
        this.statusMessage = statusMessage;
        this.serverToClient = serverToClient;
        this.serverToClientSwitched = serverToClientSwitched;
        this.remote = remote;
        this.serverToRemote = serverToRemote;
        this.remoteToServer = remoteToServer;
    }

    /**
     * @return the client address
     */
    public SocketAddress getClientAddress() {
        return clientAddress;
    }

    /**
     * @return the server address
     */
    public SocketAddress getServerAddress() {
        return serverAddress;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return the request
     */
    public URI getRequest() {
        return request;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the client to server event.
     */
    public StreamFinishedEvent getClientToServer() {
        return clientToServer;
    }

    /**
     * @return the client to server over switched stream.
     */
    public StreamFinishedEvent getClientToServerSwitched() {
        return clientToServerSwitched;
    }

    /**
     * @return the status.
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * @return the status message.
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * @return the server to client event.
     */
    public StreamFinishedEvent getServerToClient() {
        return serverToClient;
    }

    /**
     * @return the server to client after switch.
     */
    public StreamFinishedEvent getServerToClientSwitched() {
        return serverToClientSwitched;
    }

    /**
     * @return the remote name.
     */
    public String getRemote() {
        return remote;
    }

    /**
     * @return the server to remote event.
     */
    public StreamFinishedEvent getServerToRemote() {
        return serverToRemote;
    }

    /**
     * @return the remote to server event.
     */
    public StreamFinishedEvent getRemoteToServer() {
        return remoteToServer;
    }

    /**
     * @return the request id.
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ExchangeFinishedEvent{" + "id='" + id + '\'' + ", clientAddress=" + clientAddress
                + ", serverAddress=" + serverAddress + ", method='" + method + '\''
                + ", request=" + request + ", version='" + version + '\'' + ", clientToServer=" + clientToServer
                + ", clientToServerSwitched=" + clientToServerSwitched + ", status=" + status
                + ", statusMessage='" + statusMessage + '\'' + ", serverToClient=" + serverToClient
                + ", serverToClientSwitched=" + serverToClientSwitched + ", remote='" + remote + '\''
                + ", serverToRemote=" + serverToRemote + ", remoteToServer=" + remoteToServer + '}';
    }
}
