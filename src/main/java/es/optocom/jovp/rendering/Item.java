package es.optocom.jovp.rendering;

import es.optocom.jovp.structures.Eye;
import es.optocom.jovp.structures.PostType;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Item to construct the psychophysical experience
 *
 * @since 0.0.1
 */
public class Item {

  Eye eye;
  Model model;
  Texture texture;
  final Vector3d position; // radian, radian, mm
  Vector3d size; // radian, radian, mm
  Vector4f frequency;
  Vector4f contrast;
  double rotation; // in radians
  Vector3f rotationAxis;
  double texRotation; // rotation in radians
  float[] texPivot;
  final Post post;
  ItemBuffers buffers;
  boolean update = false; // Only for text

  /**
   * Create an item for psychophysics experience
   *
   * @param model   The model (square, circle, etc)
   * @param texture The texture
   *
   * @since 0.0.1
   */
  public Item(Model model, Texture texture) {
    this();
    this.model = model;
    this.texture = texture;
  }

  /**
   * Init an item, used for use with Text
   *
   * @since 0.0.1
   */
  Item() {
    eye = Eye.BOTH;
    position = new Vector3d(0, 0, VulkanSetup.Z_FAR / 2);
    defaults();
    buffers = null;
    post = new Post();
  }

  /**
   * Clean up after use
   *
   * @since 0.0.1
   */
  public void destroy() {
    buffers.destroy();
    model.destroy();
    texture.destroy();
  }

  /**
   * Show item
   *
   * @param view type of view
   * @return Whether to show the item in the Eye
   *
   * @since 0.0.1
   */
  public boolean show(int view) {
    return switch (eye) {
      case BOTH -> true;
      case LEFT -> view == 0;
      case RIGHT -> view == 1;
      default -> false;
    };
  }

  /**
   * Eye
   *
   * @param eye Eye to display
   *
   * @since 0.0.1
   */
  public void eye(Eye eye) {
    this.eye = eye;
  }

  /**
   * Get model
   *
   * @return The model
   *
   * @since 0.0.1
   */
  public Model getModel() {
    return model;
  }

  /**
   * Get texture
   *
   * @return The texture
   *
   * @since 0.0.1
   */
  public Texture getTexture() {
    return texture;
  }

  /**
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
   * Get texture color 1 for grids
   *
   * @return The RGBA values of the minimum color
   *
   * @since 0.0.1
   */
  public Vector4f rgba0() {
    return texture.rgba0();
  }

  /**
   * Get texture color 2 for grids
   *
   * @return The RGBA values of the maximum color
   *
   * @since 0.0.1
   */
  public Vector4f rgba1() {
    return texture.rgba1();
  }

  /**
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
   * Set item size
   *
   * @param x Size along the x-axis
   *
   * @since 0.0.1
   */
  public void size(double x) {
    size((float) x, (float) x, 1.0f);
  }

  /**
   * Set item size
   *
   * @param x Size along the x-axis
   * @param y Size along the y-axis
   *
   * @since 0.0.1
   */
  public void size(double x, double y) {
    size((float) x, (float) y, 1.0f);
  }

  /**
   * Set item size
   *
   * @param x Size along the x-axis
   * @param y Size along the y-axis
   * @param z Size along the z-axis
   *
   * @since 0.0.1
   */
  public void size(float x, float y, float z) {
    size.x = Math.toRadians(x / 2);
    size.y = Math.toRadians(y / 2);
    size.z = z;
  }

  /**
   * Set item size
   *
   * @param x Size along the x-axis
   * @param y Size along the y-axis
   * @param z Size along the z-axis
   *
   * @since 0.0.1
   */
  public void size(double x, double y, double z) {
    size((float) x, (float) y, (float) z);
  }

  /**
   * Move the item
   *
   * @param x Translation along the x-axis
   * @param y Translation along the y-axis
   *
   * @since 0.0.1
   */
  public void position(double x, double y) {
    position((float) x, (float) y, VulkanSetup.Z_FAR / 2);
  }

  /**
   * Move the item
   *
   * @param x Translation along the x-axis
   * @param y Translation along the y-axis
   * @param z Translation along the z-axis
   *
   * @since 0.0.1
   */
  public void position(float x, float y, float z) {
    position.x = Math.toRadians(x);
    position.y = Math.toRadians(y);
    position.z = z;
  }

