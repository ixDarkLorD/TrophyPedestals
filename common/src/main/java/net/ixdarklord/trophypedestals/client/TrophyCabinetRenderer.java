package net.ixdarklord.trophypedestals.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlock;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.TrophyDisplayBlock;
import net.ixdarklord.trophypedestals.pedestal.TrophyShelfBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Arrays;

/**
 * Draws the trophies on a trophy cabinet half or a trophy shelf, each as it's drawn on its own pedestal, shrunk to stand
 * on the shelf: the pedestal (its item's block model), and over it what {@link PedestalRenderer} draws for a placed one,
 * its mob or icon, its rank's light, flourishes and crown, and its text frame while it's looked at, all shrunk with it.
 * Also a trim of another block than gold.
 */
public final class TrophyCabinetRenderer implements BlockEntityRenderer<TrophyCabinetBlockEntity, TrophyCabinetRenderer.State> {
    // How large a trophy stands on the shelf, where its foot is (the shelf's top), and how far back from the front: in a
    // cabinet (half a block deep), on its shelf midway between its front and its back; on a wall shelf, on its board
    // (half way up), back against its rim.
    private static final float SCALE = 0.34F, SHELF_TOP = 2.0F / 16.0F, DEPTH = 11.5F / 16.0F;
    private static final float WALL_SHELF_TOP = 9.0F / 16.0F, WALL_SHELF_DEPTH = 15.0F / 16.0F - SCALE / 2.0F - 0.01F;
    // Where each spot is across the half, in the model's own units (it faces north): the viewer's left is east.
    private static final float[] ACROSS = {0.73F, 0.27F};
    // A trophy's text frame: how large, against one on a placed pedestal; how far out from the model's front (over the
    // display's own front, so its frame never hides it) and how far over the trophy's top it floats.
    private static final float TEXT_SIZE = 0.6F, TEXT_DEPTH = 0.3F, TEXT_GAP = 0.2F;

    private final ItemModelResolver items;
    private final PedestalRenderer pedestals;

