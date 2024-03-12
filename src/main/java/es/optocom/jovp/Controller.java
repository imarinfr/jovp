package es.optocom.jovp;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Arrays;
import java.util.regex.Pattern;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Input;
import es.optocom.jovp.definitions.InputType;
import es.optocom.jovp.definitions.Paradigm;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;

/**
 * 
 * Input schemes for different psychophysics paradigms
 *
 * @since 0.0.1
 */
public class Controller implements SerialPortEventListener {

    /** Byte count for USB serial controller */
    private static final int BYTE_COUNT = 5;
    /** Timeout for USB serial controller */
    private static final int TIMEOUT = 50;
    /** USB button code: Button 1 */
    private static final int BUTTON1 = 1;
    /** USB button code: Button 2 */
    private static final int BUTTON2 = 2;
    /** USB button code: Button 3 */
    private static final int BUTTON3 = 3;
    /** USB button code: Button 4 */
    private static final int BUTTON4 = 4;
    /** USB button code: Button 5 */
    private static final int BUTTON5 = 5;
    /** USB button code: Button 6 */
    private static final int BUTTON6 = 6;
    /** USB button code: Button 7 */
    private static final int BUTTON7 = 7;
    /** USB button code: Button 8 */
    private static final int BUTTON8 = 8;
    /** USB button code: Button 9 */
    private static final int BUTTON9 = 9;

    /** Error message for serial port exception */
    private static final String CANNOT_SET_CONTROLLER = "The input device '%s' is not suitable as a controller";

    /**
     * 
     * Scans the port for suitable USB connections
     *
     * @return The list of suitable USB connections
     *
     * @since 0.0.1
     */
    public static String[] getSuitableControllers() {
        Pattern pattern = switch (System.getProperty("os.name")) {
            case "Linux" -> Pattern.compile("(ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO)[0-9]{1,3}");
            case "SunOS" -> Pattern.compile("[0-9]*|[a-z]*");
            case "Mac OS X", "Darwin" -> Pattern.compile("cu\\.usbserial\\-");
            case "Win" -> Pattern.compile("");
            default -> Pattern.compile(""); // 'Win' and other unknown OS
        };
        return SerialPortList.getPortNames(pattern);
    }

    /**
     * 
     * Find a connection matching name
     *
     * @param name Name to match
     * @return The first in the list of suitable USB connections that matches the
     *         name
     *
     * @since 0.0.1
     */
    public static String searchByName(String name) {
        return Arrays.stream(getSuitableControllers())
                .filter(Pattern.compile(name).asPredicate())
                .findFirst().orElse(null).toString();
    }

    /** Input device */
    private final Input input;
    /** Input type */
    private final int inputType;
    /** USB port */
    private SerialPort usb = null;
    /** Psychophysics paradigm to map input to commands */
    private final Paradigm paradigm;
    /** Controller command */
    private Command command = Command.NONE;

    /**
     * 
     * Controller type and settings
     *
     * @param windowHandle The window handle
     * @param input Either 'mouse', 'keypad', or the name of a suitable USB controller
     * @param inputType Whether command is when pressed, released, or repeat.
     * @param paradigm Preset scheme for the psychophysics paradigm
     * 
     * @throws NullPointerException if no suitable controller is found
     *
     * @since 0.0.1
     */
    Controller(long windowHandle, String input, InputType inputType, Paradigm paradigm) throws NullPointerException {
        switch (input.toUpperCase()) {
            case "MOUSE" -> {
                this.input = Input.MOUSE;
                mouseCallbacks(windowHandle);
            }
            case "KEYPAD" -> {
                this.input = Input.KEYPAD;
                keypadCallbacks(windowHandle);
            }
            default -> {
                try {
                    this.input = Input.USB;
                    this.usb = new SerialPort(searchByName(input));
                } catch (NullPointerException ignored) {
                    throw new IllegalArgumentException(String.format(CANNOT_SET_CONTROLLER, input));
                }
            }
        }
        this.inputType = switch (inputType) {
            case PRESS -> GLFW_PRESS;
            case RELEASE -> GLFW_RELEASE;
            case REPEAT -> GLFW_REPEAT;
        };
        this.paradigm = paradigm;
        glfwSetWindowCloseCallback(windowHandle, (window) -> closeWindowClicked());
    }

