#version 450

layout(binding = 0) uniform UniformBufferObject {
    ivec4 settings;
    mat4 model; // MVP: Model
    mat4 view; // MVP: View
    mat4 projection; // MVP: Projection
    mat4 optics;
    vec4 rgba0;
    vec4 rgba1;
    vec4 frequency;
    vec3 rotation;
    vec4 contrast;
} ubo;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 uv_out;
layout(location = 1) out flat ivec4 settings;
layout(location = 2) out flat vec4 rgba0;
layout(location = 3) out flat vec4 rgba1;
layout(location = 4) out flat vec4 contrast;

// Matrix to correct Vulkan coordinates
mat4 vulkan_axis = mat4(-1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

// Functions on texture: spatial frequency
vec2 spatial(vec2 uv, vec4 frequency) {
    if (frequency.x <= 0 || frequency.y <= 0) return(uv);
    uv = (frequency.xy + frequency.zw) * uv;
    return(uv);
}
// Functions on texture: rotate
vec2 rotate(vec2 uv, vec3 rotation) {
    if (rotation.x == 0) return(uv);
    float s = sin(rotation.z);
    float c = cos(rotation.z);
    mat2 matrix = mat2(c, s, -s,  c);
    return(matrix * (uv - rotation.x) + rotation.y);
}

void main() {
    // MVP with corrected Vulkan coordinates
    gl_Position = (ubo.projection * vulkan_axis * ubo.view * ubo.model) * vec4(position, 1.0);
    uv_out = rotate(spatial(uv, ubo.frequency), ubo.rotation);
    settings = ubo.settings;
    rgba0 = ubo.rgba0;
    rgba1 = ubo.rgba1;
    contrast = ubo.contrast;
}