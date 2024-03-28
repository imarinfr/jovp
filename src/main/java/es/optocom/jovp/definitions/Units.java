package es.optocom.jovp.definitions;

/**
 * 
 * Units. The z-axis in all units are meters. The x and y can be
 * in METERS, ANGLES (degrees of visual angle), PIXELS, or in
 * ANGLES but projected on a SPHERICAL surface instead of a plane
 *
 * @since 0.0.1
 */
public enum Units {
    /** Degrees of visual angle from observer in a flat world
     * (z-axis in meters from the plane in the screen)
    */
    ANGLES,
    /** Distance from camera (in meters) */
    METERS,
    /** pixels from camera (z-axis in meters) */
    PIXELS,
    /** Degrees of visual angle (z-axis in meters) */
    SPHERICAL
}
