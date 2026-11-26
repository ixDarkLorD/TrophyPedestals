#version 330

#moj_import <coolcatcanvas:vfx.glsl>

// A pedestal's coloured light on a face (PedestalLighting): a soft round pool, brightest at the UVs' middle and gone at
// their rim. Worked out here rather than read from a texture, so it fades smoothly, and without a long faint tail, where
// the screen's colour steps would show as rings.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float k = clamp(1.0 - length(texCoord - 0.5) * 2.0, 0.0, 1.0);
    float pool = k * k * (3.0 - 2.0 * k);
    float light = pool * vertexColor.a * vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (light < 0.004) discard;
    fragColor = vec4(vertexColor.rgb, light);
}
