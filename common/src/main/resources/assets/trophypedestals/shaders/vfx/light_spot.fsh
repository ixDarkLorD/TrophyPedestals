#version 330
#extension GL_ARB_separate_shader_objects : require

#include <coolcatcanvas:vfx.glsl>

// A cone of light where it lands (SpotLights): each point of each surface it reaches carries where it is across the cone,
// either way, as a share of the cone's width there, so the spot is round wherever it falls, with a soft edge and a little
// more light at its heart. The colour is the light's, its alpha how strong it is there.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    float r = length(texCoord);
    if (r >= 1.0) discard;
    float spot = (1.0 - smoothstep(0.55, 1.0, r)) * (0.8 + 0.2 * (1.0 - r));
    float light = spot * vertexColor.a * vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (light < 0.003) discard;
    fragColor = vec4(vertexColor.rgb, light);
}
