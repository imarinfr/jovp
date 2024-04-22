package es.optocom.jovp.rendering;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Optotype;
import es.optocom.jovp.definitions.Vertex;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.PI;
import static java.util.Objects.requireNonNull;
import static org.joml.Math.cos;
import static org.joml.Math.sin;
import static org.lwjgl.assimp.Assimp.*;

/**
 * Model class and methods
 *
 * @since 0.0.1
 */
public class Model {

    private static final int VERTICES_CIRCLE = 500; // For circles and annulus: MUST BE EVEN!!!
    private static final String OPTOTYPE_OBJECT = "/es/optocom/jovp/models/Sloan.obj"; // Optotypes object file
    private static final float DEFAULT_HOLLOW_RATIO = 0.5f;
    private static final Optotype DEFAULT_OPTOTYPE = Optotype.E;

    ModelType type;
    Vertex[] vertices;
    Integer[] indices;

    /**
     * Generates an empty model
     *
     * @since 0.0.1
     */
    Model() {
        type = ModelType.TEXT;
        vertices = new Vertex[] { new Vertex() };
        indices = new Integer[] { 0 };
    }

    /**
     * Generates a solid cross, Maltese cross, circle, triangle or square
     *
     * @param type The type of model
     *
     * @since 0.0.1
     */
    public Model(ModelType type) {
        this.type = type;
        switch (type) {
            case TRIANGLE -> triangle();
            case SQUARE -> square();
            case CROSS -> cross();
            case MALTESE -> maltese();
            case CIRCLE -> circle();
            case ANNULUS -> annulus(DEFAULT_HOLLOW_RATIO);
            case OPTOTYPE -> optotype(DEFAULT_OPTOTYPE);
            default -> throw new IllegalStateException("Invalid model type: " + type);
        }
    }

    /**
     * Generates a solid polygon with more than 4 vertices (pentagon, hexagon, etc)
     *
     * @param numberOfVertices The number of vertices in the polygon
     *
     * @since 0.0.1
     */
    public Model(int numberOfVertices) {
        if (numberOfVertices < 5)
            throw new RuntimeException("Use TRIANGLE or SQUARE for less than 5 vertices");
        type = ModelType.POLYGON;
        polygon(numberOfVertices);
    }

    /**
     * Generates a hollow triangle, square, or an annulus
     *
     * @param type  The type of hollow model
     * @param ratio The size ratio between the outer and inner polygon
     *
     * @since 0.0.1
     */
    public Model(ModelType type, float ratio) {
        if (ratio < 0.0f || ratio > 1.0f)
            throw new RuntimeException("Ratio must be between 0 and 1");
        this.type = type;
        switch (type) {
            case HOLLOW_TRIANGLE -> hollowTriangle(ratio);
            case HOLLOW_SQUARE -> hollowSquare(ratio);
            case ANNULUS -> annulus(ratio);
            default -> throw new IllegalStateException("Invalid model type: " + type);
        }
    }

    /**
     * Generates a hollow polygon
     *
     * @param numberOfVertices The number of vertices in the polygon
     * @param ratio            The size ratio between the outer and inner polygon
     *
     * @since 0.0.1
     */
    public Model(int numberOfVertices, float ratio) {
        if (numberOfVertices < 5)
            throw new RuntimeException("Use TRIANGLE or SQUARE for less than 5 vertices");
        if (ratio < 0.0f || ratio > 1.0f)
            throw new RuntimeException("Ratio must be between 0 and 1");
        type = ModelType.HOLLOW_POLYGON;
        hollowPolygon(numberOfVertices, ratio);
    }

    /**
     * Generates an Optotype
     *
     * @param optotype Optotype character
     *
     * @since 0.0.1
     */
    public Model(Optotype optotype) {
        type = ModelType.OPTOTYPE;
        optotype(optotype);
    }

    /**
     * Load model from a file with default flags
     *
     * @param fileName The Model's filename
     *
     * @since 0.0.1
     */
    public Model(String fileName) {
        this(fileName, aiProcess_FlipUVs | aiProcess_DropNormals);
    }

    /**
     * Load model from a file with default flags
     *
     * @param fileName The Model's filename
     * @param flags    The aiImportFile flags
     *
     * @since 0.0.1
     */
    public Model(String fileName, int flags) {
        type = ModelType.MODEL;
        loadModel(fileName, flags);
    }

    /**
     * Clean up after use
     *
     * @since 0.0.1
     */
    public void destroy() {
        type = null;
        vertices = null;
        indices = null;
    }

    /**
     * Get model type
     *
     * @return model type
     *
     * @since 0.0.1
     */
    public ModelType getType() {
        return type;
    }

    /**
     * Set vertices
     *
     * @param vertices new model vertices
     *
     * @since 0.0.1
     */
    void setVertices(Vertex[] vertices) {
        this.vertices = vertices;
    }

