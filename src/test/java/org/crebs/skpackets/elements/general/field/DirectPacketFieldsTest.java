package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectPacketFieldsTest {
    @BeforeAll static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    static Stream<PacketTypeCommon> types() {
        return Stream.of(PacketType.Play.Server.KEEP_ALIVE, PacketType.Play.Client.KEEP_ALIVE);
    }

    @ParameterizedTest @MethodSource("types")
    @SuppressWarnings("unchecked")
    void directFieldsPreserveLongRangeAndNumericValidation(PacketTypeCommon type) {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(type);
        var field = (FieldAccessor<PacketWrapper<?>, Number>) schema.getAccessor("id");
        var packet = PacketCreationCoverageTest.create(type, Map.of("id", Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, field.getter().apply(packet));
        assertEquals(long.class, field.nativeType());
        field.setter().accept(packet, Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, field.getter().apply(packet));
        assertThrows(IllegalArgumentException.class, () -> field.setter().accept(packet, new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class, () -> PacketCreationCoverageTest.create(type,
                Map.of("id", new BigDecimal("9223372036854775808"))));
    }
}
