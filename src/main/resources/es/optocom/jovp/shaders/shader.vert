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
layout(location = 2) out flat vec4 rgba0;
layout(location = 3) out flat vec4 rgba1;
layout(location = 4) out flat vec2 uvmax;
layout(location = 5) out flat vec4 contrast;
layout(location = 6) out flat vec3 envelope;
layout(location = 7) out flat vec3 defocus;


// Functions on texture: spatial frequency
vec2 spatial(vec2 uv, vec4 frequency) {
    return(frequency.xy + frequency.zw * uv);
}

// Functions on texture: rotate
vec2 rotate(vec2 uv, vec3 rotation) {
    if (rotation.z == 0) return(uv);
    float s = sin(rotation.z);
    float c = cos(rotation.z);
    uv -= rotation.xy;
    uv = vec2(uv.x * c - uv.y * s, uv.x * s + uv.y * c);
    uv += rotation.xy;
    return uv;
}

// Brown-Conrady distortion model
vec4 distortion(vec4 position) {
    vec2 theta = (position.xy - ubo.centers.xy);
    float rSq = dot(theta, theta);
    float distortion = 1.0 +
                       ubo.coefficients.x * rSq +
                       ubo.coefficients.y * rSq * rSq +
                       ubo.coefficients.z * rSq * rSq * rSq +
                       ubo.coefficients.w * rSq * rSq * rSq * rSq;
    vec2 distortedTheta = theta * distortion;
    // Apply the distortion effect to the vertex position
    position.xy = ubo.centers.xy + distortedTheta;
    return position;
}

void main() {
    // apply distortion as necessary
    vec4 position = ubo.projection * ubo.view * ubo.model * vec4(position, 1.0);
    //gl_Position = distortion(position);
    gl_Position = position;
    // Calculate the texture coordinates for the distorted vertex
    //vec2 tc = (gl_Position.xy - ubo.centers.wz) * vec2(1.0, -1.0) + ubo.centers.xy;
    //uv_out = vec2(tc.x, tc.y);
    uv_out = rotate(spatial(uv, ubo.frequency), ubo.rotation);
    settings = ubo.settings;
    rgba0 = ubo.rgba0;
    rgba1 = ubo.rgba1;
    uvmax = ubo.frequency.xy + ubo.frequency.wz;
    contrast = ubo.contrast;
    envelope = ubo.envelope;
    defocus = ubo.defocus;
}