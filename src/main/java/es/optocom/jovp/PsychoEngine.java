package es.optocom.jovp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_NULL;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPlatformSupported;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import org.lwjgl.vulkan.VkPhysicalDevice;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.InputType;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.Projection;
import es.optocom.jovp.definitions.ViewMode;
import es.optocom.jovp.rendering.Observer;
import es.optocom.jovp.rendering.VulkanManager;
import jssc.SerialPortException;

/**
 * 
 * The JOVP engine that manages the Observer, Screens, Window, Input, and
 * Renderer
 *
 * @since 0.0.1
 */
public class PsychoEngine {

    public static final float DISTANCE = 572.943f; // 1 degree = 1 cm in screen
    public static final boolean VALIDATION_LAYERS = DEBUG.get(true);
    public static final boolean API_DUMP = false;

    private final PsychoLogic psychoLogic;
    private final Window window;
    private final Observer observer;
    private final VulkanManager vulkanManager;
    private final List<VkPhysicalDevice> physicalDevices;

    private int numberOfCores;
    private long freeMemory;
    private long maxMemory;
    private long totalMemory;

    private boolean loop;

    /**
     * 
     * Main method for the JOVP
     *
     * @param psychoLogic Logic for the psychophysics experience
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic) {
        this(psychoLogic, DISTANCE, VALIDATION_LAYERS, API_DUMP);
    }

    /**
     * 
     * Main method for the JOVP MONO with validation layers
     *
     * @param psychoLogic Logic for the psychophysics experience
     * @param distance Viewing distance in mm
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic, float distance) {
        this(psychoLogic, distance, Projection.ORTHOGRAPHIC, ViewMode.MONO, VALIDATION_LAYERS, API_DUMP);
    }

    /**
     * 
     * Main method for the JOVP MONO with validation layers
     *
     * @param psychoLogic Logic for the psychophysics experience
     * @param projection Type of projection: ORTHOGRAPHIC or PERSPECTIVE
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic, Projection projection) {
        this(psychoLogic, DISTANCE, projection, ViewMode.MONO, VALIDATION_LAYERS, API_DUMP);
    }

    /**
     * 
     * Main method for the JOVP with validation layers
     *
     * @param psychoLogic Logic for the psychophysics experience
     * @param distance Viewing distance in mm
     * @param viewMode Whether it is monocular of stereoscopic view
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic, float distance, ViewMode viewMode) {
        this(psychoLogic, distance, Projection.ORTHOGRAPHIC, viewMode, VALIDATION_LAYERS, API_DUMP);
    }

    /**
     * 
     * Main method for the JOVP with validation layers
     *
     * @param psychoLogic Logic for the psychophysics experience
     * @param distance Viewing distance in mm
     * @param projection Type of projection: ORTHOGRAPHIC or PERSPECTIVE
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic, float distance, Projection projection) {
        this(psychoLogic, distance, projection, ViewMode.MONO, VALIDATION_LAYERS, API_DUMP);
    }

    /**
     * 
     * Main method for the JOVP with validation layers
     *
     * @param psychoLogic Logic for the psychophysics experience
     * @param distance Viewing distance in mm
     * @param projection Type of projection: ORTHOGRAPHIC or PERSPECTIVE
     * @param viewMode Whether it is monocular of stereoscopic view
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic, float distance, Projection projection, ViewMode viewMode) {
        this(psychoLogic, distance, projection, viewMode, VALIDATION_LAYERS, API_DUMP);
    }

    /**
     * 
     * Main method for the JOVP MONO
     *
     * @param psychoLogic Logic for the psychophysics experience
     * @param distance Viewing distance in mm
     * @param validationLayers Whether to use validation layers
     * @param apiDump Whether to use the VK_LAYER_LUNARG_api_dump layer
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic, float distance, boolean validationLayers, boolean apiDump) {
        this(psychoLogic, distance, Projection.ORTHOGRAPHIC, ViewMode.MONO, validationLayers, apiDump);
    }

    /**
     * 
     * Main method for the JOVP
     *
     * @param psychoLogic Logic for the psychophysics experience
     * @param distance Viewing distance in mm
     * @param projection Type of projection: ORTHOGRAPHIC or PERSPECTIVE
     * @param viewMode Whether it is monocular of stereoscopic view
     * @param validationLayers Whether to use validation layers
     * @param apiDump Whether to use the VK_LAYER_LUNARG_api_dump layer
     *
     * @since 0.0.1
     */
    public PsychoEngine(PsychoLogic psychoLogic, float distance, Projection projection, ViewMode viewMode, boolean validationLayers, boolean apiDump) {
        glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
        if (!glfwInit()) throw new RuntimeException("Cannot initialize GLFW");
        this.psychoLogic = psychoLogic;
        window = new Window();
        observer = new Observer(window, distance, projection, viewMode);
        vulkanManager = new VulkanManager(observer, validationLayers, apiDump);
        physicalDevices = vulkanManager.getPhysicalDevices();
        getRunTimeInfo();
    }

