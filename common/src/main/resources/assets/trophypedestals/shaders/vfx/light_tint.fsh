#version 330
#extension GL_ARB_separate_shader_objects : require

#include <coolcatcanvas:vfx.glsl>

// A pedestal's coloured light on a pedestal's model (PedestalLighting), drawn multiplied: the same soft round pool as
// light_pool, which tints the stone toward the light's colour and keeps its detail, gold trims included.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    float k = clamp(1.0 - length(texCoord - 0.5) * 2.0, 0.0, 1.0);
    float pool = k * k * (3.0 - 2.0 * k);
    float tint = pool * vertexColor.a * vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (tint < 0.002) discard;
    fragColor = vec4(mix(vec3(1.0), vertexColor.rgb, tint), 1.0);
}
