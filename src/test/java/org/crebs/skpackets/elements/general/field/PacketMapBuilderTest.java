package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkBiomes;
import com.github.retrooper.packetevents.util.Vector2i;
import org.crebs.skpackets.api.general.Vector2;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Type;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PacketMapBuilderTest {
    private Type chunks() throws Exception {
        return WrapperPlayServerChunkBiomes.class.getMethod("getChunks").getGenericReturnType();
    }
    private PacketValue entry(int x, int z, int count) {
        return new PacketValue(new PacketRecord(Map.of("key", new Vector2(x, z), "value",
                new PacketValue(new PacketRecord(Map.of("biome id", 1, "section count", count))))));
    }
    @Test void buildsChunkBiomeMapsAndRoundTripsCopiedValues() throws Exception {
        var type = chunks();
        assertEquals(PacketValue[].class, PacketArguments.expressionType(type));
        assertTrue(PacketRecords.isList(type));
        assertNotNull(PacketRecords.shape(type));
        var error = assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert(
                new PacketValue[]{entry(2, -3, 24)}, type, "chunks"));
        assertTrue(error.getMessage().contains("bitsPerEntry"));
        var data = org.mockito.Mockito.mock(WrapperPlayServerChunkBiomes.ChunkBiomeData.class);
        var map = Map.of(new Vector2i(2, -3), data);
        assertEquals(map, PacketArguments.convert(PacketArguments.toSkript(map), type, "chunks"));
    }
    @Test void discoversAndInvokesConstructorsWithoutRegistration() {
        var shape = PacketValueDiscovery.discover(com.github.retrooper.packetevents.protocol.player.UserProfile.class);
        assertNotNull(shape);
        var values = new java.util.HashMap<String, Object>();
        var uuid = java.util.UUID.randomUUID();
        values.put("uuid", uuid);
        values.put("name", "Builder");
        values.put("texture properties", java.util.List.of());
        var profile = (com.github.retrooper.packetevents.protocol.player.UserProfile) shape.factory().apply(values);
        assertEquals(uuid, profile.getUUID());
        assertEquals("Builder", profile.getName());
    }
    @Test void rejectsDuplicateChunks() throws Exception {
        var type = chunks();
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert(
                new PacketValue[]{entry(0, 0, 24), entry(0, 0, 24)}, type, "chunks"));

    }
}
