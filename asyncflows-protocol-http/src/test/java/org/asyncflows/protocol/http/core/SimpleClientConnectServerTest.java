/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.protocol.http.core;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.protocol.http.client.AHttpClient;
import org.asyncflows.protocol.http.client.core.HttpConnectSocketFactory;
import org.asyncflows.protocol.http.client.core.SimpleHttpClient;

import java.net.URI;

import static org.asyncflows.core.util.CoreFlowsResource.aTryResource;

/**
 * The same test as {@link SimpleClientServerTest}, but uses the test server with connect proxy.
 */
@SuppressWarnings("squid:S2187")
public class SimpleClientConnectServerTest extends SimpleClientServerTest {

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
            return aTryResource(client.export()).run(action);
        });
    }
}
