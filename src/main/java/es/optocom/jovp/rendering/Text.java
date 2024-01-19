package es.optocom.jovp.rendering;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.FontType;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Vertex;
import es.optocom.jovp.definitions.ViewMode;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Text manager for rendering text
 *
 * @since 0.0.1
 */
public class Text extends Renderable {

    private static final FontType DEFAULT_FONT_TYPE = FontType.MONSERRAT;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final double[] DEFAULT_RGBA = new double[] { 1, 1, 1, 1 };
    private static final int ATLAS_WIDTH = 1024;
    private static final int ATLAS_HEIGHT = 512;
    private static final int ATLAS_PIXEL_HEIGHT = 115;
    private static final int CHAR_START = 32;
    private static final int CHAR_AMT   = 96;

    private Vector2f position = new Vector2f();
    private int size = DEFAULT_FONT_SIZE;
    private String text;

    private int ascent; // TODO: necessary?
    private int descent; // TODO: necessary?
    private int lineGap; // TODO: necessary?
 
    Matrix4f modelMatrix = new Matrix4f();
    Matrix4f projection = new Matrix4f(); // orthographic projection
 
    /**
     * Generate a text object with defaults
     *
     * @since 0.0.1
     */
    public Text() {
        this(DEFAULT_FONT_TYPE, DEFAULT_FONT_SIZE, DEFAULT_RGBA);
    }

    /**
     * Generate a text object with default font and size
     *
     * @param rgba color
     *
     * @since 0.0.1
     */
    public Text(double[] rgba) {
        this(DEFAULT_FONT_TYPE, DEFAULT_FONT_SIZE, rgba);
    }

    /**
     * Generate a text object with default size and color
     *
     * @param fontType Font type
     *
     * @since 0.0.1
     */
    public Text(FontType fontType) {
        this(fontType, DEFAULT_FONT_SIZE, DEFAULT_RGBA);
    }

    /**
     * Generate a text object with default color
     *
     * @param fontType Font type
     * @param size Font size
     *
     * @since 0.0.1
     */
    public Text(FontType fontType, int size) {
        this(fontType, size, DEFAULT_RGBA);
    }

