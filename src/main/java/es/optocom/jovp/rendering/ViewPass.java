package es.optocom.jovp.rendering;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_into_spv;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_initialize;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_bytes;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_compilation_status;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_error_message;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_release;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ZERO;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_BACK_BIT;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_LOGIC_OP_COPY;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;
import static org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;

import es.optocom.jovp.definitions.ShaderKind;

/**
 * Creates all the elements to render on a viewport
 *
 * @since 0.0.1
 */
class ViewPass {

    static final int VERTEX_FORMAT = VK_FORMAT_R32G32B32_SFLOAT;
    static final int VERTEX_OFFSET = 0;
    static final int TEXTURE_FORMAT = VK_FORMAT_R32G32_SFLOAT;
    static final int TEXTURE_OFFSET = 3 * Float.BYTES;
    static final int PRIMITIVE_TOPOLOGY = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    static final boolean PRIMITIVE_RESTART_ENABLE = false;
    static final float VIEWPORT_MIN_DEPTH = 0.0f;
    static final float VIEWPORT_MAX_DEPTH = 1.0f;
    static final boolean DEPTH_CLAMP_ENABLE = false;
    static final boolean RASTERIZER_DISCARD_ENABLE = false;
    static final int POLYGON_MODE = VK_POLYGON_MODE_FILL;
    static final float LINE_WIDTH = 1.0f;
    static final int CULL_MODE = VK_CULL_MODE_BACK_BIT;
    static final int FRONT_FACE = VK_FRONT_FACE_COUNTER_CLOCKWISE;
    static final boolean DEPTH_BIAS_ENABLE = false;
    static final boolean SAMPLE_SHADING_ENABLE = true;
    static final float MIN_SAMPLE_SHADING = 0.2f;
    static final boolean DEPTH_TEST_ENABLE = true;
    static final boolean DEPTH_WRITE_ENABLE = true;
    static final int DEPTH_COMPARE_OPERATION = VK_COMPARE_OP_LESS;
    static final boolean DEPTH_BOUNDS_TEST_ENABLE = false;
    static final boolean STENCIL_TEST_ENABLE = false;
    static final int COLOR_WRITE_MASK = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    static final boolean BLEND_ENABLE = true;
    static final int BLEND_COLOR_SOURCE_FACTOR = VK_BLEND_FACTOR_SRC_ALPHA;
    static final int BLEND_COLOR_DESTINATION_FACTOR = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    static final int BLEND_COLOR_OPERATION = VK_BLEND_OP_ADD;
    static final int BLEND_ALPHA_SOURCE_FACTOR = VK_BLEND_FACTOR_ONE;
    static final int BLEND_ALPHA_DESTINATION_FACTOR = VK_BLEND_FACTOR_ZERO;
    static final int BLEND_ALPHA_OPERATION = VK_BLEND_OP_ADD;
    static final boolean LOGIC_OPERATION_ENABLE = false;
    static final int LOGIC_OPERATION = VK_LOGIC_OP_COPY;
    static final float BLEND_CONSTANTS_X = 0.0f;
    static final float BLEND_CONSTANTS_Y = 0.0f;
    static final float BLEND_CONSTANTS_Z = 0.0f;
    static final float BLEND_CONSTANTS_W = 0.0f;

    final VkExtent2D extent;
    long graphicsPipelineLayout;
    long graphicsPipeline;
    long textPipelineLayout;
    long textPipeline;

    /**
     * Creates a single view pass for monocular or stereoscopic view
     *
     * @param renderPass Render pass handle
     * @param offset Offset of the view. For the right eye, it should be the
     *               half width of the swap chain
     * @param extent Port view extent
     *
     * @since 0.0.1
     */
    ViewPass(long renderPass, int offset, VkExtent2D extent) {
        this.extent = extent;
        createGraphicsPipeline(renderPass, offset, extent);
        createTextPipeline(renderPass, offset, extent);
    }

