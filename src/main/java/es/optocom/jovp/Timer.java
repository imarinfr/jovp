package es.optocom.jovp;

/**
 * Controls the times of PsychoEngine
 *
 * @since 0.0.1
 */
public class Timer {

    private final double creationTime;
    private double startTime = -1;

    /**
     * Create timer
     *
     * @since 0.0.1
     */
    public Timer() {
        creationTime = System.nanoTime();
    }

    /**
     * Start timer
     *
     * @since 0.0.1
     */
    public void start() {
        startTime = getTime();
    }

    /**
     * Stop timer
     *
     * @since 0.0.1
     */
    public void stop() {
        startTime = -1;
    }

    /**
     * get elapse time
     *
     * @return Elapsed time in ms
     *
     * @since 0.0.1
     */
    public long getElapsedTime() {
        return Math.round(getElapsedNanoTime() / 1e6);
    }

    /**
     * get elapse time in nano-secods
     *
     * @return Elapsed time in nano-seconds
     *
     * @since 0.0.1
     */
    public double getElapsedNanoTime() {
        if (startTime == -1)
            return 0;
        return getTime() - startTime;
    }

    /** Current time in ms to nanoseconds resolution */
    private double getTime() {
        return System.nanoTime() - creationTime;
    }

}
