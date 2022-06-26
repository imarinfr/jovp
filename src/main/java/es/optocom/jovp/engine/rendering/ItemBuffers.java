package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.Vertex;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static es.optocom.jovp.engine.rendering.VulkanSetup.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK13.*;

/**
 *
 * ItemBuffers
 *
 * <ul>
 * <li>Vulkan Objects</li>
 * Vulkan object and buffers for models, textures, and uniforms for rendering
 * </ul>
 *
 * @since 0.0.1
 */
class ItemBuffers {

    Item item;
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

    /**
     *
     * Create Vulkan objects for an item
     *
     * @param item The item to render
     *
     * @since 0.0.1
     */
    ItemBuffers(@NotNull Item item) {
        this.item = item;
        commandPool = createCommandPool();
        createModelObjects(item.model);
        createTextureObjects(item.texture);
        createDescriptorObjects();
    }

    /**
     *
     * Update buffers for the model on request
     *
     * @since 0.0.1
     */
    void update() {
        destroyModelObjects();
        createModelObjects(item.model);
    }

    /**
     *
     * Clean up after use
     *
     * @since 0.0.1
     */
    void destroy() {
        destroyDescriptorObjects();
        destroyTextureObjects();
        destroyModelObjects();
        destroyCommandPool(commandPool);
    }

    // Create vertex and index buffers
    private void createModelObjects(Model model) {
        createVertexBuffer(model);
        createIndexBuffer(model);
    }

    // Create texture image and sampler
    private void createTextureObjects(Texture texture) {
        createTextureImage(texture);
        createTextureSampler(texture);
    }

    // Create uniform buffers and descriptor pool and sets
    private void createDescriptorObjects() {
        createUniformBuffers();
        createDescriptorPool();
        createDescriptorSets();
    }

    // Destroy vertex and index buffers
    void destroyModelObjects() {
        vkDestroyBuffer(logicalDevice.device, indexBuffer, null);
        vkFreeMemory(logicalDevice.device, indexBufferMemory, null);
        vkDestroyBuffer(logicalDevice.device, vertexBuffer, null);
        vkFreeMemory(logicalDevice.device, vertexBufferMemory, null);
    }

    // Destroy texture image and sampler
    void destroyTextureObjects() {
        vkDestroySampler(logicalDevice.device, textureSampler, null);
        vkDestroyImageView(logicalDevice.device, textureImageView, null);
        vkDestroyImage(logicalDevice.device, textureImage, null);
        vkFreeMemory(logicalDevice.device, textureImageMemory, null);
    }

