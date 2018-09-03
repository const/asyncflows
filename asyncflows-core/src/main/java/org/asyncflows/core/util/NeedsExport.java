package org.asyncflows.core.util;

import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.vats.Vats.defaultVat;

public interface NeedsExport<T> {
    default T export() {
        return export(defaultVat());
    }

    default T export(Vat vat) {
        return ObjectExporter.export(vat, this);
    }

    @SuppressWarnings("unchecked")
    static <R> R exportIfNeeded(R object) {
        if(object instanceof NeedsExport) {
            return (R)((NeedsExport) object).export();
        }
        return object;
    }
}
