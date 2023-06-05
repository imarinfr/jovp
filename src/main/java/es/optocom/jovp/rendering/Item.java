package es.optocom.jovp.rendering;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
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
import es.optocom.jovp.definitions.TextureType;
import es.optocom.jovp.definitions.EnvelopeType;
import es.optocom.jovp.definitions.Vertex;
import es.optocom.jovp.definitions.ViewMode;

/**
 * 
 * Item to construct the psychophysical experience
 *
 * @since 0.0.1
 */
public class Item {

    Eye eye;
    Model model;
    Texture texture;
    private float distance; // distance of the item in meters
    private Vector2f position; // x and y position in radians
    private Vector3f scale; // scale for x and y in radians, and z in meters (size = 2 * scale)
    private Vector3f rotation; // angles of rotation in each axis
    private Matrix4f modelMatrix; // model matrix
    private Processing processing;
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

    private boolean updateModel = false;
    private boolean updateTexture = false;

    /**
     * 
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
        position = new Vector2f(0, 0);
        scale = new Vector3f();
        rotation = new Vector3f();
        modelMatrix = new Matrix4f();
        processing = new Processing(texture.getType());
        createBuffers();
    }

    /**
     * 
     * Init an item, for use with Text.
     *
     * @since 0.0.1
     */
    Item() {
        eye = Eye.BOTH;
        position = new Vector2f(0, 0);
        scale = new Vector3f();
        rotation = new Vector3f();
        modelMatrix = new Matrix4f();
        processing = new Processing(TextureType.TEXT);
    }