    /**
     * 
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
     * 
     * Start the psychoEngine in the default physical device
     * 
     * @param input Either 'mouse', 'keypad', or the name of a suitable USB controller
     * @param paradigm Psychophysics paradigm for mapping input to commands
     *
     * @since 0.0.1
     */
    public void start(String input, Paradigm paradigm) {
        start(physicalDevices.get(0), input, InputType.PRESS, paradigm);
    }

    /**
     * 
     * Start the psychoEngine in the default physical device
     * 
     * @param input Either 'mouse', 'keypad', or the name of a suitable USB controller
     * @param inputType Whether command is when pressed, released, or repeat.
     * @param paradigm Psychophysics paradigm for mapping input to commands
     *
     * @since 0.0.1
     */
    public void start(String input, InputType inputType, Paradigm paradigm) {
        start(physicalDevices.get(0), input, inputType, paradigm);
    }

    /**
     * 
     * Run the psychoEngine in a selected physical device
     *
     * @param physicalDevice The physical device for the psychoEngine run
     * @param input Either 'mouse', 'keypad', or the name of a suitable USB controller
     * @param inputType Whether command is when pressed, released, or repeat.
     * @param paradigm Psychophysics paradigm for mapping input to commands
     *
     * @since 0.0.1
     */
    public void start(VkPhysicalDevice physicalDevice, String input, InputType inputType, Paradigm paradigm) {
        try {
            window.setController(input, inputType, paradigm);
            init(physicalDevice);
        } catch (SerialPortException e) {
            throw new RuntimeException("Cannot start psychoEngine.", e);
        }
    }

    /**
     * 
     * Initialize psychoEngine
     *
     * @since 0.0.1
     */
    private void init(VkPhysicalDevice physicalDevice) {
        psychoLogic.init(this);
        vulkanManager.start(physicalDevice, PsychoLogic.view.items, PsychoLogic.view.texts);
        loop = true;
        window.show();
        psychoLoop();
        vkDeviceWaitIdle(vulkanManager.getDevice());
    }

    /**
     * 
     * Instruct the engine to finish the loop
     *
     * @since 0.0.1
     */
    public void finish() {
        loop = false;
    }

    /**
     * 
     * Clean up after use
     * 
     * @throws RuntimeException Cannot clean up the psychoEngine
     * 
     * @since 0.0.1
     */
    public void cleanup() {
        try {
            PsychoLogic.view.destroy();
            vulkanManager.cleanup();
            window.cleanup();
            glfwTerminate();
            Objects.requireNonNull(glfwSetErrorCallback(null)).free();
        } catch (SerialPortException e) {
            throw new RuntimeException("Cannot cleanup the psychoEngine", e);
        }
    }

    /**
     * 
     * Show the psychoEngine window
     *
     * @since 0.0.1
     */
    public void show() {
        window.show();
    }

    /**
     * 
     * Hide the psychoEngine window
     *
     * @since 0.0.1
     */
    public void hide() {
        window.hide();
    }

    /**
     * 
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
     * 
     * Get monitor
     *
     * @return The monitor
     *
     * @since 0.0.1
     */
    public Monitor getMonitor() {
        return window.getMonitor();
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
        return window.getMonitorManager();
    }

    /**
     * 
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
     * 
     * Set the viewing mode
     *
     * @param viewMode The viewing mode
     *
     * @since 0.0.1
     */
    public void setViewMode(ViewMode viewMode) {
        vulkanManager.setViewMode(viewMode);
    }

    /**
     * 
     * Get the projection type
     *
     * @return The projection type
     *
     * @since 0.0.1
     */
    public Projection getProjection() {
        return observer.getProjection();
    }


    /**
     * 
     * Set projection
     *
     * @param projection The projection type
     *
     * @since 0.0.1
     */
    public void setProjection(Projection projection) {
        observer.setProjection(projection);
    }

    /**
     * 
     * Get the viewing mode
     *
     * @return The view mode
     *
     * @since 0.0.1
     */
    public ViewMode getViewMode() {
        return observer.getViewMode();
    }

