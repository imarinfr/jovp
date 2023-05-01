package es.optocom.jovp;

import org.junit.jupiter.api.Test;

import es.optocom.jovp.definitions.Command;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unitary tests for window management
 *
 * @since 0.0.1
 */
public class WindowTest {

  /**
   * Unitary tests for window management
   *
   * @since 0.0.1
   */
  public WindowTest() {
  }

  /**
   * Tests for creating, showing, hiding, resizing, and hiding windows
   *
   * @since 0.0.1
   */
  @Test
  public void showAndHide() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic());
    try {
      for (int i = 0; i < 5; i++) {
        psychoEngine.show();
        Thread.sleep(250);
        psychoEngine.hide();
        Thread.sleep(250);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    psychoEngine.cleanup();
  }
 
  /**
   * Tests for creating, showing, hiding, resizing, and hiding windows
   *
   * @since 0.0.1
   */
  @Test
  public void showWindowedAndFullScreen() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic());
    psychoEngine.show();
    psychoEngine.setFullScreen();
    psychoEngine.setWindowed();
    psychoEngine.cleanup();
  }

  /**
   * Tests for updating distance and the fixation point, and for retrieving the
   * field of view of the window
   *
   * @since 0.0.1
   */
  @Test
  public void changeWindowPositionAndSize() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic());
    assertArrayEquals(new int[] {psychoEngine.getMonitor().getScaledWidth() / 2, 0}, psychoEngine.getPosition());
    // position is relative to monitor workspace in the virtual desktop
    psychoEngine.setPosition(10, 10);
    assertArrayEquals(new int[] {10, 10}, psychoEngine.getPosition());
    psychoEngine.setSize(500, 200);
    assertEquals(500, psychoEngine.getWindow().getWidth());
    assertEquals(200, psychoEngine.getWindow().getHeight());
    psychoEngine.show();
    psychoEngine.setSize(1000, 800);
    psychoEngine.setPosition(10, 10);
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    @Override
    public void init(PsychoEngine psychoEngine) {
    }

    @Override
    public void input(PsychoEngine psychoEngine, Command command) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}
