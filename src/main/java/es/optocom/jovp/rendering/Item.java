package es.optocom.jovp.rendering;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.EnvelopeType;
import es.optocom.jovp.definitions.ViewMode;

/**
 * 
 * Item to construct the psychophysical experience
 *
 * @since 0.0.1
 */
public class Item extends Renderable {

    private double distance = (Observer.ZFAR - Observer.ZNEAR) / 2; // distance of the item in meters
    private Vector3d scale; // size in x, y, and z in meters (size = 2 * scale)
    private Vector3d rotation; // angles of rotation in each axis in radians
    private Vector3d direction; // unit vector with (x, y, z) direction in meters
    private Matrix4d modelMatrix; // model matrix
    private Processing processing; // Post-processing things

    private boolean updateModelMatrix = false;

    /**
     * 
     * Create an item for psychophysics experience
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture) {
        super(model, texture);
        direction = new Vector3d(0, 0, 1);
        scale = new Vector3d(1, 1, 1);
        rotation = new Vector3d();
        modelMatrix = new Matrix4d();
        processing = new Processing(texture.getType());
    }

    /**
     * 
     * Get size in meters
     *
     * @return return position in x, y, and z in meters
     *
     * @since 0.0.1
     */
    public Vector3d getPosition() {
        return (new Vector3d(distance * direction.x,
                             distance * direction.y,
                             distance * direction.z));
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
        double xp = Math.toRadians(x);
        double yp = Math.toRadians(y);
        direction.x = Math.sin(xp);
        direction.y = Math.sin(yp);
        direction.z = Math.cos(xp) * Math.cos(yp);
        updateModelMatrix = true;
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
        if(distance == 0) distance = Observer.ZNEAR;
        else if(Math.abs(distance) < Observer.ZNEAR)
            distance = Math.signum(distance) * Observer.ZNEAR;
        // recalculate scale
        double sc = Math.abs(distance / this.distance);
        scale.x = sc * scale.x;
        scale.y = sc * scale.y;
        scale.z = sc * scale.z;
        this.distance = distance;
        updateModelMatrix = true;
    }

    /**
     * 
     * Get distance in meters
     *
     * @return distance in meters
     *
     * @since 0.0.1
     */
    public double getDistance() {
        return distance;
    }

    /**
     * 
     * Get size in meters
     *
     * @return size in x, y, and z in meters
     *
     * @since 0.0.1
     */
    public Vector3d getSize() {
        return (new Vector3d(2 * scale.x, 2 * scale.y, 2 * scale.z));
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
        if(Math.abs(x) < Observer.ZNEAR) x = Observer.ZNEAR; // size must always be positive
        if(Math.abs(y) < Observer.ZNEAR) y = Observer.ZNEAR;
        if(Math.abs(z) < Observer.ZNEAR) z = Observer.ZNEAR;
        scale.x = distance * Math.tan(Math.toRadians(Math.abs(x)) / 2);
        scale.y = distance * Math.tan(Math.toRadians(Math.abs(y)) / 2);
        scale.z = Math.abs(z) / 2;
        updateModelMatrix = true;
    }

    /**
     * Rotate the item
     *
     * @param z Angle of rotation in degrees
     *
     * @since 0.0.1
     */
    public void rotation(double z) {
        rotation(0, 0, z);
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
        rotation = new Vector3d(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
        updateModelMatrix = true;
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
        processing.frequency(xp, xf, yp, yf);
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
        processing.frequency(xp, xf, xp, xf);
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
        processing.contrast(r, g, b, a);
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
        processing.rotation(rotation, new double[] { 0.5, 0.5 });
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
        processing.envelope(type, sd, sd, 0);
    }

    /**
     * Add an envelope
     *
     * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
     * @param sdx Standard deviation in meters for the x-axis
     * @param sdy Standard deviation in meters for the y-axis
     *
     * @since 0.0.1
     */
    public void envelope(EnvelopeType type, double sdx, double sdy) {
        processing.envelope(type, sdx, sdy, 0);
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
        processing.envelope(type, sdx, sdy, angle);
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
        processing.defocus(dx, dx, 0);
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
        processing.defocus(dx, dy, angle);
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
     * 
     * Render item
     *
     * @param stack Memory stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     *
     * @since 0.0.1
     */
    @Override
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
     * @param stack  stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     * @param passNumber pass number. For MONO vision, it ought to be 0. For
     *                   STEREO, left is 0 and right is 1
     *
     * @since 0.0.1
     */
    @Override
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
                viewPass.graphicsPipelineLayout, 0,
                stack.longs(descriptorSets.get(image)), null);
        vkCmdDrawIndexed(commandBuffer, model.length, 1,
                0, 0, 0);
    }

    /**
     *
     * Update uniforms for the image to be rendered
     *
     * @param imageIndex Image to be rendered
     *
     * @since 0.0.1
     */
    @Override
     void updateUniforms(int imageIndex, int passNumber) {
        if (updateModelMatrix) {
            Vector3d position = new Vector3d(direction.x, direction.y, direction.z).mul(distance);
            Quaterniond quaternion = new Quaterniond()
                .rotationTo(new Vector3d(0, 0, 1), direction)
                .rotateZYX(rotation.z, rotation.y, rotation.x);
            modelMatrix.translationRotateScale(position, quaternion, scale);
            updateModelMatrix = false;
        }
        int n = 0;
        try (MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex), 0,
                    VulkanSetup.UNIFORM_SIZEOF, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, VulkanSetup.UNIFORM_SIZEOF);
                processing.settings.get(n * Float.BYTES, buffer); n += 4;
                (new Matrix4f(modelMatrix)).get(n * Float.BYTES, buffer); n += 16;
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

}