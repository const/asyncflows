package org.asyncflows.core.util;

import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * The range iterator.
 */
public class ControlUtils {
    /**
     * Create iterator for range of integers.
     *
     * @param start the start
     * @param endExclusive the end
     * @return the range iterator.
     */
    public static Iterator<Integer> rangeIterator(int start, int endExclusive) {
        return IntStream.range(start, endExclusive).boxed().iterator();
    }
}