    /**
     * 
     * Get observer's distance
     *
     * @return the distance in mm
     *
     * @since 0.0.1
     */
    public float getDistance() {
        return observer.getDistance();
    }

    /**
     * 
     * Get observer's distance in meters
     *
     * @return the distance in meters
     *
     * @since 0.0.1
     */
    public float getDistanceM() {
        return observer.getDistanceM();
    }

    /**
     * 
     * Get the intra-pupil distance distance
     *
     * @return The pupillary distance in mm
     *
     * @since 0.0.1
     */
    public float getPupilDistance() {
        return observer.getPupilDistance();
    }

    /**
     * 
     * Set pupilary distance
     * 
     * @param pd pupilary distance in mm
     *
     * @since 0.0.1
     */
    void setPupilDistance(double pd) {
        observer.setPupilDistance(pd);
    }

    /**
     * 
     * Set look at
     * 
     * @param eye The position of the eye in the virtual world
     * @param center The center
     * @param up The up vector
     *
     * @since 0.0.1
     */
    public void lookAt(Vector3f eye, Vector3f center, Vector3f up) {
        observer.lookAt(eye, center, up);
    }

    /**
     * 
     * Translate observer
     * 
     * @param offset The translation in x and y axes in degrees and z in meters
     *
     * @since 0.0.1
     */
    public void translateView(Vector3f offset) {
        observer.translateView(offset);
    }

    /**
     * 
     * Rotate observer
     * 
     * @param rotation The rotation in x, y, and z axes
     *
     * @since 0.0.1
     */
    public void rotateView(Vector3f rotation) {
        observer.rotateView(rotation);
    }

    /**
     * 
     * Get the field of view
     *
     * @return The field of view (x, y) in degrees
     *
     * @since 0.0.1
     */
    public float[] getFieldOfView() {
        return observer.getFieldOfView();
    }

    /**
     *
     * Set Brown-Conrady model distortion coefficients
     * 
     * @param k1 coefficient k1
     *
     * @since 0.0.1
     */
    void setDistortion(double k1) {
        observer.setCoefficients(k1, 0.0f, 0.0f, 0.0f);
    }

    /**
     *
     * Set Brown-Conrady model distortion coefficients
     * 
     * @param k1 coefficient k1
     * @param k2 coefficient k2
     *
     * @since 0.0.1
     */
    void setDistortion(double k1, double k2) {
        observer.setCoefficients(k1, k2, 0.0f, 0.0f);
    }

    /**
     *
     * Set default Brown-Conrady model distortion coefficients
     *
     * @since 0.0.1
     */
    void setNoDistortion() {
        observer.setCoefficients(0, 0, 0, 0);
    }

    /**
     * 
     * Set the window monitor
     * 
     * @param monitor Set the window monitor
     *
     * @since 0.0.1
     */
    public void setMonitor(int monitor) {
        window.setMonitor(monitor);
    }

    /**
     * 
     * Allow to input the physical size manually
     *
     * @param widthMM  The width of the monitor in mm
     * @param heightMM The height of the monitor in mm
     *
     * @since 0.0.1
     */
    public void setPhysicalSize(int widthMM, int heightMM) {
        window.getMonitor().setPhysicalSize(widthMM, heightMM);
    }

    /**
     * 
     * Set window position
     *
     * @param x Monitor x position in pixels
     * @param y Monitor y position in pixels
     *
     * @since 0.0.1
     */
    public void setPosition(int x, int y) {
        window.setPosition(x, y);
        window.update();
    }

    /**
     * 
     * Get window position
     *
     * @return The window position (x, y) in pixels relative to the monitor
     *
     * @since 0.0.1
     */
    public int[] getPosition() {
        return window.getPosition();
    }

    /**
     * 
     * Set window size
     *
     * @param width  Monitor width in pixels
     * @param height Monitor height in pixels
     *
     * @since 0.0.1
     */
    public void setSize(int width, int height) {
        window.setSize(width, height);
        window.update();
        observer.computeProjections();
    }

    /**
     * 
     * Set full-screen mode to current monitor
     *
     * @since 0.0.1
     */
    public void setFullScreen() {
        window.setFullScreen();
    }

    /**
     * 
     * Set windowed mode to current monitor
     *
     * @since 0.0.1
     */
    public void setWindowed() {
        window.setWindowed();
    }

    /** convert to string */
    @Override
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
            update();
            drawFrame();
            input();
        }
    }

    /** Process input */
    private void input() {
        Command command = window.getCommand();
        psychoLogic.input(this, command);
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