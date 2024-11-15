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
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import org.lwjgl.vulkan.VkCommandBuffer;

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
    private Vector2d position; // unit vector with (x, y) position in item's units
    private double depth; // distance from the screen in meters
    private Vector3d size; // size in x, y in item's units, and z in meters
    private Vector3d rotation; // angles of rotation in each axis in radians
    private Matrix4d modelMatrix; // model matrix
    private Processing processing; // Post-processing things

    /**
     * 
     * Create an item for psychophysics experience with default units ANGLES
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture) {
        this(model, texture, Units.ANGLES);
    }

    /**
     * 
     * Create an item for psychophysics experience
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     * @param units units of measurement (METERS, ANGLES of vision, PIXELS or angles on a SPHERICAL surface)
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture, Units units) {
        super(model, texture);
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
    @Override
     public void update(Texture texture) {
        super.update(texture);
        processing.setType(texture.getType());
    }

    /**
     * 
     * Position the item in meters or degrees of visual angle
     * depending on the mode of projection
     *
     * @param x x-axis position in item's units
     * @param y y-axis position in meters of degrees of visual angle
     *
     * @since 0.0.1
     */
    public void position(double x, double y) {
        if (units == Units.ANGLES | units == Units.SPHERICAL) { // from 0 to 360
            x = ((x + 180) % 360 + 360) % 360 - 180;
            y = ((y + 180) % 360 + 360) % 360 - 180;
        }
        position.x = x;
        position.y = y;
        updateModelMatrix();
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
     * Get item's depth (distance in meters from the screen)
     *
     * @return depth in meters
     *
     * @since 0.0.1
     */
    public double getDepth() {
        return depth;
    }

    /**
     * 
     * Get item's distance in meters from the eye
     *
     * @return distance in meters
     *
     * @since 0.0.1
     */
    public double getDistance() {
        return switch (VulkanSetup.observer.projection) {
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
     * @param size Size along the x and y axes in degrees fo visual angle
     *
     * @since 0.0.1
     */
    public void size(double s) {
        size(s, s, 0);
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
    public void size(double sx, double sy) {
        size(sx, sy, 0);
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
        Observer observer = VulkanSetup.observer;
        Matrix4f view = switch (observer.viewMode) {
            case MONO -> observer.view;
            case STEREO -> observer.projection == Projection.ORTHOGRAPHIC ? observer.view : passNumber == 0 ? observer.viewLeft : observer.viewRight;
        };
        Matrix4f projection = switch(observer.projection) {
            case ORTHOGRAPHIC -> projection = switch (observer.viewMode) {
                case MONO -> observer.orthographic;
                case STEREO -> passNumber == 0 ? observer.orthographicLeft : observer.orthographicRight;
            };
            case PERSPECTIVE -> projection = switch (observer.viewMode) {
                case MONO -> observer.perspective;
                case STEREO -> passNumber == 0 ? observer.perspectiveLeft : observer.perspectiveRight;
            };
        };

        Optics optics = passNumber == 0 ? observer.opticsLeft : observer.opticsRight;
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
        Vector3d pos = worldPosition();
        Quaterniond quaternion = (units == Units.SPHERICAL ? sphericalRotation(pos) : new Quaterniond()).rotateZYX(rotation.z, rotation.y, rotation.x);
        modelMatrix.translationRotateScale(pos, quaternion, getScale());
    }

    /** Spherical rotation */
    private Quaterniond sphericalRotation(Vector3d position) {
        return new Quaterniond().rotationTo(new Vector3d(0, 0, 1), new Vector3d(position)
                                .add(0, 0, VulkanSetup.observer.getDistanceM()).normalize());
    }

    /** get position within the world */
    private Vector3d worldPosition() {
        return switch (units) {
            case ANGLES -> new Vector3d(anglesToMeters(position.x), anglesToMeters(position.y), depth);
            case SPHERICAL -> {
                double phi = Math.toRadians(position.x);
                double theta = position.x == 0 ? Math.toRadians(position.y) : Math.atan(Math.cos(phi) * Math.tan(Math.toRadians(position.y)));
                yield new Vector3d(Math.cos(theta) * Math.sin(phi), Math.sin(theta), Math.cos(theta) * Math.cos(phi)).mul(depth + VulkanSetup.observer.getDistanceM()).add(0, 0, -VulkanSetup.observer.getDistanceM());
            }
            case PIXELS -> new Vector3d(xPixelsToMeters(position.x), yPixelsToMeters(position.y), depth);
            case METERS -> new Vector3d(position.x, position.y, depth);
        };
    }

    /** get scale factor for the item */
    private Vector3d getScale() {
        return (switch (units) {
            case ANGLES, SPHERICAL -> new Vector3d(sizeAnglesToMeters(size.x), sizeAnglesToMeters(size.y), size.z);
            case PIXELS -> new Vector3d(xPixelsToMeters(size.x), yPixelsToMeters(size.y), size.z);
            case METERS -> new Vector3d(size.x, size.y, size.z);
        }).mul(0.5);
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