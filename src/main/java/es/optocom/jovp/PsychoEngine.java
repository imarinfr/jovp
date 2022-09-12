package es.optocom.jovp;

import es.optocom.jovp.rendering.VulkanManager;
import es.optocom.jovp.structures.Command;
import es.optocom.jovp.structures.Input;
import es.optocom.jovp.structures.Paradigm;
import es.optocom.jovp.structures.ViewMode;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.vulkan.VK13.vkDeviceWaitIdle;

/**
 * The JOVP engine that manages the Observer, Screens, Window, Input, and
 * Renderer
 *
 * @since 0.0.1
 */
public class PsychoEngine {

  public static final boolean VALIDATION_LAYERS = DEBUG.get(true);
  public static final boolean API_DUMP = false;

  private final PsychoLogic psychoLogic;
  private final Window window;
  private final VulkanManager vulkanManager;
  private final List<VkPhysicalDevice> physicalDevices;
  private final Controller controller;
  private final Timer timer;

  private int numberOfCores;
  private long freeMemory;
  private long maxMemory;
  private long totalMemory;

  private ViewMode viewMode;
  private Paradigm paradigm;
  private boolean loop;

  /**
   * Main method for the JOVP
   *
   * @param psychoLogic Logic for the psychophysics experience
   * @param distance    Viewing distance of the observer in mm
   *
   * @since 0.0.1
   */
  public PsychoEngine(PsychoLogic psychoLogic, int distance) {
    this(psychoLogic, distance, ViewMode.MONO, Input.KEYPAD, Paradigm.M2AFC_HORIZONTAL,
        VALIDATION_LAYERS, API_DUMP);
  }

  /**
   * Main method for the JOVP
   *
   * @param psychoLogic Logic for the psychophysics experience
   * @param distance    Viewing distance of the observer in mm
   * @param input       Input to use as input for observer's input
   *
   * @since 0.0.1
   */
  public PsychoEngine(PsychoLogic psychoLogic, int distance, Input input) {
    this(psychoLogic, distance, ViewMode.MONO, input, Paradigm.M2AFC_HORIZONTAL,
        VALIDATION_LAYERS, API_DUMP);
  }

  /**
   * Main method for the JOVP
   *
   * @param psychoLogic Logic for the psychophysics experience
   * @param distance    Viewing distance of the observer in mm
   * @param viewMode    Viewing viewMode
   *
   * @since 0.0.1
   */
  public PsychoEngine(PsychoLogic psychoLogic, int distance, ViewMode viewMode) {
    this(psychoLogic, distance, viewMode, Input.KEYPAD, Paradigm.M2AFC_HORIZONTAL,
        VALIDATION_LAYERS, API_DUMP);
  }

  /**
   * Main method for the JOVP
   *
   * @param psychoLogic Logic for the psychophysics experience
   * @param viewMode    Viewing viewMode
   * @param distance    Viewing distance of the observer in mm
   * @param input       Input to use as input for observer's input
   *
   * @since 0.0.1
   */
  public PsychoEngine(PsychoLogic psychoLogic, int distance, ViewMode viewMode, Input input) {
    this(psychoLogic, distance, viewMode, input, Paradigm.M2AFC_HORIZONTAL, VALIDATION_LAYERS, API_DUMP);
  }

  /**
   * Main method for the JOVP
   *
   * @param psychoLogic Logic for the psychophysics experience
   * @param viewMode    Viewing viewMode
   * @param distance    Viewing distance of the observer in mm
   * @param input       Input to use as input for observer's input
   * @param paradigm    The psychophysical paradigm to use
   *
   * @since 0.0.1
   */
  public PsychoEngine(PsychoLogic psychoLogic, int distance, ViewMode viewMode, Input input, Paradigm paradigm) {
    this(psychoLogic, distance, viewMode, input, paradigm, VALIDATION_LAYERS, API_DUMP);
  }

  /**
   * Main method for the JOVP
   *
   * @param psychoLogic      Logic for the psychophysics experience
   * @param viewMode         Viewing viewMode
   * @param distance         Viewing distance of the observer in mm
   * @param input            Input to use as input for observer's input
   * @param paradigm         The psychophysical paradigm to use
   * @param validationLayers Whether to use validation layers
   * @param apiDump          Whether to use the VK_LAYER_LUNARG_api_dump layer
   *
   * @since 0.0.1
   */
  public PsychoEngine(PsychoLogic psychoLogic, int distance, ViewMode viewMode, Input input, Paradigm paradigm,
      boolean validationLayers, boolean apiDump) {
    glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
    if (!glfwInit())
      throw new RuntimeException("Cannot initialize GLFW");
    this.psychoLogic = psychoLogic;
    this.viewMode = viewMode;
    this.paradigm = paradigm;
    window = new Window();
    vulkanManager = new VulkanManager(window, distance, validationLayers, apiDump);
    physicalDevices = vulkanManager.getPhysicalDevices();
    controller = new Controller(window.getHandle(), input, paradigm);
    timer = new Timer();
    getRunTimeInfo();
    setView();
  }

