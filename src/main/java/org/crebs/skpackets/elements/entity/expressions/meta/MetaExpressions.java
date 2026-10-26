/*
 * MIT License
 *
 * Copyright (c) 2026 3add
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.crebs.skpackets.elements.entity.expressions.meta;

import com.github.shanebeee.skr.Registration;

public final class MetaExpressions {
    private MetaExpressions() {}

    public static void register(Registration registration) {
        ExprEntityMetaCustomName.register(registration);
        ExprEntityMetaCustomNameVisibleState.register(registration);
        ExprEntityMetaDisplayBackgroundColor.register(registration);
        ExprEntityMetaDisplayBillboardConstraints.register(registration);
        ExprEntityMetaDisplayBlockData.register(registration);
        ExprEntityMetaDisplayBrightnessOverride.register(registration);
        ExprEntityMetaDisplayGlowColorOverride.register(registration);
        ExprEntityMetaDisplayHeight.register(registration);
        ExprEntityMetaDisplayInterpolationDelay.register(registration);
        ExprEntityMetaDisplayItem.register(registration);
        ExprEntityMetaDisplayItemTransform.register(registration);
        ExprEntityMetaDisplayLeftRotation.register(registration);
        ExprEntityMetaDisplayLineWidth.register(registration);
        ExprEntityMetaDisplayRightRotation.register(registration);
        ExprEntityMetaDisplayScale.register(registration);
        ExprEntityMetaDisplaySeeThroughState.register(registration);
        ExprEntityMetaDisplayShadowRadius.register(registration);
        ExprEntityMetaDisplayShadowStrength.register(registration);
        ExprEntityMetaDisplayTeleportInterpolationDuration.register(registration);
        ExprEntityMetaDisplayTextAlignment.register(registration);
        ExprEntityMetaDisplayText.register(registration);
        ExprEntityMetaDisplayTextShadowedState.register(registration);
        ExprEntityMetaDisplayTransformInterpolationDuration.register(registration);
        ExprEntityMetaDisplayTranslation.register(registration);
        ExprEntityMetaDisplayUseDefaultBackgroundState.register(registration);
        ExprEntityMetaDisplayViewRange.register(registration);
        ExprEntityMetaDisplayWidth.register(registration);
        ExprEntityMetaElytraFlyingState.register(registration);
        ExprEntityMetaEntityId.register(registration);
        ExprEntityMetaFrozenTime.register(registration);
        ExprEntityMetaGlowingState.register(registration);
        ExprEntityMetaGravityState.register(registration);
        ExprEntityMetaHealth.register(registration);
        ExprEntityMetaInteractionHeight.register(registration);
        ExprEntityMetaInteractionResponsiveState.register(registration);
        ExprEntityMetaInteractionWidth.register(registration);
        ExprEntityMetaInvisibleState.register(registration);
        ExprEntityMetaItem.register(registration);
        ExprEntityMetaOnFireState.register(registration);
        ExprEntityMetaPose.register(registration);
        ExprEntityMetaSilentState.register(registration);
        ExprEntityMetaSneakingState.register(registration);
        ExprEntityMetaSprintingState.register(registration);
        ExprEntityMetaSwimmingState.register(registration);
    }
}
