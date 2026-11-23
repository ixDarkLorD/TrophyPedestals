package net.ixdarklord.trophypedestals.client;

import org.jetbrains.annotations.Nullable;

/**
 * A humanoid's render state that can carry a mannequin's pose, as degrees about x, y and z for its head, body, arms and
 * legs in {@link net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity#STAND_PARTS}' order. Null leaves the model's
 * own animation alone.
 */
public interface PosedFigure {
    float @Nullable [] trophypedestals$pose();

    void trophypedestals$setPose(float @Nullable [] pose);
}
