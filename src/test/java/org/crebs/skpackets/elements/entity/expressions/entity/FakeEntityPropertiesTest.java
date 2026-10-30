package org.crebs.skpackets.elements.entity.expressions.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.crebs.skpackets.api.util.field.BaseFieldRegistry;
import org.crebs.skpackets.api.util.field.InheritingFieldRegistry;
import org.crebs.skpackets.elements.entity.expressions.prop.ExprFakeEntityField;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;
import static org.mockito.MockMakers.INLINE;

class FakeEntityPropertiesTest {
    static Stream<Arguments> properties() {
        return Stream.of(
                arguments(ExprFakeEntityEntityId.class, "entity id", false),
                arguments(ExprFakeEntityEntityLocation.class, "entity location", true),
                arguments(ExprFakeEntityEntityType.class, "entity type", false),
                arguments(ExprFakeEntityEntityUuid.class, "entity uuid", false),
                arguments(ExprFakeEntityEntityViewers.class, "entity viewers", true),
                arguments(ExprFakeEntityPlayerSkin.class, "player skin", true),
                arguments(ExprFakeEntityPlayerUsername.class, "player username", false));
    }

    @ParameterizedTest(name = "{0}: named field read and write dispatch")
    @MethodSource("properties")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void dispatchesItsOwnFieldAndRespectsSupportedChangeModes(
            Class<? extends ExprFakeEntityField> type, String name, boolean writable) throws Exception {
        var registryField = BaseFieldRegistry.class.getDeclaredField("REGISTRY");
        registryField.setAccessible(true);
        Map registry = (Map) registryField.get(FakeEntityFieldRegistry.INSTANCE);
        Map original = new HashMap(registry);
        var classesField = InheritingFieldRegistry.class.getDeclaredField("classToTypeMap");
        classesField.setAccessible(true);
        Map classes = (Map) classesField.get(FakeEntityFieldRegistry.INSTANCE);
        Map originalClasses = new HashMap(classes);
        registry.clear();
        try (var logging = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            EntityType entityType = mock(EntityType.class);
            WrapperEntity entity = mock(WrapperEntity.class);
            when(entity.getEntityType()).thenReturn(entityType);
            AtomicReference<String> value = new AtomicReference<>("before");
            FakeEntityFieldRegistry.INSTANCE.builder(entityType, WrapperEntity.class)
                    .optionalField(String.class, ignored -> value.get(),
                            writable ? (ignored, changed) -> value.set(changed) : null, name)
                    .build();
            Expression<WrapperEntity> source = mock(Expression.class);
            when(source.getArray(null)).thenReturn(new WrapperEntity[]{entity});
            when(source.isSingle()).thenReturn(true);
            Expression<?>[] inputs = {source};
            var expression = type.getConstructor().newInstance();
            assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
            assertArrayEquals(new String[]{"before"}, expression.getArray(null));
            assertEquals(String.class, expression.getReturnType());
            assertTrue(expression.isSingle());
            assertTrue(expression.toString(null, false).contains(name));
            for (ChangeMode mode : ChangeMode.values()) {
                if (writable && mode == ChangeMode.SET)
                    assertArrayEquals(new Class<?>[]{String.class}, expression.acceptChange(mode));
                else assertNull(expression.acceptChange(mode));
            }
            expression.change(null, new Object[]{"after"}, ChangeMode.SET);
            assertEquals(writable ? "after" : "before", value.get());
            expression.change(null, null, ChangeMode.SET);
            expression.change(null, new Object[0], ChangeMode.SET);
            expression.change(null, new Object[]{"ignored"}, ChangeMode.DELETE);
            expression.change(null, new Object[]{123}, ChangeMode.SET);
            assertEquals(writable ? "after" : "before", value.get());
            when(source.getArray(null)).thenReturn(new WrapperEntity[0]);
            assertArrayEquals(new Object[0], expression.getArray(null));
            when(source.isSingle()).thenReturn(false);
            assertFalse(expression.isSingle());
        } finally {
            registry.clear();
            registry.putAll(original);
            classes.clear();
            classes.putAll(originalClasses);
        }
    }
}
