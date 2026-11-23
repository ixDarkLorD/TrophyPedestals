#version 150

#moj_import <coolcatcanvas:vfx.glsl>

// A sheen of light on a pedestal's displayed item. It is drawn on a quad through the item's middle and kept only where the
// scene's nearest surface lies close to that quad, which is the item itself, so the light follows the item's shape and
// leaves the air and the world behind it alone.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

const float VIEW_HEIGHT = 1.4;      // how many blocks the screen's height covers per block of distance, near the usual field of view
const float ITEM_DEPTH = 0.57;      // how far from the quad a surface still counts as the item, in quad heights, so it grows with the item
const float ITEM_SOFT = 0.24;       // the last share of that, over which the light fades

void main() {
    vec4 texel = texture(Sampler0, texCoord);
    float alpha = texel.a * vertexColor.a;
    if (alpha < 0.002) discard;
    float scene = vfx_linear_depth(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r);
    float here = vfx_linear_depth(gl_FragCoord.z);
    float perPixel = here * VIEW_HEIGHT / float(textureSize(DepthSampler, 0).y);
    // its flipbook is a column of square frames, so this many of them make a frame's height one whole quad
    vec2 sheet = vec2(textureSize(Sampler0, 0));
    float quadHeight = perPixel / max(length(vec2(dFdx(texCoord.y), dFdy(texCoord.y))) * sheet.y / sheet.x, 0.00001);
    float depth = max(0.12, quadHeight * ITEM_DEPTH);
    alpha *= 1.0 - smoothstep(depth * (1.0 - ITEM_SOFT), depth, abs(scene - here));
    if (alpha < 0.002) discard;
    fragColor = vec4(texel.rgb * vertexColor.rgb, alpha);
}
