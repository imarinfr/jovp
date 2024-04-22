package es.optocom.jovp.rendering;

import java.nio.ByteBuffer;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import es.optocom.jovp.definitions.EnvelopeType;
import es.optocom.jovp.definitions.Projection;
import es.optocom.jovp.definitions.Units;
import es.optocom.jovp.definitions.ViewEye;
import es.optocom.jovp.definitions.ViewMode;

/**
 * 
 * Item to construct the psychophysical experience
 *
 * @since 0.0.1
 */
public class Item extends Renderable {

    private Units units; // units to use for the item
    private Vector3d position; // unit vector with (x, y, z) position in meters
    private Vector3d size; // size in x, y, and z in meters
    private Vector3d rotation; // angles of rotation in each axis in radians
    private Matrix4d modelMatrix; // model matrix
    private Processing processing; // Post-processing things

    /**
     * 
     * Create an item for psychophysics experience with default projection
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture) {
        this(model, texture, Projection.ORTHOGRAPHIC, Units.ANGLES);
    }

    /**
     * 
     * Create an item for psychophysics experience with default projection
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     * @param projection Either ORTHOGRAPHIC or PERSPECTIVE
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture, Projection projection) {
        this(model, texture, projection, Units.ANGLES);
    }

    /**
     * 
     * Create an item for psychophysics experience with default projection
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     * @param units units of measurement (METERS, ANGLES of vision, PIXELS or angles on a SPHERICAL surface)
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture, Units units) {
        this(model, texture, Projection.ORTHOGRAPHIC, units);
    }

    /**
     * 
     * Create an item for psychophysics experience
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     * @param projection Either ORTHOGRAPHIC or PERSPECTIVE
     * @param units units of measurement (METERS, ANGLES of vision, PIXELS or angles on a SPHERICAL surface)
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture, Projection projection, Units units) {
        super(model, texture, projection);
        this.units = units;
        this.position = new Vector3d(0, 0, 0);
        this.size = new Vector3d(1, 1, 0);
        this.rotation = new Vector3d();
        this.modelMatrix = new Matrix4d();
        this.processing = new Processing(texture.getType());
    }

    /**
     * 
     * Update the texture of the item
     * 
     * @param texture The new texture
     * 
     * @since 0.0.1
     */
    public void update(Texture texture) {
        super.update(texture);
        processing.setType(texture.getType());
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
        return position;
    }

    /**
     * 
     * Position the item in meters or degrees of visual angle
     * depending on the mode of projection
     *
     * @param x x-axis position in meters of degrees of visual angle
     * @param y y-axis position in meters of degrees of visual angle
     *
     * @since 0.0.1
     */
    public void position(double x, double y) {
        position(x, y, position.z);
        //if (coordinates == Coordinates.SPHERICAL) {
        //    d = position.length();
        //    if (Double.isNaN(d)) d = VulkanSetup.observer.getDistanceM();
        //}
    }

    /**
     * 
     * Position the item
     *
     * @param x x-axis position in degrees of visual angle, meters or pixels
     * @param y y-axis position in degrees of visual angle, meters or pixels
     * @param d z-axis position or radial distance in meters
     *
     * @since 0.0.1
     */
    public void position(double x, double y, double d) {
        switch (units) {
            case ANGLES -> {
                double eye = VulkanSetup.observer.getDistanceM();
                x = Math.toRadians(((x + 180) % 360 + 360) % 360 - 180);
                y = Math.toRadians(((y + 180) % 360 + 360) % 360 - 180);
                position.x = eye * Math.tan(x);
                position.y = eye * Math.tan(y);
                position.z = d;
            }
            case METERS -> {
                position.x = x;
                position.y = y;
                position.z = d;
            }
            case PIXELS -> {
                position.x = VulkanSetup.observer.window.getMonitor().getPixelWidthM() * x;
                position.y = VulkanSetup.observer.window.getMonitor().getPixelHeightM() * y;
                position.z = d;
            }
            case SPHERICAL -> {
                double eye = VulkanSetup.observer.getDistanceM();
                x = Math.toRadians(((x + 180) % 360 + 360) % 360 - 180);
                y = Math.toRadians(((y + 180) % 360 + 360) % 360 - 180);
                double theta;
                if (x == 0) theta = y;
                else theta = Math.atan(Math.cos(x) * Math.tan(y));
                position.x = eye * Math.cos(theta) * Math.sin(x);
                position.y = eye * Math.sin(theta);
                position.z = eye * Math.cos(theta) * Math.cos(x);
            }
        }
        updateModelMatrix();
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
        position.z = distance + Observer.ZNEAR;
            //case SPHERICAL -> {
            //    double sc = distance / position.length();
            //    position = position.normalize().mul(distance);
            //    size.mul(sc);
            //}
        updateModelMatrix();
    }

