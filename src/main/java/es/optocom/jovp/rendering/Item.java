package es.optocom.jovp.rendering;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;
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

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK13.*;

import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.PostType;
import es.optocom.jovp.definitions.Vertex;
import es.optocom.jovp.definitions.ViewMode;

/**
 * Item to construct the psychophysical experience
 *
 * @since 0.0.1
 */
public class Item {

  Eye eye;
  Model model;
  Texture texture;
  private Post post;
  private Vector3f position; // position in x, y, and z (-1 to 1)
  private Vector3f scale; // scale for x, y, and z (0 to 1)
  private float theta; // rotation angle in radians
  private Vector3f rotation; // rotation axis
  private Vector4i settings;
  private long commandPool;
  private long vertexBuffer;
  private long indexBuffer;
  private long vertexBufferMemory;
  private long indexBufferMemory;
  private long textureImage;
  private long textureImageMemory;
  private long textureImageView;
  private long textureSampler;
  private List<Long> uniformBuffers;
  private List<Long> uniformBuffersMemory;
  private long descriptorPool;
  private List<Long> descriptorSets;

  /**
   * Create an item for psychophysics experience
   *
   * @param model   The model (square, circle, etc)
   * @param texture The texture
   *
   * @since 0.0.1
   */
  public Item(Model model, Texture texture) {
    this();
    this.model = model;
    this.texture = texture;
    defaults();
    createBuffers();
  }

  /**
   * Init an item, for use with Text. CARE: only for Text
   *
   * @since 0.0.1
   */
  Item() {
    eye = Eye.BOTH;
    defaults();
  }

  /**
   * Recreate buffers
   * 
   * @param model The new model
   * @param texture The new texture
   *
   * @since 0.0.1
   */
  public void update(Model model, Texture texture) {
    this.model = model;
    this.texture = texture;
    if (commandPool == 0) return;
    recreateModel();
    recreateTexture();
  }

  /**
   * Render item
   *
   * @param stack Memory stack
   * @param commandBuffer Command buffer
   * @param observer The optical configuration for rendering
   * @param image in-flight frame to render
   *
   * @since 0.0.1
   */
  void render(MemoryStack stack, VkCommandBuffer commandBuffer, int image) {
    Eye renderEye = eye;
    // For monocular view there is only 1 view pass and equivalent to binocular
    // view rendering for the left eye
    if (VulkanSetup.observer.viewMode == ViewMode.MONO & renderEye != Eye.NONE) renderEye = Eye.LEFT;
    // For binocular, left is to be rendered to pass = 0 eye and right eye to pass = 1
    switch (renderEye) {
      case LEFT -> renderEye(stack, commandBuffer, image, 0);
      case RIGHT -> renderEye(stack, commandBuffer, image, 1);
      case BOTH -> {
        renderEye(stack, commandBuffer, image, 0);
        renderEye(stack, commandBuffer, image, 1);
        }
      case NONE -> {}
    }
  }

