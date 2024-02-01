package es.optocom.jovp.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.FontType;
import es.optocom.jovp.definitions.Vertex;
import es.optocom.jovp.definitions.ViewMode;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

/**
 * Text manager for rendering text
 *
 * @since 0.0.1
 */
public class Text extends Renderable {

    private static final FontType DEFAULT_FONT_TYPE = FontType.MONSERRAT;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final double[] DEFAULT_RGBA = new double[] { 1, 1, 1, 1 };
    private static final int ATLAS_WIDTH = 512;
    private static final int ATLAS_HEIGHT = 512;
    private static final int ATLAS_PIXEL_HEIGHT = 80;
    private static final int CHAR_START = 32;
    private static final int CHAR_AMT   = 96;

    public enum Alignment {LEFT, RIGHT, CENTER};

    private Vector2f position = new Vector2f();
    private int size = DEFAULT_FONT_SIZE;
    private String text = null;
    private Alignment alignment = Alignment.LEFT;
    private int ascent;
    private int descent;
    private int lineGap;
    private ByteBuffer ttf;
    private STBTTFontinfo fontInfo = STBTTFontinfo.create();
    private STBTTBakedChar.Buffer cdata = STBTTBakedChar.create(CHAR_AMT);
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
        ByteBuffer bitmap = BufferUtils.createByteBuffer(4 * ATLAS_WIDTH * ATLAS_HEIGHT);
        try {
            ttf = loadResourceToByteBuffer(file);
            getFontMetrics(ttf);
            int result = STBTruetype.stbtt_BakeFontBitmap(ttf, ATLAS_PIXEL_HEIGHT, bitmap, ATLAS_WIDTH, ATLAS_HEIGHT, CHAR_START, cdata);
            if (result < 1)
                throw new RuntimeException("stbtt_BakeFontBitmap failed with return value: " + result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        createAtlasTexture(bitmap, rgba);
        model = new Model();
        MemoryUtil.memFree(bitmap);
    }

    /**
     * 
     * Clean up after use
     *
     * @since 0.0.1
     */
    @Override
    public void destroy() {
        super.destroy();
        fontInfo.free();
        cdata.free();
        MemoryUtil.memFree(ttf);
    }

    /**
     * Set text to render
     *
     * @param text The text to render
     *
     * @since 0.0.1
     */
    public void setText(String text) {
        this.text = text;
        float[] xpos = new float[] {0.0f}; // Current x position
        float[] ypos = new float[] {0.0f}; // Current y position
        Vertex[] vertices = new Vertex[4 * text.length()];
        Integer[] indices = new Integer[6 * text.length()];
        System.out.println("getting scale");
        float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, 2);
        System.out.println("after");
        float width = 0.0f;
        int lastCodepoint = -1;
        IntBuffer advance = BufferUtils.createIntBuffer(1);
        for (int i = 0; i < text.length(); i++) {
            int codepoint = text.charAt(i) - CHAR_START;
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null);
            width += advance.get(0) * scale;
            if (lastCodepoint != -1)
                width += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, lastCodepoint, codepoint) * scale;
            lastCodepoint = codepoint;
        }
        xpos[0] = switch (alignment) {
            case LEFT -> -1.0f;
            case CENTER -> -width * scale / 2.0f;
            case RIGHT -> -width * scale;
        };
        STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();
        lastCodepoint = -1;
        for (int i = 0; i < text.length(); i++) {
            int codepoint = text.charAt(i) - CHAR_START;
            // Apply kerning adjustment
            if (i > 0)
                xpos[0] += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, lastCodepoint, codepoint) * scale;
            STBTruetype.stbtt_GetBakedQuad(cdata, ATLAS_WIDTH, ATLAS_HEIGHT, codepoint, xpos, ypos, quad, true);
            float x0 = quad.x0() * scale;
            float x1 = quad.x1() * scale;
            float y0 = -quad.y0() * scale;
            float y1 = -quad.y1() * scale;
            vertices[4 * i] = new Vertex(new Vector3f(x0, y0, 0.0f), new Vector2f(quad.s0(), quad.t0())); // top left
            vertices[4 * i + 1] = new Vertex(new Vector3f(x0, y1, 0.0f), new Vector2f(quad.s0(), quad.t1())); // bottom left
            vertices[4 * i + 2] = new Vertex(new Vector3f(x1, y1, 0.0f), new Vector2f(quad.s1(), quad.t1())); // bottom right
            vertices[4 * i + 3] = new Vertex(new Vector3f(x1, y0, 0.0f), new Vector2f(quad.s1(), quad.t0())); // top right
            indices[6 * i] = 4 * i;
            indices[6 * i + 1] = 4 * i + 1;
            indices[6 * i + 2] = 4 * i + 2;
            indices[6 * i + 3] = 4 * i + 2;
            indices[6 * i + 4] = 4 * i + 3;
            indices[6 * i + 5] = 4 * i;
            lastCodepoint = codepoint;
        }
        quad.free();
        model.setVertices(vertices);
        model.setIndices(indices);
        update(model);
    }

    /**
     * 
     * Clear text
     *
     * @since 0.0.1
     */
    public void clear() {
        setText(null);
    }

    /**
     * 
     * Get text
     *
     * @param text the text
     *
     * @since 0.0.1
     */
    public String getText() {
        return text;
    }

    /**
     * 
     * Set alignment text
     *
     * @param alignment alignment
     *
     * @since 0.0.1
     */
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        setText(text);
    }

    /**
     * 
     * Get alignment text
     *
     * @return alignment
     *
     * @since 0.0.1
     */
    public Alignment getAlignment() {
        return alignment;
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
    public void setPosition(double x, double y) {
        setPosition((float) x, (float) y);
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
    public void setPosition(float x, float y) {
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
    public void setSize(int size) {
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
        modelMatrix.scaling(1.0f, 1.0f, 1.0f).translateLocal(-1.0f, 0.0f, 0.0f);
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

    /** get font vertical metrics */
    private void getFontMetrics(ByteBuffer ttf) {
        // vertical metrics
        if (!STBTruetype.stbtt_InitFont(fontInfo, ttf))
            throw new IllegalStateException("Failed to initialize font information.");
        IntBuffer pAscent = BufferUtils.createIntBuffer(1);
        IntBuffer pDescent = BufferUtils.createIntBuffer(1);
        IntBuffer pLineGap = BufferUtils.createIntBuffer(1);
        STBTruetype.stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap);
        ascent = pAscent.get(0);
        descent = pDescent.get(0);
        lineGap = pLineGap.get(0);
    }

    /** create Atlas texture */
    private void createAtlasTexture(ByteBuffer bitmap, double[] rgba) {
        float[] pixels = new float[4 * ATLAS_HEIGHT * ATLAS_WIDTH];
        int k = 0;
        for (int y = 0; y < ATLAS_HEIGHT; y++) {
            for (int x = 0; x < ATLAS_WIDTH; x++) {
                float alpha = (float) (bitmap.get() & 0xFF) / 255.0f; // to int then to float
                pixels[k++] = alpha;
                pixels[k++] = alpha;
                pixels[k++] = alpha;
                pixels[k++] = alpha;
            }
        }
        texture = new Texture(rgba, pixels, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

}
