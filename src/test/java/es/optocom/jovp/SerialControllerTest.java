package es.optocom.jovp;

import org.junit.jupiter.api.Test;

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
  }

}
