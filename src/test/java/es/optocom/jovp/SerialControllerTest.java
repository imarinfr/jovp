package es.optocom.jovp;

import org.junit.jupiter.api.Test;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Paradigm;
import jssc.SerialPortException;

import java.util.Arrays;

/**
 * Unitary tests for serial controller
 *
 * @since 0.0.1
 */
public class SerialControllerTest {

  /**
   * Unitary tests for serial controller
   *
   * @since 0.0.1
   */
  public SerialControllerTest() {
  }

  /**
   * Get all serial controller names
   *
   * @since 0.0.1
   */
  @Test
  public void getSerialControllerNames() {
    String [] available = Controller.getSuitableConnections();
    System.out.println(Arrays.toString(available));
    try {
      Controller controller = new Controller(Controller.byName("DK0DPMJY"), Paradigm.CLICKER);
      controller.open();
      controller.close();
    } catch (SerialPortException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get all serial controller names
   *
   * @since 0.0.1
   */
  @Test
  public void toDeleteActually() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.start("DK0DPMJY", Paradigm.CLICKER);
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    @Override
    public void init(PsychoEngine psychoEngine) {
    }

    @Override
    public void input(PsychoEngine psychoEngine, Command command) {
      if (command != Command.NONE) System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }
}