    /**
     * 
     * Render item
     *
     * @param stack         Memory stack
     * @param commandBuffer Command buffer
     * @param image         in-flight frame to render
     *
     * @since 0.0.1
     */
    void render(MemoryStack stack, VkCommandBuffer commandBuffer, int image) {
        if (VulkanSetup.observer.viewMode == ViewMode.MONO & eye != Eye.NONE) { // monoscopic view
            renderEye(stack, commandBuffer, image, 0);
            return;
        }
        switch (eye) { // stereoscopic view
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
     * 
     * Render item
     * 
     * @param stack         Memory stack
     * @param commandBuffer Command buffer
     * @param image         in-flight frame to render
     * @param passNumber    pass number. For MONO vision, it ought to be 0. For
     *                      STEREO, left is 0 and right is 1
     *
     * @since 0.0.1
     */
    void renderEye(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int passNumber) {
        ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(passNumber);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, viewPass.graphicsPipeline);
        updateUniforms(image, passNumber);
        if (updateModel) recreateModel();
        if (updateTexture) recreateTexture();
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
        if (commandPool == 0) return;
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
        if (commandPool != 0) {
            updateModel = true;
            updateTexture = true;
        }
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
     * Set eye where to render the item
     *
     * @param eye Eye to display
     *
     * @since 0.0.1
     */
    public void show(Eye eye) {
        this.eye = eye;
    }

    /**
     * 
     * Get eye where to render the item
     *
     * @return the eye to render
     *
     * @since 0.0.1
     */
    public Eye show() {
        return eye;
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
     * Position the item
     *
     * @param x x-axis position in degrees of visual angle
     * @param y y-axis position in degrees of visual angle
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
     * @param x x-axis position in degrees of visual angle
     * @param y y-axis position in degrees of visual angle
     *
     * @since 0.0.1
     */
    public void position(float x, float y) {
        position.x = (float) Math.toRadians(x);
        position.y = (float) Math.toRadians(y);
        computeModelMatrix();
    }

    /**
     *
     * Distance of the item
     *
     * @param distance distance in meters
     *
     * @since 0.0.1
     */
    public void distance(double distance) {
        distance((float) distance);
    }

    /**
     *
     * Distance of the item
     *
     * @param distance distance in meters
     *
     * @since 0.0.1
     */
    public void distance(float distance) {
        this.distance = distance;
        computeModelMatrix();
    }

    /**
     * 
     * Get size in degrees
     *
     * @return return size x and size y in degrees
     *
     * @since 0.0.1
     */
    public float[] size() {
        return new float[] {
            (float) Math.toDegrees(scale.x),
            (float) Math.toDegrees(scale.y)
        };
    }

    /**
     * 
     * Set item size
     *
     * @param x Size along the x and y axes in degrees fo visual angle
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
     * @param x Size along the x-axis in degrees fo visual angle
     * @param y Size along the y-axis in degrees fo visual angle
     *
     * @since 0.0.1
     */
    public void size(double x, double y) {
        size(x, y, 0);
    }

    /**
     * 
     * Set item size
     *
     * @param x Size along the x-axis in degrees fo visual angle
     * @param y Size along the y-axis in degrees fo visual angle
     * @param z Size along the z-axis in meters
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
     * @param x Size along the x-axis in degrees fo visual angle
     * @param y Size along the y-axis in degrees fo visual angle
     * @param z Size along the z-axis in meters
     *
     * @since 0.0.1
     */
    public void size(float x, float y, float z) {
        scale.x = (float) Math.toRadians(x);
        scale.y = (float) Math.toRadians(y);
        scale.z = z;
        computeModelMatrix();
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
     *
     * Rotate the item
     *
     * @param theta Angle of rotation in degrees
     *
     * @since 0.0.1
     */
    public void rotation(float theta) {
        rotation(0, 0, theta);
    }

    /**
     * Rotate the item
     *
     * @param x Angle on the x axis in degrees
     * @param y Angle on the y axis in degrees
     * @param z Angle on the z axis in degrees
     *
     * @since 0.0.1
     */
    public void rotation(double x, double y, double z) {
        rotation((float) x, (float) y, (float) z);
    }

    /**
     * Rotate the item
     *
     * @param x Angle on the x axis in degrees
     * @param y Angle on the y axis in degrees
     * @param z Angle on the z axis in degrees
     *
     * @since 0.0.1
     */
    public void rotation(float x, float y, float z) {
        rotation = new Vector3f((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
        computeModelMatrix();
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
        processing.frequency((float) xp, (float) xf, (float) yp, (float) yf);
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
        processing.frequency((float) xp, (float) xf, (float) xp, (float) xf);
    }

    /**
     * 
     * Contrast
     *
     * @param amplitude Amplitude for all channels
     *
     * @since 0.0.1
     */
    public void contrast(double amplitude) {
        contrast((float) amplitude);
    }

    /**
     *
     * Contrast
     *
     * @param amplitude Contrast
     *
     * @since 0.0.1
     */
    public void contrast(float amplitude) {
        contrast(amplitude, amplitude, amplitude, amplitude);
    }

    /**
     *
     * Contrast
     *
     * @param r Amplitude for R channel
     * @param g Amplitude for G channel
     * @param b Amplitude for B channel
     * @param a Amplitude for alpha channel
     *
     * @since 0.0.1
     */
    public void contrast(double r, double g, double b, double a) {
        processing.contrast((float) r, (float) g, (float) b, (float) a);
    }

    /**
     * 
     * Rotate the texture inside the model
     *
     * @param rotation Angle of rotation in degrees
     *
     * @since 0.0.1
     */
    public void texRotation(double rotation) {
        processing.rotation(rotation, new float[] { 0.5f, 0.5f });
    }

    /**
     * Add an envelope
     *
     * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
     * @param sd   Standard deviation in meters for the x- and y-axis
     *
     * @since 0.0.1
     */
    public void envelope(EnvelopeType type, double sd) {
        processing.envelope(type, (float) sd, (float) sd, 0);
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
    public void envelope(EnvelopeType type, double sdx, double sdy) {
        processing.envelope(type, (float) sdx, (float) sdy, 0);
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
    public void envelope(EnvelopeType type, double sdx, double sdy, double angle) {
        processing.envelope(type, (float) sdx, (float) sdy, (float) angle);
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
    public void envelope(EnvelopeType type, float sdx, float sdy, float angle) {
        processing.envelope(type, sdx, sdy, angle);
    }

    /**
     * Remove the envelope
     *
     * @since 0.0.1
     */
    public void removeEnvelope() {
        processing.removeEnvelope();
    }

    /**
     * Add Gaussian defocus (only spherical)
     *
     * @param dx Defocus in Diopters
     *
     * @since 0.0.1
     */
    public void defocus(double dx) {
        processing.defocus((float) dx, (float) dx, 0);
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
        processing.defocus((float) dx, (float) dy, (float) angle);
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
        processing.defocus(dx, dy, angle);
    }

    /**
     * Remove the Gaussian defocus
     *
     * @since 0.0.1
     */
    public void removeDefocus() {
        processing.removeDefocus();
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
                for (int index : model.indices)
                    buffer.putInt(index);
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
                for (float pixel : texture.getPixels())
                    buffer.putFloat(pixel);
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
                throw new AssertionError(
                        "Failed to create descriptor pool: " + VulkanSetup.translateVulkanResult(result));
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

    /**
     *
     * Update uniforms for the image to be rendered
     *
     * @param imageIndex Image to be rendered
     *
     * @since 0.0.1
     */
    void updateUniforms(int imageIndex, int passNumber) {
        int n = 0;
        try (MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex), 0,
                    VulkanSetup.UNIFORM_SIZEOF, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, VulkanSetup.UNIFORM_SIZEOF);
                processing.settings.get(n * Float.BYTES, buffer); n += 4;
                modelMatrix.get(n * Float.BYTES, buffer); n += 16;
                VulkanSetup.observer.views.get(passNumber).get(n * Float.BYTES, buffer); n += 16;
                VulkanSetup.observer.projection.get(n * Float.BYTES, buffer); n += 16;
                VulkanSetup.observer.optics.getCenters().get(n * Float.BYTES, buffer); n += 4;
                VulkanSetup.observer.optics.coefficients.get(n * Float.BYTES, buffer); n += 4;
                texture.rgba0.get(n * Float.BYTES, buffer); n += 4;
                texture.rgba1.get(n * Float.BYTES, buffer); n += 4;
                processing.frequency.get(n * Float.BYTES, buffer); n += 4;
                processing.rotation.get(n * Float.BYTES, buffer); n += 4;
                processing.contrast.get(n * Float.BYTES, buffer); n += 4;
                processing.envelope.get(n * Float.BYTES, buffer); n += 4;
                processing.defocus.get(n * Float.BYTES, buffer);
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex));
        }
    }

    /** compute model matrix from MVP */
    void computeModelMatrix() {
        float cx = distance * (float) Math.tan(position.x); // NEEDS REVISION
        float cy = distance * (float) Math.tan(position.y);
        float cz = (float) Math.sqrt(Math.pow(distance, 2) - Math.pow(position.x, 2) - Math.pow(position.y, 2));
        float sx = distance * (float) Math.tan(scale.x / 2);
        float sy = distance * (float) Math.tan(scale.y / 2);
        float sz = scale.z / 2;
        if (sz == 0.0f) sz = 1.0f;
        modelMatrix.identity().translate(cx, cy, cz)
                              .scale(sx, sy, sz);
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