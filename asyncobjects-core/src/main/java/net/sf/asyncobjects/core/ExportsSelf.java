package net.sf.asyncobjects.core;

import net.sf.asyncobjects.core.vats.Vat;

/**
 * An interface for objects that export self. The exporting object means that a proxy is created that is
 * delegating all calls to the specified vat.
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
