package es.optocom.jovp.rendering;

import es.optocom.jovp.Window;
import es.optocom.jovp.definitions.ViewMode;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 
 * Observer to apply the required transformations during rendering,
 * including view mode, distance, FOV, near and far planes, culling, etc.
 * 
 * Stereoscopic view requires the specification of the pupillary
 * distance (PD) of the left and right eye. The mean PD is 61.1 / 2 mm
 * for women , 63.6 / 2 mm for men, and 62.35 / 2 overall
 *
 * @since 0.0.1
 */
public class Observer {

    public static final float ZNEAR = 0.1f; // Near and far planes in in meters
    public static final float ZFAR = 100.1f;
    public static final float DEFAULT_DEPTH = 50.0f; // default depth: distance between item and screen
    private static final float PD = 62.35f / 2000.0f; // Default pupillary distance (PD) in meters:
                                                     
    Window window; // the observed window
    ViewMode viewMode; // view mode MONO or STEREO

    Matrix4f view; // view matrix for the midpoint between eyes
    Matrix4f viewLeft; // view matrix for the left eye
    Matrix4f viewRight; // view matrix for the left eye

    Matrix4f orthographic = new Matrix4f(); // orthographic projection for the midpoint between eyes
    Matrix4f orthographicLeft = new Matrix4f(); // orthographic projection for the left eye
    Matrix4f orthographicRight = new Matrix4f(); // orthographic projection for the left eye
    Matrix4f perspective = new Matrix4f(); // perspective projection for the midpoint between eyes
    Matrix4f perspectiveLeft = new Matrix4f(); // perspective projection for the left eye
    Matrix4f perspectiveRight = new Matrix4f(); // perspective projection for the right eye
    Optics optics = new Optics(); // optics for the cyclops
    Optics opticsLeft = new Optics(); // optics for the left eye
    Optics opticsRight = new Optics(); // optics for the right eye

    private float width; // view width and height in meters
    private float height;
    private float aspect; // aspect ratio
    private float fovx; // field of view for x in monocular view
    private float fovxhalf; // field of view for x in stereoscopic view
    private float fovy; // field of view for y

    private float distance; // viewing distance in meters
    private float pd; // pupilary distance in meters

    /**
     * 
     * Define observer: monocular view
     *
     * @param window   The window that the observer is looking at
     * @param distance Viewing distance
     * 
     * @since 0.0.1
     */
    public Observer(Window window, float distance) {
        this(window, distance, ViewMode.MONO, PD);
    }

    /**
     * 
     * Define the observer, monocular or stereoscopic view with
     * a default intra-pupil distance between left and right eyes
     * 
     * @param window   The window that the observer is looking at
     * @param distance Viewing distance
     * @param viewMode Whether it is monocular of stereoscopic view
     *
     * @since 0.0.1
     */
    public Observer(Window window, float distance, ViewMode viewMode) {
        this(window, distance, viewMode, PD);
    }

    /**
     * 
     * Define the observer, whether we use monocular or stereoscopic view
     * and the intra-pupil distance between left and right eyes
     * 
     * @param window The window that the observer is looking at
     * @param distance Viewing distance to compute fovx and fovy
     * @param viewMode Whether it is monocular of stereoscopic view
     * @param pd Intra-pupil distance in mm
     *
     * @since 0.0.1
     */
    public Observer(Window window, float distance, ViewMode viewMode, float pd) {
        this.window = window;
        this.distance = distance / 1000.0f; // to meters
        this.viewMode = viewMode;
        this.pd = pd;
        computeProjections();
        resetViewMatrices();
    }

    /**
     * 
     * Get window width in meters
     *
     * @return the window width in meters
     *
     * @since 0.0.1
     * 
     */
    public float getWidth() {
        return width;
    }

    /**
     * 
     * Get window height in meters
     *
     * @return the window height in meters
     *
     * @since 0.0.1
     * 
     */
    public float getHeight() {
        return height;
    }

