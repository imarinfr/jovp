package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.Items;
import es.optocom.jovp.engine.Window;
import es.optocom.jovp.engine.structures.ViewMode;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static es.optocom.jovp.engine.rendering.VulkanSetup.*;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

/**
 *
 * Manages all things Vulkan: instantiation, physical and logical device, queues, swap chain, views.
 *
 * @since 0.0.1
 */
public class VulkanManager {

    Items items;
    private List<Frame> inFlightFrames;
    private Map<Integer, Frame> imagesInFlight;
    private int currentFrame;

    /**
     *
     * Initiates the Vulkan manager
     *
     * @param window The window for the Vulkan manager
     * @param distance The observer's distance
     * @param validationLayers Whether to use validation layers
     * @param apiDump Whether to echo api dump
     *
     * @since 0.0.1
     */
    public VulkanManager(@NotNull Window window, int distance, boolean validationLayers, boolean apiDump) {
        VulkanSetup.window = window;
        VulkanSetup.validationLayers = validationLayers;
        VulkanSetup.apiDump = apiDump;
        VulkanSetup.distance = distance;
        addValidationLayers();
        createInstance();
        createSurface();
        if (validationLayers) setupDebugMessenger();
        enumerateSuitablePhysicalDevices();
        computeFieldOfView();
    }

    /**
     *
     * Start for a physical device
     *
     * @param physicalDevice The physical device handle
     * @param viewMode The view mode
     * @param items The initial items to start the psychophysics experience
     *
     * @since 0.0.1
     */
    public void start(VkPhysicalDevice physicalDevice, ViewMode viewMode, @NotNull Items items) {
        VulkanSetup.physicalDevice = physicalDevice;
        this.items = items;
        logicalDevice = new LogicalDevice(surface, physicalDevice);
        stereoView = viewMode == ViewMode.STEREO;
        swapChain = new SwapChain();
        for (Item item : items) item.createBuffers();
        commandPool = new VulkanCommands(items);
        createSyncObjects();
        setPerspective();
    }

