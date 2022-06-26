package es.optocom.jovp.engine.structures;

import static org.lwjgl.util.shaderc.Shaderc.*;

/**
 *
 * ShaderKind
 *
 * @since 0.0.1
 */
public enum ShaderKind {

    VERTEX_SHADER(shaderc_glsl_vertex_shader),
    FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

    public final int kind;

    ShaderKind(int kind) {
        this.kind = kind;
    }

}
