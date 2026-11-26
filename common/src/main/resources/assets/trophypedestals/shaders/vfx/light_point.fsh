#version 330
#extension GL_ARB_separate_shader_objects : require

#include <coolcatcanvas:vfx.glsl>

// The light a pedestal's effects give off, on the surfaces around them (PointLights): worked out at each point of a
// fine grid over each face and blended between them, so all this draws is the colour it's given at full brightness, its
// alpha how much of that light there is.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    float light = vertexColor.a * vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (light < 0.003) discard;
    fragColor = vec4(vertexColor.rgb, light);
}
