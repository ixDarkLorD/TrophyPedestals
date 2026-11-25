package net.ixdarklord.trophypedestals.compat;

import net.minecraft.world.InteractionResult;

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
