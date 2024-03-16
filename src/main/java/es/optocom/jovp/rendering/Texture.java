package es.optocom.jovp.rendering;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import es.optocom.jovp.definitions.TextureType;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.stb.STBImage.*;

/**
 * Texture class and methods
 *
 * @since 0.0.1
 */
public class Texture {

    private static final Vector4f TRANSPARENT = new Vector4f(0, 0, 0, 0);

    private static final int PIXEL_SIZE = 4 * Float.BYTES;
    private static final int TEXTURE_SIZE = 512;

    final TextureType type;
    int size;
    int width;
    int height;
    Vector4f rgba0;
    Vector4f rgba1;
    int mipLevels;
    float[] pixels;

    /**
     * 
     * Create a white flat texture
     *
     * @since 0.0.1
     * 
     */
    public Texture() {
        this(new double[] { 1, 1, 1, 1 });
    }

    /**
     * 
     * Create a flat texture
     *
     * @param rgba Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     * 
     */
    public Texture(double[] rgba) {
        type = TextureType.FLAT;
        setColor(rgba);
        flat();
    }

    /**
     * 
     * Creates a black and white (square) sine wave or checkerboard
     *
     * @param type Whether a CHECKERBOARD or a SINE pattern
     *
     * @since 0.0.1
     * 
     */
    public Texture(TextureType type) {
        this(type, new double[] { 0, 0, 0, 1 }, new double[] { 1, 1, 1, 1 });
    }

    /**
     * 
     * Creates a default sine spatial pattern
     *
     * @param rgba0 Vector of 4 values with the R, G, B, and alpha channels
     * @param rgba1 Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     * 
     */
    public Texture(double[] rgba0, double[] rgba1) {
        this(TextureType.SINE, rgba0, rgba1);
    }

    /**
     * 
     * Creates spatial patterns: checkerboard, sine, square sine, and 1st, 2nd, and
     * 3rd Gaussian derivatives
     *
     * @param type  Whether a CHECKERBOARD, SINE, SQUARESINE, G1, G2, or G3 pattern
     * @param rgba0 Vector of 4 values with the R, G, B, and alpha channels
     * @param rgba1 Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     * 
     */
    public Texture(TextureType type, double[] rgba0, double[] rgba1) {
        this.type = type;
        setColors(rgba0, rgba1);
        switch (type) {
            case FLAT -> flat();
            case CHECKERBOARD -> checkerboard();
            case SQUARESINE -> squareSine();
            case SINE -> sine();
            case G1 -> g1();
            case G2 -> g2();
            case G3 -> g3();
            default -> throw new RuntimeException("Wrong texture type");
        }
    }

