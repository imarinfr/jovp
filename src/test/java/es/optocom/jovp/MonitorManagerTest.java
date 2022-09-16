package es.optocom.jovp;

import es.optocom.jovp.structures.Command;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * Unitary tests for screen manager
 *
 * @since 0.0.1
 */
public class MonitorManagerTest {

  /**
   * Unitary tests for screen manager
   *
   * @since 0.0.1
   */
  public MonitorManagerTest() {
  }

  /**
   * Test for collecting all attached monitors
   *
   * @since 0.0.1
   */
  @Test
  public void retrieveMonitors() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    MonitorManager monitorManager = psychoEngine.getWindow().getMonitorManager();
    System.out.println(monitorManager);
    psychoEngine.cleanup();
  }

  /**
   * Test for retrieving the monitor's information
   *
   * @since 0.0.1
   */
  @Test
  public void setSettingsManually() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    MonitorManager monitorManager = psychoEngine.getWindow().getMonitorManager();
    Monitor monitor = monitorManager.getMonitor(0);
    monitor.setPhysicalSize(621, 341);
    monitor.setSize(1024, 640);
    monitor.setRefreshRate(10);
    System.out.println(monitor);
    psychoEngine.cleanup();
  }

  /**
   * Tests for monitor's video modes
   *
   * @since 0.0.1
   */
  @Test
  public void videoModes() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    MonitorManager monitorManager = psychoEngine.getWindow().getMonitorManager();
    Monitor monitor = monitorManager.getMonitor(0);
    GLFWVidMode videoMode = monitor.getCurrentVideoMode();
    System.out.println("Fullscreen video mode: " + monitor.getWidth() + "x" + monitor.getHeight() + " - " +
        monitor.getRefreshRate() + "Hz\n");
    System.out.println("Current video mode: " + videoMode.width() + "x" + videoMode.height() +
        " - " + videoMode.refreshRate() + "Hz\n");
    System.out.println("List of video modes:");
    for (int i = 0; i < monitor.getVideoModes().capacity(); i++) {
      System.out.println("\t" + monitor.getVideoModes().position(i).width() + "x" +
          monitor.getVideoModes().position(i).height() + " - " +
          monitor.getVideoModes().position(i).refreshRate() + "Hz");
    }
    System.out.println();
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    @Override
    public void init(PsychoEngine psychoEngine) {
    }

    @Override
    public void input(Command command) {
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}