    /**
     * Set indices
     *
     * @param indices new model indices
     *
     * @since 0.0.1
     */
    void setIndices(Integer[] indices) {
        this.indices = indices;
    }

    /** loads a model from a file with specific flags */
    private void loadModel(String fileName, int flags) {
        URL resource = this.getClass().getResource("/es/optocom/jovp/models/" + fileName);
        if (resource != null) {
            try {
                fileName = String.valueOf(Paths.get(new URI(resource.toExternalForm())));
            } catch (URISyntaxException e) {
                throw new RuntimeException("Could load model.", e);
            }
        }
        try (AIScene scene = aiImportFile(fileName, flags)) {
            if (scene == null || scene.mRootNode() == null)
                throw new RuntimeException("Could not load model: " + aiGetErrorString());
            LoadedModel model = new LoadedModel();
            processNode(requireNonNull(scene.mRootNode()), scene, model);
            final int vertexCount = model.vertices.size();
            vertices = new Vertex[vertexCount];
            for (int i = 0; i < vertexCount; i++)
                vertices[i] = new Vertex(model.vertices.get(i), model.uv.get(i));
            indices = new Integer[model.indices.size()];
            for (int i = 0; i < indices.length; i++)
                indices[i] = model.indices.get(i);
        }
    }

    /** process node */
    private static void processNode(AINode node, AIScene scene, LoadedModel model) {
        if (node.mMeshes() != null)
            processNodeMeshes(scene, node, model);
        if (node.mChildren() != null) {
            PointerBuffer children = node.mChildren();
            for (int i = 0; i < node.mNumChildren(); i++) {
                if (children == null)
                    throw new RuntimeException("Failed to process node");
                processNode(AINode.create(children.get(i)), scene, model);
            }
        }
    }

    /** process node meshes */
    private static void processNodeMeshes(AIScene scene, AINode node, LoadedModel model) {
        PointerBuffer pMeshes = scene.mMeshes();
        IntBuffer meshIndices = node.mMeshes();
        for (int i = 0; i < requireNonNull(meshIndices).capacity(); i++) {
            if (pMeshes == null)
                throw new RuntimeException("Failed to process node meshes");
            AIMesh mesh = AIMesh.create(pMeshes.get(meshIndices.get(i)));
            processMesh(mesh, model);
        }
    }

    /** process mesh */
    private static void processMesh(AIMesh mesh, LoadedModel model) {
        processPositions(mesh, model.vertices);
        processUV(mesh, model.uv);
        processIndices(mesh, model.indices);
    }

    /** process texture coordinates */
    private static void processPositions(AIMesh mesh, List<Vector3f> positions) {
        AIVector3D.Buffer vertices = requireNonNull(mesh.mVertices());
        for (int i = 0; i < vertices.capacity(); i++) {
            AIVector3D position = vertices.get(i);
            positions.add(new Vector3f(position.x(), position.y(), position.z()));
        }
    }

    /** process texture coordinates */
    private static void processUV(AIMesh mesh, List<Vector2f> uv) {
        AIVector3D.Buffer aiCoordinates = requireNonNull(mesh.mTextureCoords(0));
        for (int i = 0; i < aiCoordinates.capacity(); i++) {
            final AIVector3D coordinates = aiCoordinates.get(i);
            uv.add(new Vector2f(coordinates.x(), coordinates.y()));
        }
    }

