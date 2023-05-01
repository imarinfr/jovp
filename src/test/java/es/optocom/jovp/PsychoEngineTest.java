package es.optocom.jovp;

import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.ViewMode;

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
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()));
    Window window = psychoEngine.getWindow();
    Monitor monitor = window.getMonitor();
    // Check distance was set correctly
    assertEquals(psychoEngine.getDistance(), 500);
    psychoEngine.setDistance(300);
    assertEquals(psychoEngine.getDistance(), 300);
    // Check field of view
    int monitorWidthMM = 500;
    int monitorHeightMM = (int) (monitorWidthMM * monitor.getPixelAspect() * monitor.getHeight() / monitor.getWidth());
    double alpha = 90; // field of view
    monitor.setPhysicalSize(monitorWidthMM, monitorHeightMM);
    psychoEngine.setSize(monitor.getWidth(), monitor.getHeight());
    psychoEngine.setDistance(monitorWidthMM / (2 * Math.tan(Math.PI / 180.0 * alpha / 2.0)));
    float[] fov = psychoEngine.getFieldOfView();
    assertEquals(90, Math.round(1e3 * fov[0]) / 1e3);
    assertEquals(ViewMode.MONO, psychoEngine.getViewMode());
    psychoEngine.setViewMode(ViewMode.MONO);
    psychoEngine.setViewMode(ViewMode.STEREO);
    assertEquals(ViewMode.STEREO, psychoEngine.getViewMode());
    psychoEngine.cleanup();
  }

  /**
   * Get physical device
   *
   * @since 0.0.1
   */
  @Test
  public void getPhysicalDevices() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()));
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
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()));
    Window window = psychoEngine.getWindow();
    // window position
    int[] position = psychoEngine.getPosition();
    assertArrayEquals(position, window.getPosition());
    System.out.println("Window position is: " + Arrays.toString(position));
    // window monitor
    psychoEngine.setMonitor(0);
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
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(timer));
    new Thread(() -> {
      while (timer.getElapsedTime() == -1) Thread.onSpinWait(); // wait for the beginning of the psychophysics experience
      while (timer.getElapsedTime() < 1000) Thread.onSpinWait(); // close window after 1 second
      psychoEngine.finish();
    }).start();
    psychoEngine.start("mouse", Paradigm.CLICKER);
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
    public void input(PsychoEngine psychoEngine, Command command) {
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}
