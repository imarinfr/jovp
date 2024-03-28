package es.optocom.jovp.rendering;

import java.nio.ByteBuffer;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import es.optocom.jovp.definitions.EnvelopeType;
import es.optocom.jovp.definitions.Projection;
import es.optocom.jovp.definitions.Units;

/**
 * 
 * Item to construct the psychophysical experience
 *
 * @since 0.0.1
 */
public class Item extends Renderable {

    private Projection projection; // whether to apply orthographic or perspective projection
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
        super(model, texture);
        this.projection = projection;
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

    public void setProjection(Projection projection) {
        this.projection = projection;
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
                position.z = d + Observer.ZNEAR;
            }
            case METERS -> {
                position.x = x;
                position.y = y;
                position.z = d + Observer.ZNEAR;
            }
            case PIXELS -> {
                position.x = VulkanSetup.observer.window.getMonitor().getPixelWidthM() * x;
                position.y = VulkanSetup.observer.window.getMonitor().getPixelHeightM() * y;
                position.z = d + Observer.ZNEAR;
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
        // TODO
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
        switch (units) {
            case ANGLES -> {
                double distance = VulkanSetup.observer.getDistanceM();
                size.x = 2 * distance * Math.tan(Math.toRadians(x) / 2);
                size.y = 2 * distance * Math.tan(Math.toRadians(y) / 2);
                size.z = z;
            }
            case METERS -> {
                size.x = x;
                size.y = y;
                size.z = z;
            }
            case PIXELS -> {
                size.x = VulkanSetup.observer.window.getMonitor().getPixelWidthM() * x;
                size.y = VulkanSetup.observer.window.getMonitor().getPixelHeightM() * y;
                size.z = z;
            }
            case SPHERICAL -> {
                double distance;
                if (units == Units.ANGLES) distance = position.z;
                else distance = position.length();
                size.x = 2 * distance * Math.tan(Math.toRadians(x) / 2);
                size.y = 2 * distance * Math.tan(Math.toRadians(y) / 2);
                size.z = z;
            }
        }
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
     * TODO: Defocus not functional
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
     * TODO: Defocus not functional
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
     * TODO: Defocus not functional
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
     * Update uniforms for the image to be rendered
     *
     * @param imageIndex Image to be rendered
     *
     * @since 0.0.1
     */
    @Override
    void updateUniforms(int imageIndex, Observer.Eye eye) {
        Vector4f freq = getFrequency();
        Vector3f rot = getRotation(freq);
        try (MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex), 0,
                    VulkanSetup.UNIFORM_SIZEOF, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, VulkanSetup.UNIFORM_SIZEOF);
                int n = 0;
                processing.settings.get(n * Float.BYTES, buffer); n += 4;
                (new Matrix4f(modelMatrix)).get(n * Float.BYTES, buffer); n += 16;
                eye.getView().get(n * Float.BYTES, buffer); n += 16;
                Matrix4f proj = switch (projection) {
                    case ORTHOGRAPHIC -> VulkanSetup.observer.orthographic;
                    case PERSPECTIVE -> VulkanSetup.observer.perspective;
                };
                proj.get(n * Float.BYTES, buffer); n += 16;
                eye.optics.lensCenter.get(n * Float.BYTES, buffer); n += 4;
                eye.optics.coefficients.get(n * Float.BYTES, buffer); n += 4;
                texture.rgba0.get(n * Float.BYTES, buffer); n += 4;
                texture.rgba1.get(n * Float.BYTES, buffer); n += 4;
                freq.get(n * Float.BYTES, buffer); n += 4;
                rot.get(n * Float.BYTES, buffer); n += 4;
                processing.contrast.get(n * Float.BYTES, buffer); n += 4;
                getEnvelope().get(n * Float.BYTES, buffer); n += 4;
                processing.defocus.get(n * Float.BYTES, buffer);
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex));
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

    /** get frequency parameters to send to the shader */
    private Vector4f getFrequency() {
        Vector2d angle = visualAngle();
        Vector4f freq = new Vector4f();
        freq.x = processing.frequency.x / (float) size.x;
        freq.y = processing.frequency.y / (float) size.y;
        if (processing.frequency.z == 0) freq.z = 1;
        else freq.z = processing.frequency.z * (float) angle.x;
        if (processing.frequency.w == 0) freq.w = 1;
        else freq.w = processing.frequency.w * (float) angle.y;
        return freq;
    }

    /** get texture rotation parameters to send to the shader */
    private Vector3f getRotation(Vector4f freq) {
        return new Vector3f(
            freq.z * processing.rotation.x,
            freq.w * processing.rotation.y,
            processing.rotation.z
        );
    }

    /** get texture rotation parameters to send to the shader */
    private Vector3f getEnvelope() {
        return new Vector3f(
            processing.envelope.x / (float) size.x,
            processing.envelope.y / (float) size.y,
            processing.envelope.z
        );
    }

    /** computes the size visual angle */ 
    private Vector2d visualAngle() {
        double distance = VulkanSetup.observer.getDistanceM();
        return new Vector2d(
            2 * Math.toDegrees(Math.tan(size.x / 2 / distance)),
            2 * Math.toDegrees(Math.tan(size.y / 2 / distance))
        );
    }
    
}