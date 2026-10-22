package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

public final class NativeMetadataExpressions {
    private NativeMetadataExpressions() {}

    public static void register(Registration registration) {
        ExprPacketEntityEntityId.register(registration);
        ExprPacketEntityId.register(registration);
        ExprPacketEntityOnFireState.register(registration);
        ExprPacketEntityOnFire.register(registration);
        ExprPacketEntitySneakingState.register(registration);
        ExprPacketEntitySneaking.register(registration);
        ExprPacketEntitySprintingState.register(registration);
        ExprPacketEntitySprinting.register(registration);
        ExprPacketEntitySwimmingState.register(registration);
        ExprPacketEntitySwimming.register(registration);
        ExprPacketEntityInvisibleState.register(registration);
        ExprPacketEntityInvisible.register(registration);
        ExprPacketEntityGlowingState.register(registration);
        ExprPacketEntityGlowing.register(registration);
        ExprPacketEntityElytraFlyingState.register(registration);
        ExprPacketEntityElytraFlying.register(registration);
        ExprPacketEntityCustomName.register(registration);
        ExprPacketEntityCustomNameVisibleState.register(registration);
        ExprPacketEntityCustomNameVisible.register(registration);
        ExprPacketEntitySilentState.register(registration);
        ExprPacketEntitySilent.register(registration);
        ExprPacketEntityGravityState.register(registration);
        ExprPacketEntityGravity.register(registration);
        ExprPacketEntityFrozenTime.register(registration);
        ExprPacketEntityFrozenTicks.register(registration);
        ExprPacketEntityItem.register(registration);
        ExprPacketEntityHealth.register(registration);
        ExprPacketEntityHp.register(registration);
        ExprPacketEntityInteractionWidth.register(registration);
        ExprPacketEntityInteractionHeight.register(registration);
        ExprPacketEntityInteractionResponsiveState.register(registration);
        ExprPacketEntityInteractionResponsive.register(registration);
        ExprPacketEntityDisplayInterpolationDelay.register(registration);
        ExprPacketEntityDisplayTransformInterpolationDuration.register(registration);
        ExprPacketEntityDisplayTeleportInterpolationDuration.register(registration);
        ExprPacketEntityDisplayTranslation.register(registration);
        ExprPacketEntityDisplayScale.register(registration);
        ExprPacketEntityDisplayLeftRotation.register(registration);
        ExprPacketEntityDisplayRightRotation.register(registration);
        ExprPacketEntityDisplayBillboardConstraints.register(registration);
        ExprPacketEntityDisplayBillboard.register(registration);
        ExprPacketEntityDisplayBrightnessOverride.register(registration);
        ExprPacketEntityDisplayBrightness.register(registration);
        ExprPacketEntityDisplayViewRange.register(registration);
        ExprPacketEntityDisplayShadowRadius.register(registration);
        ExprPacketEntityDisplayShadowStrength.register(registration);
        ExprPacketEntityDisplayWidth.register(registration);
        ExprPacketEntityDisplayHeight.register(registration);
        ExprPacketEntityDisplayGlowColorOverride.register(registration);
        ExprPacketEntityDisplayGlowColor.register(registration);
        ExprPacketEntityDisplayBlockData.register(registration);
        ExprPacketEntityDisplayBlockState.register(registration);
        ExprPacketEntityDisplayItem.register(registration);
        ExprPacketEntityDisplayItemStack.register(registration);
        ExprPacketEntityDisplayItemTransform.register(registration);
        ExprPacketEntityDisplayText.register(registration);
        ExprPacketEntityDisplayContent.register(registration);
        ExprPacketEntityDisplayLineWidth.register(registration);
        ExprPacketEntityDisplayBackgroundColor.register(registration);
        ExprPacketEntityDisplayTextShadowedState.register(registration);
        ExprPacketEntityDisplayShadowedState.register(registration);
        ExprPacketEntityDisplayShadowed.register(registration);
        ExprPacketEntityDisplaySeeThroughState.register(registration);
        ExprPacketEntityDisplaySeeThrough.register(registration);
        ExprPacketEntityDisplayUseDefaultBackgroundState.register(registration);
        ExprPacketEntityDisplayUseDefaultBackground.register(registration);
        ExprPacketEntityDisplayTextAlignment.register(registration);
        ExprPacketEntityDisplayAlignment.register(registration);
    }
}
