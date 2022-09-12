package es.optocom.jovp.rendering;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK13.*;

/**
 * Class to handle the logical device. Needs to be recreated if physical device
 * changes
 *
 * @since 0.0.1
 */
public class LogicalDevice {

  VkDevice device;
  VkQueue graphicsQueue;
  VkQueue presentQueue;
  long descriptorSetLayout;
  final int msaaSamples;

  /**
   * Create logical device
   *
   * @since 0.0.1
   */
  LogicalDevice(long surface, VkPhysicalDevice physicalDevice) {
    createLogicalDevice(surface, physicalDevice);
    createDescriptorSetLayout();
    msaaSamples = getMaxUsableSampleCount(physicalDevice);
  }

  /**
   * Destroy logical device
   *
   * @since 0.0.1
   */
  void destroy() {
    vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
    vkDestroyDevice(device, null);
  }

  /** create Vulkan logical device */
  private void createLogicalDevice(long surface, VkPhysicalDevice physicalDevice) {
    try (MemoryStack stack = stackPush()) {
      VulkanSetup.QueueFamilyIndices indices = VulkanSetup.findQueueFamilies(surface, physicalDevice);
      int[] uniqueQueueFamilies = indices.unique();
      VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length,
          stack);
      for (int i = 0; i < uniqueQueueFamilies.length; i++) {
        VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
        queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
            .queueFamilyIndex(uniqueQueueFamilies[i])
            .pQueuePriorities(stack.floats(VulkanSetup.QueueFamilyIndices.PRIORITY));
      }
      VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack)
          .samplerAnisotropy(VulkanSetup.SAMPLER_ANISOTROPY)
          .sampleRateShading(VulkanSetup.SAMPLE_RATE_SHADING)
          .multiViewport(true);
      VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
          .pQueueCreateInfos(queueCreateInfos)
          .pEnabledFeatures(deviceFeatures)
          .ppEnabledExtensionNames(VulkanSetup.asPointerBuffer(VulkanSetup.DEVICE_EXTENSIONS));
      if (VulkanSetup.validationLayers)
        createInfo.ppEnabledLayerNames(VulkanSetup.asPointerBuffer(VulkanSetup.VALIDATION_LAYERS));
      PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);
      int result = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
      if (result != VK_SUCCESS)
        throw new AssertionError("Failed to create logical device: " + VulkanSetup.translateVulkanResult(result));
      device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);
      PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);
      vkGetDeviceQueue(device, indices.graphicsFamily, 0, pQueue);
      graphicsQueue = new VkQueue(pQueue.get(0), device);
      vkGetDeviceQueue(device, indices.presentFamily, 0, pQueue);
      presentQueue = new VkQueue(pQueue.get(0), device);
    }
  }

  /** create descriptor set layout */
  private void createDescriptorSetLayout() {
    try (MemoryStack stack = stackPush()) {
      VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);
      VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(0);
      uboLayoutBinding.binding(0)
          .descriptorCount(1)
          .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
          .pImmutableSamplers(null)
          .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
      VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(1);
      samplerLayoutBinding.binding(1)
          .descriptorCount(1)
          .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
          .pImmutableSamplers(null)
          .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
      VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
          .pBindings(bindings);
      LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
      int result = vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout);
      if (result != VK_SUCCESS)
        throw new AssertionError(
            "Failed to create descriptor set layout: " + VulkanSetup.translateVulkanResult(result));
      descriptorSetLayout = pDescriptorSetLayout.get(0);
    }
  }

  /** obtain the maximum usable samples for a physical device */
  static int getMaxUsableSampleCount(VkPhysicalDevice physicalDevice) {
    try (MemoryStack stack = stackPush()) {
      VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.malloc(stack);
      vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
      int sampleCountFlags = physicalDeviceProperties.limits().framebufferColorSampleCounts()
          & physicalDeviceProperties.limits().framebufferDepthSampleCounts();
      if ((sampleCountFlags & VK_SAMPLE_COUNT_64_BIT) != 0)
        return VK_SAMPLE_COUNT_64_BIT;
      if ((sampleCountFlags & VK_SAMPLE_COUNT_32_BIT) != 0)
        return VK_SAMPLE_COUNT_32_BIT;
      if ((sampleCountFlags & VK_SAMPLE_COUNT_16_BIT) != 0)
        return VK_SAMPLE_COUNT_16_BIT;
      if ((sampleCountFlags & VK_SAMPLE_COUNT_8_BIT) != 0)
        return VK_SAMPLE_COUNT_8_BIT;
      if ((sampleCountFlags & VK_SAMPLE_COUNT_4_BIT) != 0)
        return VK_SAMPLE_COUNT_4_BIT;
      if ((sampleCountFlags & VK_SAMPLE_COUNT_2_BIT) != 0)
        return VK_SAMPLE_COUNT_2_BIT;
      return VK_SAMPLE_COUNT_1_BIT;
    }
  }

}