  /**
   * Render item
   * 
   * @param stack Memory stack
   * @param commandBuffer Command buffer
   * @param image in-flight frame to render
   * @param observer the observer of the device for rendering
   * @param passNumber pass number. For MONO vision, it ought to be 0. For STEREO, left is 0 and right is 1
   *
   * @since 0.0.1
   */
  void renderEye(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int passNumber) {
    ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(passNumber);
    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, viewPass.graphicsPipeline);
    updateUniforms(image);
    LongBuffer vertexBuffers = stack.longs(vertexBuffer);
    LongBuffer offsets = stack.longs(0);
    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
    vkCmdBindIndexBuffer(commandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT32);
    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
        viewPass.pipelineLayout, 0,
        stack.longs(descriptorSets.get(image)), null);
    vkCmdDrawIndexed(commandBuffer, model.length, 1,
        0, 0, 0);
  }

  /**
   * Recreate buffers
   * 
   * @param model The new model
   *
   * @since 0.0.1
   */
  public void update(Model model) {
    this.model = model;
    if (commandPool == 0) return;
    recreateModel();
  }

  /**
   * Recreate buffers
   * 
   * @param texture The new texture
   *
   * @since 0.0.1
   */
  public void update(Texture texture) {
    this.texture = texture;
    if (commandPool == 0) return;
    recreateTexture();
  }

  /**
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
   * Set eye where to render the item
   *
   * @param eye Eye to display
   *
   * @since 0.0.1
   */
  public void eye(Eye eye) {
    this.eye = eye;
  }

  /**
   * Get eye where to render the item
   *
   * @return the eye to render
   *
   * @since 0.0.1
   */
  public Eye eye() {
    return eye;
  }

  /**
   * 
   * Position the item
   *
   * @param x x-axis position between -1 and 1
   * @param y y-axis position between -1 and 1
   *
   * @since 0.0.1
   */
  public void position(double x, double y) {
    position((float) x, (float) y);
  }

  /**
   *
   * Position the item
   *
   * @param x x-axis position between -1 and 1
   * @param y y-axis position between -1 and 1
   *
   * @since 0.0.1
   */
  public void position(float x, float y) {
    position(x, y, 0);
  }

  /**
   *
   * Position the item
   *
   * @param x x-axis position between -1 and 1
   * @param y y-axis position between -1 and 1
   * @param z z-axis position between -1 and 1
   *
   * @since 0.0.1
   */
  public void position(double x, double y, double z) {
    position((float) x, (float) y, (float) z);
  }

  /**
   *
   * Position the item
   *
   * @param x x-axis position between -1 and 1
   * @param y y-axis position between -1 and 1
   * @param z z-axis position between -1 and 1
   *
   * @since 0.0.1
   */
  public void position(float x, float y, float z) {
    position.x = x;
    position.y = y;
    position.z = z;
  }

  /**
   * 
   * Set item size
   *
   * @param x Size along the x and y axes
   *
   * @since 0.0.1
   */
  public void size(double x) {
    size(x, x);
  }

  /**
   * 
   * Set item size
   *
   * @param x Size along the x-axis from 0 to 1
   * @param y Size along the y-axis from 0 to 1
   *
   * @since 0.0.1
   */
  public void size(double x, double y) {
    size((float) x, (float) y, 0.0f);
  }

  /**
   * 
   * Set item size
   *
   * @param x Size along the x-axis from 0 to 1
   * @param y Size along the y-axis from 0 to 1
   * @param z Size along the z-axis from 0 to 1
   *
   * @since 0.0.1
   */
  public void size(double x, double y, double z) {
    size((float) x, (float) y, (float) z);
  }

  /**
   * 
   * Set item size
   *
   * @param x Size along the x-axis from 0 to 1
   * @param y Size along the y-axis from 0 to 1
   * @param z Size along the z-axis from 0 to 1
   *
   * @since 0.0.1
   */
  public void size(float x, float y, float z) {
    scale.x = x;
    scale.y = y;
    scale.z = z;
  }

  /**
   * Rotate the item
   *
   * @param theta Angle of rotation in degrees
   *
   * @since 0.0.1
   */
  public void rotation(double theta) {
    rotation((float) theta);
  }

    /**
   * Rotate the item
   *
   * @param rotation Angle of rotation in degrees
   *
   * @since 0.0.1
   */
  public void rotation(float theta) {
    rotation((float) theta, new Vector3f(0.0f, 0.0f, 1.0f));
  }

  /**
   * Rotate the item
   *
   * @param theta Angle of rotation in degrees
   * @param rotation Axis of rotation
   *
   * @since 0.0.1
   */
  public void rotation(double theta, Vector3f rotation) {
    rotation((float) theta, rotation);
  }

  /**
   * Rotate the item
   *
   * @param theta Angle of rotation in degrees
   * @param rotation Axis of rotation
   *
   * @since 0.0.1
   */
  public void rotation(float theta, Vector3f rotation) {
    this.theta = ((float) Math.PI / 180.0f) * theta;
    this.rotation = rotation;
  }

  /**
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
   * Get texture color 1 for grids
   *
   * @return The RGBA values of the minimum color
   *
   * @since 0.0.1
   */
  public Vector4f rgba0() {
    return texture.rgba0();
  }

  /**
   * Get texture color 2 for grids
   *
   * @return The RGBA values of the maximum color
   *
   * @since 0.0.1
   */
  public Vector4f rgba1() {
    return texture.rgba1();
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
   * Spatial frequency properties of the texture
   *
   * @param xp Phase on the x-axis
   * @param xf Frequency on the x-axis
   * @param yp Phase on the y-axis
   * @param yf Frequency on the y-axis
   *
   * @since 0.0.1
   */
  public void frequency(float xp, float xf, float yp, float yf) {
  }

  /**
   * Spatial frequency properties of the texture
   *
   * @param xp Phase on the x-axis
   * @param xf Frequency on the x-axis
   * @param yp Phase on the y-axis
   * @param yf Frequency on the y-axis
   *
   * @since 0.0.1
   */
  public void frequency(double xp, double xf, double yp, double yf) {
    frequency((float) xp, (float) xf, (float) yp, (float) yf);
  }

  /**
   * Spatial frequency properties of the texture 1D
   *
   * @param xp Phase on the x-axis
   * @param xf Frequency on the x-axis
   *
   * @since 0.0.1
   */
  public void frequency(double xp, double xf) {
    frequency((float) xp, (float) xf, (float) xp, (float) xf);
  }

  /**
   * Color contrast
   *
   * @param r Amplitude for R channel
   * @param g Amplitude for G channel
   * @param b Amplitude for B channel
   * @param a Amplitude for alpha channel
   *
   * @since 0.0.1
   */
  public void contrast(float r, float g, float b, float a) {
    if (r < 0) r = 0;
    if (r > 1) r = 1;
    if (g < 0) g = 0;
    if (g > 1) g = 1;
    if (b < 0) b = 0;
    if (b > 1) b = 1;
    if (a < 0) a = 0;
    if (a > 1) a = 1;
  }

  /**
   * Color contrast
   *
   * @param r Amplitude for R channel
   * @param g Amplitude for G channel
   * @param b Amplitude for B channel
   * @param a Amplitude for alpha channel
   *
   * @since 0.0.1
   */
  public void contrast(double r, double g, double b, double a) {
    contrast((float) r, (float) g, (float) b, (float) a);
  }

  /**
   * Gray contrast
   *
   * @param gray Contrast for all channels, except alpha
   *
   * @since 0.0.1
   */
  public void contrast(double gray) {
    contrast(gray, gray, gray, gray);
  }

  /**
   * Rotate the texture inside the model
   *
   * @param rotation Angle of rotation in degrees
   *
   * @since 0.0.1
   */
  public void texRotation(double rotation) {
    texRotation(rotation, new float[] { 0.5f, 0.5f });
  }

  /**
   * Rotate the texture inside the model
   *
   * @param rotation Angle of rotation in degrees
   * @param pivot    Pivot UV values
   *
   * @since 0.0.1
   */
  public void texRotation(double rotation, float[] pivot) {
    //TODO
  }

  /**
   * Add an envelope
   *
   * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sd   Standard deviation in meters for the x- and y-axis
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, double sd) {
    envelope(type, (float) sd, (float) sd, 0);
  }

  /**
   * Add an envelope
   *
   * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sdx  Standard deviation in meters for the x-axis
   * @param sdy  Standard deviation in meters for the y-axis
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, double sdx, double sdy) {
    envelope(type, (float) sdx, (float) sdy, 0);
  }

  /**
   * Add an envelope
   *
   * @param type  Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sdx   Standard deviation in meters for the x-axis
   * @param sdy   Standard deviation in meters for the y-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, double sdx, double sdy, double angle) {
    post.envelope(type, (float) sdx, (float) sdy, (float) angle);
  }

  /**
   * Add an envelope
   *
   * @param type  Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sdx   Standard deviation in meters for the x-axis
   * @param sdy   Standard deviation in meters for the y-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, float sdx, float sdy, float angle) {
    post.envelope(type, sdx, sdy, angle);
  }

  /**
   * Remove the envelope
   *
   * @since 0.0.1
   */
  public void removeEnvelope() {
    post.removeEnvelope();
  }

  /**
   * Add Gaussian defocus (only spherical)
   *
   * @param dx Defocus in Diopters
   *
   * @since 0.0.1
   */
  public void defocus(double dx) {
    defocus((float) dx, (float) dx, 0);
  }

  /**
   * Add Gaussian defocus (spherical and astigmatic defocus)
   *
   * @param dx    Defocus for the x-axis in Diopters
   * @param dy    Defocus for the x-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void defocus(double dx, double dy, double angle) {
    post.defocus((float) dx, (float) dy, (float) angle);
  }

  /**
   * Add Gaussian defocus (spherical and astigmatic defocus)
   *
   * @param dx    Defocus for the x-axis in Diopters
   * @param dy    Defocus for the x-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void defocus(float dx, float dy, float angle) {
    post.defocus(dx, dy, angle);
  }

  /**
   * Remove the Gaussian defocus
   *
   * @since 0.0.1
   */
  public void removeDefocus() {
    post.removeDefocus();
  }

  /** defaults */
  private void defaults() {
    position = new Vector3f();
    scale = new Vector3f();
    theta = 0;
    rotation = new Vector3f();
    post = new Post(); // TODO
    settings = new Vector4i(0, 0, 0, 0); // TODO
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
  private void recreateModel() {
    destroyModelObjects();
    createModelObjects();
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
  private void createIndexBuffer() {
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
  private void createTextureImage() {
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

  /**
   *
   * Update uniforms for the image to be rendered
   *
   * @param imageIndex Image to be rendered
   *
   * @since 0.0.1
   */
  void updateUniforms(int imageIndex) {
    Matrix4f transform = new Matrix4f();
    transform.rotateLocal(theta, rotation.x, rotation.y, rotation.z)
             .scaleLocal(scale.x, scale.y, scale.z)
             .translateLocal(position.x, position.y, position.z);
    try (MemoryStack stack = stackPush()) {
      final int mat4Size = 16 * Float.BYTES;
      final int vec4Size = 4 * Float.BYTES;
      Vector4f vect = new Vector4f();
      PointerBuffer data = stack.mallocPointer(1);
      vkMapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex), 0,
                  VulkanSetup.UNIFORM_SIZEOF, 0, data);
      {
        ByteBuffer buffer = data.getByteBuffer(0, VulkanSetup.UNIFORM_SIZEOF);
        settings.get(0, buffer);
        transform.get(vec4Size, buffer);
        VulkanSetup.observer.projection.get(2 * mat4Size + vec4Size, buffer);
        VulkanSetup.observer.view.get(3 * mat4Size + vec4Size, buffer);
        VulkanSetup.observer.optics.lens.get(3 * mat4Size + vec4Size, buffer);
        vect.get(4 * mat4Size + vec4Size, buffer);
        vect.get(4 * mat4Size + 2 * vec4Size, buffer);
        rgba0().get(4 * mat4Size + 3 * vec4Size, buffer);
        rgba1().get(4 * mat4Size + 4 * vec4Size, buffer);
        vect.get(4 * mat4Size + 5 * vec4Size, buffer);
        post.parameters.get(4 * mat4Size + 6 * vec4Size, buffer);
      }
      vkUnmapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex));
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