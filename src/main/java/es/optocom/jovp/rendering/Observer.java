package es.optocom.jovp.rendering;

import es.optocom.jovp.Window;
import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.ViewMode;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Observer to apply the required transformations during rendering,
 * including view mode, distance, FOV, near and far planes, culling, etc
 *
 * @since 0.0.1
 */
public class Observer {

    public static final float NEAR = 0.1f; // Near and far planes in in meters
    public static final float FAR = 1000.0f;
    private static final float IPD = 62.4f; // Default IPD in mm (mean is 61.1 mm for women and 63.6 mm for men)

    Window window; // the observed window
    ViewMode viewMode; // view mode MONO or STEREO
    Matrix4f projection = new Matrix4f(); // perspective projection
    Matrix4f view = new Matrix4f(); // view matrix
    Optics optics = new Optics(); // the system's optics

    private float distance; // viewing distance in mm
    private float ipd; // intra pupil distance in mm
    private float fovx; // in degrees;
    private float fovy; // in degrees;

    /**
     * 
     * Define the observer, whether we use monocular or stereoscopic view
     * and the intra-pupil distance between left and right eyes
     *
     * @param window   The window that the observer is looking at
     * @param distance Viewing distance
     * 
     * @since 0.0.1
     */
    public Observer(Window window, float distance) {
        this(window, distance, ViewMode.MONO, IPD);
    }

    /**
     * 
     * Define the observer, whether we use monocular or stereoscopic view
     * and the intra-pupil distance between left and right eyes
     * 
     * @param window   The window that the observer is looking at
     * @param distance Viewing distance
     * @param viewMode Whether it is monocular of stereoscopic view
     *
     * @since 0.0.1
     */
    public Observer(Window window, float distance, ViewMode viewMode) {
        this(window, distance, viewMode, IPD);
    }

    /**
     * 
     * Define the observer, whether we use monocular or stereoscopic view
     * and the intra-pupil distance between left and right eyes
     * 
     * @param window   The window that the observer is looking at
     * @param distance Viewing distance to compute fovx and fovy
     * @param viewMode Whether it is monocular of stereoscopic view
     * @param ipd      Intra-pupil distance in mm
     *
     * @since 0.0.1
     */
    public Observer(Window window, float distance, ViewMode viewMode, float ipd) {
        this.window = window;
        this.distance = distance;
        this.viewMode = viewMode;
        setView(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0));
        computeProjection();
        setPupilDistance(ipd);
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
        if (this.viewMode == viewMode)
            return;
        this.viewMode = viewMode;
        switch (viewMode) {
            case MONO -> fovx = 2 * fovx;
            case STEREO -> fovx = fovx / 2;
        }
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
        switch (viewMode) {
            case MONO -> this.ipd = 0;
            case STEREO -> this.ipd = ipd;
        }
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
        fovx = (float) (2.0 * Math.atan((width / 2.0) / distance));
        fovy = (float) (2.0 * Math.atan((height / 2.0) / distance));
        if (viewMode == ViewMode.STEREO)
            fovx = fovx / 2; // only half of the screen is used per eye
    }

    /**
     * 
     * Get field of view
     *
     * @return The x and y fields of view in degrees.
     *
     * @since 0.0.1
     */
    public float[] getFieldOfView() {
        return new float[] { (float) Math.toDegrees(fovx), (float) Math.toDegrees(fovy) };
    }

    /**
     * 
     * Compute aspect ratio, update FOVX and FOVY, and set the projection matrix
     *
     * @since 0.0.1
     */
    public void computeProjection() {
        computeFieldOfView();
        float aspect = window.getAspect();
        if (viewMode == ViewMode.STEREO)
            aspect = aspect / 2;
        projection.setPerspective(fovy, aspect, NEAR, FAR);
    }

    /**
     * 
     * Set view
     * 
     * @param eye    The position of the eye in the virtual world
     * @param center what we are looking at, I reckon, and up
     * @param up     the up vector
     *
     * @since 0.0.1
     */
    public void setView(Vector3f eye, Vector3f center, Vector3f up) {
        view.setLookAt(eye, center, up);
    }

    /**
     * 
     * Get view
     * 
     * @return the view matrix
     *
     * @since 0.0.1
     */
    public Matrix4f getView() {
        return view;
    }

    /**
     * 
     * Left-eye view
     * 
     * @param eye The position of the eye in the virtual world
     *
     * @since 0.0.1
     */
    Matrix4f eyeView(Eye eye) {
        Matrix4f renderView = switch (eye) {
            case LEFT -> view.translate(-ipd / 2, 0, 0);
            case RIGHT -> view.translate(ipd / 2, 0, 0);
            default -> view;
        };
        return renderView;
    }

}
