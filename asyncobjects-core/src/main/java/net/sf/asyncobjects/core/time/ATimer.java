package net.sf.asyncobjects.core.time;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.core.util.ACloseable;

import java.util.Date;

/**
 * The timer interface.
 */
public interface ATimer extends ACloseable {
    /**
     * Sleep for the specified time.
     *
     * @param delay the delay to sleep for
     * @return promise for the time when sleep was scheduled to awake
     */
    Promise<Long> sleep(long delay);

    /**
     * Wait until the specified time.
     *
     * @param time the time to wait for
     * @return promise for the time when sleep was scheduled to awake
     */
    Promise<Long> waitFor(Date time);

    /**
     * Start fixed rate stream timer stream.
     *
     * @param firstTime the the first time the value is returned from the stream
     * @param period    the period with which events happen
     * @return promise for stream, note the stream accumulate events if they were not asked for
     */
    Promise<AStream<Long>> fixedRate(Date firstTime, long period);

    /**
     * Start delay rate stream timer stream. The each reading from stream is delayed
     * for the specified time. It is assumed that caller would ask for the next value,
     * when it finished task for the first one.
     *
     * @param firstTime the the first time the value is returned from the stream
     * @param delay     the delay between events
     * @return promise for stream
     */
    Promise<AStream<Long>> fixedDelay(Date firstTime, long delay);

    /**
     * Start fixed rate stream timer stream.
     *
     * @param initialDelay the delay before first event
     * @param period       the period with which events happen
     * @return promise for stream, note the stream accumulate events if they were not asked for
     */
    Promise<AStream<Long>> fixedRate(long initialDelay, long period);

    /**
     * Start delay rate stream timer stream. The each reading from stream is delayed
     * for the specified time. It is assumed that caller would ask for the next value,
     * when it finished task for the first one.
     *
     * @param initialDelay the delay before first event
     * @param delay        the delay between events
     * @return promise for stream
     */
    Promise<AStream<Long>> fixedDelay(long initialDelay, long delay);
}
