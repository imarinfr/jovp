package es.optocom.jovp.engine.rendering;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static es.optocom.jovp.engine.rendering.VulkanSettings.*;
import static es.optocom.jovp.engine.structures.ShaderKind.FRAGMENT_SHADER;
import static es.optocom.jovp.engine.structures.ShaderKind.VERTEX_SHADER;
import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK13.*;

public class SwapChain {

    long swapChain;
    long commandPool;
    List<Long> swapChainImages;
    int swapChainImageFormat;
    VkExtent2D swapChainExtent;
    List<Long> swapChainImageViews;
    long renderPass;
    long pipelineLayout;
    long graphicsPipeline;
    long colorImage;
    long colorImageMemory;
    long colorImageView;
    long depthImage;
    long depthImageMemory;
    long depthImageView;
    List<Long> swapChainFramebuffers;
    float aspect;

    SwapChain() {
        commandPool = createCommandPool();
        createSwapChain();
        aspect = (float) swapChainExtent.width() / (float) swapChainExtent.height();
        createImageViews();
        createRenderPass();
        createGraphicsPipeline();
        createColorResources();
        createDepthResources();
        createFramebuffers();
    }

    void destroy() {
        vkDestroyImageView(logicalDevice.device, colorImageView, null);
        vkDestroyImage(logicalDevice.device, colorImage, null);
        vkFreeMemory(logicalDevice.device, colorImageMemory, null);
        vkDestroyImageView(logicalDevice.device, depthImageView, null);
        vkDestroyImage(logicalDevice.device, depthImage, null);
        vkFreeMemory(logicalDevice.device, depthImageMemory, null);
        swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(logicalDevice.device, framebuffer, null));
        vkDestroyPipeline(logicalDevice.device, graphicsPipeline, null);
        vkDestroyPipelineLayout(logicalDevice.device, pipelineLayout, null);
        vkDestroyRenderPass(logicalDevice.device, renderPass, null);
        swapChainImageViews.forEach(imageView -> vkDestroyImageView(logicalDevice.device, imageView, null));
        vkDestroySwapchainKHR(logicalDevice.device, swapChain, null);
        destroyCommandPool(commandPool);
    }

    // Create swap chain
    private void createSwapChain() {
        try (MemoryStack stack = stackPush()) {
            VulkanSettings.SwapChainSupportDetails swapChainSupport = swapChainSupport(stack);
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
            swapChainImages = new ArrayList<>(imageCount.get(0));
            for (int i = 0; i < pSwapchainImages.capacity(); i++) swapChainImages.add(pSwapchainImages.get(i));
            swapChainImageFormat = surfaceFormat.format();
            swapChainExtent = VkExtent2D.create().set(extent);
        }
    }

    // Create image views
    private void createImageViews() {
        swapChainImageViews = new ArrayList<>(swapChainImages.size());
        for (long swapChainImage : swapChainImages)
            swapChainImageViews.add(createImageView(logicalDevice.device, swapChainImage, swapChainImageFormat,
                    VK_IMAGE_ASPECT_COLOR_BIT, MIP_LEVELS));
    }

    // Create render pass
    private void createRenderPass() {
        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(3, stack);
            VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.calloc(3, stack);
            // MSAA Image
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(swapChainImageFormat)
                    .samples(logicalDevice.msaaSamples)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(STENCIL_LOAD_OPERATION)
                    .stencilStoreOp(STENCIL_STORE_OPERATION)
                    .initialLayout(INITIAL_LAYOUT)
                    .finalLayout(COLOR_ATTACHMENT_FINAL_LAYOUT);
            VkAttachmentReference colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0).layout(COLOR_ATTACHMENT_LAYOUT);
            // Present Image
            VkAttachmentDescription colorAttachmentResolve = attachments.get(2);
            colorAttachmentResolve.format(swapChainImageFormat)
                    .samples(COLOR_ATTACHMENT_SAMPLES)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(STENCIL_LOAD_OPERATION)
                    .stencilStoreOp(STENCIL_STORE_OPERATION)
                    .initialLayout(INITIAL_LAYOUT)
                    .finalLayout(COLOR_ATTACHMENT_RESOLVE_FINAL_LAYOUT);
            VkAttachmentReference colorAttachmentResolveRef = attachmentRefs.get(2);
            colorAttachmentResolveRef.attachment(2).layout(COLOR_ATTACHMENT_RESOLVE_LAYOUT);
            // Depth-Stencil attachments
            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(findDepthFormat())
                    .samples(logicalDevice.msaaSamples)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(STENCIL_LOAD_OPERATION)
                    .stencilStoreOp(STENCIL_STORE_OPERATION)
                    .initialLayout(INITIAL_LAYOUT)
                    .finalLayout(DEPTH_ATTACHMENT_FINAL_LAYOUT);
            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
            depthAttachmentRef.attachment(1).layout(DEPTH_ATTACHMENT_LAYOUT);
            // Pipeline bind point
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(VkAttachmentReference.calloc(1, stack).
                            put(0, colorAttachmentRef))
                    .pDepthStencilAttachment(depthAttachmentRef)
                    .pResolveAttachments(VkAttachmentReference.calloc(1, stack).
                            put(0, colorAttachmentResolveRef));
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(PIPELINE_STAGE_COLOR_ATTACHMENT)
                    .srcAccessMask(0)
                    .dstStageMask(PIPELINE_STAGE_COLOR_ATTACHMENT)
                    .dstAccessMask(PIPELINE_ACCESS);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachments)
                    .pSubpasses(subpass)
                    .pDependencies(dependency);
            LongBuffer pRenderPass = stack.mallocLong(1);
            int result = vkCreateRenderPass(logicalDevice.device, renderPassInfo, null, pRenderPass);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create render pass: " + translateVulkanResult(result));
            renderPass = pRenderPass.get(0);
        }
    }

    // Create graphics pipeline
    private void createGraphicsPipeline() {
        try (MemoryStack stack = stackPush()) {
            SPIRV vertShaderSPIRV = compileShaderFile("shaders/shader.vert", VERTEX_SHADER);
            SPIRV fragShaderSPIRV = compileShaderFile("shaders/shader.frag", FRAGMENT_SHADER);
            long vertShaderModule = createShaderModule(vertShaderSPIRV.bytecode());
            long fragShaderModule = createShaderModule(fragShaderSPIRV.bytecode());
            ByteBuffer entryPoint = stack.UTF8("main");
            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                    VkPipelineShaderStageCreateInfo.calloc(2, stack);
            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);
            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertShaderModule)
                    .pName(entryPoint);
            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragShaderModule)
                    .pName(entryPoint);
            // Vertex stage
            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(getBindingDescription())
                    .pVertexAttributeDescriptions(getAttributeDescriptions());
            // Assembly stage
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(PRIMITIVE_TOPOLOGY)
                    .primitiveRestartEnable(PRIMITIVE_RESTART_ENABLE);
            // Viewport and scissor
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(VIEWPORT_X)
                    .y(VIEWPORT_Y)
                    .width(swapChainExtent.width())
                    .height(swapChainExtent.height())
                    .minDepth(VIEWPORT_MIN_DEPTH)
                    .maxDepth(VIEWPORT_MAX_DEPTH);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .offset(VkOffset2D.calloc(stack).set(SCISSOR_OFFSET_X, SCISSOR_OFFSET_Y))
                    .extent(swapChainExtent);
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewport)
                    .pScissors(scissor);
            // Rasterization stage
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(DEPTH_CLAMP_ENABLE)
                    .rasterizerDiscardEnable(RASTERIZER_DISCARD_ENABLE)
                    .polygonMode(POLYGON_MODE)
                    .lineWidth(LINE_WIDTH)
                    .cullMode(CULL_MODE)
                    .frontFace(FRONT_FACE)
                    .depthBiasEnable(DEPTH_BIAS_ENABLE);
            // Multisampling
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(SAMPLE_SHADING_ENABLE)
                    .minSampleShading(MIN_SAMPLE_SHADING)
                    .rasterizationSamples(logicalDevice.msaaSamples);
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(DEPTH_TEST_ENABLE)
                    .depthWriteEnable(DEPTH_WRITE_ENABLE)
                    .depthCompareOp(DEPTH_COMPARE_OPERATION)
                    .depthBoundsTestEnable(DEPTH_BOUNDS_TEST_ENABLE)
                    .minDepthBounds(MIN_DEPTH_BOUNDS)
                    .maxDepthBounds(MAX_DEPTH_BOUNDS)
                    .stencilTestEnable(STENCIL_TEST_ENABLE);
            // Color blending
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack)
                            .colorWriteMask(COLOR_WRITE_MASK)
                            .blendEnable(BLEND_ENABLE);
            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(LOGIC_OPERATION_ENABLE)
                    .logicOp(LOGIC_OPERATION)
                    .pAttachments(colorBlendAttachment)
                    .blendConstants(stack.floats(BLEND_CONSTANTS_X, BLEND_CONSTANTS_Y,
                    BLEND_CONSTANTS_Z, BLEND_CONSTANTS_W));
            // Pipeline layout creation
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(logicalDevice.descriptorSetLayout));
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            int result = vkCreatePipelineLayout(logicalDevice.device, pipelineLayoutInfo,
                    null, pPipelineLayout);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create pipeline layout: " + translateVulkanResult(result));
            pipelineLayout = pPipelineLayout.get(0);
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(shaderStages)
                    .pVertexInputState(vertexInputInfo)
                    .pInputAssemblyState(inputAssembly)
                    .pViewportState(viewportState)
                    .pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling)
                    .pDepthStencilState(depthStencil)
                    .pColorBlendState(colorBlending)
                    .layout(pipelineLayout)
                    .renderPass(renderPass)
                    .subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            result = vkCreateGraphicsPipelines(logicalDevice.device, VK_NULL_HANDLE, pipelineInfo,
                    null, pGraphicsPipeline);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create graphics pipeline: " + translateVulkanResult(result));
            graphicsPipeline = pGraphicsPipeline.get(0);
            // Release resources
            vkDestroyShaderModule(logicalDevice.device, vertShaderModule, null);
            vkDestroyShaderModule(logicalDevice.device, fragShaderModule, null);
            vertShaderSPIRV.free();
            fragShaderSPIRV.free();
        }
    }

    // Create color resources
    private void createColorResources() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pColorImage = stack.mallocLong(1);
            LongBuffer pColorImageMemory = stack.mallocLong(1);
            createImage(swapChainExtent.width(), swapChainExtent.height(), MIP_LEVELS,
                    logicalDevice.msaaSamples, swapChainImageFormat,
                    VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    pColorImage, pColorImageMemory);
            colorImage = pColorImage.get(0);
            colorImageMemory = pColorImageMemory.get(0);
            colorImageView = createImageView(logicalDevice.device, colorImage, swapChainImageFormat,
                     VK_IMAGE_ASPECT_COLOR_BIT, MIP_LEVELS);
            transitionImageLayout(commandPool, colorImage, swapChainImageFormat,
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, MIP_LEVELS);
        }
    }

    // Create depth resources
    private void createDepthResources() {
        try (MemoryStack stack = stackPush()) {
            int depthFormat = findDepthFormat();
            LongBuffer pDepthImage = stack.mallocLong(1);
            LongBuffer pDepthImageMemory = stack.mallocLong(1);
            createImage(swapChainExtent.width(), swapChainExtent.height(), MIP_LEVELS,
                    logicalDevice.msaaSamples, depthFormat, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                    pDepthImage, pDepthImageMemory);
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
        swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());
        try (MemoryStack stack = stackPush()) {
            LongBuffer attachments = stack.longs(colorImageView, depthImageView, VK_NULL_HANDLE);
            LongBuffer pFramebuffer = stack.mallocLong(1);
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .width(swapChainExtent.width())
                    .height(swapChainExtent.height())
                    .layers(1);
            for (long imageView : swapChainImageViews) {
                attachments.put(2, imageView);
                framebufferInfo.pAttachments(attachments);
                int result = vkCreateFramebuffer(logicalDevice.device, framebufferInfo, null, pFramebuffer);
                if (result != VK_SUCCESS)
                    throw new AssertionError("Failed to create framebuffer: " + translateVulkanResult(result));
                swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }
    }

    // Create shader module
    private long createShaderModule(ByteBuffer spirvCode) {
        try (MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(spirvCode);
            LongBuffer pShaderModule = stack.mallocLong(1);
            int result = vkCreateShaderModule(logicalDevice.device, createInfo, null, pShaderModule);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create shader module: " + translateVulkanResult(result));
            return pShaderModule.get(0);
        }
    }

    // Compile shader file
    private static SPIRV compileShaderFile(String shaderFile, es.optocom.jovp.engine.structures.ShaderKind shaderKind) {
        return compileShaderAbsoluteFile(Objects.requireNonNull(getSystemClassLoader().
                getResource(shaderFile)).toExternalForm(), shaderKind);
    }

    // Compile shader from absolute file
    private static @Nullable SPIRV compileShaderAbsoluteFile(String shaderFile, es.optocom.jovp.engine.structures.ShaderKind shaderKind) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Compile shader
    @Contract("_, _, _ -> new")
    private static @NotNull SPIRV compileShader(String filename, String source, es.optocom.jovp.engine.structures.ShaderKind shaderKind) {
        long compiler = shaderc_compiler_initialize();
        if(compiler == NULL) throw new RuntimeException("Failed to create shader compiler");
        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind,
                filename, "main", NULL);
        if(result == NULL) throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success)
            throw new RuntimeException("Failed to compile shader " + filename + "into SPIR-V:\n " +
                    shaderc_result_get_error_message(result));
        shaderc_compiler_release(compiler);
        return new SPIRV(result, shaderc_result_get_bytes(result));
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

    // Get vertex input binding description
    public static VkVertexInputBindingDescription.@NotNull Buffer getBindingDescription() {
        return VkVertexInputBindingDescription.calloc(1)
                .binding(0).stride(MODEL_SIZEOF).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
    }

    // Get attribute descriptions
    public static VkVertexInputAttributeDescription.@NotNull Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.calloc(2);
        // Position
        VkVertexInputAttributeDescription positionDescription = attributeDescriptions.get(0);
        positionDescription.binding(0).location(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT).offset(POSITION_OFFSET);
        // Texture coordinates
        VkVertexInputAttributeDescription texturesCoordinatesDescription = attributeDescriptions.get(1);
        texturesCoordinatesDescription.binding(0).location(1)
                .format(VK_FORMAT_R32G32_SFLOAT).offset(TEXTURE_OFFSET);
        return attributeDescriptions.rewind();
    }

    // Class to handle compiled shaders
    static class SPIRV implements NativeResource {

        private final long handle;
        private ByteBuffer bytecode;

        public SPIRV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public void free() {
            shaderc_result_release(handle);
            bytecode = null;
        }

    }

}
