package net.ixdarklord.trophypedestals.compat.render;

/** 1.20.1's stand-in for the type of 26.1's block-breaking overlay, which the renderers here are handed as null. */
public final class ModelFeatureRenderer {
    private ModelFeatureRenderer() {}

    public static final class CrumblingOverlay {
        private CrumblingOverlay() {}

        /** (Never made here: the game draws a block entity's cracks over what it draws itself.) */
        public int progress() {
            return 0;
        }
    }
}
