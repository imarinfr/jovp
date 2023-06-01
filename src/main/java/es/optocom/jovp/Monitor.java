package es.optocom.jovp;

import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;

/**
 * 
 * Retrieves and stores the monitor capabilities including size in mm, pixel
 * size for the calculation of the
 * observer's field of view.
 *
 * @since 0.0.1
 */
public class Monitor {

    private final long monitor; // monitor's handle
    private String name; // monitor's name
    private final GLFWVidMode videoMode; // default user's video mode
    private int width; // in pixels
    private int height;
    private int refreshRate;
    private int widthMM; // in mm
    private int heightMM;
    private float aspect; // Aspect ratio
    private float pixelWidth; // pixel width in mm
    private float pixelHeight; // pixel height in mm
    private float pixelAspect; // Aspect ratio between pixels
    private int[] colorDepth = new int[3]; // Bit depths for all channels
    private GLFWVidMode.Buffer videoModes;

    /**
     * 
     * Monitor manager
     * 
     * @param monitor The monitor handle
     *
     * @since 0.0.1
     */
    Monitor(long monitor) {
        this.monitor = monitor;
        videoMode = glfwGetVideoMode(monitor);
        getMonitorSettings();
        setPhysicalSize();
    }

    /**
     * 
     * Get monitor handle
     *
     * @return The monitor handle
     *
     * @since 0.0.1
     */
    public long getHandle() {
        return monitor;
    }

    /**
     * 
     * Get monitor name
     *
     * @return The monitor name
     *
     * @since 0.0.1
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * Get monitor width
     *
     * @return The monitor width in pixels
     *
     * @since 0.0.1
     */
    public int getWidth() {
        return width;
    }

    /**
     * 
     * Get monitor height
     *
     * @return The monitor height in pixels
     *
     * @since 0.0.1
     */
    public int getHeight() {
        return height;
    }

    /**
     *
     * Get monitor physical width
     *
     * @return The monitor width in mm
     *
     * @since 0.0.1
     */
    public int getWidthMM() {
        return widthMM;
    }

    /**
     * 
     * Get monitor physical height
     *
     * @return The monitor height in mm
     *
     * @since 0.0.1
     */
    public int getHeightMM() {
        return heightMM;
    }

    /**
     * 
     * Get monitor aspect ratio
     *
     * @return The monitor aspect ratio
     *
     * @since 0.0.1
     */
    public float getAspect() {
        return aspect;
    }

    /**
     * 
     * Manual input the pixel size
     *
     * @param width  The width of the monitor in pixels
     * @param height The height of the monitor in pixels
     *
     * @since 0.0.1
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        computePixelSize(); // update pixel size
    }

    /**
     * 
     * Manual input the physical size
     *
     * @param widthMM  The width of the monitor in mm
     * @param heightMM The height of the monitor in mm
     *
     * @since 0.0.1
     */
    public void setPhysicalSize(int widthMM, int heightMM) {
        this.widthMM = widthMM;
        this.heightMM = heightMM;
        aspect = (float) this.widthMM / this.heightMM;
        computePixelSize(); // update pixel size
    }

    /**
     * 
     * Get monitor pixel width
     *
     * @return The pixel width in mm
     *
     * @since 0.0.1
     */
    public float getPixelWidth() {
        return pixelWidth;
    }

    /**
     * 
     * Get monitor pixel height
     *
     * @return The pixel height in mm
     *
     * @since 0.0.1
     */
    public float getPixelHeight() {
        return pixelHeight;
    }

    /**
     * 
     * Get monitor pixel aspect ratio
     *
     * @return The aspect ratio between x and y pixel sizes
     *
     * @since 0.0.1
     */
    public float getPixelAspect() {
        return pixelAspect;
    }

    /**
     * 
     * Get the pixel density in dots per inch
     *
     * @return The dots per inch
     *
     * @since 0.0.1
     */
    public float[] getDpi() {
        return new float[] { 25.4f / pixelWidth, 25.4f / pixelHeight };
    }

    /**
     * 
     * Give the option to input the refresh rate manually
     *
     * @param refreshRate Refresh rate for full-screen mode
     *
     * @since 0.0.1
     */
    public void setRefreshRate(int refreshRate) {
        this.refreshRate = refreshRate;
    }

