package es.optocom.jovp.unit;

import es.optocom.jovp.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.Timer;
import es.optocom.jovp.engine.rendering.*;
import es.optocom.jovp.engine.structures.*;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.List;

/**
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
     * Gets information about physical devices (GPUs)
     *
     * @since 0.0.1
     */
    @Test
    public void physicalDevicesInformation() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500) ;
        VulkanManager vulkanManager = psychoEngine.getVulkanManager();
        List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
        for (VkPhysicalDevice physicalDevice: physicalDevices)
            System.out.println(vulkanManager.getPhysicalDeviceProperties(physicalDevice));
        psychoEngine.cleanup();
    }

    /**
     * Gets information about physical devices (GPUs)
     *
     * @since 0.0.1
     */
    @Test
    public void swapChainInformation() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500) ;
        VulkanManager vulkanManager = psychoEngine.getVulkanManager();
        List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
        for (VkPhysicalDevice physicalDevice: physicalDevices)
            System.out.println(vulkanManager.getSwapChainSupport(physicalDevice));
        psychoEngine.cleanup();
    }

    /**
     * Render a triangle
     * TODO
     * @since 0.0.1
     */
    @Test
    public void vulkanTriangle() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 1000);
        psychoEngine.getWindow().getMonitor().setPhysicalSize(621, 341);
        psychoEngine.start();
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        Item item;
        Item item2 = null;
        Text text;
        double theta;

        int fps = 0;
        Timer timer = new Timer();

        @Override
        public void init() {
            double[] color = new double[]{0, 1, 0, 1};
            double[] color1 = new double[]{0, 1, 0, 1};
            double[] color2 = new double[]{1, 0, 0, 1};
            Item item = new Item(new Model(ModelType.MALTESE), new Texture(color));
            item.position(0, 0);
            item.size(1, 1);
            item.rotation(0);
            items.add(item);
            item = new Item(new Model(ModelType.SQUARE), new Texture(color1, color2));
            item.position(0, -1);
            item.size(1, 1);
            items.add(item);
            item = new Item(new Model(ModelType.CIRCLE), new Texture("ecceHomo.jpeg"));
            item.position(0, 2);
            item.size(1, 1);
            items.add(item);
            item = new Item(new Model(ModelType.TRIANGLE), new Texture("ivanito.jpeg"));
            item.position(0, -3);
            item.size(1, 1);
            items.add(item);
            item = new Item(new Model(ModelType.ANNULUS, 0.7f), new Texture("ivanito.jpeg"));
            item.position(0, -4);
            item.size(1, 1);
            items.add(item);
            item = new Item(new Model(Optotype.S), new Texture(color1, color2));
            item.position(1, -4);
            item.size(1, 1);
            items.add(item);
            item = new Item(new Model(7, 0.3f), new Texture("ecceIvanito.jpeg"));
            item.position(2, -4);
            item.size(1, 1);
            items.add(item);
            item = new Item(new Model(7), new Texture("ecceIvanito.jpeg"));
            item.position(-2, -4);
            item.size(1, 1);
            items.add(item);
            item = new Item(new Model("heart.obj"), new Texture("ecceIvanito.jpeg"));
            item.position(-2, -4);
            item.size(1, 1);
            items.add(item);
            // Add title
            Text title = new Text(FontType.MONSERRAT_BOLD);
            title.setText("Vulkan Manager performance test");
            title.setSize(0.35);
            title.position(-8, 4);
            items.add(title);
            // Add text to show FPS
            text = new Text();
            text.setText("Refresh rate:"); // TODO
            text.setSize(0.35);
            text.position(-8, 3.6);
            items.add(text);
            timer.start();
        }

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update() {
            theta += 1;
            item = items.get(0);
            item = items.get(1);
            item.rotation(1.5 * theta);
            item = items.get(2);
            item.rotation(2 * theta);
            item = items.get(3);
            item.rotation(-0.5 * theta);
            item = items.get(4);
            item.rotation(1.5 * theta, new Vector3f(0, 1, 0));
            item = items.get(5);
            item = items.get(6);
            item.rotation(theta);
            if (timer.getElapsedTime() <= 1000)  // restart the timer every second
                fps++;
            else {
                if (item2 == null) {
                    item2 = new Item(new Model(Optotype.S), new Texture());
                    item2.position(2, 0);
                    item2.size(1, 1);
                    items.add(item2);
                    items.get(1).show(false);
                } else {
                    items.get(1).show(true);
                    items.remove(item2);
                    item2 = null;
                }
                timer.start();
                text.setText("Refresh rate: " + fps + " fps");
                fps = 0;
            }
        }

    }

}