    /**
     * 
     * Get field of view
     *
     * @return The x and y fields of view in degrees.
     *
     * @since 0.0.1
     * 
     */
    public float[] getFieldOfView() {
        return new float[] {(float) Math.toDegrees(viewMode == ViewMode.MONO ? fovx : fovxhalf), (float) Math.toDegrees(fovy)};
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
     * Get viewing distance
     *
     * @return The distance of the observer from the display
     * 
     * @since 0.0.1
     */
    public float getDistance() {
        return 1000.0f * distance; // to mm
    }

        /**
     * 
     * Get viewing distance in meters
     *
     * @return The distance of the observer from the display
     * 
     * @since 0.0.1
     */
    public float getDistanceM() {
        return distance;
    }

    /**
     * 
     * Set pupilary distance
     * 
     * @param pd Pupilary distance in mm
     *
     * @since 0.0.1
     */
    public void setPupilDistance(double pd) {
        this.pd = (float) pd;
        translateViewMatrix(viewLeft, new Vector3f(-(float) pd + this.pd, 0.0f, 0.0f));
        translateViewMatrix(viewRight, new Vector3f((float) pd - this.pd, 0.0f, 0.0f));
        computeProjections();
    }

    /**
     * 
     * Get the pupilary distance distance
     *
     * @return The pupilary distance in mm
     *
     * @since 0.0.1
     */
    public float getPupilDistance() {
        return pd;
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
        opticsLeft.setCoefficients(k1, k2, k3, k4);
        opticsLeft.setCoefficients(k1, k2, k3, k4);
    }

    /**
     * 
     * Set default view
     * 
     * @since 0.0.1
     */
    public void lookAt() {
        lookAt(new Vector3f(0, 0, -distance), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
    }

    /**
     * 
     * Set look at
     * 
     * @param eye The position of the eye in the virtual world
     * @param center The center
     * @param up The up vector
     *
     * @since 0.0.1
     */
    public void lookAt(Vector3f eye, Vector3f center, Vector3f up) {
        up.y = -up.y;
        view.lookAt(eye, center, up);
        viewLeft = new Matrix4f(view);
        viewRight = new Matrix4f(view);
        translateViewMatrix(viewLeft, new Vector3f(-pd, 0.0f, 0.0f));
        translateViewMatrix(viewRight, new Vector3f(pd, 0.0f, 0.0f));
    }

    /**
     * 
     * Translate observer
     * 
     * @param offset The translation in x and y axes in degrees and z in meters
     *
     * @since 0.0.1
     */
    public void translateView(Vector3f offset) {
        translateViewMatrix(view, offset);
        translateViewMatrix(viewLeft, offset);
        translateViewMatrix(viewRight, offset);
    }

    /**
     * 
     * Updates the field of view depending on distance, window size, etc
     *
     * @since 0.0.1
     */
    public void computeProjections() {
        width = window.getWidthM();
        height = window.getHeightM();
        aspect = width / height;
        fovx = (float) (2 * Math.atan(width / distance / 2));
        fovxhalf = (float) (2 * Math.atan(width / distance / 4));
        fovy = (float) (2 * Math.atan(height / distance / 2));
        perspective.setPerspective(fovy, aspect, ZNEAR, ZFAR, true);
        perspectiveLeft.setPerspective(fovy, aspect / 2, ZNEAR, ZFAR, true).m20(-pd);
        perspectiveRight.setPerspective(fovy, aspect / 2, ZNEAR, ZFAR, true).m20(pd);
        orthographic.setOrtho(-width / 2, width / 2, -height / 2, height / 2, ZNEAR, ZFAR, true);
        orthographicLeft.setOrtho(-width / 4, width / 4, -height / 2, height / 2, ZNEAR, ZFAR, true);
        orthographicRight.setOrtho(-width / 4, width / 4, -height / 2, height / 2, ZNEAR, ZFAR, true);
    }

    /** Compute aspect ratio, update FOVX and FOVY, and set the projection matrix */
    public void resetViewMatrices() {
        view = new Matrix4f();
        viewLeft = new Matrix4f();
        viewRight = new Matrix4f();
        lookAt();
    }

    /**
     * 
     * Rotate observer
     * 
     * @param rotation The rotation in x, y, and z axes
     *
     * @since 0.0.1
     */
    public void rotateView(Vector3f rotation) {
        float rx = -(float) Math.toRadians(rotation.x);
        float ry = -(float) Math.toRadians(rotation.y);
        float rz = (float) Math.toRadians(rotation.z);
        view.rotateXYZ(rx, ry, rz);
        viewLeft.rotateXYZ(rx, ry, rz);
        viewRight.rotateXYZ(rx, ry, rz);
    }

    /** Translate viewMatrix */
    private void translateViewMatrix(Matrix4f viewMatrix, Vector3f offset) {
        viewMatrix.translateLocal(offset);
    }

}
