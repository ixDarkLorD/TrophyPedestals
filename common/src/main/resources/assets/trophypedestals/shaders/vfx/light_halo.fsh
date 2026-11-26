#version 330
#extension GL_ARB_separate_shader_objects : require

#include <coolcatcanvas:vfx.glsl>

// The Prism's halo projected onto the world around it (HaloLight). Each corner of a surface carries where it is as seen
// from the halo's heart: u, how far round the halo, counted in panes; v, how far off the halo's plane, where 1 is the
// band's edge. So the light runs smoothly across every face and over every edge, in the colour of the pane it came
// through, softly blended where two panes are leaded together. The colour's red says which pane glints as a streak of
// light passes (0 to 11, as a fraction of 11) and its green how brightly; its alpha is how strong the light is there.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

const vec3 GLASS[6] = vec3[](vec3(1.000, 0.275, 0.308), vec3(1.000, 0.584, 0.192), vec3(1.000, 0.897, 0.302),
                             vec3(0.410, 1.000, 0.476), vec3(0.287, 0.598, 1.000), vec3(0.672, 0.388, 1.000));
// how strongly each colour is drawn, so they all look about as bright
const float GAIN[6] = float[](1.200, 0.888, 0.705, 0.730, 0.985, 1.086);

void main() {
    float band = 1.0 - smoothstep(0.45, 1.0, abs(texCoord.y));
    if (band <= 0.0) discard;
    float pane = floor(texCoord.x), within = texCoord.x - pane;
    int here = int(mod(pane, 12.0));
    int next = within < 0.5 ? int(mod(pane + 11.0, 12.0)) : int(mod(pane + 1.0, 12.0));
    float edge = min(within, 1.0 - within);
    // the panes' colours run into each other over a short way either side of the lead between them
    float blend = 0.5 - 0.5 * smoothstep(0.0, 0.12, edge);
    vec3 color = mix(GLASS[here % 6], GLASS[next % 6], blend);
    float gain = mix(GAIN[here % 6], GAIN[next % 6], blend);
    float lead = mix(0.6, 1.0, smoothstep(0.0, 0.05, edge));
    float glint = abs(float(here) - floor(vertexColor.r * 11.0 + 0.5)) < 0.5 ? vertexColor.g : 0.0;
    float light = band * lead * gain * (1.0 + 1.2 * glint) * vertexColor.a
            * vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (light < 0.004) discard;
    fragColor = vec4(color, min(light, 1.0));
}
