package org.asyncflows.io.net.samples;

import org.asyncflows.io.net.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.asyncflows.io.IOUtil.aTryChannel;
import static org.asyncflows.io.net.selector.SelectorVatUtil.doAsyncIo;

public class EchoServerSample {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServerSample.class);

    public static void main(String[] args) {
        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setTpcNoDelay(true);
        doAsyncIo(socketFactory -> aTry(socketFactory.makeServerSocket()).run(serverSocket ->
                serverSocket.bind(new InetSocketAddress(22222), 100).thenFlatGet(() -> {
                    serverSocket.setDefaultOptions(socketOptions);
                    int[] count = new int[1];
                    return aSeqWhile(() -> serverSocket.accept().map(t -> {
                        // run handling in parallel
                        final long start = System.currentTimeMillis();
                        final long id = count[0]++;
                        aTryChannel(t).run((c, in, out) ->
                                aAll(
                                        () -> IOSample.copy(in, out, 4096, 2).toOutcomePromise()
                                ).and(
                                        c::getRemoteAddress
                                ).map((copy, address) -> {
                                    long duration = System.currentTimeMillis() - start;
                                    if (copy.isSuccess()) {
                                        LOG.info("Request " + id + " handled: from = " + address + " duration = " + duration + " amount = " + copy.value());
                                    } else {
                                        LOG.error("Request " + id + " failed: from = " + address + " duration = " + duration, copy.failure());
                                    }
                                    return aVoid();
                                })
                        );
                        return true;
                    }));
                })));
    }


}
