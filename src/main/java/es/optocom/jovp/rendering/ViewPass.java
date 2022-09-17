package es.optocom.jovp.rendering;

import es.optocom.jovp.structures.ShaderKind;
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

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Creates all the elements to render on a viewport
 *
 * @since 0.0.1
 */
class ViewPass {

    final VkExtent2D extent;
    long pipelineLayout;
    long graphicsPipeline;

    /**
     * Creates a single view pass for monocular view
     *
     * @param renderPass Render pass handle
     * @param extent Port view extent
     *
     * @since 0.0.1
     */
    ViewPass(long renderPass, VkExtent2D extent) {
        this(renderPass, 0, extent);
    }

    /**
     * Creates a single view pass for monocular or stereoscopic view
     *
     * @param renderPass Render pass handle
     * @param offset Offset of the view. For the right eye, it should be the half width of the swap chain
     * @param extent Port view extent
     *
     * @since 0.0.1
     */
    ViewPass(long renderPass, int offset, VkExtent2D extent) {
        this.extent = extent;
        createGraphicsPipeline(renderPass, offset);
    }

    /** destroy view pass object */
    void destroy() {
        vkDestroyPipeline(VulkanSetup.logicalDevice.device, graphicsPipeline, null);
        vkDestroyPipelineLayout(VulkanSetup.logicalDevice.device, pipelineLayout, null);
    }