    /** create graphics pipeline */
    private void createGraphicsPipeline(long renderPass, int offset, VkExtent2D extent) {
        // get resources
        SPIRV vertShaderSPIRV = compileShaderFile("es/optocom/jovp/shaders/shader.vert", ShaderKind.VERTEX_SHADER);
        SPIRV fragShaderSPIRV = compileShaderFile("es/optocom/jovp/shaders/shader.frag", ShaderKind.FRAGMENT_SHADER);
        long vertShaderModule = createShaderModule(vertShaderSPIRV.bytecode());
        long fragShaderModule = createShaderModule(fragShaderSPIRV.bytecode());
        try (MemoryStack stack = stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer shaderStages = createShaderStages(stack, vertShaderModule, fragShaderModule);
            VkPipelineVertexInputStateCreateInfo vertexInput = createVertexStage(stack);
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = createAssemblyStage(stack);
            VkPipelineViewportStateCreateInfo viewportState = createViewPortState(stack, offset, extent);
            VkPipelineRasterizationStateCreateInfo rasterizer = createGraphicsRasterizer(stack);
            VkPipelineMultisampleStateCreateInfo multisampling = createMultisampling(stack);
            VkPipelineDepthStencilStateCreateInfo depthStencil = createGraphicsDepthStencil(stack);
            VkPipelineColorBlendStateCreateInfo colorBlending = createColorBlending(stack);
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(stack.longs(VulkanSetup.logicalDevice.descriptorSetLayout));
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            int result = vkCreatePipelineLayout(VulkanSetup.logicalDevice.device, pipelineLayoutInfo,
                    null, pPipelineLayout);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create pipeline layout: " + VulkanSetup.translateVulkanResult(result));
            graphicsPipelineLayout = pPipelineLayout.get(0);
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).pStages(shaderStages)
                .pVertexInputState(vertexInput).pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState).pRasterizationState(rasterizer)
                .pMultisampleState(multisampling).pDepthStencilState(depthStencil)
                .pColorBlendState(colorBlending).layout(graphicsPipelineLayout)
                .renderPass(renderPass).subpass(0)
                .basePipelineHandle(VK_NULL_HANDLE).basePipelineIndex(-1);
            LongBuffer pPipeline = stack.mallocLong(1);
            result = vkCreateGraphicsPipelines(VulkanSetup.logicalDevice.device, VK_NULL_HANDLE, pipelineInfo,
                    null, pPipeline);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create graphics pipeline: " + VulkanSetup.translateVulkanResult(result));
            graphicsPipeline = pPipeline.get(0);
        }
        // Release resources
        vkDestroyShaderModule(VulkanSetup.logicalDevice.device, vertShaderModule, null);
        vkDestroyShaderModule(VulkanSetup.logicalDevice.device, fragShaderModule, null);
        vertShaderSPIRV.free();
        fragShaderSPIRV.free();
    }

    /** create overlay text pipeline */
    private void createTextPipeline(long renderPass, int offset, VkExtent2D extent) {
        // get resources
        SPIRV vertShaderSPIRV = compileShaderFile("es/optocom/jovp/shaders/text.vert", ShaderKind.VERTEX_SHADER);
        SPIRV fragShaderSPIRV = compileShaderFile("es/optocom/jovp/shaders/text.frag", ShaderKind.FRAGMENT_SHADER);
        long vertShaderModule = createShaderModule(vertShaderSPIRV.bytecode());
        long fragShaderModule = createShaderModule(fragShaderSPIRV.bytecode());
        try (MemoryStack stack = stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer shaderStages = createShaderStages(stack, vertShaderModule, fragShaderModule);
            VkPipelineVertexInputStateCreateInfo vertexInput = createVertexStage(stack);
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = createAssemblyStage(stack);
            VkPipelineViewportStateCreateInfo viewportState = createViewPortState(stack, offset, extent);
            VkPipelineRasterizationStateCreateInfo rasterizer = createTextRasterizer(stack);
            VkPipelineMultisampleStateCreateInfo multisampling = createMultisampling(stack);
            VkPipelineDepthStencilStateCreateInfo depthStencil = createTextDepthStencil(stack);
            VkPipelineColorBlendStateCreateInfo colorBlending = createColorBlending(stack);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(stack.longs(VulkanSetup.logicalDevice.descriptorSetLayout));
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            int result = vkCreatePipelineLayout(VulkanSetup.logicalDevice.device, pipelineLayoutInfo,
                    null, pPipelineLayout);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create pipeline layout: " + VulkanSetup.translateVulkanResult(result));
            textPipelineLayout = pPipelineLayout.get(0);
            VkGraphicsPipelineCreateInfo.Buffer textPipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).pStages(shaderStages)
                    .pVertexInputState(vertexInput).pInputAssemblyState(inputAssembly)
                    .pViewportState(viewportState).pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling).pDepthStencilState(depthStencil)
                    .pColorBlendState(colorBlending).layout(textPipelineLayout)
                    .renderPass(renderPass).subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE).basePipelineIndex(-1);
            LongBuffer pPipeline = stack.mallocLong(1);
            result = vkCreateGraphicsPipelines(VulkanSetup.logicalDevice.device, VK_NULL_HANDLE, textPipelineInfo,
                    null, pPipeline);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create graphics pipeline: " + VulkanSetup.translateVulkanResult(result));
            textPipeline = pPipeline.get(0);
        }
        // Release resources
        vkDestroyShaderModule(VulkanSetup.logicalDevice.device, vertShaderModule, null);
        vkDestroyShaderModule(VulkanSetup.logicalDevice.device, fragShaderModule, null);
        vertShaderSPIRV.free();
        fragShaderSPIRV.free();
    }

    /** Shader stages */
    private VkPipelineShaderStageCreateInfo.Buffer createShaderStages(MemoryStack stack, long vert, long frag) {
        ByteBuffer entryPoint = stack.UTF8("main");
        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        shaderStages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                                 .stage(VK_SHADER_STAGE_VERTEX_BIT).module(vert).pName(entryPoint);
        shaderStages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                                 .stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(frag).pName(entryPoint);
        return shaderStages;
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
        try {
            InputStream inputStream = ViewPass.class.getClassLoader().getResourceAsStream(shaderFile);
            String source = IOUtils.toString(inputStream, String.valueOf(StandardCharsets.UTF_8));
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException e) {
            throw new RuntimeException("Cannot compile shader file", e);
        }
    }

    /** compile shader */
    private static SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {
        long compiler = shaderc_compiler_initialize();
        if (compiler == NULL)
            throw new RuntimeException("Failed to create shader compiler");
        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind,
                filename, "main", NULL);
        if (result == NULL)
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success)
            throw new RuntimeException("Failed to compile shader " + filename + "into SPIR-V:\n " +
                    shaderc_result_get_error_message(result));
        shaderc_compiler_release(compiler);
        return new SPIRV(result, shaderc_result_get_bytes(result));
    }

    /** Vertex stage */
    private VkPipelineVertexInputStateCreateInfo createVertexStage(MemoryStack stack) {
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            .pVertexBindingDescriptions(getBindingDescription())
            .pVertexAttributeDescriptions(getAttributeDescriptions());
    }

    /** Assembly stage */
    private VkPipelineInputAssemblyStateCreateInfo createAssemblyStage(MemoryStack stack) {
        return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            .topology(PRIMITIVE_TOPOLOGY)
            .primitiveRestartEnable(PRIMITIVE_RESTART_ENABLE);
    }

    /** create viewport state */
    private VkPipelineViewportStateCreateInfo createViewPortState(MemoryStack stack, int offset, VkExtent2D extent) {
        VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
            .x(offset).y(0).width(extent.width()).height(extent.height())
            .minDepth(VIEWPORT_MIN_DEPTH).maxDepth(VIEWPORT_MAX_DEPTH);
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
            .offset(VkOffset2D.calloc(stack).set(offset, 0)).extent(extent);
        return VkPipelineViewportStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            .pViewports(viewport).pScissors(scissor);
    }

    /** create graphics rasterizer */
    private VkPipelineRasterizationStateCreateInfo createGraphicsRasterizer(MemoryStack stack) {
        return VkPipelineRasterizationStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            .depthClampEnable(DEPTH_CLAMP_ENABLE)
            .rasterizerDiscardEnable(RASTERIZER_DISCARD_ENABLE)
            .polygonMode(POLYGON_MODE).lineWidth(LINE_WIDTH)
            .cullMode(CULL_MODE).frontFace(FRONT_FACE)
            .depthBiasEnable(DEPTH_BIAS_ENABLE);
    }

    /** create overlay text rasterizer */
    private VkPipelineRasterizationStateCreateInfo createTextRasterizer(MemoryStack stack) {
        return VkPipelineRasterizationStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            .depthClampEnable(DEPTH_CLAMP_ENABLE)
            .rasterizerDiscardEnable(RASTERIZER_DISCARD_ENABLE)
            .polygonMode(POLYGON_MODE).lineWidth(LINE_WIDTH)
            .cullMode(VK_CULL_MODE_NONE).frontFace(VK_FRONT_FACE_CLOCKWISE)
            .depthBiasEnable(DEPTH_BIAS_ENABLE);
    }


    /** destroy view pass object */
    void destroy() {
        vkDestroyPipeline(VulkanSetup.logicalDevice.device, textPipeline, null);
        vkDestroyPipelineLayout(VulkanSetup.logicalDevice.device, textPipelineLayout, null);
        vkDestroyPipeline(VulkanSetup.logicalDevice.device, graphicsPipeline, null);
        vkDestroyPipelineLayout(VulkanSetup.logicalDevice.device, graphicsPipelineLayout, null);
    }

    /** create multisampling */
    private VkPipelineMultisampleStateCreateInfo createMultisampling(MemoryStack stack) {
        return VkPipelineMultisampleStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            .sampleShadingEnable(SAMPLE_SHADING_ENABLE)
            .minSampleShading(MIN_SAMPLE_SHADING)
            .rasterizationSamples(VulkanSetup.logicalDevice.msaaSamples);
    }

    /** create graphics depth stencil */
    private VkPipelineDepthStencilStateCreateInfo createGraphicsDepthStencil(MemoryStack stack) {
        return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
            .depthTestEnable(DEPTH_TEST_ENABLE)
            .depthWriteEnable(DEPTH_WRITE_ENABLE)
            .depthCompareOp(DEPTH_COMPARE_OPERATION)
            .depthBoundsTestEnable(DEPTH_BOUNDS_TEST_ENABLE)
            .stencilTestEnable(STENCIL_TEST_ENABLE);
    }

    /** create overlay text depth stencil */
    private VkPipelineDepthStencilStateCreateInfo createTextDepthStencil(MemoryStack stack) {
        return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
            .depthTestEnable(false).depthWriteEnable(false)
            .depthCompareOp(VK_COMPARE_OP_ALWAYS)
            .depthBoundsTestEnable(false).stencilTestEnable(false);
    }

    /** color blending */
    private VkPipelineColorBlendStateCreateInfo createColorBlending(MemoryStack stack) {
        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState
            .calloc(1, stack).colorWriteMask(COLOR_WRITE_MASK).blendEnable(BLEND_ENABLE)
            .srcColorBlendFactor(BLEND_COLOR_SOURCE_FACTOR)
            .dstColorBlendFactor(BLEND_COLOR_DESTINATION_FACTOR)
            .colorBlendOp(BLEND_COLOR_OPERATION)
            .srcAlphaBlendFactor(BLEND_ALPHA_SOURCE_FACTOR)
            .dstAlphaBlendFactor(BLEND_ALPHA_DESTINATION_FACTOR)
            .alphaBlendOp(BLEND_ALPHA_OPERATION);
        return VkPipelineColorBlendStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            .logicOpEnable(LOGIC_OPERATION_ENABLE).logicOp(LOGIC_OPERATION)
            .pAttachments(colorBlendAttachment)
            .blendConstants(stack.floats(BLEND_CONSTANTS_X,BLEND_CONSTANTS_Y, BLEND_CONSTANTS_Z,BLEND_CONSTANTS_W));
    }

    /** get vertex input binding description */
    public static VkVertexInputBindingDescription.Buffer getBindingDescription() {
        return VkVertexInputBindingDescription.calloc(1)
                .binding(0).stride(VulkanSetup.MODEL_SIZEOF).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
    }

    /** get attribute descriptions */
    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(2);
        // Position
        VkVertexInputAttributeDescription positionDescription = attributeDescriptions.get(0);
        positionDescription.binding(0).location(0).format(VERTEX_FORMAT).offset(VERTEX_OFFSET);
        // Texture coordinates
        VkVertexInputAttributeDescription texturesCoordinatesDescription = attributeDescriptions.get(1);
        texturesCoordinatesDescription.binding(0).location(1)
                .format(TEXTURE_FORMAT).offset(TEXTURE_OFFSET);
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