    /**
     * Generate a text object
     *
     * @param fontType Font type
     * @param size Font size
     * @param rgba The R, G, B, and alpha color values from 0 to 1
     *
     * @since 0.0.1
     */
    public Text(FontType fontType, int size, double[] rgba) {
        super();
        this.size = size;
        String file;
        switch (fontType) { // font types
            case MONSERRAT -> file = "es/optocom/jovp/fonts/montserrat/Montserrat-Regular.otf";
            case MONSERRAT_BOLD -> file = "es/optocom/jovp/fonts/montserrat/Montserrat-Bold.otf";
            case SANS -> file = "es/optocom/jovp/fonts/openSans/OpenSans-Regular.ttf";
            case SANS_BOLD -> file = "es/optocom/jovp/fonts/openSans/OpenSans-Bold.ttf";
            default -> file = null;
        }
        ByteBuffer ttf = BufferUtils.createByteBuffer(4 * ATLAS_WIDTH * ATLAS_HEIGHT);
        STBTTBakedChar.Buffer cdata = STBTTBakedChar.malloc(CHAR_AMT);
        try {
            int result = STBTruetype.stbtt_BakeFontBitmap(loadResourceToByteBuffer(file), ATLAS_PIXEL_HEIGHT, ttf, ATLAS_WIDTH, ATLAS_HEIGHT, CHAR_START, cdata);
            if (result < 1)
                throw new RuntimeException("stbtt_BakeFontBitmap failed with return value: " + result);
            MemoryUtil.memFree(ttf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Transfer bytes from ByteBuffer to BufferedImage
        float[] pixels = new float[4 * ATLAS_HEIGHT * ATLAS_WIDTH];
        int k = 0;
        for (int y = 0; y < ATLAS_HEIGHT; y++) {
            for (int x = 0; x < ATLAS_WIDTH; x++) {
                float alpha = (float) (ttf.get() & 0xFF) / 255.0f; // to int then to float
                pixels[k++] = alpha;
                pixels[k++] = alpha;
                pixels[k++] = alpha;
                pixels[k++] = alpha;
            }
        }
        //char character = 'A'; // Character to get info for
        //int charIndex = character - CHAR_START;
        //STBTTBakedChar cchar = cdata.get(charIndex);
        // Glyph information
        //int x0 = cchar.x0();
        //int y0 = cchar.y0();
        //int x1 = cchar.x1();
        //int y1 = cchar.y1();
        //int width = x1 - x0;
        //int height = y1 - y0;
        //float xOffset = cchar.xoff();
        //float yOffset = cchar.yoff();
        //float xAdvance = cchar.xadvance();
        model = new Model(ModelType.SQUARE);
        texture = new Texture(rgba, pixels, ATLAS_WIDTH, ATLAS_HEIGHT);
    }
 
    /**
     * Set text to render
     *
     * @param text The text to render
     *
     * @since 0.0.1
     */
    public void set(String text) {
        this.text = text;
    }

    /**
     * 
     * Get current text
     *
     * @param text the text
     *
     * @since 0.0.1
     */
    public String get() {
        return text;
    }

    /**
     * 
     * Position of the text object
     *
     * @param x relative x-axis position between 0 and 1
     * @param y relative y-axis position between 0 and 1
     *
     * @since 0.0.1
     */
    public void position(double x, double y) {
        position((float) x, (float) y);
    }

    /**
     * 
     * Position of the text object
     *
     * @param x relative x-axis position between 0 and 1
     * @param y relative y-axis position between 0 and 1
     *
     * @since 0.0.1
     */
    public void position(float x, float y) {
        this.position.x = x;
        this.position.y = y;
    }

    /**
     * 
     * Set text color
     *
     * @param rgba The RGBA channels to use
     *
     * @since 0.0.1
     */
    public void setColor(double[] rgba) {
        texture.setColor(rgba);
    }

    /**
     * Set font size
     * 
     * @param size font size
     *
     * @since 0.0.1
     */
    public void size(int size) {
        this.size = size;
    }

    /**
     * Get current font size
     * 
     * @return font size
     *
     * @since 0.0.1
     */
    public int getSize() {
        return size;
    }

    /**
     * Return the defined size for this font
     * 
     * @return font height
     * 
     * @since 0.0.1
     */
    public int getFontHeight() {
        return size;
    }
  
    /**
     * Return the width of this bitmaps texture sheet
     * 
     * @return atlas width
     * 
     * @since 0.0.1
     */
    public int getBitmapWidth() {
        return ATLAS_WIDTH;
    }
 
    /**
     * Return the height of this bitmaps texture sheet
     * 
     * @return atlas height
     * 
     * @since 0.0.1
     */
    public int getBitmapHeight() {
        return ATLAS_HEIGHT;
    }
 
    /**
     * Return the ascent of this font
     * 
     * @return font ascent
     * 
     * @since 0.0.1
     */
    public int getAscent() {
        return this.ascent;
    }
 
    /**
     * Return the descent of this font
     * 
     * @return font descent
     * 
     * @since 0.0.1
     */
    public int getDescent() {
        return this.descent;
    }
 
    /**
     * Return the line-gap of this font
     * 
     * @return line gap
     * 
     * @since 0.0.1
     */
    public int getLineGap() {
        return this.lineGap;
    }

    /**
     * 
     * Render item
     *
     * @param stack Memory stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     *
     * @since 0.0.1
     */
    @Override
    void render(MemoryStack stack, VkCommandBuffer commandBuffer, int image) {
        if (VulkanSetup.observer.viewMode == ViewMode.MONO & eye != Eye.NONE) { // monoscopic view
            renderEye(stack, commandBuffer, image, 0);
            return;
        }
        switch (eye) { // stereoscopic view
            case LEFT -> renderEye(stack, commandBuffer, image, 0);
            case RIGHT -> renderEye(stack, commandBuffer, image, 1);
            case BOTH -> {
                renderEye(stack, commandBuffer, image, 0);
                renderEye(stack, commandBuffer, image, 1);
            }
            case NONE -> {}
        }
    }

    /**
     * 
     * Render item for a specific eye
     * 
     * @param stack  stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     * @param passNumber pass number. For MONO vision, it ought to be 0. For
     *                   STEREO, left is 0 and right is 1
     *
     * @since 0.0.1
     */
    @Override
    void renderEye(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int passNumber) {
        ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(passNumber);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, viewPass.textPipeline);
        updateUniforms(image, passNumber);
        if (updateModel) recreateModel();
        if (updateTexture) recreateTexture();
        LongBuffer vertexBuffers = stack.longs(vertexBuffer);
        LongBuffer offsets = stack.longs(0);
        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
        vkCmdBindIndexBuffer(commandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT32);
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                viewPass.textPipelineLayout, 0,
                stack.longs(descriptorSets.get(image)), null);
        vkCmdDrawIndexed(commandBuffer, model.length, 1,
                0, 0, 0);
    }

    /**
     *
     * Update uniforms for the image to be rendered
     *
     * @param imageIndex Image to be rendered
     *
     * @since 0.0.1
     */
    @Override
    void updateUniforms(int imageIndex, int passNumber) {
        //if (updateModelMatrix) {
        //    Vector3d position = new Vector3d(direction.x, direction.y, direction.z).mul(distance);
        //    Quaterniond quaternion = new Quaterniond()
        //        .rotationTo(new Vector3d(0, 0, 1), direction)
        //        .rotateZYX(rotation.z, rotation.y, rotation.x);
        //    modelMatrix.translationRotateScale(position, quaternion, scale);
        //    updateModelMatrix = false;
        //}
        modelMatrix.scaling(0.25f, 0.25f, 1.0f).translateLocal(-0.75f, 0.75f, 0.0f);
        projection.setOrtho2D(-1, 1, 1, -1);
        int n = 0;
        try (MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex), 0,
                    VulkanSetup.UNIFORM_SIZEOF, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, VulkanSetup.UNIFORM_TEXTSIZEOF);
                modelMatrix.get(n * Float.BYTES, buffer); n += 16;
                projection.get(n * Float.BYTES, buffer); n += 16;
                texture.rgba0.get(n * Float.BYTES, buffer);
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, uniformBuffersMemory.get(imageIndex));
        }
    }

    /** read resource to bytebuffer */
    private ByteBuffer loadResourceToByteBuffer(String file) throws IOException{
        ByteBuffer buffer = null;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(file)) {
            if (is == null) throw new IOException("Resource not found: " + file);
            byte[] byteArray = is.readAllBytes();
            buffer = BufferUtils.createByteBuffer(byteArray.length).put(byteArray);
        }
        return buffer.flip();
    }

}