    /** create graphics pipeline */
    private void createGraphicsPipeline(long renderPass, int offset) {
        try (MemoryStack stack = stackPush()) {
            SPIRV vertShaderSPIRV = compileShaderFile("es/optocom/jovp/shaders/shader.vert", ShaderKind.VERTEX_SHADER);
            SPIRV fragShaderSPIRV = compileShaderFile("es/optocom/jovp/shaders/shader.frag", ShaderKind.FRAGMENT_SHADER);
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
                    .topology(VulkanSetup.PRIMITIVE_TOPOLOGY).primitiveRestartEnable(VulkanSetup.PRIMITIVE_RESTART_ENABLE);
            // Viewport and scissor
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(offset).y(0).width(extent.width()).height(extent.height())
                    .minDepth(VulkanSetup.VIEWPORT_MIN_DEPTH).maxDepth(VulkanSetup.VIEWPORT_MAX_DEPTH);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .offset(VkOffset2D.calloc(stack).set(offset, 0)).extent(extent);
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewport).pScissors(scissor);
            // Rasterization stage
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(VulkanSetup.DEPTH_CLAMP_ENABLE).rasterizerDiscardEnable(VulkanSetup.RASTERIZER_DISCARD_ENABLE)
                    .polygonMode(VulkanSetup.POLYGON_MODE).lineWidth(VulkanSetup.LINE_WIDTH).cullMode(VulkanSetup.CULL_MODE).frontFace(VulkanSetup.FRONT_FACE)
                    .depthBiasEnable(VulkanSetup.DEPTH_BIAS_ENABLE);
            // Multisampling
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(VulkanSetup.SAMPLE_SHADING_ENABLE).minSampleShading(VulkanSetup.MIN_SAMPLE_SHADING)
                    .rasterizationSamples(VulkanSetup.logicalDevice.msaaSamples);
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(VulkanSetup.DEPTH_TEST_ENABLE).depthWriteEnable(VulkanSetup.DEPTH_WRITE_ENABLE)
                    .depthCompareOp(VulkanSetup.DEPTH_COMPARE_OPERATION).depthBoundsTestEnable(VulkanSetup.DEPTH_BOUNDS_TEST_ENABLE)
                    .minDepthBounds(VulkanSetup.MIN_DEPTH_BOUNDS).maxDepthBounds(VulkanSetup.MAX_DEPTH_BOUNDS)
                    .stencilTestEnable(VulkanSetup.STENCIL_TEST_ENABLE);
            // Color blending
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack)
                            .colorWriteMask(VulkanSetup.COLOR_WRITE_MASK).blendEnable(VulkanSetup.BLEND_ENABLE);
            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(VulkanSetup.LOGIC_OPERATION_ENABLE).logicOp(VulkanSetup.LOGIC_OPERATION).pAttachments(colorBlendAttachment)
                    .blendConstants(stack.floats(VulkanSetup.BLEND_CONSTANTS_X, VulkanSetup.BLEND_CONSTANTS_Y, VulkanSetup.BLEND_CONSTANTS_Z,
                            VulkanSetup.BLEND_CONSTANTS_W));
            // Pipeline layout creation
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(VulkanSetup.logicalDevice.descriptorSetLayout));
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            int result = vkCreatePipelineLayout(VulkanSetup.logicalDevice.device, pipelineLayoutInfo,
                    null, pPipelineLayout);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create pipeline layout: " +
                        VulkanSetup.translateVulkanResult(result));
            pipelineLayout = pPipelineLayout.get(0);
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).pStages(shaderStages)
                    .pVertexInputState(vertexInputInfo).pInputAssemblyState(inputAssembly)
                    .pViewportState(viewportState).pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling).pDepthStencilState(depthStencil)
                    .pColorBlendState(colorBlending).layout(pipelineLayout).renderPass(renderPass).subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE).basePipelineIndex(-1);
            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            result = vkCreateGraphicsPipelines(VulkanSetup.logicalDevice.device, VK_NULL_HANDLE, pipelineInfo,
                    null, pGraphicsPipeline);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create graphics pipeline: " +
                        VulkanSetup.translateVulkanResult(result));
            graphicsPipeline = pGraphicsPipeline.get(0);
            // Release resources
            vkDestroyShaderModule(VulkanSetup.logicalDevice.device, vertShaderModule, null);
            vkDestroyShaderModule(VulkanSetup.logicalDevice.device, fragShaderModule, null);
            vertShaderSPIRV.free();
            fragShaderSPIRV.free();
        }
    }

    /** create shader module */
    private long createShaderModule(ByteBuffer spirvCode) {
        try (MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(spirvCode);
            LongBuffer pShaderModule = stack.mallocLong(1);
            int result = vkCreateShaderModule(VulkanSetup.logicalDevice.device, createInfo, null, pShaderModule);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create shader module: " +
                        VulkanSetup.translateVulkanResult(result));
            return pShaderModule.get(0);
        }
    }

    /** compile shader file */
    private static SPIRV compileShaderFile(String shaderFile, ShaderKind shaderKind) {
        return compileShaderAbsoluteFile(Objects.requireNonNull(getSystemClassLoader().
                getResource(shaderFile)).toExternalForm(), shaderKind);
    }

    /** compile shader from absolute file */
    private static SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** compile shader */
    private static SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {
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


    /** get vertex input binding description */
    public static VkVertexInputBindingDescription.Buffer getBindingDescription() {
        return VkVertexInputBindingDescription.calloc(1)
                .binding(0).stride(VulkanSetup.MODEL_SIZEOF).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
    }

    /** get attribute descriptions */
    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.calloc(2);
        // Position
        VkVertexInputAttributeDescription positionDescription = attributeDescriptions.get(0);
        positionDescription.binding(0).location(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT).offset(VulkanSetup.POSITION_OFFSET);
        // Texture coordinates
        VkVertexInputAttributeDescription texturesCoordinatesDescription = attributeDescriptions.get(1);
        texturesCoordinatesDescription.binding(0).location(1)
                .format(VK_FORMAT_R32G32_SFLOAT).offset(VulkanSetup.TEXTURE_OFFSET);
        return attributeDescriptions.rewind();
    }

    /** handle compiled shaders */
    static class SPIRV implements NativeResource {

        private final long handle;
        private ByteBuffer bytecode;

        /** create a SPIRV object */
        public SPIRV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        /** return byte code */
        public ByteBuffer bytecode() {
            return bytecode;
        }

        /** free resources */
        @Override
        public void free() {
            shaderc_result_release(handle);
            bytecode = null;
        }

    }

}