  /**
   * Move the item
   *
   * @param x Translation along the x-axis
   * @param y Translation along the y-axis
   * @param z Translation along the z-axis
   *
   * @since 0.0.1
   */
  public void position(double x, double y, double z) {
    position((float) x, (float) y, (float) z);
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
  public void frequency(float xp, float xf, float yp, float yf) {
    frequency.x = xf;
    frequency.y = yf;
    frequency.z = xp;
    frequency.w = yp;
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
    frequency((float) xp, (float) xf, (float) yp, (float) yf);
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
    frequency((float) xp, (float) xf, (float) xp, (float) xf);
  }

  /**
   * Color contrast
   *
   * @param r Amplitude for R channel
   * @param g Amplitude for G channel
   * @param b Amplitude for B channel
   * @param a Amplitude for alpha channel
   *
   * @since 0.0.1
   */
  public void contrast(float r, float g, float b, float a) {
    if (r < 0)
      r = 0;
    if (r > 1)
      r = 1;
    if (g < 0)
      g = 0;
    if (g > 1)
      g = 1;
    if (b < 0)
      b = 0;
    if (b > 1)
      b = 1;
    if (a < 0)
      a = 0;
    if (a > 1)
      a = 1;
    contrast.x = r;
    contrast.y = g;
    contrast.z = b;
    contrast.w = a;
  }

  /**
   * Color contrast
   *
   * @param r Amplitude for R channel
   * @param g Amplitude for G channel
   * @param b Amplitude for B channel
   * @param a Amplitude for alpha channel
   *
   * @since 0.0.1
   */
  public void contrast(double r, double g, double b, double a) {
    contrast((float) r, (float) g, (float) b, (float) a);
  }

  /**
   * Gray contrast
   *
   * @param gray Contrast for all channels, except alpha
   *
   * @since 0.0.1
   */
  public void contrast(double gray) {
    contrast(gray, gray, gray, gray);
  }

  /**
   * Rotate the item
   *
   * @param rotation Angle of rotation in degrees
   *
   * @since 0.0.1
   */
  public void rotation(double rotation) {
    rotation(rotation, new Vector3f(0.0f, 0.0f, 1.0f));
  }

  /**
   * Rotate the item
   *
   * @param rotation Angle of rotation in degrees
   * @param axis     Axis of rotation
   *
   * @since 0.0.1
   */
  public void rotation(double rotation, Vector3f axis) {
    this.rotation = Math.toRadians(rotation);
    this.rotationAxis = axis;
  }

  /**
   * Rotate the texture inside the model
   *
   * @param texRotation Angle of rotation in degrees
   *
   * @since 0.0.1
   */
  public void texRotation(double texRotation) {
    texRotation(texRotation, new float[] { 0.5f, 0.5f });
  }

  /**
   * Rotate the texture inside the model
   *
   * @param rotation Angle of rotation in degrees
   * @param pivot    Pivot UV values
   *
   * @since 0.0.1
   */
  public void texRotation(double rotation, float[] pivot) {
    this.texRotation = Math.toRadians(rotation);
    this.texPivot = pivot;
  }

  /**
   * Add an envelope
   *
   * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sd   Standard deviation in degrees for the x- and y-axis
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, double sd) {
    envelope(type, (float) sd, (float) sd, 0);
  }

  /**
   * Add an envelope
   *
   * @param type Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sdx  Standard deviation in degrees for the x-axis
   * @param sdy  Standard deviation in degrees for the y-axis
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, double sdx, double sdy) {
    envelope(type, (float) sdx, (float) sdy, 0);
  }

  /**
   * Add an envelope
   *
   * @param type  Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sdx   Standard deviation in degrees for the x-axis
   * @param sdy   Standard deviation in degrees for the y-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, double sdx, double sdy, double angle) {
    post.envelope(type, (float) sdx, (float) sdy, (float) angle);
  }

  /**
   * Add an envelope
   *
   * @param type  Type of envelope. Can be SQUARE, CIRCLE, or GAUSSIAN
   * @param sdx   Standard deviation in degrees for the x-axis
   * @param sdy   Standard deviation in degrees for the y-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void envelope(PostType type, float sdx, float sdy, float angle) {
    post.envelope(type, sdx, sdy, angle);
  }

  /**
   * Remove the envelope
   *
   * @since 0.0.1
   */
  public void removeEnvelope() {
    post.removeEnvelope();
  }

  /**
   * Add Gaussian defocus (only spherical)
   *
   * @param dx Defocus in Diopters
   *
   * @since 0.0.1
   */
  public void defocus(double dx) {
    defocus((float) dx, (float) dx, 0);
  }

  /**
   * Add Gaussian defocus (spherical and astigmatic defocus)
   *
   * @param dx    Defocus for the x-axis in Diopters
   * @param dy    Defocus for the x-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void defocus(double dx, double dy, double angle) {
    post.defocus((float) dx, (float) dy, (float) angle);
  }

  /**
   * Add Gaussian defocus (spherical and astigmatic defocus)
   *
   * @param dx    Defocus for the x-axis in Diopters
   * @param dy    Defocus for the x-axis
   * @param angle Angle
   *
   * @since 0.0.1
   */
  public void defocus(float dx, float dy, float angle) {
    post.defocus(dx, dy, angle);
  }

  /**
   * Remove the Gaussian defocus
   *
   * @since 0.0.1
   */
  public void removeDefocus() {
    post.removeDefocus();
  }

  /** create buffers */
  void createBuffers() {
    buffers = new ItemBuffers(this);
  }

  /** defaults */
  private void defaults() {
    rotation = 0;
    rotationAxis = new Vector3f(0, 0, 1);
    texRotation = 0;
    texPivot = new float[] { 0.5f, 0.5f };
    size = new Vector3d(Math.PI / 180, Math.PI / 180, 1);
    frequency = new Vector4f(-1, -1, 0, 0);
    contrast = new Vector4f(1, 1, 1, 1);
  }

}