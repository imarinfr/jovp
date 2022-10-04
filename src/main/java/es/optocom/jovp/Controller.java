package es.optocom.jovp;

import static org.lwjgl.glfw.GLFW.*;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Input;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.SerialController;
import jssc.SerialPortException;

/**
 * Input schemes for different psychophysics paradigms
 *
 * @since 0.0.1
 */
public class Controller {

  private static final String CANNOT_SET_INPUT = "The parameters for a USB connection are the 'device' name and paradigm";

  /** Input device */
  private final Input input;
  /** USB port */
  private final SerialController usb;
  /** Psychophysics paradigm to map input to commands */
  private final Paradigm paradigm;
  /** Controller command */
  private Command command = Command.NONE;

  /**
   * Controller type and settings
   *
   * @param windowHandle The window handle
   * @param input The input is listening to
   * @param paradigm Preset scheme for the psychophysics paradigm
   *
   * @since 0.0.1
   */
  Controller(long windowHandle, Input input, Paradigm paradigm) throws IllegalArgumentException {
    this.input = input;
    this.paradigm = paradigm;
    this.usb = null;
    switch (input) {
      case MOUSE -> mouseCallbacks(windowHandle);
      case KEYPAD -> keypadCallbacks(windowHandle);
      default -> throw new IllegalArgumentException(CANNOT_SET_INPUT);
    }
  }

  /**
   * Controller type and settings
   *
   * @param device The device name
   * @param paradigm Preset scheme for the psychophysics paradigm
   *
   * @since 0.0.1
   */
  Controller(String device, Paradigm paradigm) throws SerialPortException {
    this.input = Input.USB;
    this.paradigm = paradigm;
    this.usb = new SerialController(device);
    usbCallbacks(usb);
  }

  /**
   * Process command
   *
   * @param command Command to be processed
   *
   * @return The response from the observer
   */
  Command processCommand(int command) {
    // key or button mapping to response
    return switch (paradigm) {
      case CLICKER -> processClicker(command);
      case M2AFC -> process2AfcHorizontal(command);
      case M2AFC_VERTICAL -> process2AfcVertical(command);
      case M3AFC -> process3AfcHorizontal(command);
      case M3AFC_VERTICAL -> process3AfcVertical(command);
      case M4AFC -> process4AfcCross(command);
      case M4AFC_DIAGONAL -> process4AfcDiagonal(command);
      case M8AFC -> process8Afc(command);
      case M9AFC -> process9Afc(command);
    };
  }

  /**
   * Get Command
   *
   * @return the last command read from the input
   *
   * @since 0.0.1
   */
  Command getCommand() {
    Command cmd = command;
    command = Command.NONE;
    return cmd;
  }