    /**
     * 
     * Check whether controller is connected through a USB serial port
     *
     * @return whether controller is connected through a USB serial port
     *
     * @since 0.0.1
     */
    public boolean isUsb() {
        return input == Input.USB;
    }

    /**
     * 
     * Opens a serial controller for a specific device
     *
     * @throws SerialPortException if port cannot be opened
     *
     * @since 0.0.1
     */
    public void open() throws SerialPortException {
        usb.openPort();
        // TODO this works specifically for IMOVifa clicker. Is it generic??
        usb.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        purge();
        usb.addEventListener(this);
    }

    /**
     * 
     * Closes the port
     *
     * @throws SerialPortException if port cannot be closed
     *
     * @since 0.0.1
     */
    public void close() throws SerialPortException {
        usb.closePort();
    }

    /**
     * 
     * Purges the Serial port upon request and if it contains data
     *
     * @throws SerialPortException if device cannot be purged
     *
     * @since 0.0.1
     */
    public void purge() throws SerialPortException {
        if (usb.getInputBufferBytesCount() > 0)
            usb.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
    }

    /**
     * 
     * For USB controllers, this is where the event is captured and processed
     *
     * @since 0.0.1
     */
    public void serialEvent(SerialPortEvent event) {
//System.out.println(event);
//System.out.println("bytes in buffer (for RXCHAR): " + event.getEventValue());
//System.out.println("is RXCHAR: " + event.isRXCHAR());
        if (event.isRXCHAR()) {
            try {
                // TODO Code specifically for the ImoVifa button
                // On click it sends 5 bytes (as ints: 42 79 78 78 35)
                // On release it sends 5 bytes (as ints: 42 79 70 70 35)
                int[] msg = usb.readIntArray(BYTE_COUNT, TIMEOUT);
                for (int i : msg)
                    System.out.print(i + " ");
                if (msg[3] == 78)
                    System.out.println("Pressed");
                if (msg[3] == 70)
                    System.out.println("Released");
                if (msg[3] == 78)
                    usbButton(1, inputType);
            } catch (SerialPortTimeoutException e) { 
            } catch (SerialPortException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 
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
     * 
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
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> processButton(button, action));
    }

    /** add callbacks to keys in the specified window */
    private void keypadCallbacks(long windowHandle) {
        glfwSetMouseButtonCallback(windowHandle, null);
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> processKey(key, action));
    }

    /** close window icon clicked */
    private void closeWindowClicked() {
        command = Command.CLOSE;
    }

    /** mouse button pressed */
    private void usbButton(int button, int action) {
        if (action == inputType) command = processCommand(button);
    }

    /** mouse button pressed */
    private void processButton(int button, int action) {
        if (action == inputType) command = processCommand(button);
    }

    /** keypad key pressed */
    private void processKey(int key, int action) {
        if (action == inputType || (inputType == GLFW_REPEAT & action != 0)) command = processCommand(key);
    }

    /** response for a clicker paradigm */
    private Command processClicker(int command) {
        return switch (input) {
            case MOUSE -> command == GLFW_MOUSE_BUTTON_LEFT ? Command.YES : Command.NONE;
            case KEYPAD -> command == GLFW_KEY_KP_0 ? Command.YES : Command.NONE;
            case USB -> command == BUTTON1 ? Command.YES : Command.NONE;
        };
    }

