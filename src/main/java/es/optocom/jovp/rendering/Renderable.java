package es.optocom.jovp.rendering;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBlitImage;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
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
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import es.optocom.jovp.definitions.ViewEye;
import es.optocom.jovp.definitions.Vertex;

abstract class Renderable {
    
    ViewEye eye;
    Model model;
    Texture texture;

    long commandPool;
    long vertexBuffer;
    long indexBuffer;
    long vertexBufferMemory;
    long indexBufferMemory;
    long textureImage;
    long textureImageMemory;
    long textureImageView;
    long textureSampler;
    List<Long> uniformBuffers;
    List<Long> uniformBuffersMemory;
    long descriptorPool;
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
        this.eye = ViewEye.BOTH;
        this.model = model;
        this.texture = texture;
        createBuffers();
    }

    /**
     * 
     * Render object
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
     * Render object for a specific eye
     * 
     * @param stack  stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     * @param passNumber pass number. For MONO vision, it ought to be 0. For
     *                   STEREO, left is 0 and right is 1
     *
     * @since 0.0.1
     */
    abstract void renderEye(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int passNumber);

    /**
     *
     * Update uniforms for the object to be rendered
     *
     * @param imageIndex Image where to render
     *
     * @since 0.0.1
     */
    abstract void updateUniforms(int imageIndex, int passNumber);

    /**
     * 
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void destroy() {
        destroyDescriptorObjects();
        destroyTextureObjects();
        destroyModelObjects();
        VulkanSetup.destroyCommandPool(commandPool);
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
        if (commandPool != 0) {
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
        if (commandPool != 0) updateModel = true;
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
        if (commandPool != 0) updateTexture = true;
    }

    /**
     * 
     * Set eye where to render the item
     *
     * @param eye Eye to display
     *
     * @since 0.0.1
     */
    public void show(ViewEye eye) {
        this.eye = eye;
    }

    /**
     * 
     *
     * @return True if eye to render is not Eye.NONE, false otherwise.
     *
     * @since 0.0.1
     */
    public boolean showing() {
        return this.eye != ViewEye.NONE;
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
        return this.eye;
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
     * Create buffers for the model on request
     *
     * @since 0.0.1
     */
    void createBuffers() {
        // only if psychoEngine has started and have not yet been created
        if (VulkanSetup.physicalDevice == null | commandPool != 0) return;
        commandPool = VulkanSetup.createCommandPool();
        createModelObjects();
        createTextureObjects();
        createDescriptorObjects();
    }

    /**
     * Recreate model buffers
     * 
     * @since 0.0.1
     */
    void recreateModel() {
        destroyModelObjects();
        createModelObjects();
        updateModel = false;
    }

    /**
     * Recreate texture buffers
     * 
     * @since 0.0.1
     */
    void recreateTexture() {
        destroyDescriptorObjects();
        destroyTextureObjects();
        createTextureObjects();
        createDescriptorObjects();
        updateTexture = false;
    }

    /** create vertex and index buffers */
    private void createModelObjects() {
        createVertexBuffer();
        createIndexBuffer();
    }

    /** create texture image and sampler */
    private void createTextureObjects() {
        createTextureImage();
        createTextureSampler();
    }

    /** create uniform buffers and descriptor pool and sets */
    private void createDescriptorObjects() {
        createUniformBuffers();
        createDescriptorPool();
        createDescriptorSets();
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

    /** destroy uniform buffers and descriptor pool and sets */
    private void destroyDescriptorObjects() {
        uniformBuffers.forEach(ubo -> vkDestroyBuffer(VulkanSetup.logicalDevice.device, ubo, null));
        uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(VulkanSetup.logicalDevice.device,
                uboMemory, null));
        vkDestroyDescriptorPool(VulkanSetup.logicalDevice.device, descriptorPool, null);
    }

    /** create vertex buffer */
    void createVertexBuffer() {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = (long) VulkanSetup.MODEL_SIZEOF * model.length;
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            VulkanSetup.createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer, pBufferMemory);
            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, (int) bufferSize);
                for (Vertex vertex : model.vertices)
                    buffer.putFloat(vertex.position.x())
                        .putFloat(vertex.position.y())
                        .putFloat(vertex.position.z())
                        .putFloat(vertex.uv.x())
                        .putFloat(vertex.uv.y());
                buffer.rewind();
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory);
            VulkanSetup.createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, vertexBuffer, bufferSize);
            vkDestroyBuffer(VulkanSetup.logicalDevice.device, stagingBuffer, null);
            vkFreeMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, null);
        }
    }

    /** create index buffer */
    void createIndexBuffer() {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = (long) Integer.BYTES * model.length;
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            VulkanSetup.createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer, pBufferMemory);
            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, (int) bufferSize);
                for (int index : model.indices) buffer.putInt(index);
                buffer.rewind();
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory);
            VulkanSetup.createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            indexBuffer = pBuffer.get(0);
            indexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, indexBuffer, bufferSize);
            vkDestroyBuffer(VulkanSetup.logicalDevice.device, stagingBuffer, null);
            vkFreeMemory(VulkanSetup.logicalDevice.device, stagingBufferMemory, null);
        }
    }

    /** create texture image */
    void createTextureImage() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);
            VulkanSetup.createBuffer(texture.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer, pStagingBufferMemory);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0), 0, texture.size, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, texture.size);
                for (float pixel : texture.getPixels()) buffer.putFloat(pixel);
                buffer.rewind();
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0));
            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            VulkanSetup.createImage(texture.width, texture.height, texture.mipLevels,
                    VK_SAMPLE_COUNT_1_BIT, VulkanSetup.SAMPLER_COLOR_FORMAT, VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                            VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    pTextureImage, pTextureImageMemory);
            textureImage = pTextureImage.get(0);
            textureImageMemory = pTextureImageMemory.get(0);
            VulkanSetup.transitionImageLayout(commandPool, textureImage, VulkanSetup.SAMPLER_COLOR_FORMAT,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, texture.mipLevels);
            copyBufferToImage(pStagingBuffer.get(0), textureImage, texture.width, texture.height);
            generateMipmaps(VulkanSetup.logicalDevice, texture, textureImage);
            vkDestroyBuffer(VulkanSetup.logicalDevice.device, pStagingBuffer.get(0), null);
            vkFreeMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0), null);
        }
        textureImageView = VulkanSetup.createImageView(VulkanSetup.logicalDevice.device, textureImage,
                VulkanSetup.SAMPLER_COLOR_FORMAT, VK_IMAGE_ASPECT_COLOR_BIT, texture.mipLevels);
    }

    /** create texture sampler */
    void createTextureSampler() {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VulkanSetup.SAMPLER_FILTER)
                    .minFilter(VulkanSetup.SAMPLER_FILTER)
                    .addressModeU(VulkanSetup.SAMPLER_ADDRESS_MODE)
                    .addressModeV(VulkanSetup.SAMPLER_ADDRESS_MODE)
                    .addressModeW(VulkanSetup.SAMPLER_ADDRESS_MODE)
                    .anisotropyEnable(true)
                    .maxAnisotropy(16.0f)
                    .borderColor(VulkanSetup.SAMPLER_BORDER_COLOR)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VulkanSetup.SAMPLER_COMPARISONS)
                    .mipmapMode(VulkanSetup.SAMPLER_MIPMAP_MODE)
                    .minLod(0) // Optional
                    .maxLod((float) texture.getMipLevels())
                    .mipLodBias(0); // Optional
            LongBuffer pTextureSampler = stack.mallocLong(1);
            int result = vkCreateSampler(VulkanSetup.logicalDevice.device,
                    samplerInfo, null, pTextureSampler);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create texture sampler: " +
                        VulkanSetup.translateVulkanResult(result));
            textureSampler = pTextureSampler.get(0);
        }
    }

    /** create uniform buffers */
    void createUniformBuffers() {
        try (MemoryStack stack = stackPush()) {
            uniformBuffers = new ArrayList<>(VulkanSetup.swapChain.images.size());
            uniformBuffersMemory = new ArrayList<>(VulkanSetup.swapChain.images.size());
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            for (int i = 0; i < VulkanSetup.swapChain.images.size(); i++) {
                VulkanSetup.createBuffer(VulkanSetup.UNIFORM_SIZEOF, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        pBuffer, pBufferMemory);
                uniformBuffers.add(pBuffer.get(0));
                uniformBuffersMemory.add(pBufferMemory.get(0));
            }
        }
    }

    /** create descriptor pool */
    void createDescriptorPool() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);
            VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(VulkanSetup.swapChain.images.size());
            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(VulkanSetup.swapChain.images.size());
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSizes)
                    .maxSets(VulkanSetup.swapChain.images.size());
            LongBuffer pDescriptorPool = stack.mallocLong(1);
            int result = vkCreateDescriptorPool(VulkanSetup.logicalDevice.device, poolInfo, null, pDescriptorPool);
            if (result != VK_SUCCESS)
                throw new AssertionError(
                        "Failed to create descriptor pool: " + VulkanSetup.translateVulkanResult(result));
            descriptorPool = pDescriptorPool.get(0);
        }
    }

    /** create descriptor sets */
    void createDescriptorSets() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer layouts = stack.mallocLong(VulkanSetup.swapChain.images.size());
            for (int i = 0; i < layouts.capacity(); i++)
                layouts.put(i, VulkanSetup.logicalDevice.descriptorSetLayout);
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(layouts);
            LongBuffer pDescriptorSets = stack.mallocLong(VulkanSetup.swapChain.images.size());
            int result = vkAllocateDescriptorSets(VulkanSetup.logicalDevice.device, allocInfo, pDescriptorSets);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to allocate descriptor sets: " +
                        VulkanSetup.translateVulkanResult(result));
            descriptorSets = new ArrayList<>(pDescriptorSets.capacity());
            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .offset(0)
                    .range(VulkanSetup.UNIFORM_SIZEOF);
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
            for (int i = 0; i < pDescriptorSets.capacity(); i++) {
                long descriptorSet = pDescriptorSets.get(i);
                bufferInfo.buffer(uniformBuffers.get(i));
                uboDescriptorWrite.dstSet(descriptorSet);
                samplerDescriptorWrite.dstSet(descriptorSet);
                vkUpdateDescriptorSets(VulkanSetup.logicalDevice.device, descriptorWrites, null);
                descriptorSets.add(descriptorSet);
            }
        }
    }

    /** generate mipmaps */
    private void generateMipmaps(LogicalDevice logicalDevice, Texture texture, long image) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties formatProperties = VkFormatProperties.malloc(stack);
            vkGetPhysicalDeviceFormatProperties(logicalDevice.device.getPhysicalDevice(),
                    VulkanSetup.SAMPLER_COLOR_FORMAT,
                    formatProperties);
            if ((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0)
                throw new RuntimeException("Texture image format does not support linear blitting");
            VkCommandBuffer commandBuffer = VulkanSetup.beginCommand(commandPool);
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
                vkCmdBlitImage(commandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, image,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, blit, VulkanSetup.SAMPLER_MIPMAP_FILTER);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                        .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                        .srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                        .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                        null, null, barrier);
                if (mipWidth > 1)
                    mipWidth /= 2;
                if (mipHeight > 1)
                    mipHeight /= 2;
            }
            barrier.subresourceRange().baseMipLevel(texture.mipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0, null, null, barrier);
            VulkanSetup.endCommand(commandPool, commandBuffer);
        }
    }

    /** copy buffer */
    private void copyBuffer(long srcBuffer, long dstBuffer, long size) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = VulkanSetup.beginCommand(commandPool);
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack);
            copyRegion.size(size);
            vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);
            VulkanSetup.endCommand(commandPool, commandBuffer);
        }
    }

    /** copy buffer to image */
    private void copyBufferToImage(long buffer, long image, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = VulkanSetup.beginCommand(commandPool);
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0).bufferRowLength(0).bufferImageHeight(0);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));
            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
            VulkanSetup.endCommand(commandPool, commandBuffer);
        }
    }
    
}