  /** add callbacks to keys in the specified window */
  private void mouseCallbacks(long windowHandle) {
    glfwSetWindowCloseCallback(windowHandle, (window) -> closeWindowClicked());
    glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> buttonPressed(button, action));
  }

  /** add callbacks to keys in the specified window */
  private void keypadCallbacks(long windowHandle) {
    glfwSetWindowCloseCallback(windowHandle, (window) -> closeWindowClicked());
    glfwSetMouseButtonCallback(windowHandle, null);
    glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> keyPressed(key, action));
  }

  /** add callbacks to keys in the specified window */
  private void usbCallbacks(SerialController usb) {
    // TODO setup callbacks
  }

  /** close window icon clicked */
  private void closeWindowClicked() {
    command = Command.CLOSE;
  }

  /** key pressed */
  private void keyPressed(int key, int action) {
    if (action == GLFW_PRESS) {
      command = processCommand(key);
    }
  }

  /** mouse button pressed */
  private void buttonPressed(int button, int action) {
    if (action == GLFW_PRESS) {
      command = processCommand(button);
    }
  }

  /** response for a clicker paradigm */
  private Command processClicker(int command) {
    return switch(input) {
      case MOUSE -> command == GLFW_MOUSE_BUTTON_LEFT ? Command.YES : Command.NONE;
      case KEYPAD -> command == GLFW_KEY_KP_0 ? Command.YES : Command.NONE;
      case USB -> Command.NONE; // TODO
    };
  }

  /** response for a 2AFC paradigm */
  private Command process2AfcHorizontal(int command) {
    return switch(input) {
      case MOUSE ->
        switch (command) {
          case GLFW_MOUSE_BUTTON_LEFT -> Command.ITEM1;
          case GLFW_MOUSE_BUTTON_RIGHT -> Command.ITEM2;
          default -> Command.NONE;
        };
      case KEYPAD ->
        switch (command) {
          case GLFW_KEY_KP_4 -> Command.ITEM1;
          case GLFW_KEY_KP_6 -> Command.ITEM2;
          default -> Command.NONE;
        };
      case USB -> Command.NONE; // TODO
    };
  }

  /** response for a 2AFC paradigm, vertical setup */
  private Command process2AfcVertical(int command) {
    return switch (command) {
      case GLFW_KEY_KP_8 -> Command.ITEM1;
      case GLFW_KEY_KP_5 -> Command.ITEM2;
      default -> Command.NONE;
    };
  }

  /** response for a 3AFC paradigm, horizontal setup */
  private Command process3AfcHorizontal(int command) {
    return switch (command) {
      case GLFW_KEY_KP_4 -> Command.ITEM1;
      case GLFW_KEY_KP_5 -> Command.ITEM2;
      case GLFW_KEY_KP_6 -> Command.ITEM3;
      default -> Command.NONE;
    };
  }

  /** response for a 3AFC paradigm, vertical setup */
  private Command process3AfcVertical(int command) {
    return switch (command) {
      case GLFW_KEY_KP_8 -> Command.ITEM1;
      case GLFW_KEY_KP_5 -> Command.ITEM2;
      case GLFW_KEY_KP_2 -> Command.ITEM3;
      default -> Command.NONE;
    };
  }

  /** response for a 4AFC paradigm, cross setup */
  private Command process4AfcCross(int command) {
    return switch (command) {
      case GLFW_KEY_KP_8 -> Command.ITEM1;
      case GLFW_KEY_KP_4 -> Command.ITEM2;
      case GLFW_KEY_KP_6 -> Command.ITEM3;
      case GLFW_KEY_KP_2 -> Command.ITEM4;
      default -> Command.NONE;
    };
  }

  /** response for a 4AFC paradigm, diagonal setup */
  private Command process4AfcDiagonal(int command) {
    return switch (command) {
      case GLFW_KEY_KP_7 -> Command.ITEM1;
      case GLFW_KEY_KP_9 -> Command.ITEM2;
      case GLFW_KEY_KP_1 -> Command.ITEM3;
      case GLFW_KEY_KP_3 -> Command.ITEM4;
      default -> Command.NONE;
    };
  }

  /** response for a 8AFC paradigm */
  private Command process8Afc(int command) {
    return switch (command) {
      case GLFW_KEY_KP_7 -> Command.ITEM1;
      case GLFW_KEY_KP_8 -> Command.ITEM2;
      case GLFW_KEY_KP_9 -> Command.ITEM3;
      case GLFW_KEY_KP_4 -> Command.ITEM4;
      case GLFW_KEY_KP_6 -> Command.ITEM5;
      case GLFW_KEY_KP_1 -> Command.ITEM6;
      case GLFW_KEY_KP_2 -> Command.ITEM7;
      case GLFW_KEY_KP_3 -> Command.ITEM8;
      default -> Command.NONE;
    };
  }

  /** response for a 9AFC paradigm */
  private Command process9Afc(int command) {
    return switch (command) {
      case GLFW_KEY_KP_7 -> Command.ITEM1;
      case GLFW_KEY_KP_8 -> Command.ITEM2;
      case GLFW_KEY_KP_9 -> Command.ITEM3;
      case GLFW_KEY_KP_4 -> Command.ITEM4;
      case GLFW_KEY_KP_5 -> Command.ITEM5;
      case GLFW_KEY_KP_6 -> Command.ITEM6;
      case GLFW_KEY_KP_1 -> Command.ITEM7;
      case GLFW_KEY_KP_2 -> Command.ITEM8;
      case GLFW_KEY_KP_3 -> Command.ITEM9;
      default -> Command.NONE;
    };
  }

}
