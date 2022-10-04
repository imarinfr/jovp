package es.optocom.jovp.rendering;

import es.optocom.jovp.Items;
import es.optocom.jovp.Window;
import es.optocom.jovp.definitions.ViewMode;

import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

/**
 * Manages all things Vulkan: instantiation, physical and logical device,
 * queues, swap chain, views.
 *
 * @since 0.0.1
 */
public class VulkanManager {

  Items items;
  private List<VulkanSetup.Frame> inFlightFrames;
  private Map<Integer, VulkanSetup.Frame> imagesInFlight;
  private int currentFrame;

  /**
   * Initiates the Vulkan manager
   *
   * @param window           The window for the Vulkan manager
   * @param distance         The observer's distance
   * @param validationLayers Whether to use validation layers
   * @param apiDump          Whether to echo api dump
   *
   * @since 0.0.1
   */
  public VulkanManager(Window window, int distance, boolean validationLayers, boolean apiDump) {
    VulkanSetup.window = window;
    VulkanSetup.validationLayers = validationLayers;
    VulkanSetup.apiDump = apiDump;
    VulkanSetup.distance = distance;
    VulkanSetup.addValidationLayers();
    createInstance();
    createSurface();
    if (validationLayers) VulkanSetup.setupDebugMessenger();
    enumerateSuitablePhysicalDevices();
    computeFieldOfView();
  }

  /**
   * Start for a physical device
   *
   * @param physicalDevice The physical device handle
   * @param viewMode       The view mode
   * @param items          The initial items to start the psychophysics experience
   *
   * @since 0.0.1
   */
  public void start(VkPhysicalDevice physicalDevice, ViewMode viewMode, Items items) {
    VulkanSetup.physicalDevice = physicalDevice;
    this.items = items;
    VulkanSetup.logicalDevice = new LogicalDevice(VulkanSetup.surface, physicalDevice);
    VulkanSetup.viewMode = viewMode;
    VulkanSetup.swapChain = new SwapChain();
    for (Item item : items) item.buffers.create();
    VulkanSetup.vulkanCommands = new VulkanCommands(items);
    createSyncObjects();
    setPerspective();
  }

  /**
   * Set view mode
   *
   * @param viewMode The view mode
   *
   * @since 0.0.1
   */
  public void setViewMode(ViewMode viewMode) {
    VulkanSetup.viewMode = viewMode;
  };

  /**
   * Get view mode
   *
   * @return The view mode
   *
   * @since 0.0.1
   */
  public ViewMode getViewMode() {
    return VulkanSetup.viewMode;
  };

