package org.crebs.skpackets.elements.general.expressions.prop;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class ExprPacketFieldTest {
    @Test
    void readsAndSetsScalarPacketFields() throws Exception {
        Event event = mock(Event.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class);
        AtomicReference<String> value = new AtomicReference<>("before");
        FieldAccessor<PacketWrapper<?>, String> accessor = new FieldAccessor<>("text", new String[0], String.class, false, ignored -> value.get(), (ignored, next) -> value.set(next));
        Probe expression = expression(accessor, packet, event);
        assertArrayEquals(new Object[]{"before"}, expression.read(event, packet));
        expression.change(event, new Object[]{"after"}, ChangeMode.SET);
        assertEquals("after", value.get());
        expression.change(event, new Object[]{"ignored"}, ChangeMode.ADD);
        expression.change(event, null, ChangeMode.SET);
        expression.change(event, new Object[0], ChangeMode.SET);
        assertEquals("after", value.get());
        assertSame(String.class, expression.getReturnType());
        assertTrue(expression.isSingle());
    }

    @Test
    void flattensArrayFieldsAndReportsPlurality() throws Exception {
        Event event = mock(Event.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class);
        FieldAccessor<PacketWrapper<?>, String[]> accessor = new FieldAccessor<>("names", new String[0], String[].class, false, ignored -> new String[]{"a", "b"}, null);
        Probe expression = expression(accessor, packet, event);
        assertArrayEquals(new Object[]{"a", "b"}, expression.read(event, packet));
        assertSame(String.class, expression.getReturnType());
        assertFalse(expression.isSingle());
        try (MockedStatic<Skript> skript = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            assertNull(expression.acceptChange(ChangeMode.DELETE));
            skript.verify(() -> Skript.error("Cannot set names because it is a read-only field."));
        }
    }

    @SuppressWarnings("unchecked")
    private Probe expression(FieldAccessor<?, ?> accessor, PacketWrapper<?> packet, Event event) throws Exception {
        Probe expression = new Probe();
        Field field = ExprPacketField.class.getDeclaredField("fieldAccessor");
        field.setAccessible(true);
        field.set(expression, accessor);
        Expression<PacketWrapper<?>> source = mock(Expression.class);
        when(source.getArray(event)).thenReturn(new PacketWrapper[]{packet});
        when(source.isSingle()).thenReturn(true);
        expression.source(source);
        return expression;
    }

    static class Probe extends ExprPacketField {
        void source(Expression<? extends PacketWrapper> expression) {
            setExpr(expression);
        }

        Object[] read(Event event, PacketWrapper<?> packet) {
            return get(event, new PacketWrapper[]{packet});
        }
    }
}
