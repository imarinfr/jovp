package es.optocom.jovp.rendering;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import es.optocom.jovp.definitions.ViewMode;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkCreateRenderPass;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

/**
 * SwapChain specifications
 *
 * @since 0.0.1
 */
class SwapChain {

    final long commandPool;
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

    /**
     * create SwapChain
     *
     * @param viewMode the view mode for rendering, whether MONO or STEREO
     * 
     * @since 0.0.1
     */
    SwapChain(ViewMode viewMode) {
        commandPool = VulkanSetup.createCommandPool();
        createSwapChain();
        createImageViews();
        createColorResources();
        createDepthResources();
        createRenderPass();
        if (viewMode == ViewMode.STEREO)
            stereoSwapChain();
        else
            monoSwapChain();
        createFramebuffers();
    }

    /**
     * destroy SwapChain
     *
     * @since 0.0.1
     */
    void destroy() {
        frameBuffers.forEach(framebuffer -> vkDestroyFramebuffer(VulkanSetup.logicalDevice.device, framebuffer, null));
        for (ViewPass viewPass : viewPasses) viewPass.destroy();
        vkDestroyRenderPass(VulkanSetup.logicalDevice.device, renderPass, null);
        vkDestroyImage(VulkanSetup.logicalDevice.device, depthImage, null);
        vkFreeMemory(VulkanSetup.logicalDevice.device, depthImageMemory, null);
        vkFreeMemory(VulkanSetup.logicalDevice.device, colorImageMemory, null);
        vkDestroyImageView(VulkanSetup.logicalDevice.device, depthImageView, null);
        vkDestroyImageView(VulkanSetup.logicalDevice.device, colorImageView, null);
        vkDestroyImage(VulkanSetup.logicalDevice.device, colorImage, null);
        imageViews.forEach(imageView -> vkDestroyImageView(VulkanSetup.logicalDevice.device, imageView, null));
        vkDestroySwapchainKHR(VulkanSetup.logicalDevice.device, swapChain, null);
        VulkanSetup.destroyCommandPool(commandPool);
    }

    /** set swap chain for monocular view */
    private void monoSwapChain() {
        viewPasses = new ArrayList<>(1);
        viewPasses.add(new ViewPass(renderPass, 0, extent));
    }

    /** set swap chain for stereoscopic view */
    private void stereoSwapChain() {
        VkExtent2D halfExtent = VkExtent2D.malloc().set(extent.width() / 2, extent.height());
        viewPasses = new ArrayList<>(2);
        viewPasses.add(new ViewPass(renderPass, 0, halfExtent));
        viewPasses.add(new ViewPass(renderPass, halfExtent.width(), halfExtent));
    }

