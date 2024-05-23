package es.optocom.jovp.rendering;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBindBufferMemory;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdBlitImage;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateBuffer;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFormatProperties;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import es.optocom.jovp.definitions.ViewEye;
import es.optocom.jovp.definitions.Vertex;

abstract class Renderable {

    static final int UNIFORM_SIZEOF = 88 * Float.BYTES;
    static final int UNIFORM_TEXTSIZEOF = 44 * Float.BYTES;
    static final int SAMPLER_FILTER = VK_FILTER_NEAREST;
    static final int SAMPLER_ADDRESS_MODE = VK_SAMPLER_ADDRESS_MODE_REPEAT;
    static final int SAMPLER_BORDER_COLOR = VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK;
    static final int SAMPLER_COMPARISONS = VK_COMPARE_OP_ALWAYS;
    static final int SAMPLER_MIPMAP_MODE = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    static final int SAMPLER_MIPMAP_FILTER = VK_FILTER_LINEAR;
    static final int SAMPLER_COLOR_FORMAT = VK_FORMAT_R32G32B32A32_SFLOAT;
    static final float SAMPLER_MAX_ANISOTROPY = 16.0f;

    ViewEye viewEye;
    private Model model;
    private Texture texture;

    private long vertexBuffer;
    private long indexBuffer;
    private long vertexBufferMemory;
    private long indexBufferMemory;
    private long textureSampler;
    private long textureImage;
    private long textureImageMemory;
    private long textureImageView;
    private long descriptorPool;
    List<Long> uniformBuffers;
    List<Long> uniformBuffersMemory;
    List<Long> descriptorSets;

    boolean updateModel = false;
    boolean updateTexture = false;

    /**
     * 
     * Create a renderable object for psychophysics experience
     *
     * @since 0.0.1
     */
    public Renderable() {
        this(new Model(), new Texture());
    }

    /**
     * 
     * Create a renderable object for psychophysics experience
     *
     * @param model   The model (square, circle, etc)
     * @param texture The texture
     *
     * @since 0.0.1
     */
    public Renderable(Model model, Texture texture) {
        this.viewEye = ViewEye.BOTH;
        this.model = model;
        this.texture = texture;
        createBuffers();
    }

    /**
     * 
     * Set texture color
     *
     * @param rgba The RGBA channels to use
     *
     * @since 0.0.1
     */
    public void setColor(double[] rgba) {
        texture.setColor(rgba);
    }

    /**
     * 
     * Set texture minimum color for grids
     *
     * @param rgbaMin The RGBA values of the minimum color
     * @param rgbaMax The RGBA values of the maximum color
     *
     * @since 0.0.1
     */
    public void setColors(double[] rgbaMin, double[] rgbaMax) {
        texture.setColors(rgbaMin, rgbaMax);
    }

    /**
     * 
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void destroy() {
        destroyDescriptors();
        destroyTextureObjects();
        destroyModelObjects();
        model.destroy();
        texture.destroy();
    }

    /**
     * 
     * Recreate buffers
     * 
     * @param model   The new model
     * @param texture The new texture
     *
     * @since 0.0.1
     */
    public void update(Model model, Texture texture) {
        this.model = model;
        this.texture = texture;
        if (VulkanSetup.commandPool != 0) {
            updateModel = true;
            updateTexture = true;
        }
    }

    /**
     * 
     * Recreate buffers
     * 
     * @param model The new model
     *
     * @since 0.0.1
     */
    public void update(Model model) {
        this.model = model;
        if (VulkanSetup.commandPool != 0) updateModel = true;
    }

    /**
     * 
     * Recreate buffers
     * 
     * @param texture The new texture
     *
     * @since 0.0.1
     */
    public void update(Texture texture) {
        this.texture = texture;
        if (VulkanSetup.commandPool != 0) updateTexture = true;
    }

