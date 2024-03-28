package es.optocom.jovp;

import org.lwjgl.glfw.GLFWVidMode;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.InputType;
import es.optocom.jovp.definitions.Paradigm;
import jssc.SerialPortException;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 
 * Manages windows
 *
 * @since 0.0.1
 */
public class Window {

    private static final String TITLE = "JOVP Vulkan Engine";

    private long window;
    private final MonitorManager monitorManager;
    private Monitor monitor;
    private Controller controller;

    private int x;
    private int y;
    private int width;
    private int height;
    private float xscale;
    private float yscale;
    protected boolean fullScreen = false;
    protected boolean resized = false;

    /**
     * 
     * Creates a window
     *
     * @since 0.0.1
     */
    Window() {
        monitorManager = new MonitorManager();
        monitor = monitorManager.getMonitor(0); // primary monitor
        initWindow();
        computeScale();
        int[] workingArea = getWorkingArea(monitor);
        x = workingArea[0] + workingArea[2] / 2;
        y = workingArea[1];
        width = workingArea[2] / 2;
        height = workingArea[3] / 2;
        glfwSetWindowPos(window, x, y);
        glfwSetWindowSize(window, width, height);
    }

    /**
     * 
     * Sets the controller for predefined inputs and paradigms
     *
     * @param input    Either 'mouse', 'keypad', or the name of a suitable USB
     *                 controller
     * @param paradigm The paradigm to use for mapping
     *
     * @throws NullPointerException if no suitable controller is found
     * @throws SerialPortException  if USB serial controller cannot be opened
     * 
     * @since 0.0.1
     */
    public void setController(String input, InputType inputType, Paradigm paradigm) throws NullPointerException, SerialPortException {
        controller = new Controller(window, input, inputType, paradigm);
        if (controller.isUsb())
            controller.open();
    }

    /**
     * 
     * Returns the command recorded from the input device
     * 
     * @return command
     * @since 0.0.1
     */
    public Command getCommand() {
        return controller.getCommand();
    }

    /**
     * 
     * Shows the window
     * 
     * @since 0.0.1
     */
    public void show() {
        glfwShowWindow(window);
        update();
    }

    /**
     * 
     * Hides the window
     * 
     * @since 0.0.1
     */
    public void hide() {
        glfwHideWindow(window);
        update();
    }

    /**
     * 
     * Check whether the window has been resized
     *
     * @return Whether the window has been resized or not
     *
     * @since 0.0.1
     */
    public boolean resized() {
        return resized;
    }

    /**
     * 
     * Set resized to true or falsed
     *
     * @param resized Whether the window has been resized or not
     *
     * @since 0.0.1
     */
    public void resized(boolean resized) {
        this.resized = resized;
    }

    /**
     * 
     * Updates window content
     *
     * @since 0.0.1
     */
    void update() {
        glfwPollEvents();
    }

    /**
     * 
     * Close window and clean up
     *
     * @throws SerialPortException
     *
     * @since 0.0.1
     */
    void cleanup() throws SerialPortException {
        if (controller != null && controller.isUsb())
            controller.close();
        glfwSetWindowShouldClose(window, true);
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        window = NULL;
    }

    /**
     * 
     * Get window handle
     *
     * @return The window handle
     *
     * @since 0.0.1
     */
    public long getHandle() {
        return window;
    }

    /**
     * 
     * Sets window position relative to the current monitor
     *
     * @param x The x position in pixels
     * @param y The y position in pixels
     *
     * @since 0.0.1
     */
    void setPosition(int x, int y) {
        this.x = (int) (x / xscale);
        this.y = (int) (y / yscale);
        int[] workingArea = getWorkingArea(monitor);
        glfwSetWindowPos(window, this.x + workingArea[0], this.y + workingArea[1]);
    }

    /**
     * 
     * Get the window position (x, y) in pixels
     *
     * @return The window position (x, y) in pixels
     *
     * @since 0.0.1
     */
    public int[] getPosition() {
        return new int[] { (int) (x * xscale), (int) (y * yscale) };
    }

    /**
     * 
     * Get monitor manager
     *
     * @return The monitor manager
     *
     * @since 0.0.1
     */
    public MonitorManager getMonitorManager() {
        return monitorManager;
    }

    /**
     * 
     * Get monitor
     *
     * @return The monitor
     *
     * @since 0.0.1
     */
    public Monitor getMonitor() {
        return monitor;
    }

