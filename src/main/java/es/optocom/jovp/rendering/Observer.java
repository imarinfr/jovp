package es.optocom.jovp.rendering;

import es.optocom.jovp.Window;
import es.optocom.jovp.definitions.ViewMode;

import org.joml.Matrix4f;

/**
 * Observer to apply the required transformations during rendering,
 * including view mode, distance, FOV, near and far planes, culling, etc
 *
 * @since 0.0.1
 */
public class Observer {

  private static final float IPD = 62.4f; // Default IPD in mm (mean is 61.1 mm for women and 63.6 mm for men)
  private static final float DISTANCE = 500.0f; // Default distance in mm (half a meter)
  static final float NEAR = 0.1f; // Near and far planes in meters
  static final float FAR = 1000.0f;

  Window window; // the observed window
  ViewMode viewMode; // view mode MONO or STEREO
  Matrix4f projection; // perspective projection
  Matrix4f view; // view matrix
  Optics optics; // the system's optics

  private float ipd; // intra pupil distance in mm
  private float distance; // viewing distance in mm
  private float fovx; // in degrees;
  private float fovy; // in degrees;

  /**
   * 
   * Define the observer, whether we use monocular or stereoscopic view 
   * and the intra-pupil distance between left and right eyes
   *
   * @param window The window that the observer is looking at
   * 
   * @since 0.0.1
   */
  public Observer(Window window) {
    this(window, ViewMode.MONO, IPD);
  }

  /**
   * 
   * Define the observer, whether we use monocular or stereoscopic view 
   * and the intra-pupil distance between left and right eyes
   * 
   * @param window The window that the observer is looking at
   * @param viewMode Whether it is monocular of stereoscopic view
   * 
   * @return Whether the item was succesfully added
   *
   * @since 0.0.1
   */
  public Observer(Window window, ViewMode viewMode) {
      this(window, viewMode, IPD);
  }

  /**
   * 
   * Define the observer, whether we use monocular or stereoscopic view 
   * and the intra-pupil distance between left and right eyes
   * 
   * @param window The window that the observer is looking at
   * @param viewMode Whether it is monocular of stereoscopic view
   * @param ipd Intra-pupil distance in mm
   * 
   * @return Whether the item was succesfully added
   *
   * @since 0.0.1
   */
  public Observer(Window window, ViewMode viewMode, float ipd) {
    this.window = window;
    this.viewMode = viewMode;
    setDefaults();
    switch (viewMode) {
      case MONO -> setPupilDistance(0);
      case STEREO -> setPupilDistance(IPD);
    }
  }

  /**
   * 
   * Clean after use
   *
   * @since 0.0.1
   */
  public void cleanup() {
    setDefaults();
  }


  /**
   * 
   * Get observed window
   *
   * @return the observed window
   *
   * @since 0.0.1
   */
  public Window window() {
    return window;
  }

  /**
   * 
   * Set the view mode
   *
   * @param viewMode The view mode, whether MONO or STEREO
   *
   * @since 0.0.1
   */
  public void setViewMode(ViewMode viewMode) {
    this.viewMode = viewMode;
    computeFieldOfView();
  }

  /**
   * 
   * Get the view mode
   *
   * @return The view mode
   *
   * @since 0.0.1
   */
  public ViewMode getViewMode() {
    return viewMode;
  }

  /**
   * 
   * Set intra-pupil distance
   * 
   * @param ipd Intra-pupil distance in mm
   *
   * @since 0.0.1
   */
  public void setPupilDistance(double ipd) {
    setPupilDistance((float) ipd);
  }

  /**
   * 
   * Set intra-pupil distance
   * 
   * @param ipd Intra-pupil distance in mm
   *
   * @since 0.0.1
   */
  public void setPupilDistance(float ipd) {
    this.ipd = ipd;
  }

  /**
   * 
   * Get the intra-pupil distance distance
   *
   * @return The intra-pupil distance in mm
   *
   * @since 0.0.1
   */
  public float getPupilDistance() {
    return ipd;
  }

  /**
   * 
   * Set viewing distance
   *
   * @param distance The distance of the observer from the display in mm
   *
   * @since 0.0.1
   */
  public void setDistance(double distance) {
    setDistance((float) distance);
  }

  /**
   * 
   * Set viewing distance
   *
   * @param distance The distance of the observer from the display in mm
   *
   * @since 0.0.1
   */
  public void setDistance(float distance) {
    this.distance = distance;
    computeFieldOfView();
  }

  /**
   * 
   * Get viewing distance
   *
   * @return The distance of the observer from the display
   *
   * @since 0.0.1
   */
  public float getDistance() {
    return distance;
  }

  /**
   * 
   * Updates the field of view depending on distance, window size, etc
   *
   * @since 0.0.1
   */
  void computeFieldOfView() {
    float width = window.getPixelWidth() * window.getWidth();
    float height = window.getPixelHeight() * window.getHeight();
    if (viewMode == ViewMode.STEREO) width = width / 2; // only half of the screen is used per eye
    fovx = (float) (360.0 / Math.PI * Math.atan((width / 2.0) / distance));
    fovy = (float) (360.0 / Math.PI * Math.atan((height / 2.0) / distance));
  }

  /**
   * 
   * Get field of view
   *
   * @return The field of view in x and y directions.
   *
   * @since 0.0.1
   */
  public float[] getFieldOfView() {
    return new float[] {fovx, fovy};
  }

  /**
   * 
   * Compute aspect ratio, update FOVX and FOVY, and set the projection matrix
   *
   * @since 0.0.1
   */
  public void computeProjection() {
    computeFieldOfView();
    float aspect = window.getAspectRatio();
    if (viewMode == ViewMode.STEREO) {
      aspect = aspect / 2;
      if (window.getPixelWidth() % 2 == 1) // if number of pixels odd, then correct
        aspect = (window.getPixelWidth() - 1) / window.getPixelWidth() * aspect;
    }
    projection.setPerspective((float) fovy, (float) aspect, NEAR, FAR, true);
    //projection.frustum(-fovx / 2.0f, fovx / 2.0f, -fovy / 2.0f, fovy / 2.0f, NEAR, FAR, true);
  }

  /**
   * 
   * Default values for the optical system
   *
   * @since 0.0.1
   */
  public void setDefaults() {
    viewMode = ViewMode.MONO;
    projection = new Matrix4f();
    view = new Matrix4f();
    optics = new Optics();
    setDistance(DISTANCE);
    setPupilDistance(IPD);
    computeProjection();
  }

}
