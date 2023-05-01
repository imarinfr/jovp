package es.optocom.jovp.rendering;

import org.joml.Vector4f;

import es.optocom.jovp.definitions.PostType;

/**
 * Postprocessing information
 *
 * @since 0.0.1
 */
class Post {

  PostType type;
  boolean defocus;
  final Vector4f parameters = new Vector4f(0, 0, 0, 0);
  final float[] postEnvelope;
  final float[] postDefocus;

  /**
   * 
   * Prepare postprocessing
   *
   * @param item The item to apply the post-processing
   *
   * @since 0.0.1
   */
  Post() { //TODO: need to implement all this so that it does something
    type = PostType.NONE;
    defocus = false;
    // First two parameters are SD in degrees of visual angle, the third parameter
    // angle
    postEnvelope = new float[] {0, 0, 0};
    // First two parameters are SD in degrees of visual angle, the third parameter
    // angle
    postDefocus = new float[] {0, 0, 0};
    setParameters();
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
  void envelope(PostType type, float sdx, float sdy, float angle) {
    this.type = type;
    postEnvelope[0] = (float) Math.toRadians(sdx) / 2;
    postEnvelope[1] = (float) Math.toRadians(sdy) / 2;
    postEnvelope[2] = (float) Math.toRadians(angle);
    setParameters();
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
    defocus = true;
    float sdx = dx;
    float sdy = dy; // TODO: convert from Diopters to Gaussian SD in visual angle
    postDefocus[0] = (float) Math.toRadians(sdx) / 2;
    postDefocus[1] = (float) Math.toRadians(sdy) / 2;
    postDefocus[2] = (float) Math.toRadians(angle);
    setParameters();
  }

  /**
   *
   * Remove envelope
   *
   * @since 0.0.1
   */
  void removeEnvelope() {
    type = PostType.NONE;
    postEnvelope[0] = 0;
    postEnvelope[1] = 0;
    postEnvelope[2] = 0;
    setParameters();
  }

  /**
   *
   * Remove defocus
   *
   * @since 0.0.1
   */
  void removeDefocus() {
    type = PostType.NONE;
    postDefocus[0] = 0;
    postDefocus[1] = 0;
    postDefocus[2] = 0;
    setParameters();
  }

  /**
   *
   * Set parameters
   *
   * @since 0.0.1
   */
  private void setParameters() {
    // Post-processing: envelope and Gaussian defocus
    final Vector4f envelope = new Vector4f(0, 0, 0, 0);
    switch (type) {
      case SQUARE -> envelope.x = 1;
      case CIRCLE -> envelope.x = 2;
      case GAUSSIAN -> envelope.x = 3;
      default -> envelope.x = 0; // No envelope
    }
    //envelope.y = (float) (postEnvelope[0] / item.size.x); // SD x
    //envelope.z = (float) (postEnvelope[1] / item.size.y); // SD y
    //envelope.w = postEnvelope[2]; // angle
  }
}
