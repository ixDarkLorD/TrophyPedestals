package net.ixdarklord.trophypedestals.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlock;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.TrophyDisplayBlock;
import net.ixdarklord.trophypedestals.pedestal.TrophySets;
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
import net.minecraft.core.BlockPos;
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
 * Draws the trophies on a trophy cabinet half or a trophy shelf, each as it's drawn on its own pedestal, shrunk to
 * stand on the shelf: the pedestal (its item's block model), and over it what {@link PedestalRenderer} draws for a
 * placed one, its mob or icon, its rank's light, flourishes and crown, and its text frame while it's looked at, all
 * shrunk with it.
 */
public final class TrophyCabinetRenderer implements BlockEntityRenderer<TrophyCabinetBlockEntity, TrophyCabinetRenderer.State> {
    private static final float SCALE = 0.34F, SHELF_TOP = 2.0F / 16.0F, DEPTH = 11.5F / 16.0F;
    private static final float WALL_SHELF_TOP = 9.0F / 16.0F, WALL_SHELF_DEPTH = 15.0F / 16.0F - SCALE / 2.0F - 0.01F;
    private static final float[] ACROSS = {0.73F, 0.27F};
    private static final float TEXT_SIZE = 0.6F, TEXT_DEPTH = 0.3F, TEXT_GAP = 0.2F;
    private static final float SET_Y = 1.08F;
    private static final float ITEM_SCALE = 0.8F;

    private final ItemModelResolver items;
    private final PedestalRenderer pedestals;

    public TrophyCabinetRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
        this.pedestals = new PedestalRenderer(context);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] trophies = {new ItemStackRenderState(), new ItemStackRenderState()};
        final PedestalRenderer.State[] shows = {new PedestalRenderer.State(), new PedestalRenderer.State()};
        final float[] x = new float[TrophyCabinetBlockEntity.SLOTS], z = new float[TrophyCabinetBlockEntity.SLOTS];
        final float[] textX = new float[TrophyCabinetBlockEntity.SLOTS], textY = new float[TrophyCabinetBlockEntity.SLOTS];
        final float[] textZ = new float[TrophyCabinetBlockEntity.SLOTS];
        final Component[][] text = {new Component[4], new Component[4]};
        final boolean[] body = new boolean[TrophyCabinetBlockEntity.SLOTS];
        final boolean[] plain = new boolean[TrophyCabinetBlockEntity.SLOTS];
        final float[] itemY = new float[TrophyCabinetBlockEntity.SLOTS];
        float top;
        final boolean[] shown = new boolean[TrophyCabinetBlockEntity.SLOTS];
        float turn;
        boolean wallShelf;
        final PedestalRenderer.State set = new PedestalRenderer.State();
        boolean showsSet;
        float setX, setY, setZ;
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
        state.turn = 180.0F - facing.toYRot();
        BlockState block = cabinet.getBlockState();
        state.wallShelf = block.getBlock() instanceof TrophyShelfBlock;
        state.trim = block.getValue(TrophyDisplayBlock.GOLD_TRIM) ? null : cabinet.trim();
        state.trimModel = state.trim == null ? null : trimModel(block);
        state.top = state.wallShelf ? WALL_SHELF_TOP : SHELF_TOP;
        float depth = state.wallShelf ? WALL_SHELF_DEPTH : DEPTH;
        Vector3f spot = new Vector3f();
        extractSet(cabinet, block, state);
        for (int i = 0; i < TrophyCabinetBlockEntity.SLOTS; i++) {
            ItemStack trophy = cabinet.item(i);
            state.plain[i] = !trophy.isEmpty() && !Pedestals.isTrophy(trophy);
            if (state.plain[i]) {
                state.shown[i] = true;
                spot.set(ACROSS[i] - 0.5F, 0.0F, depth - 0.5F).rotateY(state.turn * Mth.DEG_TO_RAD);
                state.x[i] = spot.x + 0.5F;
                state.z[i] = spot.z + 0.5F;
                items.updateForTopItem(state.trophies[i], trophy, ItemDisplayContext.GROUND, cabinet.getLevel(), null, (int) cabinet.getBlockPos().asLong() + i);
                state.itemY[i] = state.top - (float) state.trophies[i].getModelBoundingBox().minY * ITEM_SCALE;
                continue;
            }
            PedestalBlockEntity pedestal = cabinet.shownPedestal(i);
            state.shown[i] = pedestal != null;
            if (pedestal == null) continue;
            spot.set(ACROSS[i] - 0.5F, 0.0F, depth - 0.5F).rotateY(state.turn * Mth.DEG_TO_RAD);
            state.x[i] = spot.x + 0.5F;
            state.z[i] = spot.z + 0.5F;
            state.body[i] = !pedestal.getBlockState().is(Pedestals.CUSTOM.get());
            ItemStack body = Pedestals.isTrophy(trophy) ? trophy.copy() : new ItemStack(Pedestals.DEFAULT.get());
            body.remove(Pedestals.RECORD.get());
            body.remove(Pedestals.MOB_TROPHY.get());
            items.updateForTopItem(state.trophies[i], body, ItemDisplayContext.NONE, cabinet.getLevel(), null, (int) cabinet.getBlockPos().asLong() + i);
            PedestalRenderer.State show = state.shows[i];
            pedestals.extractRenderState(pedestal, show, partialTicks, cameraPosition, null);
            show.turn += state.turn;
            show.originX = cabinet.getBlockPos().getX() + state.x[i] - SCALE / 2.0F;
            show.originY = cabinet.getBlockPos().getY() + state.top;
            show.originZ = cabinet.getBlockPos().getZ() + state.z[i] - SCALE / 2.0F;
            show.worldScale = SCALE;
            show.beam = state.wallShelf;
            if (pedestal.textMode() == PedestalBlockEntity.TextMode.LOOKING && !lookedAt(cabinet, i)) Arrays.fill(show.lines, null);
            System.arraycopy(show.lines, 0, state.text[i], 0, show.lines.length);
            Arrays.fill(show.lines, null);
            show.textScale = TEXT_SIZE;
            spot.set(ACROSS[i] - 0.5F, 0.0F, TEXT_DEPTH - 0.5F).rotateY(state.turn * Mth.DEG_TO_RAD);
            state.textX[i] = spot.x + 0.5F;
            state.textZ[i] = spot.z + 0.5F;
            state.textY[i] = state.top + show.displayTop * SCALE + TEXT_GAP;
        }
    }

    private static void extractSet(TrophyCabinetBlockEntity cabinet, BlockState block, State state) {
        state.showsSet = false;
        if (!(block.getBlock() instanceof TrophyCabinetBlock) || block.getValue(TrophyCabinetBlock.PART) != TrophyCabinetBlock.Part.LEFT
                || cabinet.getLevel() == null) return;
        TrophyCabinetBlockEntity.SetName shown = cabinet.setName();
        BlockPos other = TrophyCabinetBlock.partner(cabinet.getBlockPos(), block);
        if (shown == TrophyCabinetBlockEntity.SetName.HIDDEN) return;
        if (shown == TrophyCabinetBlockEntity.SetName.ON_LOOK && !(Minecraft.getInstance().hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && (hit.getBlockPos().equals(cabinet.getBlockPos()) || hit.getBlockPos().equals(other)))) return;
        Component set = TrophySets.completedName(cabinet.getLevel(), cabinet);
        if (set == null) return;
        state.showsSet = true;
        Arrays.fill(state.set.lines, null);
        state.set.lines[1] = set.copy().withColor(0xFFC24A);
        state.set.rarity = PedestalRarities.LEGENDARY;
        state.set.textScale = TEXT_SIZE;
        Vector3f front = new Vector3f(0.0F, 0.0F, TEXT_DEPTH - 0.5F).rotateY(state.turn * Mth.DEG_TO_RAD);
        state.setX = (other.getX() - cabinet.getBlockPos().getX()) / 2.0F + 0.5F + front.x;
        state.setZ = (other.getZ() - cabinet.getBlockPos().getZ()) / 2.0F + 0.5F + front.z;
        state.setY = SET_Y;
    }

    private static boolean lookedAt(TrophyCabinetBlockEntity cabinet, int slot) {
        return Minecraft.getInstance().hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(cabinet.getBlockPos()) && TrophyDisplayBlock.spotAt(cabinet.getBlockState(), cabinet.getBlockPos(), hit) == slot;
    }

    private static Identifier trimModel(BlockState block) {
        if (block.getBlock() instanceof TrophyShelfBlock) {
            boolean left = block.getValue(TrophyShelfBlock.LEFT), right = block.getValue(TrophyShelfBlock.RIGHT);
            return TrophyPedestalsMod.id("models/block/trophy_shelf" + (left && right ? "_both" : left ? "_left" : right ? "_right" : "") + ".json");
        }
        boolean up = block.getValue(TrophyCabinetBlock.UP), down = block.getValue(TrophyCabinetBlock.DOWN);
        String join = up && down ? "_middle" : up ? "_up" : down ? "_down" : "";
        return TrophyPedestalsMod.id("models/block/trophy_cabinet_" + block.getValue(TrophyCabinetBlock.PART).getSerializedName() + join + ".json");
    }

    private static final float TEXT_FORWARD = 0.4F;

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.showsSet) PedestalRenderer.submitTextAt(state.set, poseStack, collector, camera, state.setX, state.setY, state.setZ);
        if (state.trim != null && state.trimModel != null) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.rotate(Axis.YP.rotationDegrees(state.turn));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            PedestalBody.submitTrim(state.trimModel, state.trim, poseStack, collector, state.lightCoords);
            poseStack.popPose();
        }
        for (int i = 0; i < TrophyCabinetBlockEntity.SLOTS; i++) {
            if (!state.shown[i]) continue;
            if (state.plain[i]) {
                poseStack.pushPose();
                poseStack.translate(state.x[i], state.itemY[i], state.z[i]);
                poseStack.rotate(Axis.YP.rotationDegrees(state.turn));
                poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                state.trophies[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
                continue;
            }
            if (state.body[i]) {
                poseStack.pushPose();
                poseStack.translate(state.x[i], state.top + SCALE / 2.0F, state.z[i]);
                poseStack.rotate(Axis.YP.rotationDegrees(state.turn));
                poseStack.scale(SCALE, SCALE, SCALE);
                state.trophies[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
            poseStack.pushPose();
            poseStack.translate(state.x[i] - SCALE / 2.0F, state.top, state.z[i] - SCALE / 2.0F);
            poseStack.scale(SCALE, SCALE, SCALE);
            pedestals.submit(state.shows[i], poseStack, collector, camera);
            poseStack.popPose();
            PedestalRenderer.State show = state.shows[i];
            System.arraycopy(state.text[i], 0, show.lines, 0, show.lines.length);
            PedestalRenderer.submitTextAt(show, poseStack, collector, camera, state.textX[i], state.textY[i], state.textZ[i], -TEXT_FORWARD);
            Arrays.fill(show.lines, null);
        }
    }
}
