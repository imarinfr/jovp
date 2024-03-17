#version 450

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec2 uv;
layout(location = 1) in flat ivec3 settings;
layout(location = 2) in flat vec4 rgba0;
layout(location = 3) in flat vec4 rgba1;
layout(location = 4) in flat vec2 uvmax;
layout(location = 5) in flat vec4 contrast;
layout(location = 6) in flat vec3 envelope;
layout(location = 7) in flat vec3 defocus;

layout(location = 0) out vec4 color;

vec4 gaussianEnvelope(vec2 uvp, vec4 color, vec3 params) {
    float s = sin(params.z);
    float c = cos(params.z);
    uvp -= 0.5;
    uvp = vec2(uvp.x * c - uvp.y * s, uvp.x * s + uvp.y * c);
    return exp(-pow(uvp.x / params.x, 2) / 2 - pow(uvp.y / params.y, 2) / 2) * (color - 0.5) + 0.5;
}

vec4 squareEnvelope(vec2 uvp, vec4 color, vec3 params) {
    float s = sin(params.z);
    float c = cos(params.z);
    uvp -= 0.5;
    uvp = vec2(uvp.x * c - uvp.y * s, uvp.x * s + uvp.y * c);
    float scale = 1;
    if (abs(uvp.x) > params.x) scale = 0;
    if (abs(uvp.y) > params.y) scale = 0;
    return scale * (color - 0.5) + 0.5;
}

vec4 circleEnvelope(vec2 uvp, vec4 color, vec3 params) {
    float s = sin(params.z);
    float c = cos(params.z);
    uvp -= 0.5;
    uvp = vec2(uvp.x * c - uvp.y * s, uvp.x * s + uvp.y * c);
    float scale = 1;
    if (pow(uvp.x, 2) / pow(params.x, 2) + pow(uvp.y, 2) / pow(params.y, 2) > 1) scale = 0;
    return scale * (color - 0.5) + 0.5;
}

vec4 blur(vec2 uv, vec4 color, vec3 defocus) {
    return color;
}

void main() {
    color = texture(texSampler, uv);
    // for images do nothing
    if (settings.x == 0) color = rgba0; // flat
    if (settings.x == 1) color = rgba0 + color * (rgba1 - rgba0); // contrast
    // Post-processing: envelope
    if (settings.y == 1) color = squareEnvelope(uv / uvmax, color, envelope);
    if (settings.y == 2) color = circleEnvelope(uv / uvmax, color, envelope);
    if (settings.y == 3) color = gaussianEnvelope(uv / uvmax, color, envelope);
    // Post-processing: defocus
    if (settings.z == 1) color = blur(uv, color, defocus);
    color = clamp(contrast * (color - 0.5) + 0.5, 0, 1); // apply contrast and clamp
}