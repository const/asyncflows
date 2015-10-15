package net.sf.asyncobjects.protocol.http.core;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ResourceUtil;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.protocol.http.client.AHttpClient;
import net.sf.asyncobjects.protocol.http.client.core.HttpConnectSocketFactory;
import net.sf.asyncobjects.protocol.http.client.core.SimpleHttpClient;

import java.net.URI;

/**
 * The same test as {@link SimpleClientServerTest}, but uses the test server with connect proxy.
 */
public class SimpleClientConnectServerTest extends SimpleClientServerTest { // NOPMD

    @Override
    protected <T> Promise<T> runWithClient(final ASocketFactory socketFactory, final URI uri, final AFunction<T, AHttpClient> action) {
        final SimpleHttpClient baseClient = new SimpleHttpClient();
        baseClient.setUserAgent("test-proxy-client/1.0");
        baseClient.setSocketFactory(socketFactory);
        return ResourceUtil.aTryResource(baseClient.export()).run(value -> {
            final ASocketFactory connectSocketFactory
                    = new HttpConnectSocketFactory(value, uri.getAuthority()).export();
            final SimpleHttpClient client = new SimpleHttpClient();
            client.setUserAgent("test-client/1.0");
            client.setSocketFactory(connectSocketFactory);
            return ResourceUtil.aTryResource(client.export()).run(action::apply);
        });
    }
}
