package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.TextureType;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Model
 *
 * <ul>
 * <li>Model</li>
 * Model defaults
 * </ul>
 *
 * @since 0.0.1
 */
public class Texture {

    private TextureType type;
    private int size;
    private int width;
    private int height;
    private double[] rgba;
    private double[] rgbaMin;
    private double[] rgbaMax;
    private int mipLevels;
    private ByteBuffer pixels;

    /**
     * Create a white flat texture
     *
     * @since 0.0.1
     */
    public Texture() {
        this(new double[] {1, 1, 1, 1});
    }

    /**
     * Create a flat texture
     *
     * @param rgba Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    public Texture(double @NotNull [] rgba) {
        type = TextureType.FLAT;
        this.rgba = rgba;
        pixels = ByteBuffer.allocate(4);
        pixels.put((byte) (255.0f * rgba[0]));
        pixels.put((byte) (255.0f * rgba[1]));
        pixels.put((byte) (255.0f * rgba[2]));
        pixels.put((byte) (255.0f * rgba[3]));
        width = 1; height = 1;
        size = width * height * 4;
        mipLevels = 1;
        pixels.rewind();
    }

    /**
     * Create a checkerboard texture
     *
     * @param rgbaMin Vector of 4 values with the R, G, B, and alpha channels
     * @param rgbaMax Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    public Texture(double @NotNull [] rgbaMin, double @NotNull [] rgbaMax) {
        this.rgbaMin = rgbaMin;
        this.rgbaMax = rgbaMax;
        type = TextureType.CHECKERBOARD;
        pixels = ByteBuffer.allocate(16);
        pixels.put((byte) (255.0f * rgbaMin[0]));
        pixels.put((byte) (255.0f * rgbaMin[1]));
        pixels.put((byte) (255.0f * rgbaMin[2]));
        pixels.put((byte) (255.0f * rgbaMin[3]));
        pixels.put((byte) (255.0f * rgbaMax[0]));
        pixels.put((byte) (255.0f * rgbaMax[1]));
        pixels.put((byte) (255.0f * rgbaMax[2]));
        pixels.put((byte) (255.0f * rgbaMax[3]));
        pixels.put((byte) (255.0f * rgbaMax[0]));
        pixels.put((byte) (255.0f * rgbaMax[1]));
        pixels.put((byte) (255.0f * rgbaMax[2]));
        pixels.put((byte) (255.0f * rgbaMax[3]));
        pixels.put((byte) (255.0f * rgbaMin[0]));
        pixels.put((byte) (255.0f * rgbaMin[1]));
        pixels.put((byte) (255.0f * rgbaMin[2]));
        pixels.put((byte) (255.0f * rgbaMin[3]));
        width = 2; height = 2;
        size = width * height * 4;
        mipLevels = 2;
        pixels.rewind();
    }

    /**
     * Creates a (square) sine wave
     *
     * @param type Whether a SQUARESINE or a SINE pattern
     * @param phase Phase in degrees of visual angle
     * @param frequency Frequency in degrees of visual angle
     * @param rgbaMin Vector of 4 values with the R, G, B, and alpha channels
     * @param rgbaMax Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    public Texture(@NotNull TextureType type, double phase, double frequency,
                   double @NotNull [] rgbaMin, double @NotNull [] rgbaMax) {
        this.type = type;
        this.rgbaMin = rgbaMin;
        this.rgbaMax = rgbaMax;
        switch (type) {
            case SQUARESINE -> squareSine(phase, frequency, rgbaMin, rgbaMax);
            case SINE -> sine(phase, frequency, rgbaMin, rgbaMax);
            default -> throw new RuntimeException("Wrong texture type");
        }
        pixels.rewind();
    }

    /**
     * Creates a Gabor pattern
     *
     * @param phase Phase in degrees of visual angle
     * @param frequency Frequency in degrees of visual angle
     * @param sd Standard deviation of the Gabor pattern in degrees of visual angle
     * @param rgbaMin Vector of 4 values with the R, G, B, and alpha channels
     * @param rgbaMax Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    public Texture(double phase, double frequency, double sd, double @NotNull [] rgbaMin, double @NotNull [] rgbaMax) {
        type = TextureType.GABOR;
        this.rgbaMin = rgbaMin;
        this.rgbaMax = rgbaMax;
        pixels = ByteBuffer.allocate(8);
        pixels.put((byte) (255.0f * rgbaMin[0]));
        pixels.put((byte) (255.0f * rgbaMin[1]));
        pixels.put((byte) (255.0f * rgbaMin[2]));
        pixels.put((byte) (255.0f * rgbaMin[3]));
        pixels.put((byte) (255.0f * rgbaMax[0]));
        pixels.put((byte) (255.0f * rgbaMax[1]));
        pixels.put((byte) (255.0f * rgbaMax[2]));
        pixels.put((byte) (255.0f * rgbaMax[3]));
        width = 2; height = 1;
        size = width * height * 4;
        mipLevels = 2;
        pixels.rewind();
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
        String path;
        try (MemoryStack stack = stackPush()) {
            URL resource = getSystemClassLoader().getResource("samplers/" + fileName);
            if (resource == null) path = fileName;
            else path = String.valueOf(Paths.get(new URI(resource.toExternalForm())));
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);
            pixels = stbi_load(path, pWidth, pHeight, pChannels, STBI_rgb_alpha);
            if (pixels == null) throw new RuntimeException("Failed to load texture image " + path);
            width = pWidth.get(0);
            height = pHeight.get(0);
            size = width * height * 4;
            mipLevels = (int) Math.floor(log2(Math.max(width, height))) + 1;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        pixels.rewind();
    }

    /**
     *
     * Generate the texture from a buffered image
     *
     * @param width  width of the texture
     * @param height height of the texture
     * @param colorArray pixel data
     *
     * @since 0.0.1
     */
    public Texture(int width, int height, byte @NotNull [] colorArray) {
        type = TextureType.TEXT;
        this.width = width;
        this.height = height;
        size = width * height * 4;
        mipLevels = (int) Math.floor(log2(Math.max(width, height))) + 1;
        pixels = ByteBuffer.allocate(colorArray.length);
        pixels.put(colorArray);
        pixels.rewind();
    }

