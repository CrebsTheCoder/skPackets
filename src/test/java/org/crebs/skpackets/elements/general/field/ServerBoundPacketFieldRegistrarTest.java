package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerBoundPacketFieldRegistrarTest {
    @BeforeAll
    static void registerFields() {
        PacketEventsTestBootstrap.initialize();
        PluginManager plugins = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @CsvSource(
        value = {"true,true", "false,false", "NULL,false"},
        nullValues = {"NULL"}
    )
    void sneakingSetterHandlesNullableBoolean(Boolean value, boolean expected) {
        FieldSchema<PacketTypeCommon, PacketWrapper<?>> schema = PacketFieldRegistry.INSTANCE.getSchema(Client.INTERACT_ENTITY);
        FieldAccessor<PacketWrapper<?>, Boolean> field = (FieldAccessor<PacketWrapper<?>, Boolean>) schema.getAccessor("sneaking state");
        assertNotNull(field);
        assertNotNull(field.setter());
        WrapperPlayClientInteractEntity packet = mock(WrapperPlayClientInteractEntity.class);
        assertDoesNotThrow(() -> field.setter().accept(packet, value));
        verify(packet).setSneaking(expected);
    }
}
