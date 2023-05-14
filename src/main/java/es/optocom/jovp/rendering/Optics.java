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
    Vector2f screenCenter;
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
     * Specify the optics of the system, lens center, screen center, and coefficients
     *
     * @param lensCenter x, y center of the lens, hopefully in degrees of visual angle
     * @param screenCenter x, y center of the lens, hopefully in degrees of visual angle
     * @param coefficients the k1 to k4 coefficients of the Brown-Conrady model for barrel distortions
     * 
     * @since 0.0.1
     */
    Optics(Vector2f lensCenter, Vector2f screenCenter, Vector4f coefficients) {
        this.lensCenter = lensCenter;
        this.screenCenter = screenCenter;
        this.coefficients = coefficients;
    }

    /**
     *
     * Set the center of the lens
     * 
     * @param x x-center of the lens
     * @param y y-center of the lens
     *
     * @since 0.0.1
     */
    void setLensCenter(double x, double y) {
        setLensCenter((float) x, (float) y);
    }

    /**
     *
     * Set the center of the lens
     * 
     * @param x x-center of the lens
     * @param y y-center of the lens
     *
     * @since 0.0.1
     */
    void setLensCenter(float x, float y) {
        lensCenter.x = x;
        lensCenter.y = y;
    }

    /**
     *
     * Set the center of the lens
     * 
     * @param x x-center of the lens
     * @param y y-center of the lens
     *
     * @since 0.0.1
     */
    void setScreenCenter(double x, double y) {
        setScreenCenter((float) x, (float) y);
    }

    /**
     *
     * Set the center of the lens
     * 
     * @param x x-center of the lens
     * @param y y-center of the lens
     *
     * @since 0.0.1
     */
    void setScreenCenter(float x, float y) {
        screenCenter.x = x;
        screenCenter.y = y;
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
     * Get centers of lens and screen
     *
     * @since 0.0.1
     */
    Vector4f getCenters() {
        return new Vector4f(lensCenter.x, lensCenter.y, screenCenter.x, screenCenter.y);
    }

    /**
     *
     * Set defaults, i.e., all zero
     *
     * @since 0.0.1
     */
    void defaults() {
        lensCenter = new Vector2f(0, 0);
        screenCenter = new Vector2f(0, 0);
        coefficients = new Vector4f(0, 0, 0, 0);
    }

}
