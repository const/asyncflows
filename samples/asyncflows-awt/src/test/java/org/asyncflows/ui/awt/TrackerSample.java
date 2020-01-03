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

package org.asyncflows.ui.awt;

import org.asyncflows.core.data.Cell;
import org.asyncflows.core.time.Timer;
import org.asyncflows.tracker.ATracker;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.stream.Stream;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.streams.AsyncStreams.aForStream;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.core.util.CoreFlowsResource.aTryResource;
import static org.asyncflows.tracker.TrackerUtil.streamTracker;
import static org.asyncflows.tracker.TrackerUtil.trottle;
import static org.asyncflows.ui.awt.FieldsTrackerUtil.trackText;

public class TrackerSample {


    public static void main(String[] args) {
        doAsync(() -> aLater(AWTVat.instance(), () -> {
            final JFrame frame = new JFrame("sample");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            final Container contentPane = frame.getContentPane();
            contentPane.setLayout(new GridLayout(3, 1));
            final JTextField textField = new JTextField(20);
            final JLabel textOutput = new JLabel("");
            final JLabel timerOutput = new JLabel("");
            Stream.of(textField, textOutput, timerOutput).forEach(contentPane::add);
            frame.pack();
            frame.setVisible(true);
            long start = System.currentTimeMillis();
            return aTryResource(new Timer()).run(timer -> {
                final Cell<Boolean> stop = new Cell<>(false);
                final ATracker<String> tracker = trottle(trackText(textField), timer, 1000);
                return aAll(() -> {
                    int[] count = new int[1];
                    return aForStream(streamTracker(tracker)).consume(value -> {
                        count[0]++;
                        String s = value.isSuccess() ? value.value() : value.failure().toString();
                        String text = "" + count[0] + " = " + s;
                        System.out.println("Received: " + text);
                        textOutput.setText(text);
                        if ("STOP".equals(s)) {
                            stop.setValue(true);
                            return aFalse();
                        } else {
                            return aTrue();
                        }
                    });
                }).andLast(() -> aTry(timer.fixedRate(Instant.ofEpochMilli(start).plusMillis(1000), 1000)).run(stream -> {
                    return aForStream(stream).consume(l -> {
                        timerOutput.setText("Time: " + (l - start) + "ms");
                        return aBoolean(!stop.getValue());
                    });
                }));
            });

        }));
    }
}