    /**
     * 
     * Get refresh rate
     *
     * @return The refresh rate for full-screen mode
     *
     * @since 0.0.1
     */
    public int getRefreshRate() {
        return refreshRate;
    }

    /**
     * 
     * RGB color depth
     *
     * @return The color depth in the R, G, and B channels in bits
     *
     * @since 0.0.1
     */
    public int[] getColorDepth() {
        return colorDepth;
    }

    /**
     * 
     * Get stored video mode
     *
     * @return Get stored video mode
     *
     * @since 0.0.1
     */
    public GLFWVidMode getVideoMode() {
        return videoMode;
    }

    /**
     * 
     * Get current video mode
     *
     * @return Get current video mode
     *
     * @since 0.0.1
     */
    public GLFWVidMode getCurrentVideoMode() {
        return glfwGetVideoMode(monitor);
    }

    /**
     * 
     * Get video modes
     *
     * @return Get all possible video modes
     *
     * @since 0.0.1
     */
    public GLFWVidMode.Buffer getVideoModes() {
        return videoModes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String pixelSizeTxt = "[" + String.format("%.1f", 1000 * pixelWidth) + ", " +
                String.format("%.1f", 1000 * pixelHeight) + "]";
        String dpiTxt = "[" + String.format("%.1f", getDpi()[0]) + ", " + String.format("%.1f", getDpi()[1]) + "]";
        String colorDepthStr = "[" + colorDepth[0] + ", " + colorDepth[0] + ", " + colorDepth[0] + "]";
        return "Monitor name: " + getName() + "\n" + "\tHandle: " + getHandle() + "\n" +
                "\tResolution: (" + width + ", " + height + ") pixels\n" +
                "\tMonitor size: (" + widthMM + ", " + heightMM + ") mm\n" +
                "\tPixel size (x, y): " + pixelSizeTxt + " microns per pixel\n" +
                "\tDPI (x, y): " + dpiTxt + " pixels per inch\n" +
                "\tRefresh rate: " + getRefreshRate() + " Hz\n" +
                "\tRGB color depth: " + colorDepthStr + " bits\n";
    }

    /**
     * Retrieves the monitor's maximum resolution, video mode, color depth, and
     * refresh rate
     */
    private void getMonitorSettings() {
        name = glfwGetMonitorName(monitor);
        // get all video modes
        videoModes = glfwGetVideoModes(monitor);
        if (videoModes == null)
            throw new RuntimeException("No video modes found for monitor " + monitor);
        // get video mode for full screen
        GLFWVidMode fullScreenVideoMode = videoModes.get(0);
        for (int i = 1; i < videoModes.stream().count(); i++)
            if (fullScreenVideoMode.width() * fullScreenVideoMode.height() < videoModes.get(i).width()
                    * videoModes.get(i).height() ||
                    videoModes.refreshRate() < videoModes.get(i).refreshRate())
                fullScreenVideoMode = videoModes.get(i);
        width = fullScreenVideoMode.width();
        height = fullScreenVideoMode.height();
        refreshRate = fullScreenVideoMode.refreshRate();
        colorDepth[0] = fullScreenVideoMode.redBits();
        colorDepth[1] = fullScreenVideoMode.greenBits();
        colorDepth[2] = fullScreenVideoMode.blueBits();
    }

    /**
     * Get the monitor's physical size.
     * This information can be incorrect if the EDID data provided is
     * wrong or because the driver does not report it accurately
     */
    private void setPhysicalSize() {
        int[] widthMM = new int[1], heightMM = new int[1];
        glfwGetMonitorPhysicalSize(monitor, widthMM, heightMM);
        this.widthMM = widthMM[0];
        this.heightMM = heightMM[0];
        aspect = (float) this.widthMM / this.heightMM;
        computePixelSize(); // update pixel size
    }

    /**
     * Calculates pixel size from the maximum resolution of the monitor and its
     * physical dimensions
     */
    private void computePixelSize() {
        pixelWidth = (float) widthMM / (float) width;
        pixelHeight = (float) heightMM / (float) height;
        pixelAspect = pixelWidth / pixelHeight;
    }

}