    public TrophyCabinetRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
        this.pedestals = new PedestalRenderer(context);
    }

    public static final class State extends BlockEntityRenderState {
        // Each trophy's pedestal (its block model), and what's drawn over it as over a placed one.
        final ItemStackRenderState[] trophies = {new ItemStackRenderState(), new ItemStackRenderState()};
        final PedestalRenderer.State[] shows = {new PedestalRenderer.State(), new PedestalRenderer.State()};
        // Where each stands, in the block (its spot turned to the block's facing), and whether its body is drawn here
        // (a custom pedestal's is drawn with what's over it).
        final float[] x = new float[TrophyCabinetBlockEntity.SLOTS], z = new float[TrophyCabinetBlockEntity.SLOTS];
        // Where each one's text frame floats (its bottom middle, in the block), and its lines (drawn here, not with the
        // rest of what's over it).
        final float[] textX = new float[TrophyCabinetBlockEntity.SLOTS], textY = new float[TrophyCabinetBlockEntity.SLOTS];
        final float[] textZ = new float[TrophyCabinetBlockEntity.SLOTS];
        final Component[][] text = {new Component[4], new Component[4]};
        final boolean[] body = new boolean[TrophyCabinetBlockEntity.SLOTS];
        float top;
        final boolean[] shown = new boolean[TrophyCabinetBlockEntity.SLOTS];
        float turn;
        boolean wallShelf;
        // Its trim's material when it isn't gold, and the model whose gold faces it's drawn on.
        @Nullable Identifier trim;
        @Nullable Identifier trimModel;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TrophyCabinetBlockEntity cabinet, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(cabinet, state, breakProgress);
        Direction facing = cabinet.getBlockState().getValue(TrophyDisplayBlock.FACING);
        // From the model's north-facing front to the block's (as its blockstate turns it).
        state.turn = 180.0F - facing.toYRot();
        BlockState block = cabinet.getBlockState();
        state.wallShelf = block.getBlock() instanceof TrophyShelfBlock;
        state.trim = block.getValue(TrophyDisplayBlock.GOLD_TRIM) ? null : cabinet.trim();
        state.trimModel = state.trim == null ? null : trimModel(block);
        state.top = state.wallShelf ? WALL_SHELF_TOP : SHELF_TOP;
        float depth = state.wallShelf ? WALL_SHELF_DEPTH : DEPTH;
        Vector3f spot = new Vector3f();
        for (int i = 0; i < TrophyCabinetBlockEntity.SLOTS; i++) {
            ItemStack trophy = cabinet.item(i);
            PedestalBlockEntity pedestal = cabinet.shownPedestal(i);
            state.shown[i] = pedestal != null;
            if (pedestal == null) continue;
            spot.set(ACROSS[i] - 0.5F, 0.0F, depth - 0.5F).rotateY(state.turn * Mth.DEG_TO_RAD);
            state.x[i] = spot.x + 0.5F;
            state.z[i] = spot.z + 0.5F;
            // Its pedestal: the trophy's item without its trophy, so its plain block model.
            state.body[i] = !pedestal.getBlockState().is(Pedestals.CUSTOM.get());
            ItemStack body = trophy.copy();
            body.remove(Pedestals.RECORD.get());
            body.remove(Pedestals.MOB_TROPHY.get());
            items.updateForTopItem(state.trophies[i], body, ItemDisplayContext.NONE, cabinet.getLevel(), null, (int) cabinet.getBlockPos().asLong() + i);
            // What's over it, as over a placed pedestal: turned with the block, and its text frame shown only while this
            // spot is looked at.
            PedestalRenderer.State show = state.shows[i];
            pedestals.extractRenderState(pedestal, show, partialTicks, cameraPosition, null);
            show.turn += state.turn;
            // Its light and flares (world effects) shrunk into its spot with it.
            show.originX = cabinet.getBlockPos().getX() + state.x[i] - SCALE / 2.0F;
            show.originY = cabinet.getBlockPos().getY() + state.top;
            show.originZ = cabinet.getBlockPos().getZ() + state.z[i] - SCALE / 2.0F;
            show.worldScale = SCALE;
            // (No beam rising out of a cabinet: it would go up through its top, and any cabinet stacked on it.)
            show.beam = state.wallShelf;
            if (pedestal.textMode() == PedestalBlockEntity.TextMode.LOOKING && !lookedAt(cabinet, i)) Arrays.fill(show.lines, null);
            // Its text frame: over its top, out in front of the display, smaller than a placed pedestal's.
            System.arraycopy(show.lines, 0, state.text[i], 0, show.lines.length);
            Arrays.fill(show.lines, null);
            show.textScale = TEXT_SIZE;
            spot.set(ACROSS[i] - 0.5F, 0.0F, TEXT_DEPTH - 0.5F).rotateY(state.turn * Mth.DEG_TO_RAD);
            state.textX[i] = spot.x + 0.5F;
            state.textZ[i] = spot.z + 0.5F;
            state.textY[i] = state.top + show.displayTop * SCALE + TEXT_GAP;
        }
    }

    // Whether the player is looking at this spot of the block.
    private static boolean lookedAt(TrophyCabinetBlockEntity cabinet, int slot) {
        return Minecraft.getInstance().hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(cabinet.getBlockPos()) && TrophyDisplayBlock.slot(cabinet.getBlockState(), cabinet.getBlockPos(), hit.getLocation()) == slot;
    }

    // The model with this block's gold faces: the shelf's, or the cabinet half's for how it's joined.
    private static Identifier trimModel(BlockState block) {
        if (block.getBlock() instanceof TrophyShelfBlock) {
            boolean left = block.getValue(TrophyShelfBlock.LEFT), right = block.getValue(TrophyShelfBlock.RIGHT);
            return TrophyPedestalsMod.id("models/block/trophy_shelf" + (left && right ? "_both" : left ? "_left" : right ? "_right" : "") + ".json");
        }
        boolean up = block.getValue(TrophyCabinetBlock.UP), down = block.getValue(TrophyCabinetBlock.DOWN);
        String join = up && down ? "_middle" : up ? "_up" : down ? "_down" : "";
        return TrophyPedestalsMod.id("models/block/trophy_cabinet_" + block.getValue(TrophyCabinetBlock.PART).getSerializedName() + join + ".json");
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // A trim of another block: the model's gold, in that block's textures, turned as the model is.
        if (state.trim != null && state.trimModel != null) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.turn));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            PedestalBody.submitTrim(state.trimModel, state.trim, poseStack, collector, state.lightCoords);
            poseStack.popPose();
        }
        for (int i = 0; i < TrophyCabinetBlockEntity.SLOTS; i++) {
            if (!state.shown[i]) continue;
            if (state.body[i]) {
                // The pedestal's model is drawn centered on its origin: on its spot, its foot on the shelf.
                poseStack.pushPose();
                poseStack.translate(state.x[i], state.top + SCALE / 2.0F, state.z[i]);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.turn));
                poseStack.scale(SCALE, SCALE, SCALE);
                state.trophies[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
            // What's over it is drawn in its own block's space: shrunk into its spot, but not turned, so its text frame
            // still faces the camera (what it shows was turned with the block instead).
            poseStack.pushPose();
            poseStack.translate(state.x[i] - SCALE / 2.0F, state.top, state.z[i] - SCALE / 2.0F);
            poseStack.scale(SCALE, SCALE, SCALE);
            pedestals.submit(state.shows[i], poseStack, collector, camera);
            poseStack.popPose();
            PedestalRenderer.State show = state.shows[i];
            System.arraycopy(state.text[i], 0, show.lines, 0, show.lines.length);
            PedestalRenderer.submitTextAt(show, poseStack, collector, camera, state.textX[i], state.textY[i], state.textZ[i]);
            Arrays.fill(show.lines, null);
        }
    }
}
