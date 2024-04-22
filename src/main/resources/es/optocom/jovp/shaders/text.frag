#version 450

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec2 uv;
layout(location = 1) in flat vec4 centers;
layout(location = 2) in flat vec4 coefficients;
layout(location = 3) in flat vec4 rgba;

layout(location = 0) out vec4 color;

void main() {
    color = rgba * texture(texSampler, uv);
}