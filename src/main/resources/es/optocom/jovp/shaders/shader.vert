#version 450

layout(binding = 0) uniform UniformBufferObject {
    ivec4 settings;
    mat4 transform;
    mat4 proj;
    mat4 view;
    mat4 optics;
    vec4 frequency;
    vec4 rotation;
    vec4 rgba0;
    vec4 rgba1;
    vec4 contrast;
    vec4 envelope;
} ubo;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 uv_out;
layout(location = 1) out flat ivec4 settings;
layout(location = 2) out flat vec4 frequency;
layout(location = 3) out flat vec4 rotation;
layout(location = 4) out flat vec4 rgba0;
layout(location = 5) out flat vec4 rgba1;
layout(location = 6) out flat vec4 envelope;
layout(location = 7) out flat vec4 contrast;

void main() {
    //gl_Position = ubo.proj * ubo.view * ubo.optics * ubo.transform * vec4(position, 1.0);
    gl_Position = ubo.transform * vec4(position, 1.0);
    uv_out = uv;
    settings = ubo.settings;
    frequency = ubo.frequency;
    rotation = ubo.rotation;
    rgba0 = ubo.rgba0;
    rgba1 = ubo.rgba1;
    envelope = ubo.envelope;
    contrast = ubo.contrast;
}