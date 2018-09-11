/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.io.util;

import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;

import java.nio.Buffer;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.closeResourceAction;

/**
 * Simple channel over input and output. Closing channel closes both input and output.
 *
 * @param <B> the buffer type
 */
public class SimpleChannel<B extends Buffer> extends CloseableBase implements AChannel<B>, NeedsExport<AChannel<B>> {
    /**
     * Input for the channel.
     */
    private final AInput<B> input;
    /**
     * Output for the channel.
     */
    private final AOutput<B> output;

    /**
     * The constructor.
     *
     * @param input  the input
     * @param output the output
     */
    public SimpleChannel(final AInput<B> input, final AOutput<B> output) {
        this.input = input;
        this.output = output;
    }

    @Override
    protected Promise<Void> closeAction() {
        // just force closing both channels
        return aAll(closeResourceAction(input)).andLast(closeResourceAction(output)).toVoid();
    }

    @Override
    public Promise<AInput<B>> getInput() {
        return aValue(input);
    }

    @Override
    public Promise<AOutput<B>> getOutput() {
        return aValue(output);
    }

    @Override
    public AChannel<B> export(final Vat vat) {
        return IOExportUtil.export(vat, this);
    }
}
