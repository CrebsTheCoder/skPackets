package org.crebs.skpackets.elements.entity.expressions.metadata;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;
import static org.mockito.MockMakers.INLINE;

class PacketEntityMetadataExpressionsTest {
    static Stream<Arguments> expressions() {
        org.crebs.skpackets.support.PacketEventsTestBootstrap.initialize();
        return Stream.of(
                arguments(ExprPacketEntityCustomName.class, PacketEntityField.CUSTOM_NAME),
                arguments(ExprPacketEntityCustomNameVisibleState.class, PacketEntityField.CUSTOM_NAME_VISIBLE),
                arguments(ExprPacketEntityDisplayBackgroundColor.class, PacketEntityField.BACKGROUND_COLOR),
                arguments(ExprPacketEntityDisplayBillboardConstraints.class, PacketEntityField.BILLBOARD),
                arguments(ExprPacketEntityDisplayBlockData.class, PacketEntityField.BLOCK_DATA),
                arguments(ExprPacketEntityDisplayBrightnessOverride.class, PacketEntityField.BRIGHTNESS),
                arguments(ExprPacketEntityDisplayGlowColorOverride.class, PacketEntityField.GLOW_COLOR),
                arguments(ExprPacketEntityDisplayHeight.class, PacketEntityField.DISPLAY_HEIGHT),
                arguments(ExprPacketEntityDisplayInterpolationDelay.class, PacketEntityField.INTERPOLATION_DELAY),
                arguments(ExprPacketEntityDisplayItem.class, PacketEntityField.DISPLAY_ITEM),
                arguments(ExprPacketEntityDisplayItemTransform.class, PacketEntityField.ITEM_TRANSFORM),
                arguments(ExprPacketEntityDisplayLeftRotation.class, PacketEntityField.LEFT_ROTATION),
                arguments(ExprPacketEntityDisplayLineWidth.class, PacketEntityField.LINE_WIDTH),
                arguments(ExprPacketEntityDisplayRightRotation.class, PacketEntityField.RIGHT_ROTATION),
                arguments(ExprPacketEntityDisplayScale.class, PacketEntityField.SCALE),
                arguments(ExprPacketEntityDisplaySeeThroughState.class, PacketEntityField.SEE_THROUGH),
                arguments(ExprPacketEntityDisplayShadowRadius.class, PacketEntityField.SHADOW_RADIUS),
                arguments(ExprPacketEntityDisplayShadowStrength.class, PacketEntityField.SHADOW_STRENGTH),
                arguments(ExprPacketEntityDisplayTeleportInterpolationDuration.class, PacketEntityField.TELEPORT_DURATION),
                arguments(ExprPacketEntityDisplayText.class, PacketEntityField.TEXT),
                arguments(ExprPacketEntityDisplayTextAlignment.class, PacketEntityField.ALIGNMENT),
                arguments(ExprPacketEntityDisplayTextShadowedState.class, PacketEntityField.TEXT_SHADOW),
                arguments(ExprPacketEntityDisplayTransformInterpolationDuration.class, PacketEntityField.TRANSFORM_DURATION),
                arguments(ExprPacketEntityDisplayTranslation.class, PacketEntityField.TRANSLATION),
                arguments(ExprPacketEntityDisplayUseDefaultBackgroundState.class, PacketEntityField.DEFAULT_BACKGROUND),
                arguments(ExprPacketEntityDisplayViewRange.class, PacketEntityField.VIEW_RANGE),
                arguments(ExprPacketEntityDisplayWidth.class, PacketEntityField.DISPLAY_WIDTH),
                arguments(ExprPacketEntityElytraFlyingState.class, PacketEntityField.ELYTRA_FLYING),
                arguments(ExprPacketEntityEntityId.class, PacketEntityField.ENTITY_ID),
                arguments(ExprPacketEntityFrozenTime.class, PacketEntityField.FROZEN_TIME),
                arguments(ExprPacketEntityGlowingState.class, PacketEntityField.GLOWING),
                arguments(ExprPacketEntityGravityState.class, PacketEntityField.GRAVITY),
                arguments(ExprPacketEntityHealth.class, PacketEntityField.HEALTH),
                arguments(ExprPacketEntityInteractionHeight.class, PacketEntityField.INTERACTION_HEIGHT),
                arguments(ExprPacketEntityInteractionResponsiveState.class, PacketEntityField.INTERACTION_RESPONSIVE),
                arguments(ExprPacketEntityInteractionWidth.class, PacketEntityField.INTERACTION_WIDTH),
                arguments(ExprPacketEntityInvisibleState.class, PacketEntityField.INVISIBLE),
                arguments(ExprPacketEntityItem.class, PacketEntityField.ITEM),
                arguments(ExprPacketEntityOnFireState.class, PacketEntityField.ON_FIRE),
                arguments(ExprPacketEntityPose.class, PacketEntityField.POSE),
                arguments(ExprPacketEntitySilentState.class, PacketEntityField.SILENT),
                arguments(ExprPacketEntitySneakingState.class, PacketEntityField.SNEAKING),
                arguments(ExprPacketEntitySprintingState.class, PacketEntityField.SPRINTING),
                arguments(ExprPacketEntitySwimmingState.class, PacketEntityField.SWIMMING)
        );
    }

