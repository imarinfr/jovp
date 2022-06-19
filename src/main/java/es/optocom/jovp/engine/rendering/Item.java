package es.optocom.jovp.engine.rendering;

import es.optocom.jovp.engine.structures.TextureType;
import es.optocom.jovp.engine.structures.Vertex;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

import static es.optocom.jovp.engine.rendering.VulkanSettings.Z_FAR;

/**
 * Item
 *
 * <ul>
 * <li>Item</li>
 * Item to construct the psychophysical experience
 * </ul>
 *
 * @since 0.0.1
 */
public class Item {

    boolean show;
    Model model;
    Texture texture;
    VulkanBuffers buffers;
    final Vector3d position = new Vector3d(0, 0, Z_FAR); // radian, radian, mm
    final Vector3d size = new Vector3d(Math.PI / 180, Math.PI / 180, 1); // radian, radian, mm
    double rotation = 0; // in radians
    Vector3f axis = new Vector3f(0, 0, 0);
    boolean update = false;

    /**
     *
     * Create an item for psychophysics experience
     *
     * @param model The model (square, circle, etc)
     * @param texture The texture
     *
     * @since 0.0.1
     */
    public Item(Model model, Texture texture) {
        this.model = model;
        this.texture = texture;
        show = true;
    }

    /**
     *
     * Init an item, used for use with Text
     *
     * @since 0.0.1
     */
    Item() {
        show = true;
        model = new Model();
        texture = new Texture();
    }

    /**
     *
     * Create Vulkan buffers
     *
     * @since 0.0.1
     */
    public void create() {
        buffers = new VulkanBuffers(this);
    }

    /**
     *
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void destroy() {
        model.destroy();
        texture.destroy();
        if (buffers != null) buffers.destroy();
        buffers = null;
    }

    /**
     *
     * Set whether to show the item or not
     *
     * @param show Whether to show the item or not
     *
     * @since 0.0.1
     */
    public void show(boolean show) {
        this.show = show;
    }

    /**
     *
     * @return Whether item has been updated
     *
     * @since 0.0.1
     */
    public boolean update() {
        return update;
    }

    /**
     *
     * @param update Whether item has been updated
     *
     * @since 0.0.1
     */
    public void update(boolean update) {
        this.update = update;
    }

    /**
     *
     * @return The model
     *
     * @since 0.0.1
     */
    public Model getModel() {
        return model;
    }

    /**
     *
     * Get texture base color
     *
     * @return The RGBA channels to use
     *
     * @since 0.0.1
     */
    public double[] getColor() {
        return texture.getColor();
    }

    /**
     *
     * Set texture color
     *
     * @param rgba The RGBA channels to use
     *
     * @since 0.0.1
     */
    public void setColor(double[] rgba) {
        texture.setColor(rgba);
    }

    /**
     *
     * Set item size
     *
     * @param x Size along the x-axis
     * @param y Size along the y-axis
     *
     * @since 0.0.1
     */
    public void size(double x, double y) {
        size((float) x, (float) y, 1.0f);
    }

    /**
     *
     * Set item size
     *
     * @param x Size along the x-axis
     * @param y Size along the y-axis
     * @param z Size along the z-axis
     *
     * @since 0.0.1
     */
    public void size(float x, float y, float z) {
        size.x = Math.toRadians(x / 2); size.y = Math.toRadians(y / 2); size.z = z;
    }

    /**
     *
     * Set item size
     *
     * @param x Size along the x-axis
     * @param y Size along the y-axis
     * @param z Size along the z-axis
     *
     * @since 0.0.1
     */
    public void size(double x, double y, double z) {
        size((float) x, (float) y, (float) z);
    }

    /**
     *
     * Translate the item
     *
     * @param x Translation along the x-axis
     * @param y Translation along the y-axis
     *
     * @since 0.0.1
     */
    public void position(double x, double y) {
        position((float) x, (float) y, Z_FAR);
    }

    /**
     *
     * Move the item
     *
     * @param x Translation along the x-axis
     * @param y Translation along the y-axis
     * @param z Translation along the z-axis
     *
     * @since 0.0.1
     */
    public void position(float x, float y, float z) {
        position.x = Math.toRadians(x); position.y = Math.toRadians(y); position.z = z;
    }

    /**
     *
     * Move the item
     *
     * @param x Translation along the x-axis
     * @param y Translation along the y-axis
     * @param z Translation along the z-axis
     *
     * @since 0.0.1
     */
    public void position(double x, double y, double z) {
        position((float) x, (float) y, (float) z);
    }

    /**
     *
     * Rotate the item
     *
     * @param rotation Angle of rotation in degrees
     *
     * @since 0.0.1
     */
    public void rotation(double rotation) {
        rotation(rotation, new Vector3f(0.0f, 0.0f, 1.0f));
    }

    /**
     *
     * Rotate the item
     *
     * @param rotation Angle of rotation in degrees
     * @param axis Axis of rotation
     *
     * @since 0.0.1
     */
    public void rotation(double rotation, Vector3f axis) {
        this.rotation = Math.toRadians(rotation);
        this.axis = axis;
    }

    /**
     *
     * Update buffers
     *
     * @since 0.0.1
     */
    public void updateBuffers() {
        buffers.update();
        update = false;
    }

    /**
     *
     * Update model
     *
     * @param vertices new vertices
     * @param indices new indices
     *
     * @since 0.0.1
     */
    void updateModel(Vertex[] vertices, Integer @NotNull [] indices) {
        model.setVertices(vertices);
        model.setIndices(indices);
        update = true;
    }

    /**
     *
     * Update model
     *
     * @param width the width of the new texture
     * @param height the height of the new texture
     * @param pixels the new pixels of the texture
     *
     * @since 0.0.1
     */
    void updateTexture(int width, int height, ByteBuffer pixels) {
        texture.update(TextureType.TEXT, width, height, pixels);
        update = true;
    }

}