  /**
   * Draw a frame
   *
   * @since 0.0.1
   */
  public void drawFrame() {
    try (MemoryStack stack = stackPush()) {
      IntBuffer pImageIndex = stack.mallocInt(1);
      VulkanSetup.Frame thisFrame = inFlightFrames.get(currentFrame);
      vkWaitForFences(VulkanSetup.logicalDevice.device, thisFrame.pFence(), true, VulkanSetup.UINT64_MAX);
      int result = vkAcquireNextImageKHR(VulkanSetup.logicalDevice.device, VulkanSetup.swapChain.swapChain,
          VulkanSetup.UINT64_MAX,
          thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);
      if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
        recreateSwapChain();
        return;
      } else if (result != VK_SUCCESS)
        throw new AssertionError("Cannot get image: " + VulkanSetup.translateVulkanResult(result));
      final int imageIndex = pImageIndex.get(0);
      if (imagesInFlight.containsKey(imageIndex))
        vkWaitForFences(VulkanSetup.logicalDevice.device, imagesInFlight.get(imageIndex).fence(), true,
            VulkanSetup.UINT64_MAX);
      vkDeviceWaitIdle(VulkanSetup.logicalDevice.device);
      VulkanSetup.vulkanCommands.renderPass(imageIndex);
      imagesInFlight.put(imageIndex, thisFrame);
      VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
          .waitSemaphoreCount(1)
          .pWaitSemaphores(thisFrame.pImageAvailableSemaphore())
          .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
          .pSignalSemaphores(thisFrame.pRenderFinishedSemaphore())
          .pCommandBuffers(stack.pointers(VulkanSetup.vulkanCommands.commandBuffers.get(imageIndex)));
      vkResetFences(VulkanSetup.logicalDevice.device, thisFrame.pFence());
      result = vkQueueSubmit(VulkanSetup.logicalDevice.graphicsQueue, submitInfo, thisFrame.fence());
      if (result != VK_SUCCESS) {
        vkResetFences(VulkanSetup.logicalDevice.device, thisFrame.pFence());
        throw new AssertionError("Failed to submit draw command buffer: " +
            VulkanSetup.translateVulkanResult(result));
      }
      VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
          .pWaitSemaphores(thisFrame.pRenderFinishedSemaphore())
          .swapchainCount(1)
          .pSwapchains(stack.longs(VulkanSetup.swapChain.swapChain))
          .pImageIndices(pImageIndex);
      result = vkQueuePresentKHR(VulkanSetup.logicalDevice.presentQueue, presentInfo);
      if (VulkanSetup.window.resized()) {
        recreateSwapChain();
        VulkanSetup.window.resized(false);
      } else if (result != VK_SUCCESS)
        throw new AssertionError("Failed to present swap chain image: " +
            VulkanSetup.translateVulkanResult(result));
      currentFrame = (currentFrame + 1) % VulkanSetup.MAX_FRAMES_IN_FLIGHT;
    }
  }

  /**
   * Clean up after use
   *
   * @since 0.0.1
   */
  public void cleanup() {
    if (VulkanSetup.logicalDevice != null) {
      destroySyncObjects();
      VulkanSetup.vulkanCommands.destroy();
      VulkanSetup.swapChain.destroy();
      items.destroy();
      VulkanSetup.logicalDevice.destroy();
      VulkanSetup.swapChain = null;
      VulkanSetup.logicalDevice = null;
    }
    destroyInstance();
    VulkanSetup.cleanup();
  }

  /**
   * Set viewing distance
   *
   * @param distance The distance of the observer from the display
   *
   * @since 0.0.1
   */
  public void setDistance(double distance) {
    VulkanSetup.distance = distance;
    computeFieldOfView();
  }

  /**
   * Get viewing distance
   *
   * @return The distance of the observer from the display
   *
   * @since 0.0.1
   */
  public double getDistance() {
    return VulkanSetup.distance;
  }

  /**
   * Get field of view
   *
   * @return The field of view in x and y directions.
   *
   * @since 0.0.1
   */
  public double[] getFieldOfView() {
    return new double[] { Math.toDegrees(VulkanSetup.fovx), Math.toDegrees(VulkanSetup.fovy) };
  }

  /**
   * Updates the field of view depending on distance, window size, etc
   *
   * @since 0.0.1
   */
  public void computeFieldOfView() {
    double width = VulkanSetup.window.getPixelWidth() * VulkanSetup.window.getWidth();
    double height = VulkanSetup.window.getPixelHeight() * VulkanSetup.window.getHeight();
    if (VulkanSetup.viewMode == ViewMode.STEREO) width = width / 2; // only half of the screen is used per eye
    VulkanSetup.fovx = 2 * Math.atan((width / 2.0) / VulkanSetup.distance);
    VulkanSetup.fovy = 2 * Math.atan((height / 2.0) / VulkanSetup.distance);
  }

  /**
   * Set view
   *
   * @param eye    View eye
   * @param center View center
   * @param up     View up
   *
   * @since 0.0.1
   */
  public void setView(Vector3f eye, Vector3f center, Vector3f up) {
    VulkanSetup.view.lookAt(eye, center, up);
  }

  /**
   * Get list of physical devices
   *
   * @return The list of physical devices
   *
   * @since 0.0.1
   */
  public List<VkPhysicalDevice> getPhysicalDevices() {
    return VulkanSetup.physicalDevices;
  }

  /**
   * Get the current Vulkan logical device
   *
   * @return The Vulkan logical device
   *
   * @since 0.0.1
   */
  public VkDevice getDevice() {
    return VulkanSetup.logicalDevice.device;
  }

  /**
   * Get physical device properties
   *
   * @param physicalDevice The physical device
   *
   * @return A string with the physical device properties
   *
   * @since 0.0.1
   */
  public String getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice) {
    return VulkanSetup.getPhysicalDeviceProperties(physicalDevice);
  }

  /**
   * Get physical device properties
   *
   * @param physicalDevice The physical device
   *
   * @return A set with all the physical device extensions supported
   *
   * @since 0.0.1
   */
  public String getPhysicalDeviceDeviceExtensionSupport(VkPhysicalDevice physicalDevice) {
    VkPhysicalDeviceProperties deviceProperties = VulkanSetup.getDeviceProperties(physicalDevice);
    StringBuilder extensionSupportList = new StringBuilder("Physical device: ");
    extensionSupportList.append(deviceProperties.deviceNameString()).append("\n");
    Set<String> extensionSet = VulkanSetup.listDeviceExtensionSupport(physicalDevice);
    for (String extension : extensionSet)
      extensionSupportList.append("\t").append(extension).append("\n");
    return extensionSupportList.toString();
  }

  /**
   * Get swap chain support
   *
   * @return A string with the swap chain support
   *
   * @param physicalDevice The physical device
   *
   * @since 0.0.1
   */
  public String getSwapChainSupport(VkPhysicalDevice physicalDevice) {
    return VulkanSetup.getSwapChainSupport(physicalDevice);
  }

  /** destroy synchronization objects */
  private void destroySyncObjects() {
    inFlightFrames.forEach(frame -> {
      vkDestroySemaphore(VulkanSetup.logicalDevice.device, frame.renderFinishedSemaphore(), null);
      vkDestroySemaphore(VulkanSetup.logicalDevice.device, frame.imageAvailableSemaphore(), null);
      vkDestroyFence(VulkanSetup.logicalDevice.device, frame.fence(), null);
    });
    inFlightFrames.clear();
  }

  /** create Vulkan instance */
  private void createInstance() {
    if (VulkanSetup.validationLayers && !VulkanSetup.checkValidationLayerSupport())
      throw new RuntimeException("Validation requested but not supported");
    try (MemoryStack stack = stackPush()) {
      VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
          .pApplicationName(stack.UTF8Safe(VulkanSetup.TITLE))
          .applicationVersion(VK_MAKE_VERSION(VulkanSetup.MAJOR, VulkanSetup.MINOR, VulkanSetup.PATCH))
          .pEngineName(stack.UTF8Safe(VulkanSetup.ENGINE))
          .engineVersion(VK_MAKE_VERSION(VulkanSetup.MAJOR, VulkanSetup.MINOR, VulkanSetup.PATCH))
          .apiVersion(VulkanSetup.VK_API_VERSION);
      VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
          .pApplicationInfo(appInfo)
          .ppEnabledExtensionNames(VulkanSetup.getRequiredExtensions());
      if (VulkanSetup.validationLayers) {
        createInfo.ppEnabledLayerNames(VulkanSetup.asPointerBuffer(VulkanSetup.VALIDATION_LAYERS));
        VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
        VulkanSetup.populateDebugMessengerCreateInfo(debugCreateInfo);
        createInfo.pNext(debugCreateInfo.address());
      }
      PointerBuffer instancePtr = stack.mallocPointer(1);
      int result = vkCreateInstance(createInfo, null, instancePtr);
      if (result != VK_SUCCESS)
        throw new AssertionError("Failed to create instance: " + VulkanSetup.translateVulkanResult(result));
      VulkanSetup.instance = new VkInstance(instancePtr.get(0), createInfo);
    }
  }

  /** create Vulkan surface */
  private void createSurface() {
    try (MemoryStack stack = stackPush()) {
      LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
      int result = GLFWVulkan.glfwCreateWindowSurface(VulkanSetup.instance, VulkanSetup.window.getHandle(),
                                            null, pSurface);
      if (result != VK_SUCCESS)
        throw new AssertionError("Failed to create window surface: " +
            VulkanSetup.translateVulkanResult(result));
      VulkanSetup.surface = pSurface.get(0);
    }
  }

  /** destroy the Vulkan instance, surface, and debug messenger */
  private void destroyInstance() {
    if (VulkanSetup.validationLayers)
      VulkanSetup.destroyDebugUtilsMessengerEXT();
    vkDestroySurfaceKHR(VulkanSetup.instance, VulkanSetup.surface, null);
    vkDestroyInstance(VulkanSetup.instance, null);
  }

  /** enumerate suitable physical devices */
  private void enumerateSuitablePhysicalDevices() {
    VulkanSetup.physicalDevices = new ArrayList<>();
    try (MemoryStack stack = stackPush()) {
      IntBuffer deviceCount = stack.ints(0);
      vkEnumeratePhysicalDevices(VulkanSetup.instance, deviceCount, null);
      if (deviceCount.get(0) == 0)
        throw new RuntimeException("Failed to find GPUs with Vulkan support");
      PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
      vkEnumeratePhysicalDevices(VulkanSetup.instance, deviceCount, ppPhysicalDevices);
      for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
        VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), VulkanSetup.instance);
        if (VulkanSetup.isDeviceSuitable(VulkanSetup.surface, device))
          VulkanSetup.physicalDevices.add(device);
      }
    }
    if (VulkanSetup.physicalDevices.size() == 0)
      throw new RuntimeException("Failed to find a suitable GPU");
  }

  /** compute aspect ratio and set the projection matrix */
  private void setPerspective() {
    computeFieldOfView();
    double aspect = VulkanSetup.window.getPixelAspect() * VulkanSetup.swapChain.aspect;
    if (VulkanSetup.viewMode == ViewMode.STEREO) {
      aspect = aspect / 2;
      if (VulkanSetup.window.getPixelWidth() % 2 == 1) // if number of pixels odd, then correct
        aspect = (VulkanSetup.window.getPixelWidth() - 1) / VulkanSetup.window.getPixelWidth() * aspect;
    }
    VulkanSetup.projection.identity().perspective((float) VulkanSetup.fovy, (float) aspect, VulkanSetup.Z_NEAR,
        VulkanSetup.Z_FAR, true);
  }

  /** recreate the swap chain as necessary */
  private void recreateSwapChain() {
    try (MemoryStack stack = stackPush()) {
      IntBuffer width = stack.ints(0);
      IntBuffer height = stack.ints(0);
      while (width.get(0) == 0 && height.get(0) == 0) {
        GLFW.glfwGetFramebufferSize(VulkanSetup.window.getHandle(), width, height);
        glfwWaitEvents();
      }
    }
    vkDeviceWaitIdle(VulkanSetup.logicalDevice.device);
    VulkanSetup.swapChain.destroy();
    VulkanSetup.swapChain = new SwapChain();
    setPerspective();
  }

  /** create synchronization objects */
  private void createSyncObjects() {
    inFlightFrames = new ArrayList<>(VulkanSetup.MAX_FRAMES_IN_FLIGHT);
    imagesInFlight = new HashMap<>(VulkanSetup.swapChain.images.size());
    try (MemoryStack stack = stackPush()) {
      VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
      VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
          .flags(VK_FENCE_CREATE_SIGNALED_BIT);
      LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
      LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
      LongBuffer pFence = stack.mallocLong(1);
      for (int i = 0; i < VulkanSetup.MAX_FRAMES_IN_FLIGHT; i++) {
        if (vkCreateSemaphore(VulkanSetup.logicalDevice.device, semaphoreInfo,
            null, pImageAvailableSemaphore) != VK_SUCCESS
            || vkCreateSemaphore(VulkanSetup.logicalDevice.device, semaphoreInfo,
                null, pRenderFinishedSemaphore) != VK_SUCCESS
            || vkCreateFence(VulkanSetup.logicalDevice.device, fenceInfo, null, pFence) != VK_SUCCESS) {
          throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
        }
        inFlightFrames.add(new VulkanSetup.Frame(pImageAvailableSemaphore.get(0),
            pRenderFinishedSemaphore.get(0), pFence.get(0)));
      }
    }
  }

}