    /**
     *
     * Draw a frame
     *
     * @since 0.0.1
     */
    public void drawFrame() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageIndex = stack.mallocInt(1);
            Frame thisFrame = inFlightFrames.get(currentFrame);
            vkWaitForFences(logicalDevice.device, thisFrame.pFence(), true, UINT64_MAX);
            int result = vkAcquireNextImageKHR(logicalDevice.device, swapChain.swapChain, UINT64_MAX,
                    thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);
            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                recreateSwapChain();
                return;
            } else if (result != VK_SUCCESS)
                throw new AssertionError("Cannot get image: " + translateVulkanResult(result));
            final int imageIndex = pImageIndex.get(0);
            if(imagesInFlight.containsKey(imageIndex))
                vkWaitForFences(logicalDevice.device, imagesInFlight.get(imageIndex).fence(), true, UINT64_MAX);
            vkDeviceWaitIdle(logicalDevice.device);
            commandPool.renderPass(imageIndex);
            imagesInFlight.put(imageIndex, thisFrame);
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(thisFrame.pImageAvailableSemaphore())
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pSignalSemaphores(thisFrame.pRenderFinishedSemaphore())
                    .pCommandBuffers(stack.pointers(commandPool.commandBuffers.get(imageIndex)));
            vkResetFences(logicalDevice.device, thisFrame.pFence());
            result = vkQueueSubmit(logicalDevice.graphicsQueue, submitInfo, thisFrame.fence());
            if (result != VK_SUCCESS) {
                vkResetFences(logicalDevice.device, thisFrame.pFence());
                throw new AssertionError("Failed to submit draw command buffer: " +
                        translateVulkanResult(result));
            }
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(thisFrame.pRenderFinishedSemaphore())
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(swapChain.swapChain))
                    .pImageIndices(pImageIndex);
            result = vkQueuePresentKHR(logicalDevice.presentQueue, presentInfo);
            if (window.resized()) {
                recreateSwapChain();
                window.resized(false);
            } else if (result != VK_SUCCESS)
                throw new AssertionError("Failed to present swap chain image: " +
                        translateVulkanResult(result));
            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    /**
     *
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void cleanup() {
        if (logicalDevice != null) {
            destroySyncObjects();
            commandPool.destroy();
            swapChain.destroy();
            items.destroy();
            logicalDevice.destroy();
            swapChain = null;
            logicalDevice = null;
        }
        destroyInstance();
    }

    /**
     *
     * Set viewing distance
     *
     * @param distance The distance of the observer from the display
     *
     * @since 0.0.1
     */
    public void setDistance(int distance) {
        VulkanSetup.distance = distance;
        computeFieldOfView();
    }

    /**
     *
     * Get viewing distance
     *
     * @return The distance of the observer from the display
     *
     * @since 0.0.1
     */
    public int getDistance() {
        return distance;
    }

    /**
     *
     * Get field of view
     *
     * @return The field of view in x and y directions.
     *
     * @since 0.0.1
     */
    public float[] getFieldOfView() {
        return new float[] {(float) Math.toDegrees(fovx), (float) Math.toDegrees(fovy)};
    }

    /**
     *
     * Updates the field of view depending on distance, window size, etc
     *
     * @since 0.0.1
     */
    public void computeFieldOfView() {
        double xHalfSize = window.getPixelWidth() * window.getScaledWidth() / 2 / distance;
        double yHalfSize = window.getPixelHeight() * window.getScaledHeight() / 2 / distance;
        if(stereoView) {
            xHalfSize = xHalfSize / 2;
            if (window.getPixelWidth() % 2 == 1) // if number of pixels odd, then correct
                xHalfSize = (window.getPixelWidth() - 1) / window.getPixelWidth() * xHalfSize;
        }
        VulkanSetup.fovx = (float) (2 * Math.atan(xHalfSize));
        VulkanSetup.fovy = (float) (2 * Math.atan(yHalfSize));
    }

    /**
     *
     * Set view
     *
     * @param eye View eye
     * @param center View center
     * @param up View up
     *
     * @since 0.0.1
     */
    public void setView(Vector3f eye, Vector3f center, Vector3f up) {
        view.lookAt(eye, center, up);
    }

    /**
     *
     * Get list of physical devices
     *
     * @return The list of physical devices
     *
     * @since 0.0.1
     */
    public List<VkPhysicalDevice> getPhysicalDevices() {
        return physicalDevices;
    }

    /**
     *
     * Get the current Vulkan logical device
     *
     * @return The Vulkan logical device
     *
     * @since 0.0.1
     */
    public VkDevice getDevice() {
        return logicalDevice.device;
    }

    /**
     *
     * Get physical device properties
     *
     * @param physicalDevice The physical device
     *
     * @return A string with the physical device properties
     *
     * @since 0.0.1
     */
    public @NotNull String getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice) {
        return VulkanSetup.getPhysicalDeviceProperties(physicalDevice);
    }

    /**
     *
     * Get physical device properties
     *
     * @param physicalDevice The physical device
     *
     * @return A set with all the physical device extensions supported
     *
     * @since 0.0.1
     */
    public String getPhysicalDeviceDeviceExtensionSupport(VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceProperties deviceProperties = getDeviceProperties(physicalDevice);
        StringBuilder extensionSupportList = new StringBuilder("Physical device: ");
        extensionSupportList.append(deviceProperties.deviceNameString()).append("\n");
        Set<String> extensionSet = listDeviceExtensionSupport(physicalDevice);
        for (String extension : extensionSet) extensionSupportList.append("\t").append(extension).append("\n");
        return extensionSupportList.toString();
    }

    /**
     *
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

    private void destroySyncObjects() {
        inFlightFrames.forEach(frame -> {
            vkDestroySemaphore(logicalDevice.device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(logicalDevice.device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(logicalDevice.device, frame.fence(), null);
        });
        inFlightFrames.clear();
    }

     // Create Vulkan instance
    private void createInstance() {
        if (validationLayers && !checkValidationLayerSupport())
            throw new RuntimeException("Validation requested but not supported");
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8Safe(TITLE))
                    .applicationVersion(VK_MAKE_VERSION(MAJOR, MINOR, PATCH))
                    .pEngineName(stack.UTF8Safe(ENGINE))
                    .engineVersion(VK_MAKE_VERSION(MAJOR, MINOR, PATCH))
                    .apiVersion(VK_API_VERSION);
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(getRequiredExtensions());
            if (validationLayers) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(VALIDATION_LAYERS));
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }
            PointerBuffer instancePtr = stack.mallocPointer(1);
            int result = vkCreateInstance(createInfo, null, instancePtr);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create instance: " + translateVulkanResult(result));
            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    // Create Vulkan surface
    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
            int result = glfwCreateWindowSurface(instance, window.getHandle(), null, pSurface);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create window surface: " +
                        translateVulkanResult(result));
            surface = pSurface.get(0);
        }
    }

    // Destroy the Vulkan instance, surface, and debug messenger
    private void destroyInstance() {
        if(validationLayers) destroyDebugUtilsMessengerEXT();
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }

    // Returns a list of suitable physical devices
    private void enumerateSuitablePhysicalDevices() {
        physicalDevices = new ArrayList<>();
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);
            if (deviceCount.get(0) == 0)
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);
            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);
                if (isDeviceSuitable(surface, device)) physicalDevices.add(device);
            }
        }
        if (physicalDevices.size() == 0) throw new RuntimeException("Failed to find a suitable GPU");
    }

    // Compute aspect ratio and set the projection matrix
    private void setPerspective() {
        computeFieldOfView();
        double aspect = window.getPixelAspect() * swapChain.aspect;
        if(stereoView) {
            aspect = aspect / 2;
            if (window.getPixelWidth() % 2 == 1) // if number of pixels odd, then correct
                aspect = (window.getPixelWidth() - 1) / window.getPixelWidth() * aspect;
        }
        projection.identity().perspective(fovy, (float) aspect, Z_NEAR, Z_FAR, true);
    }

    // Recreate the swap chain as necessary
    private void recreateSwapChain() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);
            while(width.get(0) == 0 && height.get(0) == 0) {
                glfwGetFramebufferSize(window.getHandle(), width, height);
                glfwWaitEvents();
            }
        }
        vkDeviceWaitIdle(logicalDevice.device);
        swapChain.destroy();
        swapChain = new SwapChain();
        setPerspective();
    }

    // Create synchronization objects
    private void createSyncObjects() {
        inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
        imagesInFlight = new HashMap<>(swapChain.images.size());
        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT);
            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                if (vkCreateSemaphore(logicalDevice.device, semaphoreInfo,
                        null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(logicalDevice.device, semaphoreInfo,
                        null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(logicalDevice.device, fenceInfo, null, pFence) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }
                inFlightFrames.add(new Frame(pImageAvailableSemaphore.get(0),
                        pRenderFinishedSemaphore.get(0), pFence.get(0)));
            }
        }
    }

}