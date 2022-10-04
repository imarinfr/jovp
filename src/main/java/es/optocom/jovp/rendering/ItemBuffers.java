package es.optocom.jovp.rendering;

import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.Vertex;
import es.optocom.jovp.definitions.ViewMode;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK13.*;

/**
 * Vulkan object and buffers for models, textures, and uniforms for rendering
 *
 * @since 0.0.1
 */
class ItemBuffers {

  final Item item;
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
  private boolean updateModel = false;
  private boolean updateTexture = false;

  /**
   * Initialize Item Buffers
   *
   * @param item The item for which to create buffers
   *
   * @since 0.0.1
   */
  ItemBuffers(Item item) {
    this.item = item;
  }

  /**
   * Create buffers for the model on request
   *
   * @since 0.0.1
   */
  void create() {
    commandPool = VulkanSetup.createCommandPool();
    createModelObjects();
    createTextureObjects();
    createDescriptorObjects();
  }

  /**
   * Signal the renderer that model buffers need to be recreated
   * but only if buffers have already been created
   * 
   * @since 0.0.1
   */
  void signalUpdateModel() {
    if (commandPool != 0) updateModel = true;
  }

  /**
   * Signal the renderer that texture buffers need to be recreated
   * but only if buffers have already been created
   * 
   * @since 0.0.1
   */
  void signalUpdateTexture() {
    if (commandPool != 0) updateTexture = true;
  }
  
  /**
   * Recreate model buffers
   * 
   * @since 0.0.1
   */
  private void recreateModel() {
    destroyModelObjects();
    createModelObjects();
    updateModel = false;
  }

  /**
   * Recreate texture buffers
   * 
   * @since 0.0.1
   */
  private void recreateTexture() {
    destroyDescriptorObjects();
    destroyTextureObjects();
    createTextureObjects();
    createDescriptorObjects();
    updateTexture = false;
  }

  /**
   * Clean up after use
   *
   * @since 0.0.1
   */
  void destroy() {
    destroyDescriptorObjects();
    destroyTextureObjects();
    destroyModelObjects();
    VulkanSetup.destroyCommandPool(commandPool);
  }