    /**
     * 
     * Get distance in meters from the eye
     *
     * @return distance in meters
     *
     * @since 0.0.1
     */
    public double getDistance() {
        return switch(units) {
            case SPHERICAL -> position.length();
            default -> position.z;
        };
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
        return size;
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
        size(x, x, 0);
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
     * Set item size in meters or degrees of visual angle
     *
     * @param x Size along the x-axis
     * @param y Size along the y-axis
     * @param z Size along the z-axis always in meters
     *
     * @since 0.0.1
     */
    public void size(double x, double y, double z) {
        if(Math.abs(x) < 0) x = 0; if(Math.abs(y) < 0) y = 0; if(Math.abs(z) < 0) z = 0;
        size = switch (units) {
            case ANGLES -> anglesToMeters(x, y, z);
            case METERS -> new Vector3d(x, y, z);
            case PIXELS -> pixelsToMeters(x, y, z);
            case SPHERICAL -> anglesToMetersSpherical(x, y, z);
        };
        updateModelMatrix();
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
        updateModelMatrix();
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
        contrast(amplitude, amplitude, amplitude, 1);
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
        processing.rotation(0.5, 0.5, rotation);
    }

    /**
     * 
     * Rotate the texture inside the model
     *
     * @param ucenter Pivot on the u axis in degrees of visual angle from the center
     * @param vcenter pivot on the v axis in degrees of visual angle from the center
     * @param rotation Angle of rotation in degrees
     *
     * @since 0.0.1
     */
    public void texRotation(double ucenter, double vcenter, double rotation) {
        processing.rotation(ucenter, vcenter, rotation);
    }

    /**
     * Add an envelope
     *
     * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
     * @param len Envelope x- and y-axis window half size or (standard deviation if Gaussian)
     *
     * @since 0.0.1
     */
    public void envelope(EnvelopeType type, double len) {
        envelope(type, len, len, 0);
    }

    /**
     * Add an envelope
     *
     * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
     * @param x Envelope x-axis window half size or (standard deviation if Gaussian)
     * @param y Envelope y-axis window half size or (standard deviation if Gaussian)
     *
     * @since 0.0.1
     */
    public void envelope(EnvelopeType type, double x, double y) {
        envelope(type, x, y, 0);
    }

    /**
     * Add an envelope
     *
     * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
     * @param x Envelope x-axis window half size or (standard deviation if Gaussian)
     * @param y Envelope y-axis window half size or (standard deviation if Gaussian)
     * @param angle Angle
     *
     * @since 0.0.1
     */
    public void envelope(EnvelopeType type, double x, double y, double angle) {
        if (units == Units.ANGLES | units == Units.SPHERICAL) {
            double distance = VulkanSetup.observer.getDistanceM();
            x = distance * Math.tan(Math.toRadians(x));
            y = distance * Math.tan(Math.toRadians(y));
        }
        processing.envelope(type, x, y, angle);
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
     * 
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
     * 
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
     * 
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
     * 
     * Remove the Gaussian defocus
     *
     * @since 0.0.1
     */
    public void removeDefocus() {
        processing.removeDefocus();
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
    @Override
     void render(MemoryStack stack, VkCommandBuffer commandBuffer, int image) {
        if (viewEye == ViewEye.NONE) return;
        if (VulkanSetup.observer.viewMode == ViewMode.MONO) {
            draw(stack, commandBuffer, image, 0);
            return;
        }
        switch (viewEye) {
            case LEFT -> draw(stack, commandBuffer, image, 0);
            case RIGHT-> draw(stack, commandBuffer, image, 1);
            case BOTH-> {
                draw(stack, commandBuffer, image, 0);
                draw(stack, commandBuffer, image, 1);
            }
            default-> {return;}
        }
    }

    /** Update uniforms for the image to be rendered */
    private void draw(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int passNumber) {
        ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(passNumber);
        Matrix4f view;
        Matrix4f proj;
        Optics optics;
        switch (VulkanSetup.observer.viewMode) {
            case STEREO -> {
                view = passNumber == 0 ? VulkanSetup.observer.viewLeft : VulkanSetup.observer.viewRight;
                proj = switch (projection) {
                    case ORTHOGRAPHIC -> passNumber == 0 ? VulkanSetup.observer.orthoLeft : VulkanSetup.observer.orthoRight;
                    case PERSPECTIVE -> passNumber == 0 ? VulkanSetup.observer.perspLeft : VulkanSetup.observer.perspRight;
                };
                optics = passNumber == 0 ? VulkanSetup.observer.opticsLeft : VulkanSetup.observer.opticsRight;
            }
            default -> {
                view = VulkanSetup.observer.viewCyclops;
                proj = switch (projection) {
                    case ORTHOGRAPHIC -> VulkanSetup.observer.orthoCyclops;
                    case PERSPECTIVE -> VulkanSetup.observer.perspCyclops;
                };
                optics = VulkanSetup.observer.opticsCyclops;
            }
        }
        updateUniforms(image, passNumber, view, proj, optics);
        draw(stack, commandBuffer, image, passNumber, viewPass.graphicsPipeline, viewPass.graphicsPipelineLayout);
    }

    /**
     *
     * Update uniforms for the image to be rendered
     *
     * @param image Image to be rendered
     * @param view View matrix
     * @param projection Projection matrix
     * @param optics optics
     *
     * @since 0.0.1
     */
    void updateUniforms(int image, int eye, Matrix4f view, Matrix4f projection, Optics optics) {
        Vector4f frequency = processing.getFrequency(metersToAngles(size));
        try (MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, getUniformMemory(image, eye), 0, UNIFORM_SIZEOF, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, UNIFORM_SIZEOF);
                int n = 0;
                processing.settings.get(n * Float.BYTES, buffer); n += 4;
                new Matrix4f(modelMatrix).get(n * Float.BYTES, buffer); n += 16;
                view.get(n * Float.BYTES, buffer); n += 16;
                projection.get(n * Float.BYTES, buffer); n += 16;
                optics.lensCenter.get(n * Float.BYTES, buffer); n += 4;
                optics.coefficients.get(n * Float.BYTES, buffer); n += 4;
                getTexture().rgba0.get(n * Float.BYTES, buffer); n += 4;
                getTexture().rgba1.get(n * Float.BYTES, buffer); n += 4;
                frequency.get(n * Float.BYTES, buffer); n += 4;
                processing.getRotation(frequency).get(n * Float.BYTES, buffer); n += 4;
                processing.contrast.get(n * Float.BYTES, buffer); n += 4;
                processing.getEnvelope(size).get(n * Float.BYTES, buffer); n += 4;
                processing.defocus.get(n * Float.BYTES, buffer);
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, getUniformMemory(image, eye));
        }
    }

    /** update model matrix */
    private void updateModelMatrix() {
        Vector3d scale = new Vector3d();
        size.mul(0.5, scale);
        Quaterniond quaternion = new Quaterniond();
        if (units == Units.SPHERICAL) {
            Vector3d direction = new Vector3d(0, 0, Observer.ZNEAR);
            if (position.length() > 0) direction = position;
            quaternion.rotationTo(new Vector3d(0, 0, 1), direction);
        }
        quaternion.rotateZYX(rotation.z, rotation.y, rotation.x);
        modelMatrix.translationRotateScale(position, quaternion, scale);
    }

    /** computes the x and y in meters from visual angles */ 
    private Vector3d anglesToMeters(double x, double y, double z) {
        return new Vector3d(
            2 * VulkanSetup.observer.getDistanceM() * Math.tan(Math.toRadians(x) / 2),
            2 * VulkanSetup.observer.getDistanceM() * Math.tan(Math.toRadians(y) / 2),
            z);
    }

    /** from visual angles to meters */
    private Vector3d metersToAngles(Vector3d size) {
        return new Vector3d(
            2.0 * Math.toDegrees(Math.atan(size.x / 2 / VulkanSetup.observer.getDistanceM())),
            2.0 * Math.toDegrees(Math.atan(size.y / 2 / VulkanSetup.observer.getDistanceM())),
            size.z);
    }

    /** computes the x and y in meters from visual angles for spherical projection */
    private Vector3d anglesToMetersSpherical(double x, double y, double z) {
        return new Vector3d(
            2 * position.length() * Math.tan(Math.toRadians(x) / 2),
            2 * position.length() * Math.tan(Math.toRadians(y) / 2),
            z);
    }

    /** from visual angles to meters for spherical projection TODO
    private Vector3d metersToAnglesSpherical(Vector3d size) {
        return new Vector3d(
            2.0 * Math.toDegrees(Math.atan(size.x / 2 / position.length())),
            2.0 * Math.toDegrees(Math.atan(size.y / 2 / position.length())),
            size.z);
    }
    */

    /** computes the x and y in meters from pixels */ 
    private Vector3d pixelsToMeters(double x, double y, double z) {
        return  new Vector3d(
            VulkanSetup.observer.window.getMonitor().getPixelWidthM() * x,
            VulkanSetup.observer.window.getMonitor().getPixelHeightM() * y,
            z);
    }

}