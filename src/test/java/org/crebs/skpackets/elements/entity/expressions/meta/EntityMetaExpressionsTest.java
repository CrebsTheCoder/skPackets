package org.crebs.skpackets.elements.entity.expressions.meta;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.util.field.BaseFieldRegistry;
import org.crebs.skpackets.elements.entity.expressions.prop.ExprMetaField;
import org.crebs.skpackets.elements.entity.field.meta.MetaFieldRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;
import static org.mockito.MockMakers.INLINE;

class EntityMetaExpressionsTest {
    static Stream<Arguments> expressions() {
        return Stream.of(
                arguments(ExprEntityMetaCustomName.class, "custom name"),
                arguments(ExprEntityMetaCustomNameVisibleState.class, "custom name visible state"),
                arguments(ExprEntityMetaDisplayBackgroundColor.class, "display background color"),
                arguments(ExprEntityMetaDisplayBillboardConstraints.class, "display billboard constraints"),
                arguments(ExprEntityMetaDisplayBlockData.class, "display block data"),
                arguments(ExprEntityMetaDisplayBrightnessOverride.class, "display brightness override"),
                arguments(ExprEntityMetaDisplayGlowColorOverride.class, "display glow color override"),
                arguments(ExprEntityMetaDisplayHeight.class, "display height"),
                arguments(ExprEntityMetaDisplayInterpolationDelay.class, "display interpolation delay"),
                arguments(ExprEntityMetaDisplayItem.class, "display item"),
                arguments(ExprEntityMetaDisplayItemTransform.class, "display item transform"),
                arguments(ExprEntityMetaDisplayLeftRotation.class, "display left rotation"),
                arguments(ExprEntityMetaDisplayLineWidth.class, "display line width"),
                arguments(ExprEntityMetaDisplayRightRotation.class, "display right rotation"),
                arguments(ExprEntityMetaDisplayScale.class, "display scale"),
                arguments(ExprEntityMetaDisplaySeeThroughState.class, "display see through state"),
                arguments(ExprEntityMetaDisplayShadowRadius.class, "display shadow radius"),
                arguments(ExprEntityMetaDisplayShadowStrength.class, "display shadow strength"),
                arguments(ExprEntityMetaDisplayTeleportInterpolationDuration.class, "display teleport interpolation duration"),
                arguments(ExprEntityMetaDisplayText.class, "display text"),
                arguments(ExprEntityMetaDisplayTextAlignment.class, "display text alignment"),
                arguments(ExprEntityMetaDisplayTextShadowedState.class, "display text shadowed state"),
                arguments(ExprEntityMetaDisplayTransformInterpolationDuration.class, "display transform interpolation duration"),
                arguments(ExprEntityMetaDisplayTranslation.class, "display translation"),
                arguments(ExprEntityMetaDisplayUseDefaultBackgroundState.class, "display use default background state"),
                arguments(ExprEntityMetaDisplayViewRange.class, "display view range"),
                arguments(ExprEntityMetaDisplayWidth.class, "display width"),
                arguments(ExprEntityMetaElytraFlyingState.class, "elytra flying state"),
                arguments(ExprEntityMetaEntityId.class, "entity id"),
                arguments(ExprEntityMetaFrozenTime.class, "frozen time"),
                arguments(ExprEntityMetaGlowingState.class, "glowing state"),
                arguments(ExprEntityMetaGravityState.class, "gravity state"),
                arguments(ExprEntityMetaHealth.class, "health"),
                arguments(ExprEntityMetaInteractionHeight.class, "interaction height"),
                arguments(ExprEntityMetaInteractionResponsiveState.class, "interaction responsive state"),
                arguments(ExprEntityMetaInteractionWidth.class, "interaction width"),
                arguments(ExprEntityMetaInvisibleState.class, "invisible state"),
                arguments(ExprEntityMetaItem.class, "item"),
                arguments(ExprEntityMetaOnFireState.class, "on fire state"),
                arguments(ExprEntityMetaPose.class, "pose"),
                arguments(ExprEntityMetaSilentState.class, "silent state"),
                arguments(ExprEntityMetaSneakingState.class, "sneaking state"),
                arguments(ExprEntityMetaSprintingState.class, "sprinting state"),
                arguments(ExprEntityMetaSwimmingState.class, "swimming state")
        );
    }

    @ParameterizedTest(name = "{0}: named field access and change modes")
    @MethodSource("expressions")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void resolvesNamedFieldAndFiltersNullUnsupportedAndIncompatibleValues(
            Class<? extends ExprMetaField> type, String name) throws Exception {
        Field registryField = BaseFieldRegistry.class.getDeclaredField("REGISTRY");
        registryField.setAccessible(true);
        Map registry = (Map) registryField.get(MetaFieldRegistry.INSTANCE);
        Map original = new HashMap(registry);
        registry.clear();
        try (var logging = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            EntityType entityType = mock(EntityType.class);
            EntityMeta meta = mock(EntityMeta.class, withSettings().mockMaker(INLINE));
            EntityMeta unsupported = mock(EntityMeta.class, withSettings().mockMaker(INLINE));
            when(meta.getEntityType()).thenReturn(entityType);
            when(unsupported.getEntityType()).thenReturn(mock(EntityType.class));
            AtomicReference<String> value = new AtomicReference<>("initial");
            boolean writable = !name.equals("entity id");
            MetaFieldRegistry.INSTANCE.builder(entityType, EntityMeta.class)
                    .optionalField(String.class, ignored -> value.get(),
                            writable ? (ignored, changed) -> value.set(changed) : null, name)
                    .build();
            Expression<EntityMeta> source = mock(Expression.class);
            when(source.getArray(null)).thenReturn(new EntityMeta[]{null, unsupported, meta});
            when(source.isSingle()).thenReturn(false);
            ExprMetaField expression = type.getConstructor().newInstance();
            assertTrue(expression.init(new Expression<?>[]{source}, 0, Kleenean.FALSE,
                    new ParseResult("", new Expression<?>[]{source})));
            assertSame(String.class, expression.getReturnType());
            assertFalse(expression.isSingle());
            assertArrayEquals(new String[]{"initial"}, expression.getArray(null));
            assertTrue(expression.toString(null, false).contains(name));
            for (ChangeMode mode : ChangeMode.values()) {
                if (mode == ChangeMode.SET && writable)
                    assertArrayEquals(new Class<?>[]{String.class}, expression.acceptChange(mode));
                else assertNull(expression.acceptChange(mode));
            }
            expression.change(null, new Object[]{"changed"}, ChangeMode.SET);
            assertEquals(writable ? "changed" : "initial", value.get());
            String unchanged = value.get();
            expression.change(null, null, ChangeMode.SET);
            expression.change(null, new Object[0], ChangeMode.SET);
            expression.change(null, new Object[]{"ignored"}, ChangeMode.DELETE);
            expression.change(null, new Object[]{"ignored"}, ChangeMode.RESET);
            expression.change(null, new Object[]{123}, ChangeMode.SET);
            assertEquals(unchanged, value.get());
            when(source.getArray(null)).thenReturn(new EntityMeta[0]);
            assertArrayEquals(new Object[0], expression.getArray(null));
            when(source.isSingle()).thenReturn(true);
            assertTrue(expression.isSingle());
        } finally {
            registry.clear();
            registry.putAll(original);
        }
    }
}

