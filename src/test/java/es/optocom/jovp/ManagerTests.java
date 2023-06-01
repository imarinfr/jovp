package es.optocom.jovp;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Texture;
import es.optocom.jovp.rendering.VulkanManager;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * Unitary tests for the Vulkan manager
 *
 * @since 0.0.1
 */
public class ManagerTests {

    /**
     * 
     * Unitary tests for the Vulkan manager
     *
     * @since 0.0.1
     */
    public ManagerTests() {
    }

    /**
     *
     * List device extension support
     *
     * @since 0.0.1
     */
    @Test
    public void listDeviceExtensionSupport() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        for (VkPhysicalDevice physicalDevice : psychoEngine.getPhysicalDevices())
            System.out.println(psychoEngine.getVulkanManager().getPhysicalDeviceDeviceExtensionSupport(physicalDevice));
        psychoEngine.cleanup();
    }

    /**
     * 
     * Test for collecting all attached monitors
     *
     * @since 0.0.1
     */
    @Test
    public void retrieveMonitors() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        MonitorManager monitorManager = psychoEngine.getMonitorManager();
        System.out.println(monitorManager);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Test for retrieving the monitor's information
     *
     * @since 0.0.1
     */
    @Test
    public void setSettingsManually() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        MonitorManager monitorManager = psychoEngine.getMonitorManager();
        Monitor monitor = monitorManager.getMonitor(0);
        monitor.setPhysicalSize(621, 341);
        monitor.setSize(1024, 640);
        monitor.setRefreshRate(10);
        System.out.println(monitor);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Test monitor size change
     *
     * @since 0.0.1
     */
    @Test
    public void changeMonitorSize() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        Monitor monitor = psychoEngine.getMonitor();
        System.out.println("Monitor dimensons in mm: [" + monitor.getWidthMM() + ", " + monitor.getHeightMM() + "]");
        psychoEngine.setPhysicalSize(621, 341);
        System.out.println("Monitor dimensons in mm: [" + monitor.getWidthMM() + ", " + monitor.getHeightMM() + "]");
        psychoEngine.cleanup();
    }

    /**
     *
     * Tests for monitor's video modes
     *
     * @since 0.0.1
     */
    @Test
    public void videoModes() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        MonitorManager monitorManager = psychoEngine.getMonitorManager();
        Monitor monitor = monitorManager.getMonitor(0);
        GLFWVidMode videoMode = monitor.getCurrentVideoMode();
        System.out.println("Fullscreen video mode: " + monitor.getWidth() + "x" + monitor.getHeight() + " - " +
                monitor.getRefreshRate() + "Hz\n");
        System.out.println("Current video mode: " + videoMode.width() + "x" + videoMode.height() +
                " - " + videoMode.refreshRate() + "Hz\n");
        System.out.println("List of video modes:");
        for (int i = 0; i < monitor.getVideoModes().capacity(); i++) {
            System.out.println("\t" + monitor.getVideoModes().position(i).width() + "x" +
                    monitor.getVideoModes().position(i).height() + " - " +
                    monitor.getVideoModes().position(i).refreshRate() + "Hz");
        }
        psychoEngine.cleanup();
    }

    /**
     * 
     * Gets information about physical devices (GPUs)
     *
     * @since 0.0.1
     */
    @Test
    public void physicalDevicesInformation() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        VulkanManager vulkanManager = psychoEngine.getVulkanManager();
        List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
        for (VkPhysicalDevice physicalDevice : physicalDevices)
            System.out.println(vulkanManager.getPhysicalDeviceProperties(physicalDevice));
        psychoEngine.cleanup();
    }

    /**
     * 
     * Gets information about physical devices (GPUs)
     *
     * @since 0.0.1
     */
    @Test
    public void swapChainInformation() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        VulkanManager vulkanManager = psychoEngine.getVulkanManager();
        List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
        for (VkPhysicalDevice physicalDevice : physicalDevices)
            System.out.println(vulkanManager.getSwapChainSupport(physicalDevice));
        psychoEngine.cleanup();
    }

    /**
     *
     * Get all serial controller names
     *
     * @since 0.0.1
     */
    @Test
    public void getSerialControllerNames() {
        String[] available = Controller.getSuitableControllers();
        System.out.println(Arrays.toString(available));
    }

    /**
     * 
     * Tests for creating, showing, hiding, resizing, and hiding windows
     *
     * @since 0.0.1
     */
    @Test
    public void showAndHideWindow() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        try {
            for (int i = 0; i < 5; i++) {
                psychoEngine.show();
                Thread.sleep(250);
                psychoEngine.hide();
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        psychoEngine.cleanup();
    }

    /**
     *
     * Tests for creating, showing, hiding, resizing, and hiding windows
     *
     * @since 0.0.1
     */
    @Test
    public void showWindowedAndFullScreen() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        psychoEngine.show();
        psychoEngine.setFullScreen();
        psychoEngine.setWindowed();
        psychoEngine.cleanup();
    }

    /**
     *
     * Tests for updating distance and the fixation point, and for retrieving the
     * field of view of the window
     *
     * @since 0.0.1
     */
    @Test
    public void changeWindowPositionAndSize() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic());
        psychoEngine.show();
        psychoEngine.setSize(1000, 800);
        psychoEngine.setPosition(10, 10);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Render a triangle
     *
     * @since 0.0.1
     */
    @Test
    public void showTriangle() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicTriangle());
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    // Psychophysics logic that does nothing
    static class Logic implements PsychoLogic {

        @Override
        public void init(PsychoEngine psychoEngine) {
        }

        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
        }

    }

    // Psychophysics logic to show a simple triangle
    static class LogicTriangle implements PsychoLogic {

        @Override
        public void init(PsychoEngine psychoEngine) {
            Item item = new Item(new Model(ModelType.TRIANGLE), new Texture(new double[] { 1, 1, 1, 1 }));            
            view.add(item);
            item.distance(100);
            item.position(0, 0);
            item.size(5, 5);
            item.rotation(0);
        }

        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
        }

    }

}
