package org.crebs.skpackets.elements.general.section;

import ch.njol.skript.Skript;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class SecExprNewPacketTest {
    @Test
    void constructsPacketUsingTheSelectedSchema() throws Exception {
        PacketTypeCommon type = mock(PacketTypeCommon.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class);
        SecExprNewPacket expression = expression(type, new FieldSchema<>(type, List.of(), context -> {
            assertSame(type, context.getKey());
            return packet;
        }));
        assertArrayEquals(new PacketWrapper[]{packet}, expression.get(mock(Event.class)));
        assertTrue(expression.isSingle());
        assertSame(PacketWrapper.class, expression.getReturnType());
    }

    @Test
    void constructorReturningNullProducesNoPacket() throws Exception {
        PacketTypeCommon type = mock(PacketTypeCommon.class);
        SecExprNewPacket expression = expression(type, new FieldSchema<>(type, List.of(), context -> null));
        assertArrayEquals(new PacketWrapper<?>[0], expression.get(mock(Event.class)));
    }

    @Test
    void invalidConstructionProducesErrorAndNoPacket() throws Exception {
        PacketTypeCommon type = mock(PacketTypeCommon.class);
        SecExprNewPacket expression = expression(type, new FieldSchema<>(type, List.of(), context -> {
            throw new IllegalArgumentException("missing entity id");
        }));
        try (MockedStatic<Skript> skript = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            assertArrayEquals(new PacketWrapper<?>[0], expression.get(mock(Event.class)));
            skript.verify(() -> Skript.error("missing entity id"));
        }
    }

    private SecExprNewPacket expression(PacketTypeCommon type, FieldSchema<PacketTypeCommon, PacketWrapper<?>> schema) throws Exception {
        SecExprNewPacket expression = new SecExprNewPacket();
        Field typeField = SecExprNewPacket.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.set(expression, type);
        Field schemaField = SecExprNewPacket.class.getDeclaredField("schema");
        schemaField.setAccessible(true);
        schemaField.set(expression, schema);
        return expression;
    }
}
