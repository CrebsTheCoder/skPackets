package org.crebs.skpackets.elements.general.expressions.prop;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ExprPacketPacketTypeTest {
    @Test
    void returnsThePacketsType() {
        PacketWrapper<?> packet = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        PacketTypeCommon type = mock(PacketTypeCommon.class);
        when(packet.getPacketTypeData().getPacketType()).thenReturn(type);
        assertSame(type, new ExprPacketPacketType().convert(packet));
    }

    @Test
    void keepsDifferentPacketTypesSeparate() {
        PacketWrapper<?> first = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        PacketWrapper<?> second = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        PacketTypeCommon firstType = mock(PacketTypeCommon.class);
        PacketTypeCommon secondType = mock(PacketTypeCommon.class);
        when(first.getPacketTypeData().getPacketType()).thenReturn(firstType);
        when(second.getPacketTypeData().getPacketType()).thenReturn(secondType);
        ExprPacketPacketType expression = new ExprPacketPacketType();
        assertSame(firstType, expression.convert(first));
        assertSame(secondType, expression.convert(second));
        assertSame(firstType, expression.convert(first));
    }
}