    /**
     *
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void destroy() {
        if (type == TextureType.IMAGE) stbi_image_free(pixels);
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
     * @return width of the image
     *
     * @since 0.0.1
     */
    public int getWidth() {
        return width;
    }

    /**
     *
     * @return height of the image
     *
     * @since 0.0.1
     */
    public int getHeight() {
        return height;
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
    public ByteBuffer getPixels() {
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
    public void setColor(double[] rgba) {
        this.rgba = rgba;
        if(type == TextureType.FLAT) {
            for (int i = 0; i < pixels.capacity(); i++)
                pixels.put(i, (byte) (255.0f * rgba[(i % 4)]));
        } else if(type == TextureType.TEXT) {
            for (int i = 0; i < width * height; i++) {
                if(pixels.get(4 * i + 3) != 0) { // check alpha channel, if 0, then no color.
                    for (int pos = 0; pos < 4; pos++) {
                        pixels.put(4 * i + pos, (byte) (255.0f * rgba[pos]));
                    }
                }
            }
        } else System.err.println("Cannot set a flat color for texture type " + type);
    }

    /**
     *
     * Set base text color
     *
     * @return The RGBA values
     *
     * @since 0.0.1
     */
    public double[] getColor() {
        return rgba;
    }

    // Update texture
    public void update(TextureType type, int width, int height, @NotNull ByteBuffer pixels) {
        this.type = type;
        this.width = width;
        this.height = height;
        size = width * height * 4;
        mipLevels = (int) Math.floor(log2(Math.max(width, height))) + 1;
        this.pixels = pixels.rewind();
    }

    /**
     * Creates a square sine wave
     *
     * @param phase Phase in degrees of visual angle
     * @param frequency Frequency in degrees of visual angle
     * @param rgbaMin Vector of 4 values with the R, G, B, and alpha channels
     * @param rgbaMax Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    private void squareSine(double phase, double frequency, double @NotNull [] rgbaMin, double @NotNull [] rgbaMax) {
        pixels = ByteBuffer.allocate(8);
        pixels.put((byte) (255.0f * rgbaMin[0]));
        pixels.put((byte) (255.0f * rgbaMin[1]));
        pixels.put((byte) (255.0f * rgbaMin[2]));
        pixels.put((byte) (255.0f * rgbaMin[3]));
        pixels.put((byte) (255.0f * rgbaMax[0]));
        pixels.put((byte) (255.0f * rgbaMax[1]));
        pixels.put((byte) (255.0f * rgbaMax[2]));
        pixels.put((byte) (255.0f * rgbaMax[3]));
        pixels.rewind();
        width = 2; height = 1;
        size = width * height * 4;
        mipLevels = 2;
    }

    /**
     * Creates a sine wave
     *
     * @param phase Phase in degrees of visual angle
     * @param frequency Frequency in degrees of visual angle
     * @param rgbaMin Vector of 4 values with the R, G, B, and alpha channels
     * @param rgbaMax Vector of 4 values with the R, G, B, and alpha channels
     *
     * @since 0.0.1
     */
    private void sine(double phase, double frequency, double @NotNull [] rgbaMin, double @NotNull [] rgbaMax) {
        pixels = ByteBuffer.allocate(8);
        pixels.put((byte) (255.0f * rgbaMin[0]));
        pixels.put((byte) (255.0f * rgbaMin[1]));
        pixels.put((byte) (255.0f * rgbaMin[2]));
        pixels.put((byte) (255.0f * rgbaMin[3]));
        pixels.put((byte) (255.0f * rgbaMax[0]));
        pixels.put((byte) (255.0f * rgbaMax[1]));
        pixels.put((byte) (255.0f * rgbaMax[2]));
        pixels.put((byte) (255.0f * rgbaMax[3]));
        pixels.rewind();
        width = 2; height = 1;
        size = width * height * 4;
        mipLevels = 2;
    }

    // Log2 function
    private static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

}
