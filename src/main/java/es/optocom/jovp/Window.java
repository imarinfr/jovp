package es.optocom.jovp;

import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Manages windows
 *
 * @since 0.0.1
 */
public class Window {

  private static final String TITLE = "JOVP Vulkan Engine";

  private long window;
  private final MonitorManager monitorManager;
  private Monitor monitor;

  private int x;
  private int y;
  private int width;
  private int height;
  private float xscale;
  private float yscale;
  protected boolean fullScreen = false;
  protected boolean show = true;
  protected boolean resized = false;

  /**
   * Creates a window
   *
   * @since 0.0.1
   */
  Window() {
    monitorManager = new MonitorManager();
    monitor = monitorManager.getMonitor(0); // primary monitor
    initWindow();
    setDefaultDimensions();
    setSize(width, height);
    setPosition(x, y);
  }

  /**
   * Shows the window
   *
   * @param show whether to show the window
   * 
   * @since 0.0.1
   */
  public void show(boolean show) {
    this.show = show;
    if (show) glfwShowWindow(window);
    else glfwHideWindow(window);
    update();
  }

  /**
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
   * Set resized to true
   *
   * @param resized Whether the window has been resized or not
   *
   * @since 0.0.1
   */
  public void resized(boolean resized) {
    this.resized = resized;
  }

  /**
   * Focus on current window
   *
   * @since 0.0.1
   */
  void focus() {
    glfwFocusWindow(window);
  }

  /**
   * Updates window content
   *
   * @since 0.0.1
   */
  void update() {
    glfwPollEvents();
  }

  /**
   * Close window and clean up
   *
   * @since 0.0.1
   */
  void cleanup() {
    glfwSetWindowShouldClose(window, true);
    glfwFreeCallbacks(window);
    glfwDestroyWindow(window);
    window = NULL;
  }

  /**
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
   * Get the window position (x, y) in pixels relative to Monitor
   *
   * @return The window position (x, y) in pixels relative to Monitor
   *
   * @since 0.0.1
   */
  public int[] getPosition() {
    int[] workingArea = getWorkingArea(monitor.getHandle());
    return new int[] { x - workingArea[0], y - workingArea[1] };
  }

  /**
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
   * Set monitor
   *
   * @param monitor The monitor to set for the window
   *
   * @since 0.0.1
   */
  public void setMonitor(int monitor) {
    this.monitor = monitorManager.getMonitor(monitor);
    setContentScale();
    int[] workingArea = getWorkingArea(this.monitor.getHandle());
    x = workingArea[2] - width;
    y = 0;
    setPosition(x, y);
    update();
  }

  /**
   * Get window width
   *
   * @return The window width in screen coordinates
   *
   * @since 0.0.1
   */
  public int getWidth() {
    return (int) (xscale * width);
  }

  /**
   * Get height
   *
   * @return The window height in screen coordinates
   *
   * @since 0.0.1
   */
  public int getHeight() {
    return (int) (yscale * height);
  }

  /**
   * Get pixel width
   *
   * @return The window width in screen coordinates
   *
   * @since 0.0.1
   */
  public double getPixelWidth() {
    return monitor.getPixelWidth();
  }

  /**
   * Get pixel aspect
   *
   * @return The aspect ratio between x and y pixel sizes
   *
   * @since 0.0.1
   */
  public double getPixelAspect() {
    return monitor.getPixelAspect();
  }

  /**
   * Get pixel height
   *
   * @return The window height in screen coordinates
   *
   * @since 0.0.1
   */
  public double getPixelHeight() {
    return monitor.getPixelHeight();
  }

  /**
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
      setContentScale();
      update();
    }
  }

  /**
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
      setContentScale();
      update();
    }
  }

  /**
   * Sets window settings and updates window dimensions
   *
   * @since 0.0.1
   */
  void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    glfwSetWindowSize(window, width, height);
    resized = true;
    update();
  }

  /**
   * Sets window position relative to the current monitor
   *
   * @param x The x position in pixels
   * @param y The y position in pixels
   *
   * @since 0.0.1
   */
  void setPosition(int x, int y) {
    int[] workingArea = getWorkingArea(monitor.getHandle());
    this.x = x + workingArea[0];
    this.y = y + workingArea[1];
    glfwSetWindowPos(window, this.x, this.y);
    update();
  }

  /** Initialize the window */
  private void initWindow() {
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_FLOATING, GLFW_TRUE);
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
    setContentScale();
    setCallbacks();
  }

  /** Set content scale */
  private void setContentScale() {
    GLFWVidMode videoMode = glfwGetVideoMode(monitor.getHandle());
    if (videoMode == null)
      throw new RuntimeException("Cannot retrieve video mode");
    xscale = monitor.getWidth() / (float) videoMode.width();
    yscale = monitor.getHeight() / (float) videoMode.height();
  }

  /** Window resize callbacks */
  private void setCallbacks() {
    glfwSetWindowSizeCallback(window, (window, width, height) -> {
      this.width = width;
      this.height = height;
      resized(true);
    });
    glfwSetWindowContentScaleCallback(window, (window, xscale, yscale) -> {
      this.xscale = xscale;
      this.yscale = yscale;
    });
    glfwSetWindowPosCallback(window, (window, x, y) -> {
      this.x = x;
      this.y = y;
    });

  }

  /** Get the working area of a monitor on the virtual Desktop */
  private int [] getWorkingArea(long monitor) {
    int[] workingArea = new int[4];
    // get working area
    int[] x = new int[1];
    int[] y = new int[1];
    int[] width = new int[1];
    int[] height = new int[1];
    glfwGetMonitorWorkarea(monitor, x, y, width, height);
    workingArea[0] = x[0];
    workingArea[1] = y[0];
    workingArea[2] = width[0];
    workingArea[3] = height[0];
    if (fullScreen) { // if not full screen, add window frame size
      int[] left = new int[1];
      int[] top = new int[1];
      int[] right = new int[1];
      int[] bottom = new int[1];
      glfwGetWindowFrameSize(window, left, top, right, bottom);
      workingArea[0] += left[0];
      workingArea[1] += top[0];
      workingArea[2] -= right[0];
      workingArea[3] -= bottom[0];
    }
    return workingArea;
  }

  /** Default window dimensions for windowed mode */
  private void setDefaultDimensions() {
    int[] workingArea = getWorkingArea(monitor.getHandle());
    x = workingArea[2] / 2;
    y = 0;
    width = workingArea[2] / 2;
    height = workingArea[3] / 2;
  }

}