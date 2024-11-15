package es.optocom.jovp.definitions;

import static org.lwjgl.util.shaderc.Shaderc.*;

/**
 * 
 * ShaderKind
 *
 * @since 0.0.1
 */
public enum ShaderKind {
    /** Vertex shader */
    VERTEX_SHADER(shaderc_glsl_vertex_shader),
    /** Fragment shader */
    FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

    /** Shader kind */
    public final int kind;

    /** Set shader kind */
    ShaderKind(int kind) {
        this.kind = kind;
    }

}