  /**
   * Render item
   *
   * @since 0.0.1
   */
  void render(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int eye) {
    if (!item.show() || VulkanSetup.viewMode == ViewMode.STEREO &&
        ((item.eye == Eye.LEFT && eye == 1) || (item.eye == Eye.RIGHT && eye == 0)))
      return;
    ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(eye);
    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, viewPass.graphicsPipeline);
    updateUniforms(image, eye);
    if (updateModel) recreateModel();
    if (updateTexture) recreateTexture();
    ItemBuffers buffer = item.buffers;
    LongBuffer vertexBuffers = stack.longs(buffer.vertexBuffer);
    LongBuffer offsets = stack.longs(0);
    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
    vkCmdBindIndexBuffer(commandBuffer, buffer.indexBuffer, 0, VK_INDEX_TYPE_UINT32);
    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
        viewPass.pipelineLayout, 0,
        stack.longs(buffer.descriptorSets.get(image)), null);
    vkCmdDrawIndexed(commandBuffer, item.model.length, 1,
        0, 0, 0);
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
  void destroyModelObjects() {
    vkDestroyBuffer(VulkanSetup.logicalDevice.device, indexBuffer, null);
    vkFreeMemory(VulkanSetup.logicalDevice.device, indexBufferMemory, null);
    vkDestroyBuffer(VulkanSetup.logicalDevice.device, vertexBuffer, null);
    vkFreeMemory(VulkanSetup.logicalDevice.device, vertexBufferMemory, null);
  }

  /** destroy texture image and sampler */
  void destroyTextureObjects() {
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
  private void createVertexBuffer() {
    try (MemoryStack stack = stackPush()) {
      long bufferSize = (long) VulkanSetup.MODEL_SIZEOF * item.model.length;
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
        verticesToBuffer(item.model.vertices, data.getByteBuffer(0, (int) bufferSize));
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
  private void createIndexBuffer() {
    try (MemoryStack stack = stackPush()) {
      long bufferSize = (long) Integer.BYTES * item.model.length;
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
        indicesToBuffer(item.model.indices, data.getByteBuffer(0, (int) bufferSize));
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
  private void createTextureImage() {
    try (MemoryStack stack = stackPush()) {
      LongBuffer pStagingBuffer = stack.mallocLong(1);
      LongBuffer pStagingBufferMemory = stack.mallocLong(1);
      VulkanSetup.createBuffer(item.texture.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
          VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
          pStagingBuffer, pStagingBufferMemory);
      PointerBuffer data = stack.mallocPointer(1);
      vkMapMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0), 0, item.texture.size, 0, data);
      {
        textureToBuffer(item.texture.getPixels(), data.getByteBuffer(0, item.texture.size));
      }
      vkUnmapMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0));
      LongBuffer pTextureImage = stack.mallocLong(1);
      LongBuffer pTextureImageMemory = stack.mallocLong(1);
      VulkanSetup.createImage(item.texture.width, item.texture.height, item.texture.mipLevels,
          VK_SAMPLE_COUNT_1_BIT, VulkanSetup.SAMPLER_COLOR_FORMAT, VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
              VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
          pTextureImage, pTextureImageMemory);
      textureImage = pTextureImage.get(0);
      textureImageMemory = pTextureImageMemory.get(0);
      VulkanSetup.transitionImageLayout(commandPool, textureImage, VulkanSetup.SAMPLER_COLOR_FORMAT,
          VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, item.texture.mipLevels);
      copyBufferToImage(pStagingBuffer.get(0), textureImage, item.texture.width, item.texture.height);
      generateMipmaps(VulkanSetup.logicalDevice, item.texture, textureImage);
      vkDestroyBuffer(VulkanSetup.logicalDevice.device, pStagingBuffer.get(0), null);
      vkFreeMemory(VulkanSetup.logicalDevice.device, pStagingBufferMemory.get(0), null);
    }
    textureImageView = VulkanSetup.createImageView(VulkanSetup.logicalDevice.device, textureImage,
        VulkanSetup.SAMPLER_COLOR_FORMAT, VK_IMAGE_ASPECT_COLOR_BIT, item.texture.mipLevels);
  }

  /** create texture sampler */
  private void createTextureSampler() {
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
          .maxLod((float) item.texture.getMipLevels())
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
  private void createUniformBuffers() {
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
  private void createDescriptorPool() {
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
        throw new AssertionError("Failed to create descriptor pool: " + VulkanSetup.translateVulkanResult(result));
      descriptorPool = pDescriptorPool.get(0);
    }
  }

  /** create descriptor sets */
  private void createDescriptorSets() {
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
      vkGetPhysicalDeviceFormatProperties(logicalDevice.device.getPhysicalDevice(), VulkanSetup.SAMPLER_COLOR_FORMAT,
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

  /** vertices to buffer */
  private void verticesToBuffer(Vertex [] vertices, ByteBuffer buffer) {
    for (Vertex vertex : vertices)
      buffer.putFloat(vertex.position.x())
          .putFloat(vertex.position.y())
          .putFloat(vertex.position.z())
          .putFloat(vertex.uv.x())
          .putFloat(vertex.uv.y());
    buffer.rewind();
  }

  /** indices to buffer */
  private void indicesToBuffer(Integer [] indices, ByteBuffer buffer) {
    for (int index : indices)
      buffer.putInt(index);
    buffer.rewind();
  }

  /** texture to buffer */
  private static void textureToBuffer(float [] pixels, ByteBuffer buffer) {
    for (float pixel : pixels)
      buffer.putFloat(pixel);
    buffer.rewind();
  }

  /**
   *
   * Update uniforms for the image to be rendered
   *
   * @param imageIndex Image to be rendered
   * @param eye        0 for left eye, 1 for right eye
   *
   * @since 0.0.1
   */
  void updateUniforms(int imageIndex, int eye) {
    // Settings
    final Vector4i settings = new Vector4i(0, 0, 0, 0);
    // Type of texture
    switch (item.getTexture().type) {
      case TEXT, IMAGE -> settings.x = 0;
      case FLAT -> settings.x = 1;
      default -> settings.x = 2;
    }
    final Matrix4f transform = new Matrix4f();
    // Convert from degrees of visual angle to distances
    Vector3f position = new Vector3f((float) (item.position.z * Math.tan(item.position.x)),
        (float) (item.position.z * Math.tan(item.position.y)), (float) item.position.z);
    if (VulkanSetup.viewMode == ViewMode.STEREO) {
      // TODO: processing during stereo view when there are optical distorsions
    }
    Vector3f size = new Vector3f((float) (item.position.z * Math.tan(item.size.x)),
        (float) (item.position.z * Math.tan(item.size.y)), (float) item.size.z);
    transform.translation(position).rotate((float) item.rotation, item.rotationAxis).scale(size);
    // Spatial frequency of stimuli
    Vector4f spatial = new Vector4f(
        (float) Math.toDegrees(item.size.x) * 2 * item.frequency.x,
        (float) Math.toDegrees(item.size.y) * 2 * item.frequency.y,
        item.frequency.z, item.frequency.w);
    Vector4f texRotation = new Vector4f((float) item.texRotation, item.texPivot[0], item.texPivot[1], 1);
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
    try (MemoryStack stack = stackPush()) {
      PointerBuffer data = stack.mallocPointer(1);
      vkMapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex), 0, VulkanSetup.UNIFORM_SIZEOF,
          0, data);
      {
        uniformsToBuffer(data.getByteBuffer(0, VulkanSetup.UNIFORM_SIZEOF), settings,
            transform, spatial, texRotation, item.rgba0(), item.rgba1(),
            item.contrast, envelope);
      }
      vkUnmapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex));
    }
  }

  /** uniforms to buffer */
  private void uniformsToBuffer(ByteBuffer buffer, Vector4i settings, Matrix4f transform, Vector4f frequency,
  Vector4f texRotation, Vector4f rgba0, Vector4f rgba1, Vector4f contrast, Vector4f envelope) {
    final int mat4Size = 16 * Float.BYTES;
    final int vec4Size = 4 * Float.BYTES;
    settings.get(0, buffer);
    transform.get(vec4Size, buffer);
    VulkanSetup.lens.get(mat4Size + vec4Size, buffer);
    VulkanSetup.view.get(2 * mat4Size + vec4Size, buffer);
    VulkanSetup.projection.get(3 * mat4Size + vec4Size, buffer);
    frequency.get(4 * mat4Size + vec4Size, buffer);
    texRotation.get(4 * mat4Size + 2 * vec4Size, buffer);
    rgba0.get(4 * mat4Size + 3 * vec4Size, buffer);
    rgba1.get(4 * mat4Size + 4 * vec4Size, buffer);
    contrast.get(4 * mat4Size + 5 * vec4Size, buffer);
    envelope.get(4 * mat4Size + 6 * vec4Size, buffer);
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
