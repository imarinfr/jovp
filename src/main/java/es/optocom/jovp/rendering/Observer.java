package es.optocom.jovp.rendering;

import es.optocom.jovp.Window;
import es.optocom.jovp.definitions.ViewMode;

import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 
 * Observer to apply the required transformations during rendering,
 * including view mode, distance, FOV, near and far planes, culling, etc
 *
 * @since 0.0.1
 */
public class Observer {

    public static final float NEAR = 1.0f; // Near and far planes in in meters
    public static final float FAR = 1000.0f;
    private static final float IPD = 62.4f; // Default IPD in mm (mean is 61.1 mm for women and 63.6 mm for men)
    private static final Matrix4f VULKAN_AXIS = new Matrix4f(-1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

    Window window; // the observed window
    ViewMode viewMode; // view mode MONO or STEREO
    Matrix4f projection = new Matrix4f(); // perspective projection
    Matrix4f view = new Matrix4f(); // view matrix
    ArrayList<Matrix4f> projectionViews; // projection view matrices
    Optics optics = new Optics(); // the system's optics

    private float distance; // viewing distance in mm
    private float ipd; // intra pupil distance in mm
    private float fovx; // in radians;
    private float fovy; // in radians;

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
        resetSpaceMatrices();
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
     * Set the view mode
     *
     * @param viewMode The view mode, whether MONO or STEREO
     *
     * @since 0.0.1
     */
    void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
        this.ipd = IPD;
        resetSpaceMatrices();
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
        resetSpaceMatrices();
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
     * Set Brown-Conrady model distortion coefficients
     * 
     * @param k1 coefficient k1
     * @param k2 coefficient k2
     * @param k3 coefficient k3
     * @param k4 coefficient k4
     *
     * @since 0.0.1
     */
    public void setCoefficients(double k1, double k2, double k3, double k4) {
        optics.setCoefficients(k1, k2, k3, k4);
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
        setProjectionViews();
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
     * Updates the field of view depending on distance, window size, etc
     *
     * @since 0.0.1
     */
    public void computeFieldOfView() {
        float width = window.getPixelWidth() * window.getWidth();
        float height = window.getPixelHeight() * window.getHeight();
        if (viewMode == ViewMode.STEREO) width = width / 2.0f; // only half of the screen is used per eye
        fovx = (float) (2 * Math.atan(width / distance / 2));
        fovy = (float) (2 * Math.atan(height / distance / 2));
        projection.setPerspective(fovy, width / height, NEAR, FAR);
        setProjectionViews();
    }

    /** Compute aspect ratio, update FOVX and FOVY, and set the projection matrix */
    private void resetSpaceMatrices() {
        switch (viewMode) {
            case MONO -> {
                projectionViews = new ArrayList<Matrix4f>(1);
                projectionViews.add(new Matrix4f());
            }
            case STEREO -> {
                projectionViews = new ArrayList<Matrix4f>(2);
                projectionViews.add(new Matrix4f());
                projectionViews.add(new Matrix4f());
            }
        }
        setPupilDistance(ipd);
        setView(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0));
        computeFieldOfView();
    }

    /** Set projection view */
    private void setProjectionViews() {
        switch (viewMode) {
            case MONO -> {
                projectionViews.get(0).set(projection).mul(VULKAN_AXIS).mul(view);
            }
            case STEREO -> {
                Matrix4f eyeView = new Matrix4f(view);
                eyeView.translate(-ipd / 2000.0f, 0, 0); // left-eye offset
                projectionViews.get(0).set(projection).mul(VULKAN_AXIS).mul(eyeView);
                eyeView = new Matrix4f(view);
                eyeView.translate(ipd / 2000.0f, 0, 0); // right-eye offset
                projectionViews.get(1).set(projection).mul(VULKAN_AXIS).mul(eyeView);
            }
        }
    }

}
