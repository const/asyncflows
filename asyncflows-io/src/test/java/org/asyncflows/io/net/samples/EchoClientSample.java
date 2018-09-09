package org.asyncflows.io.net.samples;

import org.asyncflows.core.util.Semaphore;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.ByteIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.ControlUtils.rangeIterator;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsAll.aAllForUnit;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.io.net.selector.SelectorVatUtil.doAsyncIo;

public class EchoClientSample {
    private static final Logger LOG = LoggerFactory.getLogger(EchoClientSample.class);

    public static void main(String[] args) {
        final long start = System.currentTimeMillis();
        final String host = "localhost";
        final int port = 22222;
        final int size = 200020;
        final int amount = 2000;
        final List<ConnectionInfo> c = new ArrayList<>();
        final Supplier<Long> time = () -> System.currentTimeMillis() - start;
        final Semaphore connects = new Semaphore(50);
        final int[] connected = new int[1];
        final int[] maxConnected = new int[1];
        doAsyncIo(socketFactory -> aAllForUnit(rangeIterator(0, amount), i -> {
            ConnectionInfo info = new ConnectionInfo();
            c.add(info);
            info.id = i;
            return aTry(socketFactory.makeSocket()).run(socket ->
                    aSeq(() -> connects.run(() -> {
                        info.connectStart = time.get();
                        return socket.connect(new InetSocketAddress(host, port))
                                .listen(o -> {
                                    info.connectEnd = time.get();
                                    connected[0]++;
                                });
                    })).thenDoLast(() ->
                            aAll(
                                    () -> aTry(socket.getOutput()).run(
                                            out -> ByteIOUtil.charGen(out, size, 4012)
                                                    .listen(o -> info.dataSent = time.get())
                                    )
                            ).andLast(
                                    () -> aTry(socket.getInput()).run(
                                            out -> IOUtil.BYTE.discard(out, ByteBuffer.allocate(1223))
                                                    .listen(o -> info.dataReceived = time.get())
                                    )
                            )
                    )
            ).mapOutcome(o -> {
                maxConnected[0] = Math.max(maxConnected[0], connected[0]);
                connected[0]--;
                info.finished = time.get();
                if (o.isFailure()) {
                    info.failure = o.failure();
                    LOG.error(info.toString(), o.failure());
                } else {
                    LOG.info(info.toString());
                }
                return aVoid();
            });
        }));
        LOG.info("Failed: " + c.stream().filter(i -> i.failure != null).count());
        LOG.info("Max connected: " + maxConnected[0]);
        final BiConsumer<String, Function<ConnectionInfo, Long>> stats = (category, function) -> {
            long undefined = c.stream().map(function).filter(Objects::isNull).count();
            LongSummaryStatistics summary = c.stream().map(function).filter(Objects::nonNull).mapToLong(l -> l).summaryStatistics();
            LOG.info(category + ": undefined = " + undefined + " min = " + summary.getMin() + " avg = " + summary.getAverage() + " max = " + summary.getMax());
        };
        stats.accept("Connect start", i -> i.connectStart);
        stats.accept("Connect duration", i -> isSomeNull(i.connectStart, i.connectEnd) ? null : i.connectEnd - i.connectStart);
        stats.accept("Send duration", i -> isSomeNull(i.connectEnd, i.dataSent) ? null : i.dataSent - i.connectEnd);
        stats.accept("Receive duration", i -> isSomeNull(i.connectEnd, i.dataReceived) ? null : i.dataReceived - i.connectEnd);
        stats.accept("Processing duration", i -> isSomeNull(i.connectStart, i.finished) ? null : i.finished - i.connectStart);
        stats.accept("Full duration", i -> i.finished);
    }

    private static boolean isSomeNull(Object... objects) {
        for (Object o : objects) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }


    private static class ConnectionInfo {
        private int id;
        private Long connectStart;
        private Long connectEnd;
        private Long dataSent;
        private Long dataReceived;
        private Long finished;
        private Throwable failure;

        @Override
        public String toString() {
            return "{" +
                    "id=" + id +
                    ", connectStart=" + connectStart +
                    ", connectEnd=" + connectEnd +
                    ", dataSent=" + dataSent +
                    ", dataReceived=" + dataReceived +
                    ", finished=" + finished +
                    ", failure=" + failure +
                    '}';
        }
    }
}
