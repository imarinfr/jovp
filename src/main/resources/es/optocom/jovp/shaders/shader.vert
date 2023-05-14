#version 450

layout(binding = 0) uniform UniformBufferObject {
    ivec3 settings;
    mat4 model;
    mat4 projectionView;
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
layout(location = 2) out flat vec4 rgba0;
layout(location = 3) out flat vec4 rgba1;
layout(location = 4) out flat vec4 contrast;
layout(location = 5) out flat vec3 envelope;
layout(location = 6) out flat vec3 defocus;

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
    // apply distortion as necessary
    vec2 pos = position.xy;
    //vec2 theta = (pos - ubo.centers.xy);
    //float rSq = dot(theta, theta);
    //// Brown-Conrady model
    //float distortion = 1.0 +
    //                   ubo.coefficients.x * rSq +
    //                   ubo.coefficients.y * rSq * rSq +
    //                   ubo.coefficients.z * rSq * rSq * rSq +
    //                   ubo.coefficients.w * rSq * rSq * rSq * rSq;
    //vec2 distortedTheta = theta * distortion;
    // Apply the distortion effect to the vertex position
    //pos = ubo.centers.xy + distortedTheta;
    gl_Position = ubo.projectionView * ubo.model * vec4(pos, position.z, 1.0);
    // Calculate the texture coordinates for the distorted vertex
    vec2 tc = (pos - ubo.centers.wz) * vec2(1.0, -1.0) + ubo.centers.xy;
    uv_out = vec2(tc.x, tc.y);
    uv_out = rotate(spatial(uv, ubo.frequency), ubo.rotation);
    settings = ubo.settings;
    rgba0 = ubo.rgba0;
    rgba1 = ubo.rgba1;
    contrast = ubo.contrast;
    envelope = ubo.envelope;
    defocus = ubo.defocus;
}