package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.Items;
import es.optocom.jovp.engine.Window;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static es.optocom.jovp.engine.rendering.VulkanSettings.*;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

/**
 * VulkanManager
 *
 * <ul>
 * <li>Vulkan Manager</li>
 * Manages all things Vulkan: instantiation, physical and logical device, queues, swap chain, views.
 * </ul>
 *
 * @since 0.0.1
 */
public class VulkanManager {

    Items items;
    private int distance; // in mm
    private float fovx; // in radians;
    private float fovy; // in radians;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private List<Frame> inFlightFrames;
    private Map<Integer, Frame> imagesInFlight;
    private int currentFrame;

    /**
     * Initiates the Vulkan manager
     *
     * @param window The window for the Vulkan manager
     *
     * @since 0.0.1
     */
    public VulkanManager(@NotNull Window window, int distance, boolean validationLayers, boolean apiDump) {
        VulkanSettings.window = window;
        VulkanSettings.validationLayers = validationLayers;
        VulkanSettings.apiDump = apiDump;
        this.distance = distance;
        addValidationLayers();
        createInstance();
        createSurface();
        if (validationLayers) setupDebugMessenger();
        enumerateSuitablePhysicalDevices();
    }

    /**
     * Start for a physical device
     *
     * @param physicalDevice The physical device handle
     *
     * @since 0.0.1
     */
    public void start(VkPhysicalDevice physicalDevice, Items items) {
        VulkanSettings.physicalDevice = physicalDevice;
        this.items = items;
        logicalDevice = new LogicalDevice(surface, physicalDevice);
        swapChain = new SwapChain();
        commandPool = createCommandPool();
        createCommandBuffers();
        createSyncObjects();
        setPerspective();
    }

    // List to pointer buffer
    private static @NotNull PointerBuffer commandsPointerBuffer(@NotNull List<? extends Pointer> list) {
        MemoryStack stack = stackGet();
        PointerBuffer buffer = stack.mallocPointer(list.size());
        list.forEach(buffer::put);
        return buffer.rewind();
    }

    /**
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
            if (items.update()) { // For synchronization, first update buffers then deal with items marked for deletion
                updateCommandBuffers();
                items.delete();
            }
            updateUniforms(imageIndex);
            imagesInFlight.put(imageIndex, thisFrame);
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(thisFrame.pImageAvailableSemaphore())
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pSignalSemaphores(thisFrame.pRenderFinishedSemaphore())
                    .pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex)));
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
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void cleanup() {
        if (logicalDevice != null) {
            destroySyncObjects();
            vkFreeCommandBuffers(logicalDevice.device, commandPool, commandsPointerBuffer(commandBuffers));
            destroyCommandPool(commandPool);
            swapChain.destroy();
            items.destroy();
            logicalDevice.destroy();
        }
        destroyInstance();
    }

    /**
     *
     * @param distance The distance of the observer from the display
     *
     * @since 0.0.1
     */
    public void setDistance(int distance) {
        this.distance = distance;
    }

    /**
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
     * @return The field of view in x and y directions.
     *
     * @since 0.0.1
     */
    public float[] getFieldOfView() {
        return new float[] {(float) Math.toDegrees(fovx), (float) Math.toDegrees(fovy)};
    }

    /**
     * Set view
     *
     * @param eye View eye
     * @param center View center
     * @param up View up
     *
     *
     * @since 0.0.1
     */
    public void setView(Vector3f eye, Vector3f center, Vector3f up) {
        view.lookAt(eye, center, up);
    }

    /**
     * @return The list of physical devices
     *
     * @since 0.0.1
     */
    public List<VkPhysicalDevice> getPhysicalDevices() {
        return physicalDevices;
    }

    /**
     * @return The Vulkan logical device
     *
     * @since 0.0.1
     */
    public VkDevice getDevice() {
        return logicalDevice.device;
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
    public @NotNull String getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice) {
        return VulkanSettings.getPhysicalDeviceProperties(physicalDevice);
    }

