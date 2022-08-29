package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.PostType;

/**
 *
 * Post
 *
 * <ul>
 * <li>Post</li>
 * Postprocessing information
 * </ul>
 *
 * @since 0.0.1
 */
class Post {

    PostType envelope;
    boolean defocus;
    final float[] envelopeParams;
    final float[] defocusParams;

    /**
     *
     * Prepare postprocessing
     *
     * @since 0.0.1
     */
    Post() {
        envelope = PostType.NONE;
        defocus = false;
        // First two parameters are SD in degrees of visual angle, the third parameter angle
        envelopeParams = new float[] {0, 0, 0};
        // First two parameters are SD in degrees of visual angle, the third parameter angle
        defocusParams = new float[] {0, 0, 0};
    }

    /**
     *
     * Add a Gaussian envelope
     *
     * @param sdx Defocus in diopters on the x-axis
     * @param sdy Defocus in diopters on the y-axis
     * @param angle Angle
     *
     * @since 0.0.1
     */
    void envelope(PostType type, float sdx, float sdy, float angle) {
        envelope = type;
        envelopeParams[0] = (float) Math.toRadians(sdx) / 2;
        envelopeParams[1] = (float) Math.toRadians(sdy) / 2;
        envelopeParams[2] = (float) Math.toRadians(angle);
    }

    /**
     *
     * Add Gaussian defocus (spherical and astigmatic defocus)
     *
     * @param dx Defocus in diopters for the x-axis
     * @param dy Defocus in diopters for the y-axis
     * @param angle Angle
     *
     * @since 0.0.1
     */
    void defocus(float dx, float dy, float angle) {
        defocus = true;
        float sdx = dx; float sdy = dy; //TODO: convert from Diopters to Gaussian SD in visual angle
        defocusParams[0] = (float) Math.toRadians(sdx) / 2;
        defocusParams[1] = (float) Math.toRadians(sdy) / 2;
        defocusParams[2] = (float) Math.toRadians(angle);
        // TODO: does nothing yet
    }

    /**
     *
     * Remove envelope
     *
     * @since 0.0.1
     */
    void removeEnvelope() {
        envelope = PostType.NONE;
        envelopeParams[0] = 0; envelopeParams[1] = 0; envelopeParams[2] = 0;
    }

    /**
     *
     * Remove defocus
     *
     * @since 0.0.1
     */
    void removeDefocus() {
        envelope = PostType.NONE;
        defocusParams[0] = 0; defocusParams[1] = 0; defocusParams[2] = 0;
    }

}
