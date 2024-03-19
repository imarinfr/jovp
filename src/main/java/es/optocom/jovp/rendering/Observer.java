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
    private static final float PD = 0.06235f / 2.0f; // Default pupillary distance (PD) in meters:
                                                     // mean is 61.1 / 2 mm for women 
                                                     //         63.6 / 2 mm for men
                                                     //         62.35 / 2 overall

    Window window; // the observed window
    ViewMode viewMode; // view mode MONO or STEREO
    Matrix4f projection = new Matrix4f(); // perspective projection
    Matrix4f viewMatrix; // nose view matrix

    private float distance; // viewing distance in mm
    private float pd; // pupilary distance (PD) in meters
    ArrayList<Eye> eyes = new ArrayList<>(2); // Observer's eyes: 0 for monocular view or left eye 1 for right eye (null if monocular view)

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
        this.distance = distance;
        this.viewMode = viewMode;
        this.pd = pd;
        resetViewMatrix();
        computePerspective();
        switch(viewMode) {
            case MONO -> {
                eyes.add(new Eye(0));
                eyes.add(null);
            }
            case STEREO -> {
                eyes.add(new Eye(-this.pd));
                eyes.add(new Eye(this.pd));
            }
        }
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
        if (this.viewMode == viewMode) return;
        if (this.viewMode == ViewMode.MONO) {
            eyes.get(0).setPupilDistance(-pd);
            eyes.set(1, new Eye(pd));
        } else {
            eyes.get(0).setPupilDistance(0);
            eyes.set(1, null);

        }
        this.viewMode = viewMode;
        computePerspective();
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
     * Set pupilary distance
     * 
     * @param pd Pupilary distance in mm
     *
     * @since 0.0.1
     */
    public void setPupilDistance(double pd) {
        this.pd = (float) pd;
        if (viewMode == ViewMode.MONO) return;
        eyes.get(0).setPupilDistance(-this.pd);
        eyes.get(1).setPupilDistance(this.pd);
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
        return 1000.0f * pd;
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
        eyes.get(0).optics.setCoefficients(k1, k2, k3, k4);
        if (viewMode == ViewMode.STEREO) eyes.get(1).optics.setCoefficients(k1, k2, k3, k4);
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
        viewMatrix = new Matrix4f().setLookAt(eye, center, up);
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
        translateViewMatrix(viewMatrix, offset);
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
        viewMatrix.rotateXYZ(rx, ry, rz);
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
        projection.setPerspective((float) (2 * Math.atan(height / distance / 2)), width / height, ZNEAR, ZFAR);
    }

    /** Compute aspect ratio, update FOVX and FOVY, and set the projection matrix */
    public void resetViewMatrix() {
        viewMatrix = new Matrix4f();
        lookAt();
    }

    /** Translate viewMatrix */
    private void translateViewMatrix(Matrix4f viewMatrix, Vector3f offset) {
        Vector3f nose = new Vector3f(viewMatrix.m30(), viewMatrix.m31(), viewMatrix.m32());
        Vector3f forwardVector = new Vector3f(-viewMatrix.m02(), -viewMatrix.m12(), -viewMatrix.m22()).normalize();
        Vector3f rightVector = new Vector3f(viewMatrix.m00(), viewMatrix.m10(), viewMatrix.m20());
        Vector3f upVector = new Vector3f(viewMatrix.m01(), viewMatrix.m11(), viewMatrix.m21());
        Vector3f forward = new Vector3f(forwardVector).mul(offset.z);
        Vector3f right = new Vector3f(rightVector).mul(-offset.x);
        Vector3f up = new Vector3f(upVector).mul(-offset.y);
        Vector3f newNosePosition = new Vector3f(nose).add(forward).add(right).add(up);
        viewMatrix.setTranslation(newNosePosition);
    }

    /**
     * 
     * Observer's eye
     *
     * @since 0.0.1
     */
    public class Eye {
        
        private float pd; // pupilary distance (PD) in meters
        public Optics optics; // the system's optics for the eye
        
        /** Eye */
        public Eye(float pd) {
            this.pd = pd;
            optics = new Optics();
        }

        /**
         *
         * Get viewMatrix
         *
         * @return the view matrix for the eye
         * @since 0.0.1
         */
        public Matrix4f getView() {
            return new Matrix4f(viewMatrix).translate(new Vector3f(pd, 0.0f, 0.0f));
        }

        /**
         * 
         * Set pupilary distance
         * 
         * @param pd Pupilary distance in mm
         *
         * @since 0.0.1
         */
        private void setPupilDistance(float pd) {
            this.pd = pd;
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

    }
}
