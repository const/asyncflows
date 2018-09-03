package org.asyncflows.protocol.http.common.content;

/**
 * The event that is used to notify that work with stream has been finished.
 *
 * @see CountingInput
 * @see CountingOutput
 */
public class StreamFinishedEvent {
    /**
     * Stream measurements started time.
     */
    private final long started;
    /**
     * Stream measurements finished time.
     */
    private final long finished;
    /**
     * Amount of bytes transferred.
     */
    private final long transferred;
    /**
     * The failure returned from read operation.
     */
    private final Throwable failure;

    /**
     * The constructor.
     *
     * @param started     the start time (ms)
     * @param finished    the finished time (ms)
     * @param transferred the amount transferred
     * @param failure     the failure
     */
    public StreamFinishedEvent(final long started, final long finished,
                               final long transferred, final Throwable failure) {
        this.started = started;
        this.finished = finished;
        this.transferred = transferred;
        this.failure = failure;
    }

    /**
     * @return the start time
     */
    public long getStarted() {
        return started;
    }

    /**
     * @return the finish time
     */
    public long getFinished() {
        return finished;
    }

    /**
     * @return the amount of bytes transferred
     */
    public long getTransferred() {
        return transferred;
    }

    /**
     * @return the failure or null.
     */
    public Throwable getFailure() {
        return failure;
    }

    @Override
    public String toString() {
        return "StreamFinishedEvent{ started=" + started + ", duration=" + (finished - started)
                + ", transferred=" + transferred + (failure == null ? "" : ", failure=" + failure) + '}';
    }
}