    /** response for a 2AFC paradigm */
    private Command process2AfcHorizontal(int command) {
        return switch (input) {
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
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    default -> Command.NONE;
                };
        };
    }

    /** response for a 2AFC paradigm, vertical setup */
    private Command process2AfcVertical(int command) {
        return switch (input) {
            case MOUSE -> Command.NONE;
            case KEYPAD ->
                switch (command) {
                    case GLFW_KEY_KP_8 -> Command.ITEM1;
                    case GLFW_KEY_KP_5 -> Command.ITEM2;
                    default -> Command.NONE;
                };
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    default -> Command.NONE;
                };
        };
    }

    /** response for a 3AFC paradigm, horizontal setup */
    private Command process3AfcHorizontal(int command) {
        return switch (input) {
            case MOUSE -> Command.NONE;
            case KEYPAD ->
                switch (command) {
                    case GLFW_KEY_KP_4 -> Command.ITEM1;
                    case GLFW_KEY_KP_5 -> Command.ITEM2;
                    case GLFW_KEY_KP_6 -> Command.ITEM3;
                    default -> Command.NONE;
                };
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    case BUTTON3 -> Command.ITEM3;
                    default -> Command.NONE;
                };
        };
    }

    /** response for a 3AFC paradigm, vertical setup */
    private Command process3AfcVertical(int command) {
        return switch (input) {
            case MOUSE -> Command.NONE;
            case KEYPAD ->
                switch (command) {
                    case GLFW_KEY_KP_8 -> Command.ITEM1;
                    case GLFW_KEY_KP_5 -> Command.ITEM2;
                    case GLFW_KEY_KP_2 -> Command.ITEM3;
                    default -> Command.NONE;
                };
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    case BUTTON3 -> Command.ITEM3;
                    default -> Command.NONE;
                };
        };
    }

    /** response for a 4AFC paradigm, cross setup */
    private Command process4AfcCross(int command) {
        return switch (input) {
            case MOUSE -> Command.NONE;
            case KEYPAD ->
                switch (command) {
                    case GLFW_KEY_KP_8 -> Command.ITEM1;
                    case GLFW_KEY_KP_4 -> Command.ITEM2;
                    case GLFW_KEY_KP_6 -> Command.ITEM3;
                    case GLFW_KEY_KP_2 -> Command.ITEM4;
                    default -> Command.NONE;
                };
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    case BUTTON3 -> Command.ITEM3;
                    case BUTTON4 -> Command.ITEM4;
                    default -> Command.NONE;
                };
        };
    }

    /** response for a 4AFC paradigm, diagonal setup */
    private Command process4AfcDiagonal(int command) {
        return switch (input) {
            case MOUSE -> Command.NONE;
            case KEYPAD ->
                switch (command) {
                    case GLFW_KEY_KP_7 -> Command.ITEM1;
                    case GLFW_KEY_KP_9 -> Command.ITEM2;
                    case GLFW_KEY_KP_1 -> Command.ITEM3;
                    case GLFW_KEY_KP_3 -> Command.ITEM4;
                    default -> Command.NONE;
                };
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    case BUTTON3 -> Command.ITEM3;
                    case BUTTON4 -> Command.ITEM4;
                    default -> Command.NONE;
                };
        };
    }

    /** response for a 8AFC paradigm */
    private Command process8Afc(int command) {
        return switch (input) {
            case MOUSE -> Command.NONE;
            case KEYPAD ->
                switch (command) {
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
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    case BUTTON3 -> Command.ITEM3;
                    case BUTTON4 -> Command.ITEM4;
                    case BUTTON5 -> Command.ITEM5;
                    case BUTTON6 -> Command.ITEM6;
                    case BUTTON7 -> Command.ITEM7;
                    case BUTTON8 -> Command.ITEM8;
                    default -> Command.NONE;
                };
        };
    }

    /** response for a 9AFC paradigm */
    private Command process9Afc(int command) {
        return switch (input) {
            case MOUSE -> Command.NONE;
            case KEYPAD ->
                switch (command) {
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
            case USB ->
                switch (command) {
                    case BUTTON1 -> Command.ITEM1;
                    case BUTTON2 -> Command.ITEM2;
                    case BUTTON3 -> Command.ITEM3;
                    case BUTTON4 -> Command.ITEM4;
                    case BUTTON5 -> Command.ITEM5;
                    case BUTTON6 -> Command.ITEM6;
                    case BUTTON7 -> Command.ITEM7;
                    case BUTTON8 -> Command.ITEM8;
                    case BUTTON9 -> Command.ITEM9;
                    default -> Command.NONE;
                };
        };
    }

}
