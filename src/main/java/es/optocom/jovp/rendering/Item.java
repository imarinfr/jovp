package es.optocom.jovp.rendering;

import java.nio.ByteBuffer;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import es.optocom.jovp.definitions.EnvelopeType;
import es.optocom.jovp.definitions.ProjectionType;
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
    private Vector2d position; // unit vector with (x, y) position in item's units
    private double depth; // distance from the screen in meters
    private Vector3d size; // size in x, y in item's units, and z in meters
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
        this(model, texture, ProjectionType.ORTHOGRAPHIC, Units.ANGLES);
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
    public Item(Model model, Texture texture, ProjectionType projection) {
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
        this(model, texture, ProjectionType.ORTHOGRAPHIC, units);
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
    public Item(Model model, Texture texture, ProjectionType projection, Units units) {
        super(model, texture, projection);
        this.units = units;
        this.position = new Vector2d(0, 0);
        this.depth = Observer.DEFAULT_DEPTH;
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
     * Get position in item's units
     *
     * @return return position x, y in item's units
     *
     * @since 0.0.1
     */
    public Vector2d getPosition() {
        return position;
    }

    /**
     * 
     * Position the item in meters or degrees of visual angle
     * depending on the mode of projection
     *
     * @param x x-axis position in item's units
     * @param y y-axis position in meters of degrees of visual angle
     *
     * Computing meters from visual angles needs special consideration as d is
     * interpreted in different ways
     * 
     * For orthographic projection the distance is always between the observer
     * and the screen. d is only to account for occlusion accounting for
     * different depths.
     *
     * For perspective projection the distance is between the observer
     * and the object. d is the z-postion (depth) so the distance between
     * screen and observer needs to be subtracted.
     *
     * @since 0.0.1
     */
    public void position(double x, double y) {
        position.x = x;
        position.y = y;
        updateModelMatrix();
    }

    /**
     * 
     * Change the depth position of the item
     *
     * @param depth
     *
     * @since 0.0.1
     */
    public void depth(double depth) {
        this.depth = depth;
        updateModelMatrix();
    }

    /**
     * 
     * Get item's distance in meters from the eye. For ORTHOGRAPHIC projection
     * all objects are the same as the observer's distance for all computational
     * purposes
     *
     * @return distance in meters
     *
     * @since 0.0.1
     */
    public double getDistance() {
        return switch (projectionType) {
            case ORTHOGRAPHIC -> VulkanSetup.observer.getDistanceM();
            case PERSPECTIVE -> depth + VulkanSetup.observer.getDistanceM();
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
     * Set item size in corresponding units
     *
     * @param x Size along the x-axis
     * @param y Size along the y-axis
     * @param z Size along the z-axis always in meters
     *
     * @since 0.0.1
     */
    public void size(double x, double y, double z) {
        size.x = x;
        size.y = y;
        size.z = z;
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
            case RIGHT -> draw(stack, commandBuffer, image, 1);
            case BOTH -> {
                draw(stack, commandBuffer, image, 0);
                draw(stack, commandBuffer, image, 1);
            }
            default -> { return; }
        }
    }

    /** Update uniforms for the image to be rendered */
    private void draw(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int passNumber) {
        ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(passNumber);
        Matrix4f view = switch (VulkanSetup.observer.viewMode) {
            case MONO -> VulkanSetup.observer.view;
            case STEREO -> passNumber == 0 ? VulkanSetup.observer.viewLeft : VulkanSetup.observer.viewRight;
        };
        Matrix4f projection = switch(projectionType) {
            case ORTHOGRAPHIC -> projection = switch (VulkanSetup.observer.viewMode) {
                case MONO -> VulkanSetup.observer.orthographic;
                case STEREO -> passNumber == 0 ? VulkanSetup.observer.orthographicLeft : VulkanSetup.observer.orthographicRight;
            };
            case PERSPECTIVE -> projection = switch (VulkanSetup.observer.viewMode) {
                case MONO -> VulkanSetup.observer.perspective;
                case STEREO -> passNumber == 0 ? VulkanSetup.observer.perspectiveLeft : VulkanSetup.observer.perspectiveRight;
            };
        };

        Optics optics = passNumber == 0 ? VulkanSetup.observer.opticsLeft : VulkanSetup.observer.opticsRight;
        updateUniforms(image, passNumber, view, projection, optics);
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
        Vector4f frequency = processing.getFrequency(sizeUnitsToAngles());
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
        Vector3d scale  = switch (units) {
            case ANGLES, SPHERICAL -> new Vector3d(sizeAnglesToMeters(size.x), sizeAnglesToMeters(size.y), size.z);
            case PIXELS -> new Vector3d(xPixelsToMeters(size.x), yPixelsToMeters(size.y), size.z);
            case METERS -> new Vector3d(size.x, size.y, size.z);
        };
        scale.mul(0.5);
        Vector3d pos = switch (units) {
            case ANGLES, SPHERICAL -> new Vector3d(anglesToMeters(position.x), anglesToMeters(position.y), depth);
            case PIXELS -> new Vector3d(xPixelsToMeters(position.x), yPixelsToMeters(position.y), depth);
            case METERS -> new Vector3d(position.x, position.y, depth);
        };


        //x = Math.toRadians(((x + 180) % 360 + 360) % 360 - 180);
        //y = Math.toRadians(((y + 180) % 360 + 360) % 360 - 180);
        //double theta;
        //if (x == 0) theta = y;
        //else theta = Math.atan(Math.cos(x) * Math.tan(y));
        //position.x = distance * Math.cos(theta) * Math.sin(x);
        //position.y = distance * Math.sin(theta);
        Quaterniond quaternion = new Quaterniond();
        if (units == Units.SPHERICAL)
            quaternion.rotationTo(new Vector3d(0, 0, VulkanSetup.observer.getDistanceM()),
                                  new Vector3d(anglesToMeters(position.x), anglesToMeters(position.y), depth));
        quaternion.rotateZYX(rotation.z, rotation.y, rotation.x);
        modelMatrix.translationRotateScale(pos, quaternion, scale);
    }

    /** from visual angles to meters */
    private double anglesToMeters(double ang) {
        return getDistance() * Math.tan(Math.toRadians(ang));
    }

    /** from meters to visual angles */
    private double metersToAngles(double m) {
        return Math.toDegrees(Math.atan(m / getDistance()));
    }

    /** computes meters from pixels for the x axis */ 
    private double xPixelsToMeters(double x) {
        return VulkanSetup.observer.window.getMonitor().getPixelWidthM() * x;
    }

    /** computes meters from pixels for the y axis */ 
    private double yPixelsToMeters(double y) {
        return VulkanSetup.observer.window.getMonitor().getPixelHeightM() * y;
    }

    /** size from visual angles to meters */
    private double sizeAnglesToMeters(double ang) {
        return 2.0 * getDistance() * Math.tan(Math.toRadians(ang) / 2.0);
    }
    
    /** returns the size in visual angles */ 
    private Vector2d sizeUnitsToAngles() {
        return switch (units) {
            case ANGLES, SPHERICAL -> new Vector2d(size.x, size.y);
            case METERS -> new Vector2d(metersToAngles(size.x), metersToAngles(size.y));
            case PIXELS -> new Vector2d(xPixelsToMeters(size.x), xPixelsToMeters(size.y));
        };
    }


}