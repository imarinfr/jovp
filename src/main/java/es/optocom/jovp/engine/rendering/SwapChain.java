package es.optocom.jovp.engine.rendering;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static es.optocom.jovp.engine.rendering.VulkanSetup.*;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK13.*;

/**
 *
 * SwapChain
 *
 * <ul>
 * <li>SwapChain</li>
 * SwapChain specifications
 * </ul>
 *
 * @since 0.0.1
 */
class SwapChain {

    long commandPool;
    long swapChain;
    List<Long> images;
    List<Long> imageViews;
    int imageFormat;
    VkExtent2D extent;
    long colorImage;
    long colorImageMemory;
    long colorImageView;
    long depthImage;
    long depthImageMemory;
    long depthImageView;
    long renderPass;
    List<ViewPass> viewPasses;
    List<Long> frameBuffers;
    float aspect;

    /**
     *
     * create SwapChain
     *
     * @since 0.0.1
     */
    SwapChain() {
        commandPool = createCommandPool();
        createSwapChain();
        createImageViews();
        createColorResources();
        createDepthResources();
        createRenderPass();
        if (stereoView) stereoSwapChain();
        else monoSwapChain();
        createFramebuffers();
        aspect = (float) extent.width() / (float) extent.height();
    }

    // Set swap chain for monocular view
    private void monoSwapChain() {
        viewPasses = new ArrayList<>(1);
        viewPasses.add(new ViewPass(renderPass, extent));
    }

    // Set swap chain for stereoscopic view
    private void stereoSwapChain() {
        IntBuffer width = stackGet().ints(0);
        if (extent.width() % 2 == 0) width.put(extent.width() / 2);
        else width.put((extent.width() - 1) / 2);
        VkExtent2D viewExtent = VkExtent2D.malloc().set(width.get(0), extent.height());
        viewPasses = new ArrayList<>(2);
        viewPasses.add(new ViewPass(renderPass, viewExtent));
        viewPasses.add(new ViewPass(renderPass, width.get(0), viewExtent));
    }

    /**
     *
     * destroy SwapChain
     *
     * @since 0.0.1
     */
    void destroy() {
        frameBuffers.forEach(framebuffer -> vkDestroyFramebuffer(logicalDevice.device, framebuffer, null));
        for (ViewPass viewPass : viewPasses) viewPass.destroy();
        vkDestroyRenderPass(logicalDevice.device, renderPass, null);
        vkDestroyImage(logicalDevice.device, depthImage, null);
        vkFreeMemory(logicalDevice.device, depthImageMemory, null);
        vkFreeMemory(logicalDevice.device, colorImageMemory, null);
        vkDestroyImageView(logicalDevice.device, depthImageView, null);
        vkDestroyImageView(logicalDevice.device, colorImageView, null);
        vkDestroyImage(logicalDevice.device, colorImage, null);
        imageViews.forEach(imageView -> vkDestroyImageView(logicalDevice.device, imageView, null));
        vkDestroySwapchainKHR(logicalDevice.device, swapChain, null);
        destroyCommandPool(commandPool);
    }

