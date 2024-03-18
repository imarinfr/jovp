package es.optocom.jovp.rendering;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

import es.optocom.jovp.definitions.RenderType;
import es.optocom.jovp.definitions.ViewMode;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCreateFence;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;

/**
 * 
 * Manages all things Vulkan: instantiation, physical and logical device,
 * queues, swap chain, views.
 *
 * @since 0.0.1
 */
public class VulkanManager {

    private VulkanCommands vulkanCommands;
    private List<VulkanSetup.Frame> inFlightFrames;
    private Map<Integer, VulkanSetup.Frame> imagesInFlight;
    private int currentFrame;

    /**
     * 
     * Initiates the Vulkan manager
     *
     * @param observer The observer for the Vulkan manager
     * @param validationLayers Whether to use validation layers
     * @param apiDump Whether to echo api dump
     *
     * @since 0.0.1
     */
    public VulkanManager(Observer observer, boolean validationLayers, boolean apiDump) {
        VulkanSetup.observer = observer;
        VulkanSetup.validationLayers = validationLayers;
        VulkanSetup.apiDump = apiDump;
        VulkanSetup.addValidationLayers();
        createInstance();
        createSurface();
        if (validationLayers) VulkanSetup.setupDebugMessenger();
        enumerateSuitablePhysicalDevices();
    }

    /**
     *
     * Start for a physical device
     *
     * @param physicalDevice The physical device handle
     * @param items          List of items to render
     *
     * @since 0.0.1
     */
    public void start(VkPhysicalDevice physicalDevice, ArrayList<Item> items, ArrayList<Text> texts) {
        VulkanSetup.physicalDevice = physicalDevice;
        VulkanSetup.logicalDevice = new LogicalDevice(VulkanSetup.surface, physicalDevice);
        VulkanSetup.swapChain = new SwapChain(VulkanSetup.observer.viewMode);
        for (Item item : items) item.createBuffers();
        for (Text text : texts) text.createBuffers();
        vulkanCommands = new VulkanCommands(items, texts);
        createSyncObjects();
        VulkanSetup.observer.computePerspective();
    }

