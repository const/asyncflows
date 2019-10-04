/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

import org.asyncflows.core.Outcome;
import org.asyncflows.core.data.Cell;
import org.asyncflows.tracker.ATracker;
import org.asyncflows.tracker.VariableTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public class FieldsTrackerUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FieldsTrackerUtil.class);

    private FieldsTrackerUtil() {
    }

    public static ATracker<String> trackText(final JTextComponent text) {
        final Cell<DocumentListener> listenerCell = new Cell<>();
        final VariableTracker<String> tracker = new VariableTracker<>(subscriptionStatus -> {
            switch (subscriptionStatus) {
                case NO_SUBSCRIPTIONS:
                    text.getDocument().removeDocumentListener(listenerCell.getValue());
                    break;
                case SUBSCRIPTIONS_EXISTS:
                    text.getDocument().addDocumentListener(listenerCell.getValue());
                    break;
                default:
                    if (LOG.isErrorEnabled()) {
                        LOG.error(String.format("Unknown status: %s", subscriptionStatus));
                    }
                    break;
            }
        });

        final DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                tracker.setValue(Outcome.success(text.getText()));
            }
        };

        listenerCell.setValue(listener);
        return tracker.export();
    }
}
