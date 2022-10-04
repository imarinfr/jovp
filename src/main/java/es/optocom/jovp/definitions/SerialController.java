package es.optocom.jovp.definitions;

import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;
import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortEvent;

import java.util.regex.Pattern;

/**
 * Serial Controller
 *
 * @since 0.0.1
 */
public class SerialController extends Thread implements SerialPortEventListener {

  private static final int BYTE_COUNT = 5;
  private static final int TIMEOUT = 50;
  private SerialPort port;

  /**
   * Scans the port for suitable USB connections
   *
   * @return The list of suitable USB connections
   *
   * @since 0.0.1
  */
  public static String[] getSuitableConnections() {
    Pattern pattern = switch (System.getProperty("os.name")) {
      case "Linux" -> Pattern.compile("(ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO)[0-9]{1,3}");
      case "SunOS" -> Pattern.compile("[0-9]*|[a-z]*");
      //case "Mac OS X", "Darwin" -> Pattern.compile("cu\\.usbserial\\-");
      case "Mac OS X", "Darwin" -> Pattern.compile("cu\\.usbserial\\-");
      case "Win" -> Pattern.compile("");
      default -> Pattern.compile("");
    };
    return SerialPortList.getPortNames(pattern);
  }

  /**
   * Set up a serial controller for a specific device
   *
   * @param device device name
   * @throws SerialPortException if device is not found or cannot be opened
   *
   * @since 0.0.1
  */
  public SerialController(String device) throws SerialPortException {
    this.port = new SerialPort(device);
    open();
    port.addEventListener(this);
  }

  /**
   * Opens a serial controller for a specific device
   *
   * @throws SerialPortException if port cannot be opened
   *
   * @since 0.0.1
  */
  public void open() throws SerialPortException {
    port.openPort();
    purge();
  }

  /**
   * Closes the port
   *
   * @throws SerialPortException if port cannot be closed
   *
   * @since 0.0.1
  */
  public void close() throws SerialPortException {
    port.closePort();
  }

  /**
   * Purges the Serial port upon request and if it contains data
   *
   * @throws SerialPortException if device cannot be purged
   *
   * @since 0.0.1
  */
  public void purge() throws SerialPortException {
    if (port.getInputBufferBytesCount() > 0)
      port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
  }

  /**
   * Purges the Serial port upon request and if it contains data
   *
   * @throws SerialPortException if device is not found or cannot be initialized
   *
   * @since 0.0.1
  */
  public void serialEvent(SerialPortEvent event) {
    if (event.isRXCHAR()) {
      try {
        // TODO Code specifically for the ImoVifa button
        // TODO On click it sends 5 bytes (as ints: 42 79 78 78 35)
        // TODO On release it sends 5 bytes (as ints: 42 79 70 70 35)
        int[] msg = port.readIntArray(BYTE_COUNT, TIMEOUT);
        for (int i : msg) System.out.print(i + " ");
        if (msg[3] == 78) System.out.println("Pressed");
        if (msg[3] == 70) System.out.println("Released");
      } catch (SerialPortException | SerialPortTimeoutException e) {
        throw new RuntimeException(e);
      }    
    }
  }

}