    // Create swap chain
    private void createSwapChain() {
        try (MemoryStack stack = stackPush()) {
            VulkanSetup.SwapChainSupportDetails swapChainSupport = swapChainSupport(stack);
            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(window.getHandle(), swapChainSupport.capabilities);
            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);
            if (swapChainSupport.capabilities.maxImageCount() > 0 &&
                    imageCount.get(0) > swapChainSupport.capabilities.maxImageCount())
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(imageCount.get(0))
                    .imageFormat(surfaceFormat.format())
                    .imageColorSpace(surfaceFormat.colorSpace())
                    .imageExtent(extent)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            QueueFamilyIndices indices = queueFamilies();
            if (!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            createInfo.preTransform(swapChainSupport.capabilities.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);
            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateSwapchainKHR(logicalDevice.device, createInfo, null, pSwapChain);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create swap chain: " + translateVulkanResult(result));
            swapChain = pSwapChain.get(0);
            vkGetSwapchainImagesKHR(logicalDevice.device, swapChain, imageCount, null);
            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));
            vkGetSwapchainImagesKHR(logicalDevice.device, swapChain, imageCount, pSwapchainImages);
            images = new ArrayList<>(imageCount.get(0));
            for (int i = 0; i < pSwapchainImages.capacity(); i++) images.add(pSwapchainImages.get(i));
            imageFormat = surfaceFormat.format();
            this.extent = VkExtent2D.create().set(extent);
        }
    }

    // Create image views
    private void createImageViews() {
        imageViews = new ArrayList<>(images.size());
        for (long swapChainImage : images)
            imageViews.add(createImageView(logicalDevice.device, swapChainImage, imageFormat,
                    VK_IMAGE_ASPECT_COLOR_BIT, MIP_LEVELS));
    }


    // Create color resources
    private void createColorResources() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pColorImage = stack.mallocLong(1);
            LongBuffer pColorImageMemory = stack.mallocLong(1);
            createImage(extent.width(), extent.height(), MIP_LEVELS,logicalDevice.msaaSamples, imageFormat,
                    VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    pColorImage, pColorImageMemory);
            colorImage = pColorImage.get(0);
            colorImageMemory = pColorImageMemory.get(0);
            colorImageView = createImageView(logicalDevice.device, colorImage, imageFormat,
                    VK_IMAGE_ASPECT_COLOR_BIT, MIP_LEVELS);
            transitionImageLayout(commandPool, colorImage, imageFormat,
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, MIP_LEVELS);
        }
    }

    // Create render pass
    private void createRenderPass() {
        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(3, stack);
            VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.calloc(3, stack);
            // MSAA Image
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(imageFormat).samples(logicalDevice.msaaSamples)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(STENCIL_LOAD_OPERATION).stencilStoreOp(STENCIL_STORE_OPERATION)
                    .initialLayout(INITIAL_LAYOUT).finalLayout(COLOR_ATTACHMENT_FINAL_LAYOUT);
            VkAttachmentReference colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0).layout(COLOR_ATTACHMENT_LAYOUT);
            // Present Image
            VkAttachmentDescription colorAttachmentResolve = attachments.get(2);
            colorAttachmentResolve.format(imageFormat).samples(COLOR_ATTACHMENT_SAMPLES)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(STENCIL_LOAD_OPERATION).stencilStoreOp(STENCIL_STORE_OPERATION)
                    .initialLayout(INITIAL_LAYOUT).finalLayout(COLOR_ATTACHMENT_RESOLVE_FINAL_LAYOUT);
            VkAttachmentReference colorAttachmentResolveRef = attachmentRefs.get(2);
            colorAttachmentResolveRef.attachment(2).layout(COLOR_ATTACHMENT_RESOLVE_LAYOUT);
            // Depth-Stencil attachments
            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(findDepthFormat()).samples(logicalDevice.msaaSamples)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(STENCIL_LOAD_OPERATION).stencilStoreOp(STENCIL_STORE_OPERATION)
                    .initialLayout(INITIAL_LAYOUT).finalLayout(DEPTH_ATTACHMENT_FINAL_LAYOUT);
            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
            depthAttachmentRef.attachment(1).layout(DEPTH_ATTACHMENT_LAYOUT);
            // Pipeline bind point
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS).colorAttachmentCount(1)
                    .pColorAttachments(VkAttachmentReference.calloc(1, stack).
                            put(0, colorAttachmentRef))
                    .pDepthStencilAttachment(depthAttachmentRef)
                    .pResolveAttachments(VkAttachmentReference.calloc(1, stack).
                            put(0, colorAttachmentResolveRef));
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0)
                    .srcStageMask(PIPELINE_STAGE_COLOR_ATTACHMENT).srcAccessMask(0)
                    .dstStageMask(PIPELINE_STAGE_COLOR_ATTACHMENT).dstAccessMask(PIPELINE_ACCESS);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachments).pSubpasses(subpass).pDependencies(dependency);
            LongBuffer pRenderPass = stack.mallocLong(1);
            int result = vkCreateRenderPass(logicalDevice.device, renderPassInfo, null, pRenderPass);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create render pass: " + translateVulkanResult(result));
            renderPass = pRenderPass.get(0);
        }
    }

    // Create depth resources
    private void createDepthResources() {
        try (MemoryStack stack = stackPush()) {
            int depthFormat = findDepthFormat();
            LongBuffer pDepthImage = stack.mallocLong(1);
            LongBuffer pDepthImageMemory = stack.mallocLong(1);
            createImage(extent.width(), extent.height(), MIP_LEVELS, logicalDevice.msaaSamples, depthFormat,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, pDepthImage, pDepthImageMemory);
            depthImage = pDepthImage.get(0);
            depthImageMemory = pDepthImageMemory.get(0);
            depthImageView = createImageView(logicalDevice.device, depthImage, depthFormat,
                    VK_IMAGE_ASPECT_DEPTH_BIT, MIP_LEVELS);
            transitionImageLayout(commandPool, depthImage, depthFormat,
                    VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, MIP_LEVELS);
        }
    }

    // Create frame buffers
    private void createFramebuffers() {
        frameBuffers = new ArrayList<>(imageViews.size());
        try (MemoryStack stack = stackPush()) {
            LongBuffer attachments = stack.longs(colorImageView, depthImageView, VK_NULL_HANDLE);
            LongBuffer pFramebuffer = stack.mallocLong(1);
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).renderPass(renderPass)
                    .width(extent.width()).height(extent.height()).layers(1);
            for (long imageView : imageViews) {
                attachments.put(2, imageView);
                framebufferInfo.pAttachments(attachments);
                int result = vkCreateFramebuffer(logicalDevice.device, framebufferInfo, null, pFramebuffer);
                if (result != VK_SUCCESS)
                    throw new AssertionError("Failed to create framebuffer: " +
                            translateVulkanResult(result));
                frameBuffers.add(pFramebuffer.get(0));
            }
        }
    }

    // Choose swap surface format
    private static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.@NotNull Buffer availableFormats) {
        return availableFormats.stream()
                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_SRGB)
                .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny()
                .orElse(availableFormats.get(0));
    }

    // Choose swap present mode
    private static int chooseSwapPresentMode(@NotNull IntBuffer availablePresentModes) {
        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if(availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR)
                return availablePresentModes.get(i);
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    // Choose swap extent
    private static @NotNull VkExtent2D chooseSwapExtent(long window, @NotNull VkSurfaceCapabilitiesKHR capabilities) {
        if(capabilities.currentExtent().width() != UINT32_MAX) return capabilities.currentExtent();
        IntBuffer width = stackGet().ints(0);
        IntBuffer height = stackGet().ints(0);
        glfwGetFramebufferSize(window, width, height);
        VkExtent2D actualExtent = VkExtent2D.malloc().set(width.get(0), height.get(0));
        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();
        actualExtent.width(clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));
        return actualExtent;
    }

    // Clamp
    private static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

}
