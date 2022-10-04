package es.optocom.jovp;

import org.junit.jupiter.api.Test;

import es.optocom.jovp.definitions.SerialController;
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
    String [] available = SerialController.getSuitableConnections();
    System.out.println(Arrays.toString(available));
    try {
      SerialController controller = new SerialController(available[0]);
    } catch (SerialPortException e) {
      e.printStackTrace();
    }
  }
 
}
