package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.FontType;
import es.optocom.jovp.engine.structures.Vertex;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Text
 *
 * <ul>
 * <li>Text</li>
 * Text manager for rendering text
 * </ul>
 *
 * @since 0.0.1
 */
public class Text extends Item {

    private static final float FONT_SIZE = 200.0f;
    private static final FontType DEFAULT_FONT_TYPE = FontType.MONSERRAT;
    private static final double[] DEFAULT_RGBA = new double[] {1, 1, 1, 1};

    private final FontRenderContext fontRenderContext;
    private final Font font;
    HashMap<Character, CharInfo> map;
    String text;
    double width;
    double height = 1;

    /**
     *
     * Generate a white text item with default font type MONSERRAT
     *
     * @since 0.0.1
     */
    public Text() {
        this(DEFAULT_FONT_TYPE, DEFAULT_RGBA);
    }

    /**
     *
     * Generate a white text item
     *
     * @param fontType The font time
     *
     * @since 0.0.1
     */
    public Text(@NotNull FontType fontType) {
        this(fontType, DEFAULT_RGBA);
    }

    /**
     *
     * Generate a text item with default font type MONSERRAT
     *
     * @param rgba The R, G, B, and alpha color values from 0 to 1
     *
     * @since 0.0.1
     */
    public Text(double[] rgba) {
        this(DEFAULT_FONT_TYPE, rgba);
    }

    /**
     *
     * Generate a text item
     *
     * @param fontType FontType type
     * @param rgba The R, G, B, and alpha color values from 0 to 1
     *
     * @since 0.0.1
     */
    public Text(@NotNull FontType fontType, double[] rgba) {
        super();
        map = new HashMap<>();
        fontRenderContext = new FontRenderContext(null, true, false);
        String file;
        switch (fontType) { // font types
            case MONSERRAT -> file = "fonts/montserrat/Montserrat-Regular.otf";
            case MONSERRAT_BOLD -> file = "fonts/montserrat/Montserrat-Bold.otf";
            case SANS -> file = "fonts/openSans/OpenSans-Regular.ttf";
            case SANS_BOLD -> file = "fonts/openSans/OpenSans-Bold.ttf";
            default -> file = null;
        }
        try (InputStream reader = Thread.currentThread().getContextClassLoader().getResourceAsStream(file)) {
            if (reader == null) throw new RuntimeException("Could not read font");
            font = Font.createFont(Font.TRUETYPE_FONT, reader).deriveFont(FONT_SIZE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        createFontTexture(rgba);
    }

    /**
     *
     * Set text to render
     *
     * @param text The text to render
     *
     * @since 0.0.1
     */
    public void setText(@NotNull String text) {
        width = 0;
        this.text = text;
        final Vertex[] vertices = new Vertex[4 * text.length()];
        final Integer[] indices = new Integer[6 * text.length()];
        GlyphVector glyphVector = font.createGlyphVector(fontRenderContext, text);
        double width = glyphVector.getLogicalBounds().getWidth();
        char[] chars = text.toCharArray();
        for (int i = 0; i < glyphVector.getNumGlyphs(); i++) {
            glyphVector.getGlyphCode(i);
            Point2D pos = glyphVector.getGlyphPosition(i);
            Rectangle2D bounds = glyphVector.getGlyphMetrics(i).getBounds2D();
            CharInfo charInfo = map.get(chars[i]);
            float xmin = (float) ((pos.getX() + bounds.getMinX()) / width);
            float xmax = (float) ((pos.getX() + bounds.getMaxX()) / width);
            float x0 = charInfo.x;
            float x1 = x0 + charInfo.width;
            vertices[4 * i] = new Vertex(new Vector3f(xmin, -1.0f, 0.0f), new Vector2f(x0, 1.0f));
            vertices[4 * i + 1] = new Vertex(new Vector3f(xmax, -1.0f, 0.0f), new Vector2f(x1, 1.0f));
            vertices[4 * i + 2] = new Vertex(new Vector3f(xmax, 1.0f, 0.0f), new Vector2f(x1, 0.0f));
            vertices[4 * i + 3] = new Vertex(new Vector3f(xmin, 1.0f, 0.0f), new Vector2f(x0, 0.0f));
            indices[6 * i] = 4 * i;
            indices[6 * i + 1] = 4 * i + 1;
            indices[6 * i + 2] = 4 * i + 2;
            indices[6 * i + 3] = 4 * i + 2;
            indices[6 * i + 4] = 4 * i + 3;
            indices[6 * i + 5] = 4 * i;
            this.width += map.get(chars[i]).width();
        }
        updateModel(vertices, indices);
        setSize();
    }

    /**
     *
     * @param height in degrees of visual angle
     *
     * @since 0.0.1
     */
    public void setSize(double height) {
        this.height = height;
        setSize();
    }

    // Set size
    private void setSize() {
        size(FONT_SIZE * width / 2 * height, height, 1);
    }

    // Creates a font texture from specified AWT font.
    private void createFontTexture(double[] rgba) {
        StringBuilder text = new StringBuilder();
        // Consider only standard ASCII characters
        for (int i = 32; i < 127; i++) if (font.canDisplay(i)) text.append((char) i);
        GlyphVector glyphs = font.createGlyphVector(fontRenderContext, text.toString());
        int height = glyphs.getLogicalBounds().getBounds().height;
        int width = glyphs.getLogicalBounds().getBounds().width;
        int ascent = glyphs.getLogicalBounds().getBounds().y;
        int x = 0;
        // Find the image regions to fill
        boolean[] fill = new boolean[width * height];
        for (int glyph = 0; glyph < glyphs.getNumGlyphs(); glyph++) {
            float charWidth = (int) glyphs.getGlyphMetrics(glyph).getBounds2D().getWidth();
            int advance = (int) glyphs.getGlyphMetrics(glyph).getAdvanceX();
            boolean[] glyphFill = getFillRegion(glyphs.getGlyphOutline(glyph), advance, height, ascent);
            for (int i = 0; i < height; i++) for (int j = 0; j < advance; j++)
                fill[i * width + x + j] = glyphFill[i * advance + j];
            // Add map to the catalog
            map.put(text.charAt(glyph), new CharInfo(x / (float) width, charWidth / (float) width));
            x += advance;
        }
        // Colors for the texture
        byte[] color = new byte[4 * width * height];
        for (int i = 0; i < fill.length; i++) {
            if (fill[i]) for (int pos = 0; pos < 4; pos++) // Fill regions with value -1
                color[4 * i + pos] = (byte) (255.0f * rgba[pos]);
            else for (int pos = 0; pos < 4; pos++)
                color[4 * i + pos] = (byte) 0;
        }
        updateTexture(width, height, ByteBuffer.wrap(color));
    }

    // Get fill region for a character for rendering
    private boolean @NotNull [] getFillRegion(@NotNull Shape glyphShape, int width, int height, int ascent) {
        boolean[] fill = new boolean[width * height];
        int xmin = glyphShape.getBounds().x;
        int ymin = glyphShape.getBounds().y - ascent;
        int ymax = ymin + glyphShape.getBounds().height;
        for (int y = ymin; y < ymax; y++) for (int x = 0; x < width; x++)
            fill[y * width + x] = glyphShape.contains(x + xmin, y + ascent);
        return fill;
    }

    // Internal class to map characters with position and width in Atlas
    record CharInfo(float x, float width) {}

}
