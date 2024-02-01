#version 450

layout(binding = 0) uniform UBO {
    mat4 model;
    mat4 projection;
    vec4 rgba0;
} ubo;

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 uv_out;
layout(location = 1) out flat vec4 rgba0;

void main() {
    gl_Position = ubo.projection * ubo.model * vec4(position, 0.0, 1.0);
    uv_out = uv;
    rgba0 = ubo.rgba0;
}