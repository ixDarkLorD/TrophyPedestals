package net.ixdarklord.trophypedestals.compat;

import net.minecraft.world.InteractionResult;

/**
 * 1.20.1's stand-in for 1.20.5+'s {@code ItemInteractionResult}: what using an item on a block did, where passing on
 * means the block is then used as with an empty hand (1.20.1 has one {@code use} for both: see the blocks' own).
 */
public enum ItemInteractionResult {
    SUCCESS(InteractionResult.SUCCESS),
    CONSUME(InteractionResult.CONSUME),
    CONSUME_PARTIAL(InteractionResult.CONSUME_PARTIAL),
    PASS_TO_DEFAULT_BLOCK_INTERACTION(InteractionResult.PASS),
    SKIP_DEFAULT_BLOCK_INTERACTION(InteractionResult.PASS),
    FAIL(InteractionResult.FAIL);

    private final InteractionResult result;

    ItemInteractionResult(InteractionResult result) {
        this.result = result;
    }

    public InteractionResult result() {
        return result;
    }
}