    // Destroy uniform buffers and descriptor pool and sets
    private void destroyDescriptorObjects() {
        uniformBuffers.forEach(ubo -> vkDestroyBuffer(logicalDevice.device, ubo, null));
        uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(logicalDevice.device,
                uboMemory, null));
        vkDestroyDescriptorPool(logicalDevice.device, descriptorPool, null);
    }

    // Create vertex buffer
    private void createVertexBuffer(@NotNull Model model) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = (long) MODEL_SIZEOF * model.length;
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer, pBufferMemory);
            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(logicalDevice.device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                verticesToBuffer(model.vertices, data.getByteBuffer(0, (int) bufferSize));
            }
            vkUnmapMemory(logicalDevice.device, stagingBufferMemory);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, vertexBuffer, bufferSize);
            vkDestroyBuffer(logicalDevice.device, stagingBuffer, null);
            vkFreeMemory(logicalDevice.device, stagingBufferMemory, null);
        }
    }

    // Create index buffer
    private void createIndexBuffer(@NotNull Model model) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = (long) Integer.BYTES * model.length;
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer, pBufferMemory);
            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(logicalDevice.device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                indicesToBuffer(model.indices, data.getByteBuffer(0, (int) bufferSize));
            }
            vkUnmapMemory(logicalDevice.device, stagingBufferMemory);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            indexBuffer = pBuffer.get(0);
            indexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, indexBuffer, bufferSize);
            vkDestroyBuffer(logicalDevice.device, stagingBuffer, null);
            vkFreeMemory(logicalDevice.device, stagingBufferMemory, null);
        }
    }

    // Create texture image
    private void createTextureImage(@NotNull Texture texture) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);
            createBuffer(texture.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer, pStagingBufferMemory);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(logicalDevice.device, pStagingBufferMemory.get(0), 0, texture.size, 0, data);
            {
                textureToBuffer(texture.getPixels(), data.getByteBuffer(0, texture.size));
            }
            vkUnmapMemory(logicalDevice.device, pStagingBufferMemory.get(0));
            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            createImage(texture.width, texture.height, texture.mipLevels,
                    VK_SAMPLE_COUNT_1_BIT, COLOR_FORMAT, VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                            VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    pTextureImage, pTextureImageMemory);
            textureImage = pTextureImage.get(0);
            textureImageMemory = pTextureImageMemory.get(0);
            transitionImageLayout(commandPool, textureImage, COLOR_FORMAT,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, texture.mipLevels);
            copyBufferToImage(pStagingBuffer.get(0), textureImage, texture.width, texture.height);
            generateMipmaps(logicalDevice, texture, textureImage);
            vkDestroyBuffer(logicalDevice.device, pStagingBuffer.get(0), null);
            vkFreeMemory(logicalDevice.device, pStagingBufferMemory.get(0), null);
        }
        textureImageView = createImageView(logicalDevice.device, textureImage, COLOR_FORMAT,
                VK_IMAGE_ASPECT_COLOR_BIT, texture.mipLevels);
    }

    // Create texture sampler
    private void createTextureSampler(@NotNull Texture texture) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(SAMPLER_FILTER)
                    .minFilter(SAMPLER_FILTER)
                    .addressModeU(SAMPLER_ADDRESS_MODE)
                    .addressModeV(SAMPLER_ADDRESS_MODE)
                    .addressModeW(SAMPLER_ADDRESS_MODE)
                    .anisotropyEnable(true)
                    .maxAnisotropy(16.0f)
                    .borderColor(SAMPLER_BORDER_COLOR)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(SAMPLER_MIPMAP_MODE)
                    .minLod(0) // Optional
                    .maxLod((float) texture.getMipLevels())
                    .mipLodBias(0); // Optional
            LongBuffer pTextureSampler = stack.mallocLong(1);
            int result = vkCreateSampler(logicalDevice.device,
                    samplerInfo, null, pTextureSampler);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create texture sampler: " +
                        translateVulkanResult(result));
            textureSampler = pTextureSampler.get(0);
        }
    }

    // Create uniform buffers
    private void createUniformBuffers() {
        try (MemoryStack stack = stackPush()) {
            uniformBuffers = new ArrayList<>(swapChain.swapChainImages.size());
            uniformBuffersMemory = new ArrayList<>(swapChain.swapChainImages.size());
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            for(int i = 0;i < swapChain.swapChainImages.size(); i++) {
                createBuffer(UNIFORM_SIZEOF, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        pBuffer, pBufferMemory);
                uniformBuffers.add(pBuffer.get(0));
                uniformBuffersMemory.add(pBufferMemory.get(0));
            }
        }
    }

    // Create descriptor pool
    private void createDescriptorPool() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);
            VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(swapChain.swapChainImages.size());
            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(swapChain.swapChainImages.size());
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSizes)
                    .maxSets(swapChain.swapChainImages.size());
            LongBuffer pDescriptorPool = stack.mallocLong(1);
            int result = vkCreateDescriptorPool(logicalDevice.device, poolInfo, null, pDescriptorPool);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create descriptor pool: " + translateVulkanResult(result));
            descriptorPool = pDescriptorPool.get(0);
        }
    }

    // Create descriptor sets
    private void createDescriptorSets() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer layouts = stack.mallocLong(swapChain.swapChainImages.size());
            for (int i = 0; i < layouts.capacity(); i++) layouts.put(i, logicalDevice.descriptorSetLayout);
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(layouts);
            LongBuffer pDescriptorSets = stack.mallocLong(swapChain.swapChainImages.size());
            int result = vkAllocateDescriptorSets(logicalDevice.device, allocInfo, pDescriptorSets);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to allocate descriptor sets: " +
                        translateVulkanResult(result));
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
            for (int i = 0; i < pDescriptorSets.capacity(); i++) {
                long descriptorSet = pDescriptorSets.get(i);
                bufferInfo.buffer(uniformBuffers.get(i));
                uboDescriptorWrite.dstSet(descriptorSet);
                samplerDescriptorWrite.dstSet(descriptorSet);
                vkUpdateDescriptorSets(logicalDevice.device, descriptorWrites, null);
                descriptorSets.add(descriptorSet);
            }
        }
    }

    // Generate mipmaps
    private void generateMipmaps(@NotNull LogicalDevice logicalDevice, Texture texture, long image) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties formatProperties = VkFormatProperties.malloc(stack);
            vkGetPhysicalDeviceFormatProperties(logicalDevice.device.getPhysicalDevice(), COLOR_FORMAT,
                    formatProperties);
            if ((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0)
                throw new RuntimeException("Texture image format does not support linear blitting");
            VkCommandBuffer commandBuffer = beginCommand(commandPool);
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
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, blit, VK_FILTER_LINEAR);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                        .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                        .srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                        .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                        null, null, barrier);
                if (mipWidth > 1) mipWidth /= 2;
                if (mipHeight > 1) mipHeight /= 2;
            }
            barrier.subresourceRange().baseMipLevel(texture.mipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0, null, null, barrier);
            endCommand(commandPool, commandBuffer);
        }
    }

    // Vertices to buffer
    private void verticesToBuffer(Vertex @NotNull [] vertices, ByteBuffer buffer) {
        for (Vertex vertex : vertices)
            buffer.putFloat(vertex.position.x())
                    .putFloat(vertex.position.y())
                    .putFloat(vertex.position.z())
                    .putFloat(vertex.uv.x())
                    .putFloat(vertex.uv.y());
        buffer.rewind();
    }

    // Indices to buffer
    private void indicesToBuffer(Integer @NotNull [] indices, ByteBuffer buffer) {
        for(int index : indices) buffer.putInt(index);
        buffer.rewind();
    }

    // Texture to buffer
    private static void textureToBuffer(float @NotNull [] pixels, ByteBuffer buffer) {
        for(float pixel : pixels) buffer.putFloat(pixel);
        buffer.rewind();
    }

    /**
     *
     * Update uniforms for the image to be rendered
     *
     * @param imageIndex Image to be rendered
     *
     * @since 0.0.1
     */
    void updateUniforms(int imageIndex) {
        // Settings
        final Vector4i settings = new Vector4i(0, 0, 0, 0);
        // Type of texture
        switch(item.getTexture().type) {
            case FLAT -> settings.x = 0;
            case TEXT -> settings.x = 2;
            case IMAGE -> settings.x = 3;
            default -> settings.x = 1;
        }
        final Matrix4f transform = new Matrix4f();
        // Convert from degrees of visual angle to distances
        Vector3f position = new Vector3f((float) (item.position.z * Math.tan(item.position.x)),
                (float) (item.position.z * Math.tan(item.position.y)), (float) item.position.z);
        Vector3f size = new Vector3f((float) (item.position.z * Math.tan(item.size.x)),
                (float) (item.position.z * Math.tan(item.size.y)), (float) item.size.z);
        transform.translation(position).rotate((float) item.rotation, item.rotationAxis).scale(size);
        // Spatial frequency of stimuli
        Vector4f spatial = new Vector4f(
                (float) Math.toDegrees(item.size.x) * 2 * item.frequency.x,
                (float) Math.toDegrees(item.size.y) * 2 * item.frequency.y,
                item.frequency.z, item.frequency.w);
        Vector4f rotation = new Vector4f((float) item.texRotation, item.texPivot[0], item.texPivot[1], 0);
        // Post-processing: envelope and Gaussian defocus
        final Vector4f envelope = new Vector4f(0, 0, 0, 0);
        switch (item.post.envelope) {
            case SQUARE -> envelope.x = 1;
            case CIRCLE -> envelope.x = 2;
            case GAUSSIAN -> envelope.x = 3;
            default -> envelope.x = 0; // No envelope
        }
        envelope.y = (float) (item.post.envelopeParams[0] / item.size.x); // SD x
        envelope.z = (float) (item.post.envelopeParams[1] / item.size.y); // SD y
        envelope.w = item.post.envelopeParams[2]; // angle
        try(MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(logicalDevice.device, uniformBuffersMemory.get(imageIndex), 0, UNIFORM_SIZEOF, 0, data);
            {
                uniformsToBuffer(data.getByteBuffer(0, UNIFORM_SIZEOF), settings,
                        transform, spatial, rotation, item.rgba0(), item.rgba1(),
                        item.contrast, envelope);
            }
            vkUnmapMemory(logicalDevice.device, uniformBuffersMemory.get(imageIndex));
        }
    }

    // Uniforms to buffer
    private void uniformsToBuffer(ByteBuffer buffer, @NotNull Vector4i settings,
                                  @NotNull Matrix4f transform, @NotNull Vector4f frequency, @NotNull Vector4f rotation,
                                  @NotNull Vector4f rgba0, @NotNull Vector4f rgba1, @NotNull Vector4f contrast,
                                  @NotNull Vector4f envelope) {
        final int mat4Size = 16 * Float.BYTES;
        final int vec4Size = 4 * Float.BYTES;
        settings.get(0, buffer);
        transform.get(vec4Size, buffer);
        lens.get(mat4Size + vec4Size, buffer);
        view.get(2 * mat4Size + vec4Size, buffer);
        projection.get(3 * mat4Size + vec4Size, buffer);
        frequency.get(4 * mat4Size + vec4Size, buffer);
        rotation.get(4 * mat4Size + 2 * vec4Size, buffer);
        rgba0.get(4 * mat4Size + 3 * vec4Size, buffer);
        rgba1.get(4 * mat4Size + 4 * vec4Size, buffer);
        contrast.get(4 * mat4Size + 5 * vec4Size, buffer);
        envelope.get(4 * mat4Size + 6 * vec4Size, buffer);
    }

    // Copy buffer
    private void copyBuffer(long srcBuffer, long dstBuffer, long size) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = beginCommand(commandPool);
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack);
            copyRegion.size(size);
            vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);
            endCommand(commandPool, commandBuffer);
        }
    }

    // Copy buffer to image
    private void copyBufferToImage(long buffer, long image, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = beginCommand(commandPool);
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));
            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
            endCommand(commandPool, commandBuffer);
        }
    }

}
