#version 450

layout(binding = 0) uniform UBO {
    ivec3 settings;
    mat4 model;
    mat4 view;
    mat4 projection;
    vec4 centers; // lens center and screen center
    vec4 coefficients; // distortion coefficients
    vec4 rgba0;
    vec4 rgba1;
    vec4 frequency;
    vec3 rotation;
    vec4 contrast;
    vec3 envelope;
    vec3 defocus;
} ubo;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 uv_out;
layout(location = 1) out flat ivec3 settings;
layout(location = 2) out flat vec4 centers;
layout(location = 3) out flat vec4 coefficients;
layout(location = 4) out flat vec4 frequency;
layout(location = 5) out flat vec3 rotation;
layout(location = 6) out flat vec4 rgba0;
layout(location = 7) out flat vec4 rgba1;
layout(location = 8) out flat vec4 contrast;
layout(location = 9) out flat vec3 envelope;
layout(location = 10) out flat vec3 defocus;

void main() {
    // apply distortion as necessary
    vec4 position = ubo.projection * ubo.view * ubo.model * vec4(position, 1.0);
    gl_Position = position;
    uv_out = uv;
    settings = ubo.settings;
    centers = ubo.centers;
    coefficients = ubo.coefficients;
    frequency = ubo.frequency;
    rotation = ubo.rotation;
    rgba0 = ubo.rgba0;
    rgba1 = ubo.rgba1;
    contrast = ubo.contrast;
    envelope = ubo.envelope;
    defocus = ubo.defocus;
}