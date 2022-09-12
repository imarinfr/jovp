package es.optocom.jovp;

import es.optocom.jovp.structures.Command;
import es.optocom.jovp.structures.Input;
import es.optocom.jovp.structures.Paradigm;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Input schemes for different psychophysics paradigms
 *
 * @since 0.0.1
 */
public class Controller {

  private final Input input;
  private final Paradigm paradigm;

  private Command command = Command.NONE;

  /**
   * Controller type and settings
   *
   * @param windowHandle The window handle
   * @param input        The input is listening to
   * @param paradigm     Preset scheme for the psychophysics paradigm
   *
   * @since 0.0.1
   */
  public Controller(long windowHandle, Input input, Paradigm paradigm) {
    this.input = input;
    this.paradigm = paradigm;
    callbacks(windowHandle);
  }

  /**
   * Process command
   *
   * @param command Command to be processed
   *
   * @return The response from the observer
   */
  Command processCommand(int command) {
    if (input == Input.NONE)
      throw new RuntimeException("No input assigned yet");
    if ((input == Input.MOUSE && command == GLFW_MOUSE_BUTTON_MIDDLE) ||
        (input == Input.KEYPAD && command == GLFW_KEY_KP_ENTER))
      return Command.PAUSE;
    if (command == GLFW_KEY_ESCAPE)
      return Command.CLOSE;
    // OpenGL key or button mapping to response
    return switch (paradigm) {
      case CLICKER -> processClicker(command);
      case M2AFC_HORIZONTAL -> process2AfcHorizontal(command);
      case M2AFC_VERTICAL -> process2AfcVertical(command);
      case M3AFC_HORIZONTAL -> process3AfcHorizontal(command);
      case M3AFC_VERTICAL -> process3AfcVertical(command);
      case M4AFC_CROSS -> process4AfcCross(command);
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
  public Command getCommand() {
    Command cmd = command;
    command = Command.NONE;
    return cmd;
  }

  /** add callbacks to keys in the specified window */
  private void callbacks(long windowHandle) {
    // response-related callbacks
    glfwSetWindowCloseCallback(windowHandle, (window) -> closeWindowClicked());
    if (input == Input.KEYPAD) {
      glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> keyPressed(key, action));
      glfwSetMouseButtonCallback(windowHandle, null);
    }
    if (input == Input.MOUSE) {
      // set a callback so that it closes when escape is pressed
      glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> EscPressed(key));
      glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> buttonPressed(button, action));
    }
  }

  /** close window icon clicked */
  private void closeWindowClicked() {
    command = processCommand(GLFW_KEY_ESCAPE);
  }

  /** listen too to the Esc key to close window */
  private void EscPressed(int key) {
    if (key == GLFW_KEY_ESCAPE) {
      command = processCommand(GLFW_KEY_ESCAPE);
    }
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
    if (input == Input.MOUSE)
      if (command == GLFW_MOUSE_BUTTON_LEFT)
        return Command.YES;
    if (input == Input.KEYPAD)
      if (command == GLFW_KEY_KP_0)
        return Command.YES;
    return Command.NONE;
  }

  /** response for a 2AFC paradigm */
  private Command process2AfcHorizontal(int command) {
    if (input == Input.MOUSE) {
      if (command == GLFW_MOUSE_BUTTON_LEFT)
        return Command.ITEM1;
      if (command == GLFW_MOUSE_BUTTON_RIGHT)
        return Command.ITEM2;
    }
    if (input == Input.KEYPAD) {
      if (command == GLFW_KEY_KP_4)
        return Command.ITEM1;
      if (command == GLFW_KEY_KP_6)
        return Command.ITEM2;
    }
    return Command.NONE;
  }

  /** response for a 2AFC paradigm, vertical setup */
  private Command process2AfcVertical(int command) {
    // only valid for keypad
    if (command == GLFW_KEY_KP_8)
      return Command.ITEM1;
    if (command == GLFW_KEY_KP_2)
      return Command.ITEM2;
    return Command.NONE;
  }

  /** response for a 3AFC paradigm, horizontal setup */
  private Command process3AfcHorizontal(int command) {
    // only valid for keypad
    if (command == GLFW_KEY_KP_4)
      return Command.ITEM1;
    if (command == GLFW_KEY_KP_5)
      return Command.ITEM2;
    if (command == GLFW_KEY_KP_6)
      return Command.ITEM3;
    return Command.NONE;
  }

  /** response for a 3AFC paradigm, vertical setup */
  private Command process3AfcVertical(int command) {
    // only valid for keypad
    if (command == GLFW_KEY_KP_8)
      return Command.ITEM1;
    if (command == GLFW_KEY_KP_5)
      return Command.ITEM2;
    if (command == GLFW_KEY_KP_2)
      return Command.ITEM3;
    return Command.NONE;
  }

  /** response for a 4AFC paradigm, cross setup */
  private Command process4AfcCross(int command) {
    // only valid for keypad
    if (command == GLFW_KEY_KP_8)
      return Command.ITEM1;
    if (command == GLFW_KEY_KP_4)
      return Command.ITEM2;
    if (command == GLFW_KEY_KP_6)
      return Command.ITEM3;
    if (command == GLFW_KEY_KP_2)
      return Command.ITEM4;
    return Command.NONE;
  }

  /** response for a 4AFC paradigm, diagonal setup */
  private Command process4AfcDiagonal(int command) {
    // only valid for keypad
    if (command == GLFW_KEY_KP_7)
      return Command.ITEM1;
    if (command == GLFW_KEY_KP_9)
      return Command.ITEM2;
    if (command == GLFW_KEY_KP_1)
      return Command.ITEM3;
    if (command == GLFW_KEY_KP_3)
      return Command.ITEM4;
    return Command.NONE;
  }

  /** response for a 8AFC paradigm */
  private Command process8Afc(int command) {
    // only valid for keypad
    if (command == GLFW_KEY_KP_7)
      return Command.ITEM1;
    if (command == GLFW_KEY_KP_8)
      return Command.ITEM2;
    if (command == GLFW_KEY_KP_9)
      return Command.ITEM3;
    if (command == GLFW_KEY_KP_4)
      return Command.ITEM4;
    if (command == GLFW_KEY_KP_6)
      return Command.ITEM5;
    if (command == GLFW_KEY_KP_1)
      return Command.ITEM6;
    if (command == GLFW_KEY_KP_2)
      return Command.ITEM7;
    if (command == GLFW_KEY_KP_3)
      return Command.ITEM8;
    return Command.NONE;
  }

  /** response for a 9AFC paradigm */
  private Command process9Afc(int command) {
    // only valid for keypad
    if (command == GLFW_KEY_KP_7)
      return Command.ITEM1;
    if (command == GLFW_KEY_KP_8)
      return Command.ITEM2;
    if (command == GLFW_KEY_KP_9)
      return Command.ITEM3;
    if (command == GLFW_KEY_KP_4)
      return Command.ITEM4;
    if (command == GLFW_KEY_KP_5)
      return Command.ITEM5;
    if (command == GLFW_KEY_KP_6)
      return Command.ITEM6;
    if (command == GLFW_KEY_KP_1)
      return Command.ITEM7;
    if (command == GLFW_KEY_KP_2)
      return Command.ITEM8;
    if (command == GLFW_KEY_KP_3)
      return Command.ITEM9;
    return Command.NONE;
  }

}
