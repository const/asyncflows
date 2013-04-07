package net.sf.asyncobjects.core.util;

/**
 * The simple mutable cell for values.
 *
 * @param <T> the value type
 */
public final class Cell<T> {
    /**
     * the value.
     */
    private T value;
    /**
     * if true, the value has been set.
     */
    private boolean hasValue;

    /**
     * The constructor for the cell.
     *
     * @param value the value
     */
    public Cell(final T value) {
        this.value = value;
        this.hasValue = true;
    }

    /**
     * The constructor with null initial value.
     */
    public Cell() {
        // do nothing
    }

    /**
     * @return the value (null if there is no value or value is null)
     */
    public T getValue() {
        return value;
    }

    /**
     * Set value.
     *
     * @param value the value to set
     */
    public void setValue(final T value) {
        this.hasValue = true;
        this.value = value;
    }

    /**
     * @return true if there is no value
     */
    public boolean isEmpty() {
        return !hasValue;
    }

    /**
     * Remove value from cell.
     */
    public void clearValue() {
        hasValue = false;
        value = null;
    }
}
