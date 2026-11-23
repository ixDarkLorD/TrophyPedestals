#version 330

#moj_import <coolcatcanvas:vfx.glsl>

// The inside of the Ender lens's rift and glass: layers of stars that hold still on the screen rather than the surface, as
// in an end portal, so moving past it slides the surface over a sky that seems to lie far behind it. The stars are sized in
// blocks at the rift's distance, so they shrink as it does and melt into a faint shimmer far away. The texture gives the
// shape: its black, solid pixels are the opening, and the rest (the rift's bright edge) is drawn as it is.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

const int LAYERS = 10;
const float STAR_PIXELS = 5.0;      // a star is drawn on a grid this many pixels from its middle to its tip
const float VIEW_HEIGHT = 1.4;      // how many blocks the screen's height covers per block of distance, near the usual field of view

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 layerColor(int layer) {
    int kind = layer % 4;
    if (kind == 0) return vec3(0.16, 0.74, 0.63);
    if (kind == 1) return vec3(0.52, 0.28, 0.96);
    if (kind == 2) return vec3(0.22, 0.40, 0.92);
    return vec3(0.88, 0.72, 1.00);
}

// A star at o (-1 to 1 across it). x: its white-hot core; y: the tinted light around it.
vec2 starShape(vec2 o, float kind, float twinkle) {
    vec2 a = abs(o);
    float r = length(o);
    if (kind < 0.4) {                                                  // a four-pointed sparkle, its rays reaching as it twinkles
        float reach = 0.55 + 0.45 * twinkle;
        float rays = max(0.0, 1.0 - a.x / 0.16) * max(0.0, 1.0 - a.y / reach)
                   + max(0.0, 1.0 - a.y / 0.16) * max(0.0, 1.0 - a.x / reach);
        return vec2(1.0 - smoothstep(0.08, 0.24, r), rays + 0.28 * max(0.0, 1.0 - r / 0.55));
    }
    if (kind < 0.75) {                                                 // an eight-pointed star, with short diagonal rays
        vec2 d = abs(vec2(o.x + o.y, o.x - o.y)) * 0.7071;
        float rays = max(0.0, 1.0 - a.x / 0.13) * max(0.0, 1.0 - a.y)
                   + max(0.0, 1.0 - a.y / 0.13) * max(0.0, 1.0 - a.x)
                   + 0.65 * (max(0.0, 1.0 - d.x / 0.13) * max(0.0, 1.0 - d.y / 0.62)
                           + max(0.0, 1.0 - d.y / 0.13) * max(0.0, 1.0 - d.x / 0.62));
        return vec2(1.0 - smoothstep(0.10, 0.26, r), rays);
    }
    float ring = max(0.0, 1.0 - abs(r - 0.62) / 0.13);                 // a small round world, ringed with light
    float body = 1.0 - smoothstep(0.26, 0.40, r);
    return vec2(body * (0.45 + 0.55 * max(0.0, 1.0 - length(o + vec2(0.11)) / 0.28)), 0.55 * body + 0.7 * ring * (0.6 + 0.4 * twinkle));
}

vec3 endSky(vec2 screen, float blocksAway, float time) {
    vec2 world = screen * blocksAway * VIEW_HEIGHT;
    vec3 color = vec3(0.016, 0.010, 0.042);
    color += vec3(0.050, 0.018, 0.105) * (0.5 + 0.5 * sin(world.x * 3.1 + world.y * 2.3 + time * 0.09));
    color += vec3(0.020, 0.045, 0.070) * (0.5 + 0.5 * sin(world.x * -1.9 + world.y * 4.2 - time * 0.07));
    for (int layer = 0; layer < LAYERS; layer++) {
        float f = float(layer);
        float angle = f * 1.73 + 0.4;
        mat2 turn = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
        vec2 p = turn * screen * (blocksAway + f * 0.35) * VIEW_HEIGHT * (4.5 + f * 1.1);    // the deeper the layer, the smaller its stars
        p.y += time * (0.05 + 0.028 * f);
        float foot = max(fwidth(p.x), fwidth(p.y));                    // how much of a cell one screen pixel covers
        vec2 cell = floor(p);
        float seed = hash(cell + f * 17.3);
        if (seed < 0.62) continue;
        vec2 place = 0.35 + 0.3 * vec2(hash(cell + f + 3.1), hash(cell - f + 7.7));
        float radius = 0.16 + 0.14 * hash(cell + 11.9);
        float reach = max(radius, foot);                               // past a pixel across, spread the star's light over that pixel
        vec2 o = (fract(p) - place) / reach;
        if (max(abs(o.x), abs(o.y)) >= 1.0) continue;
        if (foot * STAR_PIXELS < radius) o = (floor(o * STAR_PIXELS) + 0.5) / STAR_PIXELS;   // close up, crisp pixels
        float twinkle = 0.5 + 0.5 * sin(time * (1.0 + seed * 2.0) + seed * 40.0);
        vec2 star = starShape(o, hash(cell + f * 5.7), twinkle);
        float share = radius / reach;
        float light = (0.6 + 0.4 * twinkle) * (1.15 - f / 12.0) * share * share;
        color += (layerColor(layer) * star.y + mix(layerColor(layer), vec3(1.0), 0.8) * star.x * 1.2) * light;
    }
    return color;
}

void main() {
    vec4 texel = texture(Sampler0, texCoord);
    float alpha = texel.a * vertexColor.a * vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (alpha < 0.002) discard;
    vec3 color = texel.rgb * vertexColor.rgb;
    float bright = max(texel.r, max(texel.g, texel.b));
    float opening = (1.0 - smoothstep(0.02, 0.10, bright)) * step(0.9, texel.a);
    if (opening > 0.0) {
        vec2 size = vec2(textureSize(DepthSampler, 0));
        vec2 screen = (gl_FragCoord.xy - 0.5 * size) / size.y;
        color = mix(color, endSky(screen, vfx_linear_depth(gl_FragCoord.z), mod(VfxTime.x, 3600.0)), opening);
    }
    fragColor = vec4(color, alpha);
}