    /**
     * 
     * Set monitor
     *
     * @param monitor The monitor to set for the window
     *
     * @since 0.0.1
     */
    public void setMonitor(int monitor) {
        this.monitor = monitorManager.getMonitor(monitor);
        setPosition(x, y);
        int[] workingArea = getWorkingArea(this.monitor);
        x = workingArea[0] + workingArea[2] / 2;
        y = workingArea[1];
        width = workingArea[2] / 2;
        height = workingArea[3] / 2;
        glfwSetWindowPos(window, x, y);
        glfwSetWindowSize(window, width, height);
        update();
    }

    /**
     * 
     * Get window width
     *
     * @return The window width in pixels
     *
     * @since 0.0.1
     */
    public int getWidth() {
        return (int) (xscale * width);
    }

    /**
     * 
     * Get height
     *
     * @return The window height in pixels
     *
     * @since 0.0.1
     */
    public int getHeight() {
        return (int) (yscale * height);
    }

    /**
     * 
     * Get pixel width
     *
     * @return The pixel width
     *
     * @since 0.0.1
     */
    public float getPixelWidth() {
        return monitor.getPixelWidth();
    }

    /**
     * 
     * Get pixel height
     *
     * @return The pixel height
     *
     * @since 0.0.1
     */
    public float getPixelHeight() {
        return monitor.getPixelHeight();
    }

    /**
     * 
     * Get window width in meters
     *
     * @return The window width in meters
     *
     * @since 0.0.1
     */
    public float getWidthM() {
        return getPixelWidth() * getWidth() / 1000.0f;
    }

    /**
     * 
     * Get window height in meters
     *
     * @return The window height in meters
     *
     * @since 0.0.1
     */
    public float getHeightM() {
        return getPixelHeight() * getHeight() / 1000.0f;
    }

    /**
     * 
     * Change to full-screen mode on demand
     *
     * @since 0.0.1
     */
    void setFullScreen() {
        if (!fullScreen) {
            fullScreen = true;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
            glfwSetWindowMonitor(window, monitor.getHandle(), 0, 0,
                    monitor.getWidth(), monitor.getHeight(), monitor.getRefreshRate());
            update();
            xscale = 1.0f;
            yscale = 1.0f;
        }
    }

    /**
     * 
     * Change to windowed mode on demand
     *
     * @since 0.0.1
     */
    void setWindowed() {
        if (fullScreen) {
            fullScreen = false;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            GLFWVidMode videoMode = monitor.getVideoMode();
            glfwSetWindowMonitor(window, 0, 0, 0,
                    videoMode.width(), videoMode.height(), videoMode.refreshRate());
            setPosition(0, 0);
            update();
            computeScale();
        }
    }

    /**
     * 
     * Sets window settings and updates window dimensions
     *
     * @since 0.0.1
     */
    void setSize(int width, int height) {
        this.width = (int) (width / xscale);
        this.height = (int) (height / yscale);
        glfwSetWindowSize(window, this.width, this.height);
        resized = true;
    }

    /** compute scale */
    private void computeScale() {
        GLFWVidMode videoMode = monitor.getVideoMode();
        xscale = (float) monitor.getWidth() / videoMode.width();
        yscale = (float) monitor.getHeight() / videoMode.height();
    }

    /** Initialize the window */
    private void initWindow() {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_FLOATING, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);
        glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_COCOA_GRAPHICS_SWITCHING, GLFW_FALSE);
        window = glfwCreateWindow(1, 1, TITLE, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");
        setCallbacks();
    }

    /** Window resize callbacks */
    private void setCallbacks() {
        glfwSetWindowSizeCallback(window, (window, width, height) -> {
            this.width = width;
            this.height = height;
            resized(true);
        });
        glfwSetWindowPosCallback(window, (window, x, y) -> {
            int[] workingArea = getWorkingArea(monitor);
            this.x = x - workingArea[0];
            this.y = y - workingArea[1];
        });

    }

    /** Get the working area of a monitor on the virtual Desktop */
    private int[] getWorkingArea(Monitor monitor) {
        int[] workingArea = new int[4];
        // get working area
        int[] x = new int[1];
        int[] y = new int[1];
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetMonitorWorkarea(monitor.getHandle(), x, y, width, height);
        workingArea[0] = x[0];
        workingArea[1] = y[0];
        workingArea[2] = width[0];
        workingArea[3] = height[0];
        return workingArea;
    }

}