    @ParameterizedTest(name = "{0}: field and change contract")
    @MethodSource("expressions")
    void exposesCorrectFieldAndChangeContract(Class<? extends ExprPacketEntityField> type,
            PacketEntityField expected) throws Exception {
        ExprPacketEntityField expression = type.getConstructor().newInstance();
        Field field = ExprPacketEntityField.class.getDeclaredField("field");
        field.setAccessible(true);
        assertSame(expected, field.get(expression));
        Class<?> returnType = expected.valueType() == org.bukkit.inventory.ItemStack.class
                ? ch.njol.skript.aliases.ItemType.class : expected.valueType();
        assertSame(returnType, expression.getReturnType());
        for (ChangeMode mode : ChangeMode.values()) {
            boolean accepted = expected.isWritable()
                    && (mode == ChangeMode.SET || mode == ChangeMode.DELETE || mode == ChangeMode.RESET);
            if (accepted) assertArrayEquals(new Class<?>[]{returnType}, expression.acceptChange(mode));
            else assertNull(expression.acceptChange(mode), mode.name());
        }
    }

    @ParameterizedTest(name = "{0}: dispatch")
    @MethodSource("expressions")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void dispatchesReadsWritesAndResets(Class<? extends ExprPacketEntityField> type,
            PacketEntityField expected) throws Exception {
        ExprPacketEntityField expression = type.getConstructor().newInstance();
        PacketEntityField field = mock(PacketEntityField.class, withSettings().mockMaker(INLINE));
        when(field.valueType()).thenReturn((Class) String.class);
        Field dependency = ExprPacketEntityField.class.getDeclaredField("field");
        dependency.setAccessible(true);
        dependency.set(expression, field);
        Expression<WrapperEntity> source = mock(Expression.class);
        WrapperEntity first = mock(WrapperEntity.class);
        WrapperEntity second = mock(WrapperEntity.class);
        when(source.getArray(null)).thenReturn(new WrapperEntity[]{first, second});
        when(source.isSingle()).thenReturn(false);
        when(field.get(first)).thenReturn("first");
        when(field.get(second)).thenReturn("second");
        assertTrue(expression.init(new Expression<?>[]{source}, 0, Kleenean.FALSE,
                new ParseResult("", new Expression<?>[]{source})));
        assertFalse(expression.isSingle());
        assertArrayEquals(new String[]{"first", "second"}, expression.getArray(null));
        expression.change(null, new Object[]{"updated", "ignored"}, ChangeMode.SET);
        verify(field).set(first, "updated");
        verify(field).set(second, "updated");
        expression.change(null, null, ChangeMode.RESET);
        expression.change(null, null, ChangeMode.DELETE);
        verify(field, times(2)).reset(first);
        verify(field, times(2)).reset(second);
        clearInvocations(field);
        expression.change(null, null, ChangeMode.SET);
        expression.change(null, new Object[0], ChangeMode.SET);
        expression.change(null, new Object[]{"ignored"}, ChangeMode.ADD);
        verifyNoInteractions(field);
        when(source.getArray(null)).thenReturn(new WrapperEntity[0]);
        assertArrayEquals(new String[0], expression.getArray(null));
        when(source.isSingle()).thenReturn(true);
        assertTrue(expression.isSingle());
    }
}

