package es.optocom.jovp;

/**
 * Controls the times of PsychoEngine
 *
 * @since 0.0.1
 */
public class Timer {

  private final double creationTime;
  private double startTime;

  /**
   * Create timer
   *
   * @since 0.0.1
   */
  public Timer() {
    creationTime = System.nanoTime();
  }

  /**
   * Initialize timer
   *
   * @since 0.0.1
   */
  public void start() {
    startTime = getTime();
  }

  /**
   * Get start time
   *
   * @return Time start time in ms
   *
   * @since 0.0.1
   */
  public long getStartTime() {
    return Math.round(getStartNanoTime() / 1e6);
  }

  /**
   * Get start time
   *
   * @return Time start time in nano-seconds
   *
   * @since 0.0.1
   */
  public double getStartNanoTime() {
    return startTime;
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
    return getTime() - startTime;
  }

  /** Current time in ms to nanoseconds resolution */
  private double getTime() {
    return System.nanoTime() - creationTime;
  }

}
