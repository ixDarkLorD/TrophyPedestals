package net.ixdarklord.trophypedestals.mixin.client;

import net.ixdarklord.trophypedestals.client.PosedFigure;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns a player-model mannequin's parts to its pose, for its body and the armor drawn over it alike. On 1.21.1 the pose
 * is on the figure itself. The player model copies its sleeves, jacket and trousers after this, and the armor layer
 * copies these parts, but the hat was copied before: it's copied again here.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart hat;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftLeg;
    @Shadow @Final public ModelPart rightLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void trophypedestals$applyPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                           CallbackInfo callback) {
        if (!(entity instanceof PosedFigure figure)) return;
        float[] pose = figure.trophypedestals$pose();
        if (pose == null) return;
        trophypedestals$turn(head, pose, 0);
        hat.copyFrom(head);
        trophypedestals$turn(body, pose, 1);
        trophypedestals$turn(leftArm, pose, 2);
        trophypedestals$turn(rightArm, pose, 3);
        trophypedestals$turn(leftLeg, pose, 4);
        trophypedestals$turn(rightLeg, pose, 5);
    }

    @Unique
    private static void trophypedestals$turn(ModelPart part, float[] pose, int index) {
        part.xRot = pose[index * 3] * Mth.DEG_TO_RAD;
        part.yRot = pose[index * 3 + 1] * Mth.DEG_TO_RAD;
        part.zRot = pose[index * 3 + 2] * Mth.DEG_TO_RAD;
    }
}