    /**
     *
     * @return A string with the swap chain support
     *
     * @param physicalDevice The physical device
     *
     * @since 0.0.1
     */
    public String getSwapChainSupport(VkPhysicalDevice physicalDevice) {
        return VulkanSettings.getSwapChainSupport(physicalDevice);
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

    // Compute field of view and aspect ratio
    private void setPerspective() {
        fovx = 2.0f * (float) Math.atan(window.getPixelWidth() * window.getWidth() / 2.0 / distance);
        fovy = 2.0f * (float) Math.atan(window.getPixelHeight() * window.getHeight() / 2.0 / distance);
        float aspect = window.getPixelAspect() * swapChain.aspect;
        projection.identity().perspective(fovy, aspect, Z_NEAR, Z_FAR, true);
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
        recreateCommandBuffers();
        setPerspective();
    }

    // Update uniforms
    void updateUniforms(int imageIndex) {
        for (Item item : items) item.buffers.updateUniforms(imageIndex, projection, view);
    }

    // Create command buffers
    private void createCommandBuffers() {
        final int commandBuffersCount = swapChain.swapChainFramebuffers.size();
        commandBuffers = new ArrayList<>(commandBuffersCount);
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(commandBuffersCount);
            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);
            int result = vkAllocateCommandBuffers(logicalDevice.device, allocInfo, pCommandBuffers);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to allocate command buffers: " +
                        translateVulkanResult(result));
            for (int i = 0; i < commandBuffersCount; i++)
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), logicalDevice.device));
        }
        updateCommandBuffers();
    }

    // Update command buffers
    private void updateCommandBuffers() {
        final int commandBuffersCount = swapChain.swapChainFramebuffers.size();
        try (MemoryStack stack = stackPush()) {
            for (int image = 0; image < commandBuffersCount; image++) {
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
                VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                        .renderPass(swapChain.renderPass);
                VkRect2D renderArea = VkRect2D.calloc(stack)
                        .offset(VkOffset2D.calloc(stack).set(0, 0))
                        .extent(swapChain.swapChainExtent);
                renderPassInfo.renderArea(renderArea);
                VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
                clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
                clearValues.get(1).depthStencil().set(1.0f, 0);
                renderPassInfo.pClearValues(clearValues);
                VkCommandBuffer commandBuffer = commandBuffers.get(image);
                int result = vkBeginCommandBuffer(commandBuffer, beginInfo);
                if (result != VK_SUCCESS)
                    throw new AssertionError("Failed to begin recording command buffers: " +
                            translateVulkanResult(result));
                renderPassInfo.framebuffer(swapChain.swapChainFramebuffers.get(image));
                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, swapChain.graphicsPipeline);
                    for (Item item : items) if (item.show) {
                        LongBuffer vertexBuffers = stack.longs(item.buffers.vertexBuffer);
                        LongBuffer offsets = stack.longs(0);
                        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                        vkCmdBindIndexBuffer(commandBuffer, item.buffers.indexBuffer,
                                0, VK_INDEX_TYPE_UINT32);
                        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                swapChain.pipelineLayout, 0,
                                stack.longs(item.buffers.descriptorSets.get(image)), null);
                        vkCmdDrawIndexed(commandBuffer, item.model.length, 1,
                                0, 0, 0);
                    }
                }
                vkCmdEndRenderPass(commandBuffer);
                result = vkEndCommandBuffer(commandBuffer);
                if (result != VK_SUCCESS)
                    throw new AssertionError("Failed to record command buffer: " +
                            translateVulkanResult(result));
            }
        }
    }

    // Recreate commands
    private void recreateCommandBuffers() {
        vkFreeCommandBuffers(logicalDevice.device, commandPool, commandsPointerBuffer(commandBuffers));
        createCommandBuffers();
    }

    // Create synchronization objects
    private void createSyncObjects() {
        inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
        imagesInFlight = new HashMap<>(swapChain.swapChainImages.size());
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