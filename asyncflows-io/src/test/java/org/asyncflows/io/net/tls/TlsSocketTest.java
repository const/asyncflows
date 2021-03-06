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

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.data.Tuple3;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.selector.SelectorVatUtil;
import org.asyncflows.io.util.AbstractDigestingStream;
import org.asyncflows.io.util.LimitedInput;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.constantSupplier;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.io.net.SocketUtil.aTrySocket;
import static org.asyncflows.io.util.DigestingInput.digestAndDiscardInput;
import static org.asyncflows.io.util.DigestingOutput.digestOutput;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for TLS socket
 */
public class TlsSocketTest {
    /**
     * The smoke test for TlsSocket
     *
     * @throws Throwable in case of the problem
     */
    @Test
    public void smokeTest() throws Throwable {
        final TlsTestData tlsTestData = new TlsTestData();
        final Random rnd = new Random();
        final long length = rnd.nextInt(10240) + 1024;
        final Tuple2<Long, Tuple3<byte[], byte[], Long>> result = SelectorVatUtil.doAsyncIoThrowable(rawSocketFactory -> {
            TlsSocketFactory tlsFactory = new TlsSocketFactory();
            tlsFactory.setSocketFactory(rawSocketFactory);
            tlsFactory.setClientEngineFactory(value -> {
                final InetSocketAddress inetSocketAddress = (InetSocketAddress) value;
                SSLEngine rc = tlsTestData.getSslClientContext().createSSLEngine(inetSocketAddress.getHostName(),
                        inetSocketAddress.getPort());
                rc.setUseClientMode(true);
                return aValue(rc);
            });
            tlsFactory.setServerEngineFactory(value -> {
                final SSLEngine rc = tlsTestData.getSslServerContext().createSSLEngine();
                rc.setUseClientMode(false);
                return aValue(rc);
            });

            final ASocketFactory socketFactory = tlsFactory.export();
            return aTry(socketFactory.makeServerSocket()).run(
                    serverSocket -> serverSocket.bind(new InetSocketAddress(0)).flatMap(
                            socketAddress -> aAll(
                                    () -> aTrySocket(serverSocket.accept()).run(
                                            (socket, input, output) -> IOUtil.BYTE.copy(
                                                    LimitedInput.limit(input, length), output, false,
                                                    ByteBuffer.allocate(1024))
                                    )
                            ).andLast(
                                    () -> aTry(socketFactory.makeSocket()).run(
                                            socket -> digestingClient(socket, socketAddress, rnd, length))
                            )
                    )
            );
        });
        assertEquals(result.getValue1(), result.getValue2().getValue3());
        assertArrayEquals(result.getValue2().getValue1(), result.getValue2().getValue2());
    }

    /**
     * The client that generates data
     *
     * @param socket        the socket
     * @param socketAddress the socket address
     * @param rnd           the random number generator
     * @param length        the data length
     * @return digests and data length
     */
    private Promise<Tuple3<byte[], byte[], Long>> digestingClient(final ASocket socket,
                                                                  final SocketAddress socketAddress,
                                                                  final Random rnd, final long length) {
        final int port = ((InetSocketAddress) socketAddress).getPort();
        final SocketAddress connectAddress = new InetSocketAddress("localhost", port);
        return socket.connect(connectAddress).thenFlatGet(() -> aAll(() -> {
            final Promise<byte[]> digest = new Promise<>();
            return aTry(() -> socket.getOutput().flatMap(
                    socketOutput -> aValue(digestOutput(socketOutput, digest.resolver()).md5()))).run(output -> {
                final byte[] data = new byte[(int) length];
                rnd.nextBytes(data);
                return output.write(ByteBuffer.wrap(data));
            }).thenPromise(digest);
        }).and(
                () -> aTry(socket.getInput()).run(
                        socketInput -> digestAndDiscardInput(
                                LimitedInput.limit(socketInput, length),
                                AbstractDigestingStream.MD5)
                )
        ).andLast(constantSupplier(length)));
    }
}
