package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.Vertex;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static es.optocom.jovp.engine.rendering.VulkanSettings.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK13.*;

/**
 * VulkanBuffers
 *
 * <ul>
 * <li>Vulkan Objects</li>
 * Vulkan object and buffers for models, textures, and uniforms for rendering
 * </ul>
 *
 * @since 0.0.1
 */
class VulkanBuffers {

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
     * Create Vulkan objects for an item
     *
     * @param item The item to render
     *
     * @since 0.0.1
     */
    VulkanBuffers(@NotNull Item item) {
        this.item = item;
        commandPool = createCommandPool();
        createModelObjects(item.model);
        createTextureObjects(item.texture);
        createDescriptorObjects();
    }

    // Update model objects
    void update() {
        destroyDescriptorObjects();
        destroyTextureObjects();
        destroyModelObjects();
        createModelObjects(item.model);
        createTextureObjects(item.texture);
        createDescriptorObjects();
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
        VkDevice device = logicalDevice.device;
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
            vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                verticesToBuffers(model.vertices, data.getByteBuffer(0, (int) bufferSize));
            }
            vkUnmapMemory(device, stagingBufferMemory);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, vertexBuffer, bufferSize);
            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
        }
    }

    // Create index buffer
    private void createIndexBuffer(@NotNull Model model) {
        VkDevice device = logicalDevice.device;
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
            vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                indicesToBuffer(model.indices, data.getByteBuffer(0, (int) bufferSize));
            }
            vkUnmapMemory(device, stagingBufferMemory);
            createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory);
            indexBuffer = pBuffer.get(0);
            indexBufferMemory = pBufferMemory.get(0);
            copyBuffer(stagingBuffer, indexBuffer, bufferSize);
            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
        }
    }

    // Create texture image
    private void createTextureImage(@NotNull Texture texture) {
        int size = texture.getSize();
        int width = texture.getWidth();
        int height = texture.getHeight();
        int mipLevels = texture.getMipLevels();
        VkDevice device = logicalDevice.device;
        try (MemoryStack stack = stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);
            createBuffer(size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer, pStagingBufferMemory);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, pStagingBufferMemory.get(0), 0, size, 0, data);
            {
                memcpy(data.getByteBuffer(0, size), texture.getPixels(), size);
            }
            vkUnmapMemory(device, pStagingBufferMemory.get(0));
            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            createImage(width, height, mipLevels,
                    VK_SAMPLE_COUNT_1_BIT, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                            VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    pTextureImage, pTextureImageMemory);
            textureImage = pTextureImage.get(0);
            textureImageMemory = pTextureImageMemory.get(0);
            transitionImageLayout(commandPool, textureImage, VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, mipLevels);
            copyBufferToImage(pStagingBuffer.get(0), textureImage, width, height);
            generateMipmaps(logicalDevice, textureImage, width, height, mipLevels);
            vkDestroyBuffer(device, pStagingBuffer.get(0), null);
            vkFreeMemory(device, pStagingBufferMemory.get(0), null);
        }
        textureImageView = createImageView(device, textureImage, VK_FORMAT_R8G8B8A8_SRGB,
                VK_IMAGE_ASPECT_COLOR_BIT, mipLevels);
    }

    // Create texture sampler
    private void createTextureSampler(@NotNull Texture texture) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_NEAREST)
                    .minFilter(VK_FILTER_NEAREST)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .anisotropyEnable(true)
                    .maxAnisotropy(16.0f)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
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

    // Copy vertices to buffer
    private void verticesToBuffers(Vertex @NotNull [] vertices, ByteBuffer buffer) {
        for (Vertex vertex : vertices) {
            buffer.putFloat(vertex.position.x())
                    .putFloat(vertex.position.y())
                    .putFloat(vertex.position.z())
                    .putFloat(vertex.uv.x())
                    .putFloat(vertex.uv.y());
        }
    }

    // Copy indices to buffer
    private void indicesToBuffer(Integer @NotNull [] indices, ByteBuffer buffer) {
        for(int index : indices) buffer.putInt(index);
        buffer.rewind();
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

    // Generate mipmaps
    private void generateMipmaps(@NotNull LogicalDevice logicalDevice, long image,
                                 int width, int height, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties formatProperties = VkFormatProperties.malloc(stack);
            vkGetPhysicalDeviceFormatProperties(logicalDevice.device.getPhysicalDevice(), VK_FORMAT_R8G8B8A8_SRGB,
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
            int mipWidth = width;
            int mipHeight = height;
            for (int i = 1; i < mipLevels; i++) {
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
            barrier.subresourceRange().baseMipLevel(mipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0, null, null, barrier);
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

    // Copy to memory
    private static void memcpy(@NotNull ByteBuffer dst, @NotNull ByteBuffer src, long size) {
        src.limit((int) size);
        dst.put(src);
        src.limit(src.capacity()).rewind();
    }

    /**
     *
     * Update uniforms for the image to be rendered
     *
     * @param image Image to be rendered
     * @param projection Projection matrix
     * @param view view matrix
     *
     * @since 0.0.1
     */
    void updateUniforms(int image, Matrix4f projection, Matrix4f view) {
        final Matrix4f transform = new Matrix4f();
        // Convert from degrees of visual angle to distances
        Vector3f position = new Vector3f(Z_FAR * (float) Math.tan(item.position.x),
                Z_FAR * (float) Math.tan(item.position.y), (float) item.position.z);
        Vector3f size = new Vector3f(Z_FAR * (float) Math.tan(item.size.x),
                Z_FAR * (float) Math.tan(item.size.y), (float) item.size.z);
        transform.translation(position).scale(size).rotate((float) item.rotation, item.axis);
        try(MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(logicalDevice.device, uniformBuffersMemory.get(image), 0, UNIFORM_SIZEOF, 0, data);
            {
                memcpy(data.getByteBuffer(0, UNIFORM_SIZEOF), projection, view, transform);
            }
            vkUnmapMemory(logicalDevice.device, uniformBuffersMemory.get(image));
        }
    }

    // UBO to byte buffer
    private void memcpy(ByteBuffer buffer, @NotNull Matrix4f projection,
                        @NotNull Matrix4f view, @NotNull Matrix4f transform) {
        final int mat4Size = 16 * Float.BYTES;
        transform.get(0, buffer);
        view.get(alignas(mat4Size, alignof(view)), buffer);
        projection.get(alignas(mat4Size * 2, alignof(view)), buffer);
    }

    // Alignments for copying to memory
    private static int alignof(Object obj) {
        return obj == null ? 0 : SIZEOF_CACHE.getOrDefault(obj.getClass(), Integer.BYTES);
    }
    private static int alignas(int offset, int alignment) {
        return offset % alignment == 0 ? offset : ((offset - 1) | (alignment - 1)) + 1;
    }

}
