package org.crebs.skpackets.elements.general.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.Skin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExprSkinFromValueTest {
    @Test
    void createsSignedAndUnsignedSkins() {
        Event event = mock(Event.class);
        Expression<String> value = mock(Expression.class);
        Expression<String> signature = mock(Expression.class);
        when(value.getSingle(event)).thenReturn("texture");
        when(signature.getSingle(event)).thenReturn("signature");
        ExprSkinFromValue expression = expression(value, signature);
        assertArrayEquals(new Skin[]{new Skin("texture", "signature")}, expression.get(event));
        assertArrayEquals(new Skin[]{new Skin("texture", null)}, expression(value, null).get(event));
        assertTrue(expression.isSingle());
        assertSame(Skin.class, expression.getReturnType());
    }

    @Test
    void missingValueReturnsNoSkin() {
        assertEquals(0, expression(mock(Expression.class), null).get(mock(Event.class)).length);
    }

    private ExprSkinFromValue expression(Expression<?> value, Expression<?> signature) {
        ExprSkinFromValue expression = new ExprSkinFromValue();
        assertTrue(expression.init(new Expression<?>[]{value, signature}, 0, Kleenean.FALSE, new SkriptParser.ParseResult("", new Expression<?>[0])));
        return expression;
    }
}