    /** process indices */
    private static void processIndices(AIMesh mesh, List<Integer> indices) {
        AIFace.Buffer aiFaces = mesh.mFaces();
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = aiFaces.get(i);
            IntBuffer pIndices = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++) {
                indices.add(pIndices.get(j));
            }
        }
    }

    /** create vertices and indices for a cross */
    private void cross() {
        vertices = new Vertex[8];
        vertices[0] = new Vertex(new Vector3f(-1.0f, 0.1f, 0.0f));
        vertices[1] = new Vertex(new Vector3f(1.0f, 0.1f, 0.0f));
        vertices[2] = new Vertex(new Vector3f(-1.0f, -0.1f, 0.0f));
        vertices[3] = new Vertex(new Vector3f(1.0f, -0.1f, 0.0f));
        vertices[4] = new Vertex(new Vector3f(-0.1f, 1.0f, 0.0f));
        vertices[5] = new Vertex(new Vector3f(0.1f, 1.0f, 0.0f));
        vertices[6] = new Vertex(new Vector3f(-0.1f, -1.0f, 0.0f));
        vertices[7] = new Vertex(new Vector3f(0.1f, -1.0f, 0.0f));
        indices = new Integer[] {
                0, 1, 2, 2, 1, 3,
                3, 1, 2, 2, 1, 0,
                4, 5, 6, 6, 5, 7,
                7, 5, 6, 6, 5, 4
        };
    }

    /** create vertices and indices for a Maltese cross */
    private void maltese() {
        vertices = new Vertex[12];
        vertices[0] = new Vertex(new Vector3f(-1.00f, 0.20f, 0.00f));
        vertices[1] = new Vertex(new Vector3f(0.02f, 0.00f, 0.00f));
        vertices[2] = new Vertex(new Vector3f(-1.00f, -0.20f, 0.00f));
        vertices[3] = new Vertex(new Vector3f(1.00f, 0.20f, 0.00f));
        vertices[4] = new Vertex(new Vector3f(1.00f, -0.20f, 0.00f));
        vertices[5] = new Vertex(new Vector3f(-0.02f, 0.00f, 0.00f));
        vertices[6] = new Vertex(new Vector3f(-0.20f, 1.00f, 0.00f));
        vertices[7] = new Vertex(new Vector3f(0.20f, 1.00f, 0.00f));
        vertices[8] = new Vertex(new Vector3f(0.00f, -0.02f, 0.00f));
        vertices[9] = new Vertex(new Vector3f(-0.20f, -1.00f, 0.00f));
        vertices[10] = new Vertex(new Vector3f(0.00f, 0.02f, 0.00f));
        vertices[11] = new Vertex(new Vector3f(0.20f, -1.00f, 0.00f));
        indices = new Integer[] {
                0, 1, 2, 3, 4, 5,
                5, 4, 3, 2, 1, 0,
                6, 7, 8, 9, 10, 11,
                11, 10, 9, 8, 7, 6
        };
    }

    /** create vertices and indices for a triangle */
    private void triangle() {
        vertices = new Vertex[3];
        vertices[0] = new Vertex(new Vector3f(-1.0f, -1.0f, 0.0f));
        vertices[1] = new Vertex(new Vector3f(1.0f, -1.0f, 0.0f));
        vertices[2] = new Vertex(new Vector3f(0.0f, 1.0f, 0.0f));
        indices = new Integer[] { 0, 1, 2 };
        indices = expandIndices(indices);
    }

    /** create vertices and indices for a square */
    private void square() {
        vertices = new Vertex[4];
        vertices[0] = new Vertex(new Vector3f(-1.0f, -1.0f, 0.0f));
        vertices[1] = new Vertex(new Vector3f(1.0f, -1.0f, 0.0f));
        vertices[2] = new Vertex(new Vector3f(1.0f, 1.0f, 0.0f));
        vertices[3] = new Vertex(new Vector3f(-1.0f, 1.0f, 0.0f));
        indices = new Integer[] { 0, 1, 2, 2, 3, 0 };
        indices = expandIndices(indices);
    }

    /** create vertices and indices for a circle */
    private void circle() {
        polygon(VERTICES_CIRCLE);
    }

    /** create vertices and indices for a hollow triangle */
    private void hollowTriangle(float ratio) {
        vertices = new Vertex[6];
        vertices[0] = new Vertex(new Vector3f(-ratio, -ratio, 0.0f));
        vertices[1] = new Vertex(new Vector3f(ratio, -ratio, 0.0f));
        vertices[2] = new Vertex(new Vector3f(0.0f, ratio, 0.0f));
        vertices[3] = new Vertex(new Vector3f(-1.0f, -1.0f, 0.0f));
        vertices[4] = new Vertex(new Vector3f(1.0f, -1.0f, 0.0f));
        vertices[5] = new Vertex(new Vector3f(0.0f, 1.0f, 0.0f));
        indices = new Integer[] {
                0, 3, 4,
                0, 4, 1,
                1, 4, 5,
                1, 5, 2,
                2, 5, 3,
                2, 3, 0
        };
        indices = expandIndices(indices);
    }

    /** create vertices and indices for a hollow square */
    private void hollowSquare(float ratio) {
        vertices = new Vertex[8];
        vertices[0] = new Vertex(new Vector3f(-ratio, -ratio, 0.0f));
        vertices[1] = new Vertex(new Vector3f(ratio, -ratio, 0.0f));
        vertices[2] = new Vertex(new Vector3f(ratio, ratio, 0.0f));
        vertices[3] = new Vertex(new Vector3f(-ratio, ratio, 0.0f));
        vertices[4] = new Vertex(new Vector3f(-1.0f, -1.0f, 0.0f));
        vertices[5] = new Vertex(new Vector3f(1.0f, -1.0f, 0.0f));
        vertices[6] = new Vertex(new Vector3f(1.0f, 1.0f, 0.0f));
        vertices[7] = new Vertex(new Vector3f(-1.0f, 1.0f, 0.0f));
        indices = new Integer[] {
                0, 4, 5,
                0, 5, 1,
                1, 5, 6,
                1, 6, 2,
                2, 6, 7,
                2, 7, 3,
                3, 7, 4,
                3, 4, 0
        };
        indices = expandIndices(indices);
    }

    /** create vertices and indices for an annulus */
    private void annulus(float ratio) {
        hollowPolygon(VERTICES_CIRCLE, ratio);
    }

    /** create vertices and indices for a polygon */
    private void polygon(int numberOfVertices) {
        vertices = new Vertex[numberOfVertices + 1];
        vertices[0] = new Vertex(new Vector3f(0.0f, 0.0f, 0.0f));
        indices = new Integer[3 * (numberOfVertices + 1)];
        for (int i = 0; i < numberOfVertices; i++) {
            int k = 3 * i;
            float theta = (float) (2.0f * PI * i / numberOfVertices);
            vertices[i + 1] = new Vertex(new Vector3f(cos(theta), sin(theta), 0.00f));
            indices[k] = 0;
            indices[k + 1] = i + 1;
            indices[k + 2] = i + 2;
        }
        indices[3 * numberOfVertices] = 0;
        indices[3 * numberOfVertices + 1] = numberOfVertices;
        indices[3 * numberOfVertices + 2] = 1;
        indices = expandIndices(indices);
    }

    /** create vertices and indices for a hollow polygon */
    private void hollowPolygon(int numberOfVertices, float ratio) {
        vertices = new Vertex[2 * numberOfVertices];
        indices = new Integer[2 * 3 * numberOfVertices];
        for (int i = 0; i < numberOfVertices; i++) {
            int kv = 2 * i;
            int ki = 6 * i;
            float theta = (float) (2.0f * PI * i / numberOfVertices);
            vertices[kv] = new Vertex(new Vector3f(ratio * cos(theta), ratio * sin(theta), 0.00f));
            vertices[kv + 1] = new Vertex(new Vector3f(cos(theta), sin(theta), 0.00f));
            indices[ki] = kv;
            indices[ki + 1] = kv + 1;
            indices[ki + 2] = kv + 3;
            indices[ki + 3] = kv + 3;
            indices[ki + 4] = kv + 2;
            indices[ki + 5] = kv;
        }
        indices[2 * 3 * (numberOfVertices - 1) + 2] = 1;
        indices[2 * 3 * (numberOfVertices - 1) + 3] = 1;
        indices[2 * 3 * (numberOfVertices - 1) + 4] = 0;
        indices[2 * 3 * (numberOfVertices - 1) + 5] = 2 * numberOfVertices - 2;
        indices = expandIndices(indices);
    }

    /** create vertices and indices for an optotype */
    private void optotype(Optotype optotype) {
        try (InputStream in = this.getClass().getResourceAsStream(OPTOTYPE_OBJECT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            ArrayList<Vertex> vertexArrayList = new ArrayList<>();
            ArrayList<Integer> indicesArrayList = new ArrayList<>();
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break; // if end of line, then exit
                if (line.length() == 0 || line.charAt(0) != 'g' ||
                        !line.substring(2, 3).equals(optotype.toString()))
                    continue; // if not our optotype, do nothing
                while (true) {
                    line = reader.readLine();
                    if (line.length() == 0)
                        break; // empty line indicates we are done
                    switch (line.charAt(0)) {
                        case 'v' -> { // process vertices
                            String[] stringArray = line.substring(2).split(" ");
                            vertexArrayList.add(new Vertex(new Vector3f(Float.parseFloat(stringArray[0]),
                                    Float.parseFloat(stringArray[1]), Float.parseFloat(stringArray[2]))));
                        }
                        case 'f' -> { // process indices
                            String[] stringArray = line.substring(2).split(" ");
                            for (String s : stringArray)
                                indicesArrayList.add(Integer.parseInt(s) - 1);
                        }
                    }
                }
            }
            vertices = vertexArrayList.toArray(new Vertex[0]);
            indices = indicesArrayList.toArray(new Integer[0]);
        } catch (IOException e) {
            throw new RuntimeException("Could not load Optotype", e);
        }
        indices = expandIndices(indices);
    }

    /** expand indices for the back sides of the 2D model */
    private Integer[] expandIndices(Integer[] in) {
        Integer[] out = new Integer[2 * in.length];
        System.arraycopy(in, 0, out, 0, in.length);
        for (int i = in.length; i < out.length; i++) {
            out[i] = in[out.length - 1 - i];
        }
        return out;
    }

    /** load model from file for processing */
    static class LoadedModel {

        final List<Vector3f> vertices;
        final List<Vector2f> uv;
        final List<Integer> indices;

        /** get vertices, indices and uv map for a load model from file */
        LoadedModel() {
            this.vertices = new ArrayList<>();
            this.uv = new ArrayList<>();
            this.indices = new ArrayList<>();
        }

    }

}
