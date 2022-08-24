package es.optocom.jovp.unit;

import es.optocom.jovp.engine.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.rendering.*;
import es.optocom.jovp.engine.structures.*;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.List;

/**
 *
 * VulkanManagerTest
 *
 * <ul>
 * <li>VulkanManagerTest test</li>
 * Unitary tests for the Vulkan manager
 * </ul>
 *
 * @since 0.0.1
 */
public class VulkanManagerTest {

    /**
     *
     * Gets information about physical devices (GPUs)
     *
     * @since 0.0.1
     */
    @Test
    public void physicalDevicesInformation() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500) ;
        VulkanManager vulkanManager = psychoEngine.getVulkanManager();
        List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
        for (VkPhysicalDevice physicalDevice: physicalDevices)
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
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500) ;
        VulkanManager vulkanManager = psychoEngine.getVulkanManager();
        List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
        for (VkPhysicalDevice physicalDevice: physicalDevices)
            System.out.println(vulkanManager.getSwapChainSupport(physicalDevice));
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
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 1000);
        psychoEngine.getWindow().getMonitor().setPhysicalSize(621, 341);
        psychoEngine.start();
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        @Override
        public void init(PsychoEngine psychoEngine) {
            Item item = new Item(new Model(ModelType.TRIANGLE), new Texture());
            item.position(0, 0);
            item.size(1, 1);
            items.add(item);
        }

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update(PsychoEngine psychoEngine) {}

    }

}
