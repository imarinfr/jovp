#version 450

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec2 uv;
layout(location = 1) in flat ivec3 settings;
layout(location = 2) in flat vec4 centers;
layout(location = 3) in flat vec4 coefficients;
layout(location = 4) in flat vec4 frequency;
layout(location = 5) in flat vec3 rotation;
layout(location = 6) in flat vec4 rgba0;
layout(location = 7) in flat vec4 rgba1;
layout(location = 8) in flat vec4 contrast;
layout(location = 9) in flat vec3 envelope;
layout(location = 10) in flat vec3 defocus;

layout(location = 0) out vec4 color;

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

vec4 gaussianEnvelope(vec2 uv, vec4 color, vec3 envelope) {
    float s = sin(envelope.z);
    float c = cos(envelope.z);
    uv = 2 * uv - 1;
    uv = vec2(uv.x * c - uv.y * s, uv.x * s + uv.y * c);
    return exp(-(pow(uv.x / envelope.x, 2) + pow(uv.y / envelope.y, 2)) / 2) * (color - 0.5) + 0.5;
}

vec4 squareEnvelope(vec2 uv, vec4 color, vec3 envelope) {
    float s = sin(envelope.z);
    float c = cos(envelope.z);
    uv = 2 * uv - 1;
    uv = vec2(uv.x * c - uv.y * s, uv.x * s + uv.y * c);
    float scale = 1;
    if (abs(uv.x) > envelope.x) scale = 0;
    if (abs(uv.y) > envelope.y) scale = 0;
    return scale * color;
}

vec4 circleEnvelope(vec2 uv, vec4 color, vec3 envelope) {
    float s = sin(envelope.z);
    float c = cos(envelope.z);
    uv = 2 * uv - 1;
    uv = vec2(uv.x * c - uv.y * s, uv.x * s + uv.y * c);
    float scale = 1;
    if (pow(uv.x, 2) / pow(envelope.x, 2) + pow(uv.y, 2) / pow(envelope.y, 2) > 1) scale = 0;
    return scale * color;
}

vec4 blur(vec2 uv, vec4 color, vec3 defocus) {
    return color;
}

void main() {
    vec2 uvmax = frequency.xy + frequency.zw;
    vec2 uvsp = rotate(spatial(uv, frequency), rotation);
    color = texture(texSampler, uvsp);
    // for images do nothing
    if (settings.x == 0) color = rgba0; // flat
    if (settings.x == 1) color = rgba0 + color * (rgba1 - rgba0); // contrast
    // Post-processing: envelope
    if (settings.y == 1) color = squareEnvelope(uvsp / uvmax, color, envelope);
    if (settings.y == 2) color = circleEnvelope(uvsp / uvmax, color, envelope);
    if (settings.y == 3) color = gaussianEnvelope(uvsp / uvmax, color, envelope);
    // Post-processing: defocus
    if (settings.z == 1) color = blur(uvsp, color, defocus);
    color = clamp(contrast * (color - 0.5) + 0.5, 0, 1); // apply contrast and clamp
    //if(uvsp.x < 0) color = vec4(1, 0, 0, 1);
    //if(uvsp.x / uvmax.x > 1) color = vec4(1, 0, 0, 1);
    //if(uvsp.y < 0) color = vec4(1, 0, 0, 1);
    //if(uvsp.y / uvmax.y > 1) color = vec4(1, 0, 0, 1);
}