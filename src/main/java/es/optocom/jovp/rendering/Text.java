package es.optocom.jovp.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import org.lwjgl.vulkan.VkCommandBuffer;

import es.optocom.jovp.definitions.FontType;
import es.optocom.jovp.definitions.Vertex;
import es.optocom.jovp.definitions.ViewEye;
import es.optocom.jovp.definitions.ViewMode;

/**
 * Text manager for rendering text
 *
 * @since 0.0.1
 */
public class Text extends Renderable {

    private static final FontType DEFAULT_FONT_TYPE = FontType.SANS;
    private static final float DEFAULT_FONT_SIZE = 0.1f;
    private static final double[] DEFAULT_RGBA = new double[] { 1, 1, 1, 1 };
    private static final int ATLAS_WIDTH = 1024;
    private static final int ATLAS_HEIGHT = 1024;
    private static final int ATLAS_PIXEL_HEIGHT = 200;
    private static final int CHAR_START = 32;
    private static final int CHAR_AMT   = 96;

    private Vector2f position = new Vector2f(0.5f, 0.5f);
    private float size;
    private String text = null;
    private int ascent;
    private int descent;
    private int lineGap;
    private ByteBuffer ttf;
    private ByteBuffer bitmap;
    private STBTTFontinfo fontInfo = STBTTFontinfo.create();
    private STBTTBakedChar.Buffer cdata = STBTTBakedChar.create(CHAR_AMT);
    private Matrix4f modelMatrix = new Matrix4f();
    private Matrix4f projection = new Matrix4f().setOrtho2D(0, 1, 0, 1);
 
    private boolean updateModelMatrix = true;

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
    public Text(FontType fontType, float size) {
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
    public Text(FontType fontType, float size, double[] rgba) {
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
        bitmap = BufferUtils.createByteBuffer(4 * ATLAS_WIDTH * ATLAS_HEIGHT);
        try {
            ttf = loadResourceToByteBuffer(file);
            getFontMetrics(ttf);
            int result = STBTruetype.stbtt_BakeFontBitmap(ttf, ATLAS_PIXEL_HEIGHT, bitmap, ATLAS_WIDTH, ATLAS_HEIGHT, CHAR_START, cdata);
            if (result < 1) throw new RuntimeException("stbtt_BakeFontBitmap failed with return value: " + result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        createAtlasTexture(bitmap, rgba);
        update(new Model());
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
        MemoryUtil.memFree(bitmap);
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
        float[] xpos = new float[] {0.0f};
        float[] ypos = new float[] {0.0f};
        Vertex[] vertices = new Vertex[4 * text.length()];
        Integer[] indices = new Integer[6 * text.length()];
        int lastCodepoint = -1;
        STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();
        for (int i = 0; i < text.length(); i++) {
            int codepoint = text.charAt(i) - CHAR_START;
            if (lastCodepoint != -1) xpos[0] += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, lastCodepoint, codepoint);
            STBTruetype.stbtt_GetBakedQuad(cdata, ATLAS_WIDTH, ATLAS_HEIGHT, codepoint, xpos, ypos, quad, true);
            lastCodepoint = codepoint;
        }
        float width = quad.x1();
        lastCodepoint = -1;
        xpos[0] = 0;
        for (int i = 0; i < text.length(); i++) {
            int codepoint = text.charAt(i) - CHAR_START;
            // Apply kerning adjustment
            if (lastCodepoint != -1) xpos[0] += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, lastCodepoint, codepoint);
            STBTruetype.stbtt_GetBakedQuad(cdata, ATLAS_WIDTH, ATLAS_HEIGHT, codepoint, xpos, ypos, quad, true);
            float x0 = quad.x0() / width;
            float x1 = quad.x1() / width;
            float y0 = quad.y0() / width;
            float y1 = quad.y1() / width;
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
        Model model = getModel();
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
        updateModelMatrix = true;
    }

    /**
     * Set font size
     * 
     * @param size font size
     *
     * @since 0.0.1
     */
    public void setSize(double size) {
        setSize((float) size);
    }

    /**
     * Set font size
     * 
     * @param size font size
     *
     * @since 0.0.1
     */
    public void setSize(float size) {
        this.size = size;
        updateModelMatrix = true;
    }

    /**
     * Return the defined size for this font
     * 
     * @return font size
     * 
     * @since 0.0.1
     */
    public float getFontSize() {
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
     * Render item or text
     *
     * @param stack Memory stack
     * @param commandBuffer Command buffer
     * @param image in-flight frame to render
     *
     * @since 0.0.1
     */
     void render(MemoryStack stack, VkCommandBuffer commandBuffer, int image) {
        if (viewEye == ViewEye.NONE) return;
        if (VulkanSetup.observer.viewMode == ViewMode.MONO) {
            draw(stack, commandBuffer, image, 0);
            return;
        }
        switch (viewEye) {
            case LEFT -> draw(stack, commandBuffer, image, 0);
            case RIGHT-> draw(stack, commandBuffer, image, 1);
            case BOTH-> {
                draw(stack, commandBuffer, image, 0);
                draw(stack, commandBuffer, image, 1);
            }
            default-> {return;}
        }
    }

    /** Update uniforms for the image to be rendered */
    private void draw(MemoryStack stack, VkCommandBuffer commandBuffer, int image, int passNumber) {
        ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(passNumber);
        updateUniforms(image, passNumber, VulkanSetup.observer.optics);
        draw(stack, commandBuffer, image, passNumber, viewPass.textPipeline, viewPass.textPipelineLayout);
    }

    /** Update uniforms for the image to be rendered */
    private void updateUniforms(int image, int eye, Optics optics) {
        if (updateModelMatrix) {
            modelMatrix.translationRotateScale(new Vector3f(position.x, position.y, 0.0f), new Quaternionf(), new Vector3f(size, size, 0.0f));
            updateModelMatrix = false;
        }
        int n = 0;
        try (MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(VulkanSetup.logicalDevice.device, getUniformMemory(image, eye), 0, UNIFORM_SIZEOF, 0, data);
            {
                ByteBuffer buffer = data.getByteBuffer(0, UNIFORM_TEXTSIZEOF);
                modelMatrix.get(n * Float.BYTES, buffer); n += 16;
                projection.get(n * Float.BYTES, buffer); n += 16;
                optics.lensCenter.get(n * Float.BYTES, buffer); n += 4;
                optics.coefficients.get(n * Float.BYTES, buffer); n += 4;
                getTexture().rgba0.get(n * Float.BYTES, buffer);
            }
            vkUnmapMemory(VulkanSetup.logicalDevice.device, getUniformMemory(image, eye));
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
        update(new Texture(rgba, pixels, ATLAS_WIDTH, ATLAS_HEIGHT));
    }

}
