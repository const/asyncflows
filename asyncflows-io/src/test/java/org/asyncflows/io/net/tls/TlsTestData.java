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

package org.asyncflows.io.net.tls;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * The test configuration data used for SSL
 */
public class TlsTestData {
    /**
     * The client context for the SSL
     */
    private final SSLContext sslClientContext;
    /**
     * The server context for the SSL
     */
    private final SSLContext sslServerContext;

    /**
     * The constructor
     *
     * @throws CertificateException      if there is a problem
     * @throws NoSuchAlgorithmException  if there is a problem
     * @throws KeyStoreException         if there is a problem
     * @throws IOException               if there is a problem
     * @throws UnrecoverableKeyException if there is a problem
     * @throws KeyManagementException    if there is a problem
     */
    public TlsTestData() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
        final char[] passphrase = "passphrase".toCharArray();
        final KeyStore ksTrust = getKeyStore(passphrase, "data/trust.jks");
        final KeyStore ksClient = getKeyStore(passphrase, "data/client.jks");
        final KeyStore ksServer = getKeyStore(passphrase, "data/server.jks");

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ksTrust);

        final KeyManagerFactory kmfClient = KeyManagerFactory.getInstance("SunX509");
        kmfClient.init(ksClient, passphrase);
        final KeyManagerFactory kmfServer = KeyManagerFactory.getInstance("SunX509");
        kmfServer.init(ksServer, passphrase);

        sslClientContext = SSLContext.getInstance("TLS");
        sslClientContext.init(kmfClient.getKeyManagers(), tmf.getTrustManagers(), null);
        sslServerContext = SSLContext.getInstance("TLS");
        sslServerContext.init(kmfServer.getKeyManagers(), tmf.getTrustManagers(), null);

    }

    /**
     * Load keystore
     *
     * @param passphrase the phassphrase to use
     * @param name       the keystore name
     * @return the loaded keystore
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    private KeyStore getKeyStore(final char[] passphrase, final String name) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        final KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream stream = TlsTestData.class.getResourceAsStream(
                "/org/asyncflows/io/net/tls/" + name)) {
            ks.load(stream, passphrase);
        }
        return ks;
    }

    /**
     * @return the client context for TLS
     */
    public SSLContext getSslClientContext() {
        return sslClientContext;
    }

    /**
     * @return the server context for TLS
     */
    public SSLContext getSslServerContext() {
        return sslServerContext;
    }
}
