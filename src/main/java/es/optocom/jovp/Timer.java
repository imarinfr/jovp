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
    creationTime = System.nanoTime() / 1e6;
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
  public double getStartTime() {
    return startTime;
  }

  /**
   * get elapse time
   *
   * @return Elapsed time in ms
   *
   * @since 0.0.1
   */
  public double getElapsedTime() {
    return getTime() - startTime;
  }

  /** Current time in ms to nanoseconds resolution */
  private double getTime() {
    return System.nanoTime() / 1e6 - creationTime;
  }

}