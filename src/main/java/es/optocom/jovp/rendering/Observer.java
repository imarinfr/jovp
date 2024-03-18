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

    public static final float ZNEAR = 0.01f; // Near and far planes in in meters
    public static final float ZFAR = 1000.0f;
    private static final float IPD = 0.2f; // Default IPD in mm (mean is 61.1 mm for women and 63.6 mm for men)

    Window window; // the observed window
    ViewMode viewMode; // view mode MONO or STEREO
    Matrix4f projection = new Matrix4f(); // perspective projection
    ArrayList<Matrix4f> views; // projection view matrices
    Optics optics = new Optics(); // the system's optics

    private float distance; // viewing distance in mm
    private float ipd; // intra pupil distance in mm

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
     * @param window The window that the observer is looking at
     * @param distance Viewing distance to compute fovx and fovy
     * @param viewMode Whether it is monocular of stereoscopic view
     * @param ipd Intra-pupil distance in mm
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
     * 
     */
    public float[] getFieldOfView() {
        return new float[] {
            (float) Math.toDegrees(2.0 * Math.atan(1.0 / projection.get(0, 0))),
            (float) Math.toDegrees(2.0 * Math.atan(1.0 / projection.get(1, 1)))
        };
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
        computePerspective();
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
     * Set default view
     * 
     * @since 0.0.1
     */
    public void lookAt() {
        lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0));
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
        switch (viewMode) {
            case MONO -> {
                views.get(0).set(new Matrix4f().setLookAt(eye, center, up));
            }
            case STEREO -> {
                Vector3f leftEye = new Vector3f();
                Vector3f rightEye = new Vector3f();
                leftEye.x = eye.x - ipd / 2.0f;
                leftEye.y = eye.y;
                leftEye.z = eye.z;
                rightEye.x = eye.x + ipd / 2.0f;
                rightEye.y = eye.y;
                rightEye.z = eye.z;
                views.get(0).set(new Matrix4f().setLookAt(leftEye, center, up));
                views.get(1).set(new Matrix4f().setLookAt(rightEye, center, up));
            }
        }
        up.y = -up.y;
    }

    /**
     * 
     * Translate observer
     * 
     * @param offset The translation in x and y axes in degrees and z in meters
     *
     * @since 0.0.1
     */
    public void translate(Vector3f offset) {
        translateViewMatrix(views.get(0), offset);
        if (viewMode == ViewMode.STEREO) translateViewMatrix(views.get(1), offset);
    }

    /**
     * 
     * Rotate observer
     * 
     * @param rotation The rotation in x, y, and z axes
     *
     * @since 0.0.1
     */
    public void rotate(Vector3f rotation) {
        float rx = -(float) Math.toRadians(rotation.x);
        float ry = -(float) Math.toRadians(rotation.y);
        float rz = (float) Math.toRadians(rotation.z);
        views.get(0).rotateXYZ(rx, ry, rz);
        if (viewMode == ViewMode.STEREO) views.get(1).rotateXYZ(rx, ry, rz);
    }

    /**
     * 
     * Updates the field of view depending on distance, window size, etc
     *
     * @since 0.0.1
     */
    public void computePerspective() {
        float width = window.getPixelWidth() * window.getWidth();
        float height = window.getPixelHeight() * window.getHeight();
        if (viewMode == ViewMode.STEREO) width = width / 2.0f; // only half of the screen is used per eye
        projection.setPerspective((float) (2 * Math.atan(height / distance / 2)),
                                  width / height, ZNEAR, ZFAR);
    }

    /** Compute aspect ratio, update FOVX and FOVY, and set the projection matrix */
    private void resetSpaceMatrices() {
        switch (viewMode) {
            case MONO -> {
                views = new ArrayList<Matrix4f>(1);
                views.add(new Matrix4f());
            }
            case STEREO -> {
                views = new ArrayList<Matrix4f>(2);
                views.add(new Matrix4f());
                views.add(new Matrix4f());
            }
        }
        lookAt();
        setPupilDistance(ipd);
        computePerspective();
    }

    /**
     * 
     * Translate viewMatrix
     * 
     * @param offset The translation in x and y axes in degrees and z in meters
     *
     * @since 0.0.1
     */
    private void translateViewMatrix(Matrix4f viewMatrix, Vector3f offset) {
        Vector3f eye = new Vector3f(viewMatrix.m30(), viewMatrix.m31(), viewMatrix.m32());
        Vector3f forwardVector = new Vector3f(-viewMatrix.m02(), -viewMatrix.m12(), -viewMatrix.m22()).normalize();
        Vector3f rightVector = new Vector3f(viewMatrix.m00(), viewMatrix.m10(), viewMatrix.m20());
        Vector3f upVector = new Vector3f(viewMatrix.m01(), viewMatrix.m11(), viewMatrix.m21());
        Vector3f forward = new Vector3f(forwardVector).mul(offset.z);
        Vector3f right = new Vector3f(rightVector).mul(-offset.x);
        Vector3f up = new Vector3f(upVector).mul(-offset.y);
        Vector3f newEyePosition = new Vector3f(eye).add(forward).add(right).add(up);
        viewMatrix.setTranslation(newEyePosition);
    }

}
