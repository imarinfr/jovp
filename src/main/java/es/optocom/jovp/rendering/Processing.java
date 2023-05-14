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

    Vector3i settings = new Vector3i();
    Vector4f frequency = new Vector4f();
    Vector4f contrast = new Vector4f();
    Vector3f rotation = new Vector3f();
    Vector3f envelope = new Vector3f();;
    Vector3f defocus = new Vector3f();;

    /**
     * 
     * Prepare postprocessing
     *
     * @param item The item to apply the post-processing
     *
     * @since 0.0.1
     */
    Processing(TextureType type) {
        settings.x = switch (type) {
            case FLAT -> 0;
            case CHECKERBOARD, SINE, SQUARESINE, G1, G2, G3 -> 1;
            case TEXT, IMAGE -> 2;
        };
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
        this.frequency = new Vector4f(xp, xf, yp, yf);
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
    public void contrast(float r, float g, float b, float a) {
        this.contrast = new Vector4f(r, g, b, a);
    }

    /**
     * 
     * Rotate the texture inside the model
     *
     * @param rotation Angle of rotation in degrees
     * @param pivot    Pivot UV values
     *
     * @since 0.0.1
     */
    public void rotation(double rotation, float[] pivot) {
    }

    /**
     *
     * Add a Gaussian envelope
     *
     * @param sdx   Defocus in diopters on the x-axis
     * @param sdy   Defocus in diopters on the y-axis
     * @param angle Angle
     *
     * @since 0.0.1
     */
    void envelope(EnvelopeType type, float sdx, float sdy, float angle) {
        settings.y = switch(type) {
            case NONE -> 0;
            case SQUARE -> 1;
            case CIRCLE -> 2;
            case GAUSSIAN -> 3;
        };
        envelope.x = (float) Math.toRadians(sdx) / 2;
        envelope.y = (float) Math.toRadians(sdy) / 2;
        envelope.z = (float) Math.toRadians(angle);
    }

    /**
     *
     * Add Gaussian defocus (spherical and astigmatic defocus)
     *
     * @param dx    Defocus in diopters for the x-axis
     * @param dy    Defocus in diopters for the y-axis
     * @param angle Angle
     *
     * @since 0.0.1
     */
    void defocus(float dx, float dy, float angle) {
        settings.z = 1;
        // TODO: convert from Diopters to Gaussian SD in visual angle
        float sdx = dx;
        float sdy = dy;
        defocus.x = (float) Math.toRadians(sdx) / 2;
        defocus.y = (float) Math.toRadians(sdy) / 2;
        defocus.z = (float) Math.toRadians(angle);
    }

    /**
     *
     * Remove envelope
     *
     * @since 0.0.1
     */
    void removeEnvelope() {
        settings.y = 0;
        envelope = new Vector3f();;
    }

    /**
     *
     * Remove defocus
     *
     * @since 0.0.1
     */
    void removeDefocus() {
        settings.z = 0;
        defocus = new Vector3f();;
    }

}
