package org.crebs.skpackets.elements.entity.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;
import me.tofaa.entitylib.wrapper.WrapperEntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FakeEquipmentExpressionTest {
    private final Event event = mock(Event.class);

    @SuppressWarnings("unchecked")
    private ExprFakeEquipment expression(EquipmentSlot slot, WrapperEntity... entities) {
        Expression<EquipmentSlot> slotInput = mock(Expression.class);
        when(slotInput.getSingle(event)).thenReturn(slot);
        Expression<WrapperEntity> entityInput = mock(Expression.class);
        when(entityInput.getArray(event)).thenReturn(entities);
        when(entityInput.isSingle()).thenReturn(entities.length == 1);
        Expression<?>[] inputs = {slotInput, entityInput};
        var expression = new ExprFakeEquipment();
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        return expression;
    }

    @ParameterizedTest
    @EnumSource(ChangeMode.class)
    void advertisesOnlySupportedChanges(ChangeMode mode) {
        var expression = new ExprFakeEquipment();
        if (mode == ChangeMode.SET || mode == ChangeMode.RESET || mode == ChangeMode.DELETE)
            assertArrayEquals(new Class<?>[]{ItemType.class}, expression.acceptChange(mode));
        else assertNull(expression.acceptChange(mode));
    }

    @ParameterizedTest
    @EnumSource(value = ChangeMode.class, names = {"RESET", "DELETE"})
    void deletingAndResettingClearTheSelectedSlotForEveryEntity(ChangeMode mode) {
        var first = mock(WrapperLivingEntity.class);
        var second = mock(WrapperLivingEntity.class);
        var firstEquipment = mock(WrapperEntityEquipment.class);
        var secondEquipment = mock(WrapperEntityEquipment.class);
        when(first.getEquipment()).thenReturn(firstEquipment);
        when(second.getEquipment()).thenReturn(secondEquipment);
        expression(EquipmentSlot.HEAD, first, second).change(event, null, mode);
        verify(firstEquipment).clearSlot(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET);
        verify(secondEquipment).clearSlot(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET);
    }

    @Test
    void absentSlotReturnsNoItemsAndDoesNotMutateEntities() {
        var entity = mock(WrapperLivingEntity.class);
        var expression = expression(null, entity);
        assertArrayEquals(new ItemType[0], expression.get(event));
        expression.change(event, null, ChangeMode.RESET);
        verifyNoInteractions(entity);
    }

    @Test
    void emptySetDoesNotClearExistingEquipment() {
        var entity = mock(WrapperLivingEntity.class);
        var expression = expression(EquipmentSlot.HAND, entity);
        expression.change(event, null, ChangeMode.SET);
        expression.change(event, new Object[0], ChangeMode.SET);
        verifyNoInteractions(entity);
        assertTrue(expression.isSingle());
        assertEquals(ItemType.class, expression.getReturnType());
        assertFalse(expression(EquipmentSlot.HAND, entity, entity).isSingle());
    }
}
