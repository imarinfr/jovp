package es.optocom.jovp.rendering;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import es.optocom.jovp.definitions.EnvelopeType;
import es.optocom.jovp.definitions.TextureType;

/**
 *
 * Postprocessing information
 *
 * @since 0.0.1
 */
class Processing {

    Vector3i settings = new Vector3i(); // x = texture type, y = envelope type, z = apply defocus (1) or not (0)
    Vector4f frequency = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f); // x = x phase y = y phase, z = x frequency, w = y frequency
    Vector4f contrast = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f); // xyzw = amplitudes for R, G, B, and alpha channels
    Vector3f rotation = new Vector3f(); // texture rotation around u and v values (x and y) and by an angle of z radians.
    Vector3f envelope = new Vector3f(); // xy = SD of ellipse x and y axes, z = ellipse rotation in radians
    //TODO: Defocus not functional
    Vector3f defocus = new Vector3f(); // xy = Geometric defocus x and y axes, z = astigmatism axis

    TextureType type;

    /**
     * 
     * Prepare postprocessing
     *
     * @param type Texture type
     *
     * @since 0.0.1
     */
    Processing(TextureType type) {
        setType(type);
    }

    /**
     * 
     * Get texture type for postprocessing
     *
     * @return Texture type
     *
     * @since 0.0.1
     */
    public TextureType getType() {
        return type;
    }

    /**
     * 
     * Set texture type for postprocessing
     *
     * @param type Texture type
     *
     * @since 0.0.1
     */
    public void setType(TextureType type) {
        this.type = type;
        settings.x = switch (type) {
            case FLAT -> 0;
            case CHECKERBOARD, SINE, SQUARESINE, G1, G2, G3 -> 1;
            case IMAGE -> 2;
            case TEXT -> -1;
        };
    }

    /**
     * 
     * Spatial frequency properties of the texture
     *
     * @param xp Phase on the x-axis in degrees
     * @param xf Frequency on the x-axis on cycles per degree
     * @param yp Phase on the y-axis in degrees
     * @param yf Frequency on the y-axis on cycles per degree
     *
     * @since 0.0.1
     */
    public void frequency(double xp, double xf, double yp, double yf) {
        this.frequency = new Vector4f((float) (Math.toRadians(xp) / (2 * Math.PI)),
                                      (float) (Math.toRadians(yp) / (2 * Math.PI)),
                                      (float) xf, (float) yf);
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
        this.contrast = new Vector4f((float) r, (float) g, (float) b, (float) a);
    }

    /**
     * 
     * Rotate the texture inside the model
     *
     * @param u Pivot on the u axis
     * @param v pivot on the v axis
     * @param rotation Angle of rotation in degrees
     *
     * @since 0.0.1
     */
    public void rotation(double u, double v, double rotation) {
        this.rotation = new Vector3f((float) u, (float) v, (float) Math.toRadians(rotation));
    }

    /**
     *
     * Add a Gaussian envelope
     *
     * @param type Envelope type
     * @param sdx Envelope Gaussian standard deviation for x-axis in degrees of visual angle
     * @param sdy Envelope Gaussian standard deviation for y-axis in degrees of visual angle
     * @param angle Angle of rotation in degrees
     *
     * @since 0.0.1
     */
    void envelope(EnvelopeType type, double sdx, double sdy, double angle) {
        settings.y = switch(type) {
            case NONE -> 0;
            case SQUARE -> 1;
            case CIRCLE -> 2;
            case GAUSSIAN -> 3;
        };
        envelope.x = (float) sdx;
        envelope.y = (float) sdy;
        envelope.z = (float) Math.toRadians(angle);
    }

    /**
     *
     * Remove envelope
     *
     * @since 0.0.1
     */
    void removeEnvelope() {
        settings.y = 0;
        envelope = new Vector3f();
    }

    /**
     *
     * Add Gaussian defocus (spherical and astigmatic defocus)
     *
     * @param dx Defocus in diopters for the x-axis
     * @param dy Defocus in diopters for the y-axis
     * @param angle Angle
     * 
     * TODO: Defocus not functional
     *
     * @since 0.0.1
     */
    void defocus(double dx, double dy, double angle) {
        settings.z = 0;
        //double sdx = dx;
        //double sdy = dy;
        //defocus.x = (float) Math.toRadians(sdx) / 2;
        //defocus.y = (float) Math.toRadians(sdy) / 2;
        //defocus.z = (float) Math.toRadians(angle);
    }

    /**
     *
     * Remove defocus
     * 
     * TODO: Defocus not functional
     *
     * @since 0.0.1
     */
    void removeDefocus() {
        settings.z = 0;
        defocus = new Vector3f();
    }

}