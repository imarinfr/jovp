package es.optocom.jovp;

import es.optocom.jovp.structures.Command;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

/**
 * Unitary tests for the physical and logical device support
 *
 * @since 0.0.1
 */
public class DeviceSupportTest {

  /**
   * Unitary tests for the physical and logical device support
   *
   * @since 0.0.1
   */
  public DeviceSupportTest() {
  }

  /**
   * List device extension support
   *
   * @since 0.0.1
   */
  @Test
  public void listDeviceExtensionSupport() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    for (VkPhysicalDevice physicalDevice : psychoEngine.getPhysicalDevices())
      System.out.println(psychoEngine.getVulkanManager().getPhysicalDeviceDeviceExtensionSupport(physicalDevice));
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    @Override
    public void init(PsychoEngine psychoEngine) {
    }

    @Override
    public void input(Command command, double time) {
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}
