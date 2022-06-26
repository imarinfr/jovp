package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.TextureType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 *
 * Texture
 *
 * <ul>
 * <li>Texture</li>
 * Texture class and methods
 * </ul>
 *
 * @since 0.0.1
 */
public class Texture {

    private static final Vector4f VOID = new Vector4f(0, 0, 0, -1);
    private static final Vector4f TRANSPARENT = new Vector4f(0, 0, 0, 0);

    private static final int PIXEL_SIZE = 4 * Float.BYTES;
    private static final int TEXTURE_SIZE = 512;

    TextureType type;
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
     */
    public Texture() {
        this(new double[] {1, 1, 1, 1});
    }

    /**
     *
     * Create a flat texture
     *
     * @param rgba Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    public Texture(double @NotNull [] rgba) {
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
     */
    public Texture(@NotNull TextureType type) {
        this(type, new double[] {0, 0, 0, 1}, new double[] {1, 1, 1, 1});
    }

    /**
     *
     * Creates spatial patterns: checkerboard, sine, square sine, and 1st, 2nd, and 3rd Gaussian derivatives
     *
     * @param type Whether a CHECKERBOARD, SINE, SQUARESINE, G1, G2, or G3 pattern
     * @param rgba0 Vector of 4 values with the R, G, B, and alpha channels
     * @param rgba1 Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    public Texture(@NotNull TextureType type, double @NotNull [] rgba0, double @NotNull [] rgba1) {
        this.type = type;
        setColors(rgba0, rgba1);
        switch (type) {
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
     * @param rgba texture color
     * @param pixels texture pixels
     * @param width texture width
     * @param height texture height
     *
     * @since 0.0.1
     */
    Texture(double @NotNull [] rgba, float[] pixels, int width, int height) {
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
     */
    public Texture(String fileName) {
        type = TextureType.IMAGE;
        rgba0 = VOID;
        rgba1 = VOID;
        String path;
        try (MemoryStack stack = stackPush()) {
            URL resource = getSystemClassLoader().getResource("samplers/" + fileName);
            if (resource == null) path = fileName;
            else path = String.valueOf(Paths.get(new URI(resource.toExternalForm())));
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);
            stbi_ldr_to_hdr_gamma(1.0f);
            FloatBuffer floats = stbi_loadf(path, pWidth, pHeight, pChannels, STBI_rgb_alpha);
            if (floats == null) throw new RuntimeException("Failed to load texture image " + path);
            pixels = new float[floats.capacity()];
            for(int i = 0; i < floats.capacity(); i++) pixels[i] = floats.get(i);
            stbi_image_free(floats);
            width = pWidth.get(0);
            height = pHeight.get(0);
            size = PIXEL_SIZE * width * height;
            mipLevels = (int) Math.floor(log2(Math.max(width, height))) + 1;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @return The texture type
     *
     * @since 0.0.1
     */
    public TextureType getType() {
        return type;
    }

    /**
     *
     * Clean up after use
     *
     * @since 0.0.1
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
     * @return size of the image
     *
     * @since 0.0.1
     */
    public int getSize() {
        return size;
    }

    /**
     *
     * @return mipmap levels of the image
     *
     * @since 0.0.1
     */
    public int getMipLevels() {
        return mipLevels;
    }

    /**
     *
     * @return pixels the image
     *
     * @since 0.0.1
     */
    public float[] getPixels() {
        return pixels;
    }

    /**
     *
     * Set base texture color
     *
     * @param rgba The RGBA values
     *
     * @since 0.0.1
     */
    public void setColor(double @NotNull [] rgba) {
        switch (type) {
            case FLAT -> {
                rgba0 = VOID;
                rgba1 = new Vector4f((float) rgba[0], (float) rgba[1], (float) rgba[2], (float) rgba[3]);
            }
            case TEXT -> {
                rgba0 = TRANSPARENT;
                rgba1 = new Vector4f((float) rgba[0], (float) rgba[1], (float) rgba[2], (float) rgba[3]);
            }
            case IMAGE -> {
                rgba0 = VOID;
                rgba1 = VOID;
            }
            default -> setColors(rgba, rgba);
        }
    }

    /**
     *
     * Set texture minimum color for grids
     *
     * @param rgbaMin The RGBA values of the minimum color
     * @param rgbaMax The RGBA values of the maximum color
     *
     * @since 0.0.1
     */
    public void setColors(double @NotNull [] rgbaMin, double @NotNull [] rgbaMax) {
        if (type == TextureType.FLAT || type == TextureType.TEXT || type == TextureType.IMAGE) return;
        this.rgba0 = new Vector4f((float) rgbaMin[0], (float) rgbaMin[1], (float) rgbaMin[2], (float) rgbaMin[3]);
        this.rgba1 = new Vector4f((float) rgbaMax[0], (float) rgbaMax[1], (float) rgbaMax[2], (float) rgbaMax[3]);
    }

    /**
     *
     * Get texture color 0 for grids
     *
     * @return The RGBA values of the minimum color
     *
     * @since 0.0.1
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
     */
    public Vector4f rgba1() {
        return rgba1;
    }

    /**
     *
     * Creates a sampler for a flat surface
     *
     * @since 0.0.1
     */
    private void flat() {
        pixels = new float[4];
        pixels[0] = 1; pixels[1] = 1; pixels[2] = 1; pixels[3] = 1;
        width = 1; height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = 1;
    }

    /**
     *
     * Creates a sampler for checkerboard
     *
     * @since 0.0.1
     */
    private void checkerboard() {
        pixels = new float[16];
        pixels[0] = 0; pixels[1] = 0; pixels[2] = 0; pixels[3] = 1;
        pixels[4] = 1; pixels[5] = 1; pixels[6] = 1; pixels[7] = 1;
        pixels[8] = 1; pixels[9] = 1; pixels[10] = 1; pixels[11] = 1;
        pixels[12] = 0; pixels[13] = 0; pixels[14] = 0; pixels[15] = 1;
        width = 2; height = 2;
        size = PIXEL_SIZE * width * height;
        mipLevels = 2;
    }

    /**
     *
     * Creates a sampler for square sin and cos
     *
     * @since 0.0.1
     */
    private void sine() {
        pixels = new float[4 * TEXTURE_SIZE];
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            float level = (float) (0.5 * Math.sin(2 * Math.PI * i / (double) TEXTURE_SIZE) + 0.5);
            pixels[4 * i] = level; pixels[4 * i + 1] = level; pixels[4 * i + 2] = level; pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE; height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /**
     *
     * Creates a sampler the first Gaussian derivative
     *
     * @since 0.0.1
     */
    private void g1() {
        pixels = new float[4 * TEXTURE_SIZE];
        double scale = 2 * Math.exp(-0.5);
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            double x = (8 * i / (double) (TEXTURE_SIZE - 1) - 4);
            float level = (float) (0.5 - x * phi(x) / scale);
            pixels[4 * i] = level; pixels[4 * i + 1] = level; pixels[4 * i + 2] = level; pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE; height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /**
     *
     * Creates a sampler the second Gaussian derivative
     *
     * @since 0.0.1
     */
    private void g2() {
        pixels = new float[4 * TEXTURE_SIZE];
        double scale = 2 * phi(Math.sqrt(3)) + 1;
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            double x = (8 * i / (double) (TEXTURE_SIZE - 1) - 4);
            float level = (float) ((1 + (Math.pow(x, 2) - 1) * phi(x)) / scale);
            pixels[4 * i] = level; pixels[4 * i + 1] = level; pixels[4 * i + 2] = level; pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE; height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /**
     *
     * Creates a sampler the third Gaussian derivative
     *
     * @since 0.0.1
     */
    private void g3() {
        double xmin = Math.sqrt(3 - Math.sqrt(6));
        double scale = 2 * (3 * xmin - (Math.pow(xmin, 3))) * phi(xmin);
        pixels = new float[4 * TEXTURE_SIZE];
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            double x = (8 * i / (double) (TEXTURE_SIZE - 1) - 4);
            float level = (float) (0.5 + (3 * x - (Math.pow(x, 3))) * phi(x) / scale);
            pixels[4 * i] = level; pixels[4 * i + 1] = level; pixels[4 * i + 2] = level; pixels[4 * i + 3] = 1;
        }
        width = TEXTURE_SIZE; height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = (int) Math.floor(log2(width)) + 1;
    }

    /**
     *
     * Creates a sampler for sin and cos
     *
     * @since 0.0.1
     */
    private void squareSine() {
        pixels = new float[8];
        pixels[0] = 0; pixels[1] = 0; pixels[2] = 0; pixels[3] = 1;
        pixels[4] = 1; pixels[5] = 1; pixels[6] = 1; pixels[7] = 1;
        width = 2; height = 1;
        size = PIXEL_SIZE * width * height;
        mipLevels = 2;
    }

    // Log2 function
    private static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    // Gaussian phi function for
    private static double phi(double x) {
        return Math.exp(-Math.pow(x, 2) / 2);
    }
}