    /** create swap chain */
    private void createSwapChain() {
        try (MemoryStack stack = stackPush()) {
            VulkanSetup.SwapChainSupportDetails swapChainSupport = VulkanSetup.swapChainSupport(stack);
            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(VulkanSetup.observer.window.getHandle(),
                    swapChainSupport.capabilities);
            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);
            if (swapChainSupport.capabilities.maxImageCount() > 0 &&
                    imageCount.get(0) > swapChainSupport.capabilities.maxImageCount())
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(VulkanSetup.surface)
                    .minImageCount(imageCount.get(0))
                    .imageFormat(surfaceFormat.format())
                    .imageColorSpace(surfaceFormat.colorSpace())
                    .imageExtent(extent)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            VulkanSetup.QueueFamilyIndices indices = VulkanSetup.queueFamilies();
            if (!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            createInfo.preTransform(swapChainSupport.capabilities.currentTransform())
                    .compositeAlpha(VulkanSetup.COMPOSITE_ALPHA_MODE)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);
            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateSwapchainKHR(VulkanSetup.logicalDevice.device, createInfo, null, pSwapChain);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create swap chain: " + VulkanSetup.translateVulkanResult(result));
            swapChain = pSwapChain.get(0);
            vkGetSwapchainImagesKHR(VulkanSetup.logicalDevice.device, swapChain, imageCount, null);
            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));
            vkGetSwapchainImagesKHR(VulkanSetup.logicalDevice.device, swapChain, imageCount, pSwapchainImages);
            images = new ArrayList<>(imageCount.get(0));
            for (int i = 0; i < pSwapchainImages.capacity(); i++)
                images.add(pSwapchainImages.get(i));
            imageFormat = surfaceFormat.format();
            this.extent = VkExtent2D.create().set(extent);
        }
    }

    /** create image views */
    private void createImageViews() {
        imageViews = new ArrayList<>(images.size());
        for (long swapChainImage : images)
            imageViews.add(VulkanSetup.createImageView(VulkanSetup.logicalDevice.device, swapChainImage, imageFormat,
                    VK_IMAGE_ASPECT_COLOR_BIT, VulkanSetup.MIP_LEVELS));
    }

    /** create color resources */
    private void createColorResources() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pColorImage = stack.mallocLong(1);
            LongBuffer pColorImageMemory = stack.mallocLong(1);
            VulkanSetup.createImage(extent.width(), extent.height(), VulkanSetup.MIP_LEVELS,
                    VulkanSetup.logicalDevice.msaaSamples, imageFormat,
                    VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    pColorImage, pColorImageMemory);
            colorImage = pColorImage.get(0);
            colorImageMemory = pColorImageMemory.get(0);
            colorImageView = VulkanSetup.createImageView(VulkanSetup.logicalDevice.device, colorImage, imageFormat,
                    VK_IMAGE_ASPECT_COLOR_BIT, VulkanSetup.MIP_LEVELS);
            VulkanSetup.transitionImageLayout(commandPool, colorImage, imageFormat,
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VulkanSetup.MIP_LEVELS);
        }
    }

    /** create render pass */
    private void createRenderPass() {
        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(3, stack);
            VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.calloc(3, stack);
            // MSAA Image
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(imageFormat).samples(VulkanSetup.logicalDevice.msaaSamples)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkAttachmentReference colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            // Present Image
            VkAttachmentDescription colorAttachmentResolve = attachments.get(2);
            colorAttachmentResolve.format(imageFormat).samples(VulkanSetup.COLOR_ATTACHMENT_SAMPLES)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            VkAttachmentReference colorAttachmentResolveRef = attachmentRefs.get(2);
            colorAttachmentResolveRef.attachment(2).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            // Depth-Stencil attachments
            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(VulkanSetup.findDepthFormat()).samples(VulkanSetup.logicalDevice.msaaSamples)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
            depthAttachmentRef.attachment(1).layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            // Pipeline bind point
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS).colorAttachmentCount(1)
                    .pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef))
                    .pDepthStencilAttachment(depthAttachmentRef)
                    .pResolveAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentResolveRef));
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).srcAccessMask(0)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachments).pSubpasses(subpass).pDependencies(dependency);
            LongBuffer pRenderPass = stack.mallocLong(1);
            int result = vkCreateRenderPass(VulkanSetup.logicalDevice.device, renderPassInfo, null, pRenderPass);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create render pass: " + VulkanSetup.translateVulkanResult(result));
            renderPass = pRenderPass.get(0);
        }
    }

    /** create depth resources */
    private void createDepthResources() {
        try (MemoryStack stack = stackPush()) {
            int depthFormat = VulkanSetup.findDepthFormat();
            LongBuffer pDepthImage = stack.mallocLong(1);
            LongBuffer pDepthImageMemory = stack.mallocLong(1);
            VulkanSetup.createImage(extent.width(), extent.height(), VulkanSetup.MIP_LEVELS,
                    VulkanSetup.logicalDevice.msaaSamples, depthFormat,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, pDepthImage, pDepthImageMemory);
            depthImage = pDepthImage.get(0);
            depthImageMemory = pDepthImageMemory.get(0);
            depthImageView = VulkanSetup.createImageView(VulkanSetup.logicalDevice.device, depthImage, depthFormat,
                    VK_IMAGE_ASPECT_DEPTH_BIT, VulkanSetup.MIP_LEVELS);
            VulkanSetup.transitionImageLayout(commandPool, depthImage, depthFormat,
                    VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, VulkanSetup.MIP_LEVELS);
        }
    }

    /** create frame buffers */
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
                int result = vkCreateFramebuffer(VulkanSetup.logicalDevice.device, framebufferInfo, null, pFramebuffer);
                if (result != VK_SUCCESS)
                    throw new AssertionError("Failed to create framebuffer: " +
                            VulkanSetup.translateVulkanResult(result));
                frameBuffers.add(pFramebuffer.get(0));
            }
        }
    }

    /** choose swap surface format */
    private static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream()
                .filter(availableFormat -> availableFormat.format() == VulkanSetup.SURFACE_FORMAT)
                .filter(availableFormat -> availableFormat.colorSpace() == VulkanSetup.COLOR_SPACE)
                .findAny()
                .orElse(availableFormats.get(0));
    }

    /** choose swap present mode */
    private static int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if (availablePresentModes.get(i) == VulkanSetup.PRESENT_MODE)
                return availablePresentModes.get(i);
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    /** choose swap extent */
    private static VkExtent2D chooseSwapExtent(long window, VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != VulkanSetup.UINT32_MAX)
            return capabilities.currentExtent();
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

    /** clamp */
    private static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

}
