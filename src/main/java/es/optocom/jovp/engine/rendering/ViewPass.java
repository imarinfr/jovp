package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.ShaderKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static es.optocom.jovp.engine.rendering.VulkanSetup.*;
import static es.optocom.jovp.engine.structures.ShaderKind.FRAGMENT_SHADER;
import static es.optocom.jovp.engine.structures.ShaderKind.VERTEX_SHADER;
import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 *
 * ViewPass
 *
 * <ul>
 * <li>View Pass</li>
 * Creates all the elements to render on a viewport viewport
 * </ul>
 *
 * @since 0.0.1
 */
class ViewPass {

    VkExtent2D extent;
    long pipelineLayout;
    long graphicsPipeline;

    /**
     *
     * Creates a single view pass for monocular view
     *
     * @param renderPass Render pass handle
     * @param extent Port view extent
     *
     *
     * @since 0.0.1
     */
    ViewPass(long renderPass, VkExtent2D extent) {
        this(renderPass, 0, extent);
    }

    /**
     *
     * Creates a single view pass for monocular or stereoscopic view
     *
     * @param renderPass Render pass handle
     * @param offset Offset of the view. For the right eye, it should be the half width of the swap chain
     * @param extent Port view extent
     *
     *
     * @since 0.0.1
     */
    ViewPass(long renderPass, int offset, VkExtent2D extent) {
        this.extent = extent;
        createGraphicsPipeline(renderPass, offset);
    }

    void destroy() {
        vkDestroyPipeline(logicalDevice.device, graphicsPipeline, null);
        vkDestroyPipelineLayout(logicalDevice.device, pipelineLayout, null);
    }

    // Create graphics pipeline
    private void createGraphicsPipeline(long renderPass, int offset) {
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
                    .stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertShaderModule).pName(entryPoint);
            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragShaderModule).pName(entryPoint);
            // Vertex stage
            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(getBindingDescription())
                    .pVertexAttributeDescriptions(getAttributeDescriptions());
            // Assembly stage
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(PRIMITIVE_TOPOLOGY).primitiveRestartEnable(PRIMITIVE_RESTART_ENABLE);
            // Viewport and scissor
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(offset).y(0).width(extent.width()).height(extent.height())
                    .minDepth(VIEWPORT_MIN_DEPTH).maxDepth(VIEWPORT_MAX_DEPTH);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .offset(VkOffset2D.calloc(stack).set(offset, 0)).extent(extent);
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewport).pScissors(scissor);
            // Rasterization stage
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(DEPTH_CLAMP_ENABLE).rasterizerDiscardEnable(RASTERIZER_DISCARD_ENABLE)
                    .polygonMode(POLYGON_MODE).lineWidth(LINE_WIDTH).cullMode(CULL_MODE).frontFace(FRONT_FACE)
                    .depthBiasEnable(DEPTH_BIAS_ENABLE);
            // Multisampling
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(SAMPLE_SHADING_ENABLE).minSampleShading(MIN_SAMPLE_SHADING)
                    .rasterizationSamples(logicalDevice.msaaSamples);
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(DEPTH_TEST_ENABLE).depthWriteEnable(DEPTH_WRITE_ENABLE)
                    .depthCompareOp(DEPTH_COMPARE_OPERATION).depthBoundsTestEnable(DEPTH_BOUNDS_TEST_ENABLE)
                    .minDepthBounds(MIN_DEPTH_BOUNDS).maxDepthBounds(MAX_DEPTH_BOUNDS)
                    .stencilTestEnable(STENCIL_TEST_ENABLE);
            // Color blending
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack)
                            .colorWriteMask(COLOR_WRITE_MASK).blendEnable(BLEND_ENABLE);
            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(LOGIC_OPERATION_ENABLE).logicOp(LOGIC_OPERATION).pAttachments(colorBlendAttachment)
                    .blendConstants(stack.floats(BLEND_CONSTANTS_X, BLEND_CONSTANTS_Y, BLEND_CONSTANTS_Z,
                            BLEND_CONSTANTS_W));
            // Pipeline layout creation
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(logicalDevice.descriptorSetLayout));
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            int result = vkCreatePipelineLayout(logicalDevice.device, pipelineLayoutInfo,
                    null, pPipelineLayout);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create pipeline layout: " +
                        translateVulkanResult(result));
            pipelineLayout = pPipelineLayout.get(0);
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).pStages(shaderStages)
                    .pVertexInputState(vertexInputInfo).pInputAssemblyState(inputAssembly)
                    .pViewportState(viewportState).pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling).pDepthStencilState(depthStencil)
                    .pColorBlendState(colorBlending).layout(pipelineLayout).renderPass(renderPass).subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE).basePipelineIndex(-1);
            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            result = vkCreateGraphicsPipelines(logicalDevice.device, VK_NULL_HANDLE, pipelineInfo,
                    null, pGraphicsPipeline);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create graphics pipeline: " +
                        translateVulkanResult(result));
            graphicsPipeline = pGraphicsPipeline.get(0);
            // Release resources
            vkDestroyShaderModule(logicalDevice.device, vertShaderModule, null);
            vkDestroyShaderModule(logicalDevice.device, fragShaderModule, null);
            vertShaderSPIRV.free();
            fragShaderSPIRV.free();
        }
    }

    // Create shader module
    private long createShaderModule(ByteBuffer spirvCode) {
        try (MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(spirvCode);
            LongBuffer pShaderModule = stack.mallocLong(1);
            int result = vkCreateShaderModule(logicalDevice.device, createInfo, null, pShaderModule);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create shader module: " +
                        translateVulkanResult(result));
            return pShaderModule.get(0);
        }
    }

    // Compile shader file
    private static SPIRV compileShaderFile(String shaderFile, ShaderKind shaderKind) {
        return compileShaderAbsoluteFile(Objects.requireNonNull(getSystemClassLoader().
                getResource(shaderFile)).toExternalForm(), shaderKind);
    }

    // Compile shader from absolute file
    private static @Nullable SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Compile shader
    private static @NotNull SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {
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