  /**
   * Get physical devices
   *
   * @return A list of physical devices
   *
   * @since 0.0.1
   */
  public List<VkPhysicalDevice> getPhysicalDevices() {
    return physicalDevices;
  }

  /**
   * Start the psychoEngine in the default physical device
   *
   * @since 0.0.1
   */
  public void start() {
    start(physicalDevices.get(0)); // Run with default physical device
  }

  /**
   * Run the psychoEngine in a selected physical device
   *
   * @param physicalDevice The physical device for the psychoEngine run
   *
   * @since 0.0.1
   */
  public void start(VkPhysicalDevice physicalDevice) {
    try {
      init();
      vulkanManager.start(physicalDevice, viewMode, PsychoLogic.items);
      psychoLoop();
      vkDeviceWaitIdle(vulkanManager.getDevice());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Initializes the window, controller, timer, and other running parameters */
  private void init() {
    window.show();
    psychoLogic.init(this);
    timer.start();
    loop = true;
  }

  /**
   * Instruct the engine to finish the loop
   *
   * @since 0.0.1
   */
  public void finish() {
    loop = false;
  }

  /**
   * Clean up after use
   *
   * @since 0.0.1
   */
  public void cleanup() {
    vulkanManager.cleanup();
    window.cleanup();
    glfwTerminate();
    Objects.requireNonNull(glfwSetErrorCallback(null)).free();
  }

  /**
   * Get window
   *
   * @return The window
   *
   * @since 0.0.1
   */
  public Window getWindow() {
    return window;
  }

  /**
   * Get the Vulkan manager
   *
   * @return The Vulkan manager
   *
   * @since 0.0.1
   */
  public VulkanManager getVulkanManager() {
    return vulkanManager;
  }

  /**
   * Set the viewing mode
   *
   * @param viewMode The viewing mode
   *
   * @since 0.0.1
   */
  public void setViewMode(ViewMode viewMode) {
    this.viewMode = viewMode;
  }

  /**
   * Get the viewing mode
   *
   * @return The viewing viewMode
   *
   * @since 0.0.1
   */
  public ViewMode getViewMode() {
    return viewMode;
  }

  /**
   * Set observer's distance
   *
   * @param distance The distance of the observer from the display
   *
   * @since 0.0.1
   */
  public void setDistance(int distance) {
    vulkanManager.setDistance(distance);
  }

  /**
   * Get observer's distance
   *
   * @return The distance of the observer from the display
   *
   * @since 0.0.1
   */
  public int getDistance() {
    return vulkanManager.getDistance();
  }

  /**
   * Set the psychophysics paradigm
   *
   * @param paradigm The psychophysics paradigm
   *
   * @since 0.0.1
   */
  public void setParadigm(Paradigm paradigm) {
    this.paradigm = paradigm;
  }

  /**
   * Get the psychophysics paradigm
   *
   * @return The psychophysics paradigm
   *
   * @since 0.0.1
   */
  public Paradigm getParadigm() {
    return paradigm;
  }

  /**
   * Get the field of view
   *
   * @return The field of view (x, y) in degrees
   *
   * @since 0.0.1
   */
  public float[] getFieldOfView() {
    return vulkanManager.getFieldOfView();
  }

  /**
   * Set view
   *
   * @since 0.0.1
   */
  public void setView() { // TODO
    Vector3f eye = new Vector3f(0.0f, 0.0f, 0.0f);
    Vector3f center = new Vector3f(0.0f, 0.0f, 1.0f);
    Vector3f up = new Vector3f(0.0f, -1f, 0.0f);
    Matrix4f view = new Matrix4f();
    view.lookAt(eye, center, up);
    vulkanManager.setView(eye, center, up);
  }

  /**
   * Set the window monitor
   * 
   * @param monitor Set the window monitor
   *
   * @since 0.0.1
   */
  public void setWindowMonitor(int monitor) {
    window.setMonitor(monitor);
  }

  /**
   * Set window position
   *
   * @param x Monitor x position in pixels
   * @param y Monitor y position in pixels
   *
   * @since 0.0.1
   */
  public void setWindowPosition(int x, int y) {
    window.setPosition(x, y);
  }

  /**
   * Get window position
   *
   * @return The window position (x, y) in pixels relative to the monitor
   *
   * @since 0.0.1
   */
  public int[] getWindowPosition() {
    return window.getPosition();
  }

  /**
   * Set window size
   *
   * @param width  Monitor width in pixels
   * @param height Monitor height in pixels
   *
   * @since 0.0.1
   */
  public void setWindowSize(int width, int height) {
    window.setSize(width, height);
    vulkanManager.computeFieldOfView();
  }

  /**
   * Set full-screen mode to current monitor
   *
   * @since 0.0.1
   */
  public void setFullScreen() {
    window.setFullScreen();
  }

  /**
   * Set windowed mode to current monitor
   *
   * @since 0.0.1
   */
  public void setWindowed() {
    window.setWindowed();
  }

  /**
   * Get command
   *
   * @return The command from the controller
   *
   * @since 0.0.1
   */
  public Command getCommand() {
    return controller.getCommand();
  }

  /**
   * Get start time
   *
   * @return The start time in ms
   *
   * @since 0.0.1
   */
  public double getStartTime() {
    return timer.getStartTime();
  }

  /**
   * Get elapsed time
   *
   * @return Elapse time in ms
   *
   * @since 0.0.1
   */
  public double getElapsedTime() {
    return timer.getElapsedTime();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    String freeMemoryTxt = String.format("%.1f", (float) freeMemory / 1048576);
    String maxMemoryTxt = String.format("%.1f", (float) maxMemory / 1048576);
    String totalMemoryTxt = String.format("%.1f", (float) totalMemory / 1048576);
    StringBuilder txt = new StringBuilder("Supported platforms:");
    for (String platform : getSupportedPlatforms()) {
      txt.append(" ").append(platform);
    }
    txt.append("\n");
    txt.append("Selected platform: ").append(getPlatform()).append("\n");
    txt.append("Number of cores: ").append(numberOfCores).append("\n");
    txt.append("Free memory: ").append(freeMemoryTxt).append(" MB").append("\n");
    txt.append("Maximum memory: ").append(maxMemoryTxt).append(" MB").append("\n");
    txt.append("Total memory: ").append(totalMemoryTxt).append(" MB").append("\n");
    return txt.toString();
  }

  /** Performs the loop for the psychophysics experience */
  private void psychoLoop() {
    while (loop) {
      if (window.shown())
        window.focus();
      update();
      drawFrame();
      input();
    }
  }

  /** Process input */
  private void input() {
    Command command = getCommand();
    psychoLogic.input(command, timer.getElapsedTime());
    if (command == Command.CLOSE)
      loop = false;
  }

  /** Update and get ready for rendering */
  private void update() {
    psychoLogic.update(this);
  }

  /** render and update window */
  private void drawFrame() {
    vulkanManager.drawFrame();
    window.update();
  }

  /** Get runtime information */
  private void getRunTimeInfo() {
    numberOfCores = Runtime.getRuntime().availableProcessors();
    freeMemory = Runtime.getRuntime().freeMemory();
    maxMemory = Runtime.getRuntime().maxMemory();
    totalMemory = Runtime.getRuntime().totalMemory();
  }

  /** retrieves all supported platforms */
  private ArrayList<String> getSupportedPlatforms() {
    ArrayList<String> supportedPlatform = new ArrayList<>();
    if (glfwPlatformSupported(GLFW_PLATFORM_WIN32))
      supportedPlatform.add("WIN32");
    if (glfwPlatformSupported(GLFW_PLATFORM_COCOA))
      supportedPlatform.add("COCOA");
    if (glfwPlatformSupported(GLFW_PLATFORM_WAYLAND))
      supportedPlatform.add("WAYLAND");
    if (glfwPlatformSupported(GLFW_PLATFORM_X11))
      supportedPlatform.add("X11");
    return supportedPlatform;
  }

  /** get current platform */
  private String getPlatform() {
    return switch (glfwGetPlatform()) {
      case GLFW_PLATFORM_WIN32 -> "WIN32";
      case GLFW_PLATFORM_COCOA -> "COCOA";
      case GLFW_PLATFORM_WAYLAND -> "WAYLAND";
      case GLFW_PLATFORM_X11 -> "X11";
      case GLFW_PLATFORM_NULL -> "No Platform selected";
      default -> "No available Platform";
    };
  }

}