    /**
     * 
     * Set the view mode
     *
     * @param viewMode The view mode, whether MONO or STEREO
     *
     * @since 0.0.1
     */
    public void setViewMode(ViewMode viewMode) {
        if (VulkanSetup.observer.getViewMode() == viewMode) return;
        VulkanSetup.observer.setViewMode(viewMode);
        if (VulkanSetup.swapChain != null) recreateSwapChain();
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
            vulkanCommands.renderPass(imageIndex);
            imagesInFlight.put(imageIndex, thisFrame);
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(thisFrame.pImageAvailableSemaphore())
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pSignalSemaphores(thisFrame.pRenderFinishedSemaphore())
                    .pCommandBuffers(stack.pointers(vulkanCommands.commandBuffers.get(imageIndex)));
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
            if (VulkanSetup.observer.window.resized()) {
                recreateSwapChain();
                VulkanSetup.observer.window.resized(false);
            } else if (result != VK_SUCCESS)
                throw new AssertionError("Failed to present swap chain image: " +
                        VulkanSetup.translateVulkanResult(result));
            currentFrame = (currentFrame + 1) % VulkanSetup.MAX_FRAMES_IN_FLIGHT;
        }
    }

    /**
     * 
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void cleanup() {
        if (VulkanSetup.logicalDevice != null) {
            destroySyncObjects();
            vulkanCommands.destroy();
            VulkanSetup.swapChain.destroy();
            VulkanSetup.logicalDevice.destroy();
            VulkanSetup.swapChain = null;
            VulkanSetup.logicalDevice = null;
        }
        destroyInstance();
        VulkanSetup.cleanup();
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
        return VulkanSetup.physicalDevices;
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
        return VulkanSetup.logicalDevice.device;
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
    public String getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice) {
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
        VkPhysicalDeviceProperties deviceProperties = VulkanSetup.getDeviceProperties(physicalDevice);
        StringBuilder extensionSupportList = new StringBuilder("Physical device: ");
        extensionSupportList.append(deviceProperties.deviceNameString()).append("\n");
        Set<String> extensionSet = VulkanSetup.listDeviceExtensionSupport(physicalDevice);
        for (String extension : extensionSet)
            extensionSupportList.append("\t").append(extension).append("\n");
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
            int result = GLFWVulkan.glfwCreateWindowSurface(VulkanSetup.instance,
                    VulkanSetup.observer.window.getHandle(),
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

    /** recreate the swap chain as necessary */
    public void recreateSwapChain() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);
            while (width.get(0) == 0 && height.get(0) == 0) {
                GLFW.glfwGetFramebufferSize(VulkanSetup.observer.window.getHandle(), width, height);
                glfwWaitEvents();
            }
        }
        vkDeviceWaitIdle(VulkanSetup.logicalDevice.device);
        VulkanSetup.swapChain.destroy();
        VulkanSetup.swapChain = new SwapChain(VulkanSetup.observer.viewMode);
        VulkanSetup.observer.computePerspective();
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

    /**
     * Command buffers for Vulkan rendering
     *
     * @since 0.0.1
     */
    class VulkanCommands {

        final long commandPool;
        List<VkCommandBuffer> commandBuffers;
        final ArrayList<Item> items;
        final ArrayList<Text> texts;

        /**
         * 
         * Creates the command pool and buffers
         *
         * @param items Items to render
         *
         * @since 0.0.1
         */
        VulkanCommands(ArrayList<Item> items, ArrayList<Text> texts) {
            this.items = items;
            this.texts = texts;
            commandPool = VulkanSetup.createCommandPool();
            createCommandBuffers();
        }

        /** destroy command pool and buffers */
        void destroy() {
            vkFreeCommandBuffers(VulkanSetup.logicalDevice.device, commandPool, commandsPointerBuffer(commandBuffers));
            VulkanSetup.destroyCommandPool(commandPool);
        }

        /** do a render pass */
        void renderPass(int image) {
            try (MemoryStack stack = stackPush()) {
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
                VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                        .renderPass(VulkanSetup.swapChain.renderPass);
                VkRect2D renderArea = VkRect2D.calloc(stack).offset(VkOffset2D.calloc(stack)
                        .set(0, 0)).extent(VulkanSetup.swapChain.extent);
                renderPassInfo.renderArea(renderArea);
                VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
                clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
                clearValues.get(1).depthStencil().set(1.0f, 0);
                renderPassInfo.pClearValues(clearValues);
                VkCommandBuffer commandBuffer = commandBuffers.get(image);
                int result = vkBeginCommandBuffer(commandBuffer, beginInfo);
                if (result != VK_SUCCESS)
                    throw new AssertionError("Failed to begin recording command buffers: " +
                            VulkanSetup.translateVulkanResult(result));
                renderPassInfo.framebuffer(VulkanSetup.swapChain.frameBuffers.get(image));
                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    for (Item item : items) item.render(stack, commandBuffer, image, RenderType.ITEM);
                    for (Text text : texts) text.render(stack, commandBuffer, image, RenderType.TEXT);
                }
                vkCmdEndRenderPass(commandBuffer);
                result = vkEndCommandBuffer(commandBuffer);
                if (result != VK_SUCCESS)
                    throw new AssertionError(
                            "Failed to record command buffer: " + VulkanSetup.translateVulkanResult(result));
            }
        }

        /** create command buffers for each command pool */
        private void createCommandBuffers() {
            int size = VulkanSetup.swapChain.frameBuffers.size();
            commandBuffers = new ArrayList<>(size);
            try (MemoryStack stack = stackPush()) {
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).commandPool(commandPool)
                        .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(size);
                PointerBuffer pCommandBuffers = stack.mallocPointer(size);
                int result = vkAllocateCommandBuffers(VulkanSetup.logicalDevice.device, allocInfo, pCommandBuffers);
                if (result != VK_SUCCESS)
                    throw new AssertionError("Failed to allocate command buffers: " +
                            VulkanSetup.translateVulkanResult(result));
                for (int i = 0; i < size; i++)
                    commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), VulkanSetup.logicalDevice.device));
            }
            for (int image = 0; image < size; image++)
                renderPass(image);
        }

        /** list to pointer buffer */
        private static PointerBuffer commandsPointerBuffer(List<? extends Pointer> list) {
            MemoryStack stack = stackGet();
            PointerBuffer buffer = stack.mallocPointer(list.size());
            list.forEach(buffer::put);
            return buffer.rewind();
        }

    }

}