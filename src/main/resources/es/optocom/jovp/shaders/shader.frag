#version 450

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec2 uv;
layout(location = 1) in flat ivec4 settings;
layout(location = 2) in flat vec4 rgba0;
layout(location = 3) in flat vec4 rgba1;
layout(location = 4) in flat vec4 contrast;

layout(location = 0) out vec4 color;

vec4 gaussianEnvelope(vec2 uv, vec4 color, vec3 params) {
    float s = sin(params.z);
    float c = cos(params.z);
    mat2 matrix = mat2(c, s, -s,  c);
    vec2 uvp = matrix * (uv - 0.5) + 0.5;
    float xp = pow((uvp.x - 0.5) / params.x, 2);
    float yp = pow((uvp.y - 0.5) / params.y, 2);
    float scale = exp(-xp / 2 - yp / 2);
    return scale * (color - 0.5) + 0.5;
}

vec4 squareEnvelope(vec2 uv, vec4 color, vec3 params) {
    float s = sin(params.z);
    float c = cos(params.z);
    mat2 matrix = mat2(c, s, -s,  c);
    vec2 uvp = matrix * (uv - 0.5) + 0.5;
    float scale = 1;
    if (abs(uvp.x - 0.5) > params.x) scale = 0;
    if (abs(uvp.y - 0.5) > params.y) scale = 0;
    return scale * (color - 0.5) + 0.5;
}

vec4 circleEnvelope(vec2 uv, vec4 color, vec3 params) {
    float s = sin(params.z);
    float c = cos(params.z);
    mat2 matrix = mat2(c, s, -s,  c);
    vec2 uvp = matrix * (uv - 0.5) + 0.5;
    float scale = 1;
    if (pow(uvp.x - 0.5, 2) / pow(params.x, 2) + pow(uvp.y - 0.5, 2) / pow(params.y, 2) > 1) scale = 0;
    return scale * (color - 0.5) + 0.5;
}

void main() {
    color = texture(texSampler, uv);
    // for images or text do nothing
    if (settings.x == 0) color = rgba0; // flat, image, or text color
    if (settings.x == 1) color = rgba0 + color * (rgba1 - rgba0); // contrast
    // Post-processing: envelope?
    //if (envelope.x == 1) color = squareEnvelope(uv, color, envelope.yzw);
    //if (envelope.x == 2) color = circleEnvelope(uv, color, envelope.yzw);
    //if (envelope.x == 3) color = gaussianEnvelope(uv, color, envelope.yzw);
    //color = clamp(contrast * (color - 0.5) + 0.5, 0, 1); // apply contrast and clamp
}