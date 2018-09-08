package org.asyncflows.protocol.websocket.core;

import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.websocket.WebSocketMessage;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.StreamExportUtil;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;

import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The web socket input.
 */
public class WebSocketInput extends CloseableInvalidatingBase
        implements AStream<WebSocketMessage>, NeedsExport<AStream<WebSocketMessage>> {
    /**
     * The queue for the next requests.
     */
    private final RequestQueue requestQueue = new RequestQueue();
    /**
     * The context.
     */
    private final ByteParserContext input;

    public WebSocketInput(final ByteParserContext input) {
        this.input = input;
    }

    @Override
    public Promise<Maybe<WebSocketMessage>> next() {
        final Promise<Maybe<WebSocketMessage>> next = new Promise<>();
        final AResolver<Maybe<WebSocketMessage>> resolver = next.resolver();
        requestQueue.run(() -> FrameHeader.read(input).map(
                header -> {
                    final int opCode = header.getOpCode();
                    if(opCode == FrameHeader.OP_CLOSE) {
                        notifySuccess(resolver, Maybe.<WebSocketMessage>empty());
                        //return IOUtil.BYTE.discard(input.input(), header.getLength(), ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE)).toVoid();
                    }
                    return null;
                }
        ));
        return next;
    }

    @Override
    public AStream<WebSocketMessage> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
