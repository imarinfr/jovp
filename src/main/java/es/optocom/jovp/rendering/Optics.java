package es.optocom.jovp.rendering;

import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 *
 * All optics-related stuff of the system.
 *
 * @since 0.0.1
 */
class Optics {

    Vector2f lensCenter;
    Vector4f coefficients;

    /**
     * 
     * Set default optics of the system
     * 
     * @since 0.0.1
     */
    Optics() {
        defaults();
    }

    /**
     *
     * Set Brown-Conrady model distortion coefficients
     * 
     * @param k1 coefficient k1
     * @param k2 coefficient k2
     * @param k3 coefficient k3
     * @param k4 coefficient k4
     *
     * @since 0.0.1
     */
    void setCoefficients(double k1, double k2, double k3, double k4) {
        setCoefficients((float) k1, (float) k2, (float) k3, (float) k4);
    }

    /**
     *
     * Set Brown-Conrady model distortion coefficients
     * 
     * @param k1 coefficient k1
     * @param k2 coefficient k2
     * @param k3 coefficient k3
     * @param k4 coefficient k4
     *
     * @since 0.0.1
     */
    void setCoefficients(float k1, float k2, float k3, float k4) {
        coefficients.x = k1;
        coefficients.y = k2;
        coefficients.z = k3;
        coefficients.w = k4;
    }

    /**
     *
     * Set defaults, i.e., all zero
     *
     * @since 0.0.1
     */
    final void defaults() {
        lensCenter = new Vector2f(0, 0);
        coefficients = new Vector4f(0, 0, 0, 0);
    }

}
