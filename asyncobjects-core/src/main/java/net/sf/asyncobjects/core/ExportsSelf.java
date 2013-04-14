package net.sf.asyncobjects.core;

import net.sf.asyncobjects.core.vats.Vat;

/**
 * This interface must be implemented by all services that need to be exported for safe usage from other vats.
 * If this interface is not implemented, it is assumed that object is safe to use on other vats.
 *
 * @param <T> the exported type
 */
public interface ExportsSelf<T> {
    /**
     * @return the object exported on the current vat
     */
    T export();

    /**
     * Export on the specified vat.
     *
     * @param vat the vat to export to
     * @return the exported object
     */
    T export(Vat vat);
}
