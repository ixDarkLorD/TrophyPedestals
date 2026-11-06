package net.ixdarklord.trophypedestals.compat;

import net.minecraft.util.Mth;

/**
 * 1.21.1's stand-in for 26.1's {@code ARGB}: the packed-color helpers Trophy Pedestals uses, by the same names, so its
 * drawing code is the same on every version. Colors are 0xAARRGGBB.
 */
public final class ARGB {
    private ARGB() {}

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    /** This alpha (0 to 255, clamped) with the color's red, green and blue. */
    public static int color(int alpha, int rgb) {
        return Mth.clamp(alpha, 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    public static int colorFromFloat(float alpha, float red, float green, float blue) {
        return color(channel(alpha), channel(red), channel(green), channel(blue));
    }

    /** Red, green and blue each times {@code scale} (clamped), alpha kept. */
    public static int scaleRGB(int color, float scale) {
        return color(alpha(color), clamp(red(color) * scale), clamp(green(color) * scale), clamp(blue(color) * scale));
    }

    /** Each channel {@code t} of the way from {@code from} to {@code to} (as stored, without converting to linear light). */
    public static int srgbLerp(float t, int from, int to) {
        return color(Mth.lerpInt(t, alpha(from), alpha(to)), Mth.lerpInt(t, red(from), red(to)),
                Mth.lerpInt(t, green(from), green(to)), Mth.lerpInt(t, blue(from), blue(to)));
    }

    private static int channel(float value) {
        return Mth.floor(Mth.clamp(value, 0.0F, 1.0F) * 255.0F);
    }

    private static int clamp(float value) {
        return Mth.clamp((int) value, 0, 255);
    }
}
