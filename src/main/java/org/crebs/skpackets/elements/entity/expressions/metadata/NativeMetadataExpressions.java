package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

public final class NativeMetadataExpressions {
    private NativeMetadataExpressions() {}

    public static void register(Registration registration) {
        ExprPacketEntityEntityId.register(registration);
        ExprPacketEntityOnFireState.register(registration);
        ExprPacketEntitySneakingState.register(registration);
        ExprPacketEntitySprintingState.register(registration);
        ExprPacketEntitySwimmingState.register(registration);
        ExprPacketEntityInvisibleState.register(registration);
        ExprPacketEntityGlowingState.register(registration);
        ExprPacketEntityElytraFlyingState.register(registration);
        ExprPacketEntityCustomName.register(registration);
        ExprPacketEntityCustomNameVisibleState.register(registration);
        ExprPacketEntitySilentState.register(registration);
        ExprPacketEntityGravityState.register(registration);
        ExprPacketEntityFrozenTime.register(registration);
        ExprPacketEntityItem.register(registration);
        ExprPacketEntityPose.register(registration);
        ExprPacketEntityHealth.register(registration);
        ExprPacketEntityInteractionWidth.register(registration);
        ExprPacketEntityInteractionHeight.register(registration);
        ExprPacketEntityInteractionResponsiveState.register(registration);
        ExprPacketEntityDisplayInterpolationDelay.register(registration);
        ExprPacketEntityDisplayTransformInterpolationDuration.register(registration);
        ExprPacketEntityDisplayTeleportInterpolationDuration.register(registration);
        ExprPacketEntityDisplayTranslation.register(registration);
        ExprPacketEntityDisplayScale.register(registration);
        ExprPacketEntityDisplayLeftRotation.register(registration);
        ExprPacketEntityDisplayRightRotation.register(registration);
        ExprPacketEntityDisplayBillboardConstraints.register(registration);
        ExprPacketEntityDisplayBrightnessOverride.register(registration);
        ExprPacketEntityDisplayViewRange.register(registration);
        ExprPacketEntityDisplayShadowRadius.register(registration);
        ExprPacketEntityDisplayShadowStrength.register(registration);
        ExprPacketEntityDisplayWidth.register(registration);
        ExprPacketEntityDisplayHeight.register(registration);
        ExprPacketEntityDisplayGlowColorOverride.register(registration);
        ExprPacketEntityDisplayBlockData.register(registration);
        ExprPacketEntityDisplayItem.register(registration);
        ExprPacketEntityDisplayItemTransform.register(registration);
        ExprPacketEntityDisplayText.register(registration);
        ExprPacketEntityDisplayLineWidth.register(registration);
        ExprPacketEntityDisplayBackgroundColor.register(registration);
        ExprPacketEntityDisplayTextShadowedState.register(registration);
        ExprPacketEntityDisplaySeeThroughState.register(registration);
        ExprPacketEntityDisplayUseDefaultBackgroundState.register(registration);
        ExprPacketEntityDisplayTextAlignment.register(registration);
    }
}