    /**
     * 
     * Set eye where to render the item
     *
     * @param viewEye Eye to display
     *
     * @since 0.0.1
     */
    public void show(ViewEye viewEye) {
        this.viewEye = viewEye;
    }

    /**
     * 
     *
     * @return True if eye to render is not Eye.NONE, false otherwise.
     *
     * @since 0.0.1
     */
    public boolean showing() {
        return this.viewEye != ViewEye.NONE;
    }

    /**
     * 
     * Get eye that is currently showing. (NONE if no eye showing.)
     *
     * @return The eye that is currently showing (Eye.NONE).
     *
     * @since 0.0.1
     */
    public ViewEye getEye() {
        return this.viewEye;
    }

    /**
     * 
     * Get model
     *
     * @return The model
     *
     * @since 0.0.1
     */
    public Model getModel() {
        return model;
    }

    /**
     * 
     * Get texture
     *
     * @return The texture
     *
     * @since 0.0.1
     */
    public Texture getTexture() {
        return texture;
    }

    /**
     * 
     * Render item or text
     *
     * @param stack Memory stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     *
     * @since 0.0.1
     */
    abstract void render(MemoryStack stack, VkCommandBuffer commandBuffer, int image);

    /**
     * 
     * Render item for a specific eye
     * 
     * @param stack  stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     * @param pipeline pipeline
     * @param pipelineLayout pipeline layout
     *
     * @since 0.0.1
     */
    void draw(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int eye, long pipeline, long pipelineLayout) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        if (updateModel) updateModel();
        if (updateTexture) updateTexture();
        LongBuffer vertexBuffers = stack.longs(vertexBuffer);
        LongBuffer offsets = stack.longs(0);
        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
        vkCmdBindIndexBuffer(commandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT32);
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(descriptorSets.get(image + VulkanSetup.swapChain.images.size() * eye)), null);
        vkCmdDrawIndexed(commandBuffer, model.indices.length, 1, 0, 0, 0);
    }

    /**
     * 
     * Get uniform buffer memory
     * 
     * @since 0.0.1
     */
    long getUniformMemory(int index, int eye) {
        return uniformBuffersMemory.get(index + VulkanSetup.swapChain.images.size() * eye);
    }

    /**
     * Create buffers for the model on request
     *
     * @since 0.0.1
     */
    void createBuffers() {
        // only if psychoEngine has started and have not yet been created
        if (VulkanSetup.physicalDevice == null | VulkanSetup.commandPool == 0) return;
        createModelObjects();
        createTextureObjects();
        createDescriptors();
    }

    /** create vertex and index buffers */
    private void createModelObjects() {
        createVertexBuffer();
        createIndexBuffer();
    }

    /** create texture image and sampler */
    private void createTextureObjects() {
        createTextureSampler();
        createTextureImage();
    }

    /** create texture image and sampler */
    private void createDescriptors() {
        createDescriptorPool();
        createUniformBuffers();
        createDescriptorSets();
    }

    /**
     * Update model buffers
     * 
     * @since 0.0.1
     */
    private void updateModel() {
        destroyModelObjects();
        createModelObjects();
        updateModel = false;
    }

    /**
     * 
     * Update texture buffers
     * 
     * @since 0.0.1
     */
    private void updateTexture() {
        destroyDescriptors();
        destroyTextureObjects();
        createTextureObjects();
        createDescriptors();
        updateTexture = false;
    }

    /** destroy texture image and sampler */
    private void destroyDescriptors() {
        uniformBuffers.forEach(ubo -> vkDestroyBuffer(VulkanSetup.logicalDevice.device, ubo, null));
        uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(VulkanSetup.logicalDevice.device, uboMemory, null));
        vkDestroyDescriptorPool(VulkanSetup.logicalDevice.device, descriptorPool, null);
    }

    /** destroy vertex and index buffers */
    private void destroyModelObjects() {
        vkDestroyBuffer(VulkanSetup.logicalDevice.device, indexBuffer, null);
        vkFreeMemory(VulkanSetup.logicalDevice.device, indexBufferMemory, null);
        vkDestroyBuffer(VulkanSetup.logicalDevice.device, vertexBuffer, null);
        vkFreeMemory(VulkanSetup.logicalDevice.device, vertexBufferMemory, null);
    }

    /** destroy texture image and sampler */
    private void destroyTextureObjects() {
        vkDestroySampler(VulkanSetup.logicalDevice.device, textureSampler, null);
        vkDestroyImageView(VulkanSetup.logicalDevice.device, textureImageView, null);
        vkDestroyImage(VulkanSetup.logicalDevice.device, textureImage, null);
        vkFreeMemory(VulkanSetup.logicalDevice.device, textureImageMemory, null);
    }

    /** create vertex buffer */
    private void createVertexBuffer() {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = (long) VulkanSetup.MODEL_SIZEOF * model.indices.length;
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pBufferMemory);
            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);
            PointerBuffer data = stack.mallocPointer(1);
            int result = vkMapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, 0, bufferSize, 0, data);
            if (result != VK_SUCCESS) throw new RuntimeException("Failed to map staging buffer memory");
            ByteBuffer buffer = data.getByteBuffer(0, (int) bufferSize);
            for (Vertex vertex : model.vertices)
                buffer.putFloat(vertex.position.x())
                      .putFloat(vertex.position.y())
                      .putFloat(vertex.position.z())
                      .putFloat(vertex.uv.x())
                      .putFloat(vertex.uv.y());
            buffer.flip();
            vkUnmapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, vertexBuffer, bufferSize);
            vkDestroyBuffer(VulkanSetup.logicalDevice.device, stagingBuffer, null);
            vkFreeMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, null);
        }
    }

    /** create index buffer */
    private void createIndexBuffer() {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = (long) Integer.BYTES * model.indices.length;
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pBufferMemory);
            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);
            PointerBuffer data = stack.mallocPointer(1);
            int result = vkMapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, 0, bufferSize, 0, data);
            if (result != VK_SUCCESS) throw new RuntimeException("Failed to map staging buffer memory");
            ByteBuffer buffer = data.getByteBuffer(0, (int) bufferSize);
            for (int index : model.indices) buffer.putInt(index);
            buffer.flip();
            vkUnmapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            indexBuffer = pBuffer.get(0);
            indexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, indexBuffer, bufferSize);
            vkDestroyBuffer(VulkanSetup.logicalDevice.device, stagingBuffer, null);
            vkFreeMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, null);
        }
    }

    /** create texture sampler */
    private void createTextureSampler() {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(SAMPLER_FILTER)
                    .minFilter(SAMPLER_FILTER)
                    .addressModeU(SAMPLER_ADDRESS_MODE)
                    .addressModeV(SAMPLER_ADDRESS_MODE)
                    .addressModeW(SAMPLER_ADDRESS_MODE)
                    .anisotropyEnable(true)
                    .maxAnisotropy(SAMPLER_MAX_ANISOTROPY)
                    .borderColor(SAMPLER_BORDER_COLOR)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(SAMPLER_COMPARISONS)
                    .mipmapMode(SAMPLER_MIPMAP_MODE)
                    .minLod(0)
                    .maxLod((float) texture.getMipLevels())
                    .mipLodBias(0);
            LongBuffer pTextureSampler = stack.mallocLong(1);
            int result = vkCreateSampler(VulkanSetup.logicalDevice.device, samplerInfo, null, pTextureSampler);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create texture sampler: " + VulkanSetup.translateVulkanResult(result));
            textureSampler = pTextureSampler.get(0);
        }
    }

    /** create texture image */
    private void createTextureImage() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);
            createBuffer(texture.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pStagingBuffer, pStagingBufferMemory);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0), 0, texture.size, 0, data);
            ByteBuffer buffer = data.getByteBuffer(0, texture.size);
            for (float pixel : texture.getPixels()) buffer.putFloat(pixel);
            buffer.flip();
            vkUnmapMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0));
            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            VulkanSetup.createImage(texture.width, texture.height, texture.mipLevels, VK_SAMPLE_COUNT_1_BIT, SAMPLER_COLOR_FORMAT, VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, pTextureImage, pTextureImageMemory);
            textureImage = pTextureImage.get(0);
            textureImageMemory = pTextureImageMemory.get(0);
            VulkanSetup.transitionImageLayout(VulkanSetup.commandPool, textureImage, SAMPLER_COLOR_FORMAT, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, texture.mipLevels);
            copyBufferToImage(pStagingBuffer.get(0), textureImage, texture.width, texture.height);
            generateMipmaps(VulkanSetup.logicalDevice, texture, textureImage);
            vkDestroyBuffer(VulkanSetup.logicalDevice.device, pStagingBuffer.get(0), null);
            vkFreeMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0), null);
        }
        textureImageView = VulkanSetup.createImageView(VulkanSetup.logicalDevice.device, textureImage, SAMPLER_COLOR_FORMAT, VK_IMAGE_ASPECT_COLOR_BIT, texture.mipLevels);
    }

    /** create descriptor pool */
    private void createDescriptorPool() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(2, stack);
            VkDescriptorPoolSize uniformBufferPoolSize = poolSize.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(2 * VulkanSetup.swapChain.images.size());
            VkDescriptorPoolSize textureSamplerPoolSize = poolSize.get(1);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(2 *VulkanSetup.swapChain.images.size());
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSize)
                    .maxSets(2 * VulkanSetup.swapChain.images.size());
            LongBuffer pDescriptorPool = stack.mallocLong(1);
            int result = vkCreateDescriptorPool(VulkanSetup.logicalDevice.device, poolInfo, null, pDescriptorPool);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create descriptor pool: " + VulkanSetup.translateVulkanResult(result));
            descriptorPool = pDescriptorPool.get(0);
        }
    }

    /** create uniform buffers */
    private void createUniformBuffers() {
        try (MemoryStack stack = stackPush()) {
            uniformBuffers = new ArrayList<>(2 * VulkanSetup.swapChain.images.size());
            uniformBuffersMemory = new ArrayList<>(2 * VulkanSetup.swapChain.images.size());
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            for (int i = 0; i < 2 * VulkanSetup.swapChain.images.size(); i++) {
                createBuffer(UNIFORM_SIZEOF, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pBufferMemory);
                uniformBuffers.add(pBuffer.get(0));
                uniformBuffersMemory.add(pBufferMemory.get(0));
            }
        }
    }

    /** create descriptor sets */
    private void createDescriptorSets() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer layouts = stack.mallocLong(2 * VulkanSetup.swapChain.images.size());
            for (int i = 0; i < layouts.capacity(); i++) layouts.put(i, VulkanSetup.logicalDevice.descriptorSetLayout);
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(layouts);
            LongBuffer pDescriptorSets = stack.mallocLong(2 * VulkanSetup.swapChain.images.size());
            int result = vkAllocateDescriptorSets(VulkanSetup.logicalDevice.device, allocInfo, pDescriptorSets);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to allocate descriptor sets: " + VulkanSetup.translateVulkanResult(result));
            descriptorSets = new ArrayList<>(pDescriptorSets.capacity());
            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .offset(0)
                    .range(UNIFORM_SIZEOF);
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(textureImageView)
                    .sampler(textureSampler);
            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);
            VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(0);
            uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);
            VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(1);
            samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstBinding(1)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);
            for (int eye = 0; eye < 2; eye++) {
                for (int frame = 0; frame < VulkanSetup.swapChain.frameBuffers.size(); frame++) {
                    int index = eye * VulkanSetup.swapChain.frameBuffers.size() + frame;
                    long descriptorSet = pDescriptorSets.get(index);
                    bufferInfo.buffer(uniformBuffers.get(index));
                    uboDescriptorWrite.dstSet(descriptorSet);
                    samplerDescriptorWrite.dstSet(descriptorSet);
                    vkUpdateDescriptorSets(VulkanSetup.logicalDevice.device, descriptorWrites, null);
                    descriptorSets.add(descriptorSet);
                }
            }
        }
    }

    /** generate mipmaps */
    private void generateMipmaps(LogicalDevice logicalDevice, Texture texture, long image) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties formatProperties = VkFormatProperties.malloc(stack);
            vkGetPhysicalDeviceFormatProperties(logicalDevice.device.getPhysicalDevice(), SAMPLER_COLOR_FORMAT, formatProperties);
            if ((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0)
                throw new RuntimeException("Texture image format does not support linear blitting");
            VkCommandBuffer commandBuffer = VulkanSetup.beginCommand(VulkanSetup.commandPool);
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .image(image)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstAccessMask(VK_QUEUE_FAMILY_IGNORED);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseArrayLayer(0)
                    .layerCount(1)
                    .levelCount(1);
            int mipWidth = texture.width;
            int mipHeight = texture.height;
            for (int i = 1; i < texture.mipLevels; i++) {
                barrier.subresourceRange().baseMipLevel(i - 1);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                       .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                       .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                       .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0, null, null, barrier);
                VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
                blit.srcOffsets(0).set(0, 0, 0);
                blit.srcOffsets(1).set(mipWidth, mipHeight, 1);
                blit.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(i - 1)
                    .baseArrayLayer(0)
                    .layerCount(1);
                blit.dstOffsets(0).set(0, 0, 0);
                blit.dstOffsets(1).set(mipWidth > 1 ? mipWidth / 2 : 1, mipHeight > 1 ? mipHeight / 2 : 1, 1);
                blit.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(i)
                    .baseArrayLayer(0)
                    .layerCount(1);
                vkCmdBlitImage(commandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, blit, SAMPLER_MIPMAP_FILTER);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                       .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                       .srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                       .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier);
                if (mipWidth > 1) mipWidth /= 2;
                if (mipHeight > 1) mipHeight /= 2;
            }
            barrier.subresourceRange().baseMipLevel(texture.mipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                   .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                   .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                   .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier);
            VulkanSetup.endCommand(VulkanSetup.commandPool, commandBuffer);
        }
    }

    /** copy buffer */
    private void copyBuffer(long srcBuffer, long dstBuffer, long size) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = VulkanSetup.beginCommand(VulkanSetup.commandPool);
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack);
            copyRegion.size(size);
            vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);
            VulkanSetup.endCommand(VulkanSetup.commandPool, commandBuffer);
        }
    }

    /** copy buffer to image */
    private void copyBufferToImage(long buffer, long image, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = VulkanSetup.beginCommand(VulkanSetup.commandPool);
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0).bufferRowLength(0).bufferImageHeight(0);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));
            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
            VulkanSetup.endCommand(VulkanSetup.commandPool, commandBuffer);
        }
    }


    /** create buffer */
    private static void createBuffer(long size, int usage, int properties, LongBuffer pBuffer, LongBuffer pBufferMemory) {
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            int result = vkCreateBuffer(VulkanSetup.logicalDevice.device, bufferInfo, null, pBuffer);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to create buffer: " + VulkanSetup.translateVulkanResult(result));
            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(VulkanSetup.logicalDevice.device, pBuffer.get(0), memRequirements);
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(VulkanSetup.findMemoryType(memRequirements.memoryTypeBits(), properties));
            result = vkAllocateMemory(VulkanSetup.logicalDevice.device, allocInfo, null, pBufferMemory);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to allocate buffer memory: " + VulkanSetup.translateVulkanResult(result));
            vkBindBufferMemory(VulkanSetup.logicalDevice.device, pBuffer.get(0), pBufferMemory.get(0), 0);
        }
    }

}
