package net.ixdarklord.trophypedestals.client;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mobs made only to be drawn (on a pedestal, in a slot, on a recipe page) never join a level, so nothing numbers them.
 * Newer versions refuse to draw a mob without a number, so each gets one of its own here: below zero, where the
 * server's numbers never reach, so it can't be taken for a real one.
 */
public final class DrawnMobs {
    private static final AtomicInteger NEXT = new AtomicInteger(-1_000_000);

    private DrawnMobs() {}

    /** Numbers a mob made only to be drawn. */
    public static <T extends Entity> T numbered(T mob) {
        mob.setId(NEXT.decrementAndGet());
        return mob;
    }
}
