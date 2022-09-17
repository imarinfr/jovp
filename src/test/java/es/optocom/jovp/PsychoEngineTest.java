package es.optocom.jovp;

import es.optocom.jovp.structures.Command;
import es.optocom.jovp.structures.ViewMode;
import es.optocom.jovp.structures.Paradigm;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unitary tests for the psychophysics engine
 *
 * @since 0.0.1
 */
public class PsychoEngineTest {

  /**
   * Unitary tests for the psychophysics engine
   *
   * @since 0.0.1
   */
  public PsychoEngineTest() {
  }

  /**
   * Gets information about the system and attached devices
   *
   * @since 0.0.1
   */
  @Test
  public void initializeEngine() {
    // Init psychoEngine and show some general info
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()), 500);
    System.out.println(psychoEngine);
    System.out.println(psychoEngine.getWindow().getMonitorManager());
    // Check distance was set correctly
    assertEquals(psychoEngine.getDistance(), 500);
    psychoEngine.setDistance(300);
    assertEquals(psychoEngine.getDistance(), 300);
    // Check field of view
    psychoEngine.getWindow().getMonitor().setPhysicalSize(psychoEngine.getWindow().getMonitor().getWidth(),
        psychoEngine.getWindow().getMonitor().getHeight());
    psychoEngine.setDistance((int) (psychoEngine.getWindow().getScaledWidth() / 2.0));
    float[] fov = psychoEngine.getFieldOfView();
    assertEquals(90, fov[0]);
    assertEquals(ViewMode.MONO, psychoEngine.getViewMode());
    psychoEngine.setViewMode(ViewMode.MONO);
    psychoEngine.setViewMode(ViewMode.STEREO);
    assertEquals(ViewMode.STEREO, psychoEngine.getViewMode());
    // Set paradigm
    psychoEngine.setParadigm(Paradigm.CLICKER);
    assertEquals(Paradigm.CLICKER, psychoEngine.getParadigm());
    psychoEngine.cleanup();
  }

  /**
   * Get physical device
   *
   * @since 0.0.1
   */
  @Test
  public void getPhysicalDevices() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()), 500);
    List<VkPhysicalDevice> physicalDevices = psychoEngine.getPhysicalDevices();
    for (VkPhysicalDevice vkPhysicalDevice : physicalDevices)
      System.out.println(vkPhysicalDevice.toString());
    psychoEngine.cleanup();
  }

  /**
   * Get window, check window position, and set window monitor
   *
   * @since 0.0.1
   */
  @Test
  public void getWindow() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()), 500);
    Window window = psychoEngine.getWindow();
    // window position
    int[] position = psychoEngine.getWindowPosition();
    assertArrayEquals(position, window.getPosition());
    System.out.println("Window position is: " + Arrays.toString(position));
    // window monitor
    psychoEngine.setWindowMonitor(0);
    psychoEngine.cleanup();
  }

  /**
   * Sets background for single-screen mode, and for split-screen mode
   *
   * @since 0.0.1
   */
  @Test
  public void runPsychoEngine() {
    Timer timer = new Timer();
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(timer), 500);
    new Thread(() -> {
      double time = 0;
      while (time == 0) { // wait for the beginning of the psychophysics experience
        time = timer.getStartTime();
        Thread.onSpinWait();
      }
      time = 0;
      while (time < 2000) { // close window after 1 second
        time = timer.getElapsedTime();
        Thread.onSpinWait();
      }
      psychoEngine.finish();
    }).start();
    psychoEngine.start();
    System.out.println("Engine was running for " + timer.getElapsedTime() / 1000 + " seconds");
    psychoEngine.cleanup();
  }

  /** Psychophysics logic class */
  static class Logic implements PsychoLogic {

    /** Logic timer */
    Timer timer;

    /** Init with timer */
    Logic(Timer timer) {
      this.timer = timer;
    }

    @Override
    public void init(PsychoEngine psychoEngine) {
      timer.start();
    }

    @Override
    public void input(Command command) {
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}