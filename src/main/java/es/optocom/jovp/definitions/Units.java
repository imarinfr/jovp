package es.optocom.jovp.definitions;

/**
 * 
 * Coordinates: CARTESIAN (in meters), spherical, and angular
 * (both in degrees of visual angle). The z-axis in both
 * SPHERICAL and ANGULAR are also in meters but x and y are in
 * degrees of visual angle. The difference between ANGULAR and
 * SPHERICAL is that the former projects onto a the x-y plane
 * at a distance z whereas the later projects on a sphere so that
 * all items are at the same distance from the space origin.
 *
 * @since 0.0.1
 */
public enum Units {
    /** Degrees of visual angle from observer in a flat world (z-axis in meters from the plane in the screen) */
    ANGLES,
    /** Distance from camera (in meters) */
    METERS,
    /** pixels from camera (z-axis in meters) */
    PIXELS,
    /** Degrees of visual angle (z-axis in meters) */
    SPHERICAL
}
