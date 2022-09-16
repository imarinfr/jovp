package es.optocom.jovp;

import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Texture;
import es.optocom.jovp.rendering.VulkanManager;
import es.optocom.jovp.structures.Command;
import es.optocom.jovp.structures.ModelType;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.List;

/**
 * Unitary tests for the Vulkan manager
 *
 * @since 0.0.1
 */
public class VulkanManagerTest {

  /**
   * Unitary tests for the Vulkan manager
   *
   * @since 0.0.1
   */
  public VulkanManagerTest() {
  }

  /**
   * Gets information about physical devices (GPUs)
   *
   * @since 0.0.1
   */
  @Test
  public void physicalDevicesInformation() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    VulkanManager vulkanManager = psychoEngine.getVulkanManager();
    List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
    for (VkPhysicalDevice physicalDevice : physicalDevices)
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
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    VulkanManager vulkanManager = psychoEngine.getVulkanManager();
    List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
    for (VkPhysicalDevice physicalDevice : physicalDevices)
      System.out.println(vulkanManager.getSwapChainSupport(physicalDevice));
    psychoEngine.cleanup();
  }

  /**
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
    public void input(Command command) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}
