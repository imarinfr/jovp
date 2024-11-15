package es.optocom.jovp.definitions;

import org.joml.*;

/**
 * 
 * Vertex properties of a model
 *
 * @since 0.0.1
 */
public class Vertex {

    /** Vertex position */
    final public Vector3f position;
    /** Vertex uv coordinates */
    final public Vector2f uv;

    /**
     * Create an empty vertex object
     *
     * @since 0.0.1
     */
    public Vertex() {
        position = new Vector3f();
        uv = new Vector2f();
    }

    /**
     * Create a vertex object
     *
     * @param position The positions of the vertex
     * @param uv       The UV map coordinates of the vertex
     *
     * @since 0.0.1
     */
    public Vertex(Vector3f position, Vector2f uv) {
        this.position = position;
        this.uv = uv;
    }

    /**
     * Create a vertex object with calculated UV map positions
     *
     * @param position The positions of the vertex
     *
     * @since 0.0.1
     */
    public Vertex(Vector3f position) {
        this(position, uv(position));
    }

    /** Compute UV map coordinates from vertices */
    private static Vector2f uv(Vector3f positions) {
        return new Vector2f(positions.x() / 2 + 0.5f, -positions.y() / 2 + 0.5f);
    }

}