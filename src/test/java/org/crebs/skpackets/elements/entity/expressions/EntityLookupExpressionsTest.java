package org.crebs.skpackets.elements.entity.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityLookupExpressionsTest {
    private final Event event = mock(Event.class);

    @SuppressWarnings("unchecked")
    private <T> Expression<T> input(T value) {
        Expression<T> expression = mock(Expression.class);
        when(expression.getSingle(event)).thenReturn(value);
        return expression;
    }

    @Test
    void entityIdReturnsBukkitIdAndHandlesMissingInput() {
        Entity entity = mock(Entity.class);
        when(entity.getEntityId()).thenReturn(123);
        var expression = new ExprEntityId();
        Expression<?>[] inputs = {input(entity)};
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        assertArrayEquals(new Number[]{123}, expression.get(event));
        inputs[0] = input(null);
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        assertArrayEquals(new Number[0], expression.get(event));
        assertTrue(expression.isSingle());
        assertEquals(Number.class, expression.getReturnType());
    }

    @Test
    void lookupByIdReturnsExactEntityAndMissingIdsAreUnset() {
        EntityLib lib = mock(EntityLib.class, withSettings().mockMaker(MockMakers.INLINE));
        WrapperEntity entity = mock(WrapperEntity.class);
        when(lib.getEntity(42)).thenReturn(entity);
        var expression = new ExprFakeEntityFromId();
        Expression<?>[] inputs = {input(42)};
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        try (var library = mockStatic(EntityLib.class, withSettings().mockMaker(MockMakers.INLINE))) {
            library.when(EntityLib::get).thenReturn(lib);
            assertArrayEquals(new WrapperEntity[]{entity}, expression.get(event));
            inputs[0] = input(99);
            expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs));
            assertNull(expression.get(event));
            inputs[0] = input(null);
            expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs));
            assertNull(expression.get(event));
            verify(lib).getEntity(42);
            verify(lib).getEntity(99);
            verifyNoMoreInteractions(lib);
        }
        assertTrue(expression.isSingle());
        assertEquals(WrapperEntity.class, expression.getReturnType());
    }

    @Test
    void lookupByUuidSearchesTheWholeCollection() {
        EntityLib lib = mock(EntityLib.class, withSettings().mockMaker(MockMakers.INLINE));
        WrapperEntity first = mock(WrapperEntity.class), second = mock(WrapperEntity.class);
        UUID wanted = UUID.randomUUID();
        when(first.getUuid()).thenReturn(UUID.randomUUID());
        when(second.getUuid()).thenReturn(wanted);
        when(lib.getEntities()).thenReturn(List.of(first, second));
        var expression = new ExprFakeEntityFromUuid();
        Expression<?>[] inputs = {input(wanted)};
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        try (var library = mockStatic(EntityLib.class, withSettings().mockMaker(MockMakers.INLINE))) {
            library.when(EntityLib::get).thenReturn(lib);
            assertArrayEquals(new WrapperEntity[]{second}, expression.get(event));
            inputs[0] = input(UUID.randomUUID());
            expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs));
            assertNull(expression.get(event));
            inputs[0] = input(null);
            expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs));
            clearInvocations(lib);
            assertNull(expression.get(event));
            verifyNoInteractions(lib);
        }
        assertTrue(expression.isSingle());
    }

    @Test
    void allEntitiesReflectsCurrentLibraryContents() {
        EntityLib lib = mock(EntityLib.class, withSettings().mockMaker(MockMakers.INLINE));
        WrapperEntity first = mock(WrapperEntity.class), second = mock(WrapperEntity.class);
        when(lib.getEntities()).thenReturn(List.of(first, second), List.of());
        var expression = new ExprFakeEntitiesAll();
        assertTrue(expression.init(new Expression<?>[0], 0, Kleenean.FALSE, new ParseResult("", new Expression<?>[0])));
        try (var library = mockStatic(EntityLib.class, withSettings().mockMaker(MockMakers.INLINE))) {
            library.when(EntityLib::get).thenReturn(lib);
            assertArrayEquals(new WrapperEntity[]{first, second}, expression.get(event));
            assertArrayEquals(new WrapperEntity[0], expression.get(event));
        }
        assertFalse(expression.isSingle());
        assertEquals(WrapperEntity.class, expression.getReturnType());
    }
}
