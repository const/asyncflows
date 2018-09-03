package org.asyncflows.protocol.http.core;

import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.protocol.http.client.AHttpClient;
import org.asyncflows.protocol.http.client.core.HttpConnectSocketFactory;
import org.asyncflows.protocol.http.client.core.SimpleHttpClient;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AFunction;

import java.net.URI;

import static org.asyncflows.core.util.CoreFlowsResource.aTryResource;

/**
 * The same test as {@link SimpleClientServerTest}, but uses the test server with connect proxy.
 */
public class SimpleClientConnectServerTest extends SimpleClientServerTest { // NOPMD

    @Override
    protected <T> Promise<T> runWithClient(final ASocketFactory socketFactory, final URI uri, final AFunction<AHttpClient, T> action) {
        final SimpleHttpClient baseClient = new SimpleHttpClient();
        baseClient.setUserAgent("test-proxy-client/1.0");
        baseClient.setSocketFactory(socketFactory);
        return aTryResource(baseClient.export()).run(value -> {
            final ASocketFactory connectSocketFactory
                    = new HttpConnectSocketFactory(value, uri.getAuthority()).export();
            final SimpleHttpClient client = new SimpleHttpClient();
            client.setUserAgent("test-client/1.0");
            client.setSocketFactory(connectSocketFactory);
            return aTryResource(client.export()).run(action::apply);
        });
    }
}