    /**
     * 
     * Generate the texture for text
     *
     * @param rgba   texture color
     * @param pixels texture pixels
     * @param width  texture width
     * @param height texture height
     *
     * @since 0.0.1
     * 
     */
    Texture(double[] rgba, float[] pixels, int width, int height) {
        type = TextureType.TEXT;
        setColor(rgba);
        this.width = width;
        this.height = height;
        this.pixels = pixels;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(Math.max(width, height))) + 1;
    }

    /**
     * 
     * Generate the texture from an image file
     *
     * @param fileName The Image's filename
     *
     * @since 0.0.1
     * 
     */
    public Texture(String fileName) {
        type = TextureType.IMAGE;
        this.rgba0 = TRANSPARENT;
        this.rgba1 = TRANSPARENT;
        String path;
        try (MemoryStack stack = stackPush()) {
            URL resource = getSystemClassLoader().getResource("es/optocom/jovp/samplers/" + fileName);
            if (resource == null)
                path = fileName;
            else
                path = String.valueOf(Paths.get(new URI(resource.toExternalForm())));
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);
            stbi_ldr_to_hdr_gamma(1.0f);
            FloatBuffer floats = stbi_loadf(path, pWidth, pHeight, pChannels, STBI_rgb_alpha);
            if (floats == null)
                throw new RuntimeException("Failed to load texture image " + path);
            pixels = new float[floats.capacity()];
            for (int i = 0; i < floats.capacity(); i++)
                pixels[i] = floats.get(i);
            stbi_image_free(floats);
            width = pWidth.get(0);
            height = pHeight.get(0);
            size = PIXEL_SIZE * width * height;
            mipLevels = (int) Math.floor(log2(Math.max(width, height))) + 1;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot create texture.", e);
        }
    }

    /**
     * 
     * Get texture type
     *
     * @return The texture type
     *
     * @since 0.0.1
     * 
     */
    public TextureType getType() {
        return type;
    }

    /**
     * 
     * Clean up after use
     *
     * @since 0.0.1
     * 
     */
    public void destroy() {
        size = 0;
        width = 0;
        height = 0;
        mipLevels = 0;
        pixels = null;
    }

    /**
     * 
     * Get image size
     *
     * @return size of the image
     *
     * @since 0.0.1
     * 
     */
    public int getSize() {
        return size;
    }

    /**
     * 
     * Get mipmap levels
     *
     * @return mipmap levels of the image
     *
     * @since 0.0.1
     * 
     */
    public int getMipLevels() {
        return mipLevels;
    }

    /**
     * 
     * Get image pixels
     *
     * @return pixels the image
     *
     * @since 0.0.1
     * 
     */
    public float[] getPixels() {
        return pixels;
    }

    /**
     * 
     * Get texture colors
     *
     * @return Two colors of the texture
     *
     * @since 0.0.1
     * 
     */
    public Vector4f[] getColors() {
        return new Vector4f[] { rgba0, rgba1 };
    }

    /**
     * 
     * Set base texture color
     *
     * @param rgba The RGBA values
     *
     * @since 0.0.1
     * 
     */
    public void setColor(double[] rgba) {
        setColors(rgba, rgba);
    }

    /**
     * 
     * Set texture minimum color for grids
     *
     * @param rgba0 The RGBA values of color 0 (background or minimum color
     *              depending on context)
     * @param rgba1 The RGBA values of color 1 (foreground or maximum color
     *              depending on context)
     *
     * @since 0.0.1
     * 
     */
    public void setColors(double[] rgba0, double[] rgba1) {
        this.rgba0 = type == TextureType.IMAGE ? TRANSPARENT
                : new Vector4f((float) rgba0[0], (float) rgba0[1], (float) rgba0[2], (float) rgba0[3]);
        this.rgba1 = type == TextureType.IMAGE ? TRANSPARENT
                : new Vector4f((float) rgba1[0], (float) rgba1[1], (float) rgba1[2], (float) rgba1[3]);
    }

    /**
     * 
     * Get texture color 0 for grids
     *
     * @return The RGBA values of the minimum color
     *
     * @since 0.0.1
     * 
     */
    public Vector4f rgba0() {
        return rgba0;
    }

    /**
     * 
     * Get texture color 1 for grids
     *
     * @return The RGBA values of the maximum color
     *
     * @since 0.0.1
     * 
     */
    public Vector4f rgba1() {
        return rgba1;
    }

    /** creates a sampler for a flat surface */
    private void flat() {
        pixels = new float[4];
        pixels[0] = 1;
        pixels[1] = 1;
        pixels[2] = 1;
        pixels[3] = 1;
        width = 1;
        height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = 1;
    }

    /** creates a sampler for checkerboard */
    private void checkerboard() {
        pixels = new float[16];
        pixels[0] = 0;
        pixels[1] = 0;
        pixels[2] = 0;
        pixels[3] = 1;
        pixels[4] = 1;
        pixels[5] = 1;
        pixels[6] = 1;
        pixels[7] = 1;
        pixels[8] = 1;
        pixels[9] = 1;
        pixels[10] = 1;
        pixels[11] = 1;
        pixels[12] = 0;
        pixels[13] = 0;
        pixels[14] = 0;
        pixels[15] = 1;
        width = 2;
        height = 2;
        size = PIXEL_SIZE * width * height;
        mipLevels = 2;
    }

    /** creates a sampler for square sin and cos */
    private void sine() {
        pixels = new float[4 * TEXTURE_SIZE];
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            float level = (float) (0.5 * Math.sin(2 * Math.PI * i / (double) TEXTURE_SIZE) + 0.5);
            pixels[4 * i] = level;
            pixels[4 * i + 1] = level;
            pixels[4 * i + 2] = level;
            pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE;
        height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /** creates a sampler the first Gaussian derivative */
    private void g1() {
        pixels = new float[4 * TEXTURE_SIZE];
        double scale = 2 * Math.exp(-0.5);
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            double x = (8 * i / (double) (TEXTURE_SIZE - 1) - 4);
            float level = (float) (0.5 - x * phi(x) / scale);
            pixels[4 * i] = level;
            pixels[4 * i + 1] = level;
            pixels[4 * i + 2] = level;
            pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE;
        height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /** creates a sampler the second Gaussian derivative */
    private void g2() {
        pixels = new float[4 * TEXTURE_SIZE];
        double scale = 2 * phi(Math.sqrt(3)) + 1;
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            double x = (8 * i / (double) (TEXTURE_SIZE - 1) - 4);
            float level = (float) ((1 + (Math.pow(x, 2) - 1) * phi(x)) / scale);
            pixels[4 * i] = level;
            pixels[4 * i + 1] = level;
            pixels[4 * i + 2] = level;
            pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE;
        height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /** creates a sampler the third Gaussian derivative */
    private void g3() {
        double xmin = Math.sqrt(3 - Math.sqrt(6));
        double scale = 2 * (3 * xmin - (Math.pow(xmin, 3))) * phi(xmin);
        pixels = new float[4 * TEXTURE_SIZE];
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            double x = (8 * i / (double) (TEXTURE_SIZE - 1) - 4);
            float level = (float) (0.5 + (3 * x - (Math.pow(x, 3))) * phi(x) / scale);
            pixels[4 * i] = level;
            pixels[4 * i + 1] = level;
            pixels[4 * i + 2] = level;
            pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE;
        height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /** creates a sampler for sin and cos */
    private void squareSine() {
        pixels = new float[8];
        pixels[0] = 0;
        pixels[1] = 0;
        pixels[2] = 0;
        pixels[3] = 1;
        pixels[4] = 1;
        pixels[5] = 1;
        pixels[6] = 1;
        pixels[7] = 1;
        width = 2;
        height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = 2;
    }

    /** log2 function */
    private static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    /** Gaussian phi function */
    private static double phi(double x) {
        return Math.exp(-Math.pow(x, 2) / 2);
    }

}
