package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.*;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.api.general.packet.PacketTypeRegistry;
import org.crebs.skpackets.api.util.field.*;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketCreationCoverageTest {
    @BeforeAll static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    static Stream<PacketTypeCommon> packetTypes() { return PacketTypeRegistry.getAllPackets().stream(); }

    @ParameterizedTest(name = "{0}") @MethodSource("packetTypes")
    void everyPacketTypeHasAValidatedCreationSchema(PacketTypeCommon type) {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(type);
        assertNotNull(schema, () -> type + " has no schema");
        assertNotNull(schema.constructor(), () -> type + " has no constructor");
        Set<String> names = new HashSet<>();
        for (var field : schema.accessors()) {
            assertTrue(names.add(field.name()), () -> type + " duplicates " + field.name());
            assertNotNull(field.expectedType());
            
            assertFalse(field.expectedType().isPrimitive());
        }
    }

    @Test void generatedSchemasExposeNativeTypesOrTheSharedCarrier() {
        for (var schema : PacketFieldRegistry.INSTANCE.getAllSchemas()) {
            for (var field : schema.accessors()) {
                Class<?> type = field.expectedType();
                while (type.isArray()) type = type.getComponentType();
                assertTrue(type == PacketValue.class || type == Component.class || PacketValueTypes.managedByModule(type)
                        || type.getName().startsWith("java.") || type.getName().startsWith("org.bukkit.")
                        || type.getName().startsWith("ch.njol.skript.") || type.getName().startsWith("org.skriptlang.skript."),
                        "Leaked PacketEvents implementation type: " + type.getName());
            }
        }
    }

    @Test void onlyTheKnownUnconstructableTypesUseRawPayloads() {
        Set<PacketTypeCommon> expected = Set.of(
                PacketType.Handshaking.Client.LEGACY_SERVER_LIST_PING,
                PacketType.Handshaking.Server.LEGACY_SERVER_LIST_RESPONSE,
                PacketType.Configuration.Server.UPDATE_TAGS,
                PacketType.Play.Client.RECIPE_BOOK_DATA,
                PacketType.Play.Server.MAP_CHUNK_BULK,
                PacketType.Play.Server.UPDATE_SIGN,
                PacketType.Play.Server.SCULK_VIBRATION_SIGNAL,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,
                PacketType.Play.Server.UNLOCK_RECIPES,
                PacketType.Play.Server.STOP_SOUND);
        Set<PacketTypeCommon> actual = new HashSet<>();
        for (PacketTypeCommon type : PacketTypeRegistry.getAllPackets())
            if (PacketFieldRegistry.INSTANCE.getSchema(type).getAccessor("raw data") != null) actual.add(type);
        assertEquals(expected, actual, "A new or existing typed wrapper must not silently become raw-only");
    }

    @Test void reportsCoverage() {
        long raw = PacketTypeRegistry.getAllPackets().stream().filter(t -> PacketFieldRegistry.INSTANCE.getSchema(t).getAccessor("raw data") != null).count();
        System.out.println("Packet creation coverage: " + PacketTypeRegistry.getAllPackets().size() + " types, " + raw + " raw-payload fallbacks");
        assertTrue(PacketTypeRegistry.getAllPackets().size() > 280);
    }

    @Test void createsUseItemWithBothHandsAndAllFields() {
        for (InteractionHand hand : InteractionHand.values()) {
            WrapperPlayClientUseItem packet = (WrapperPlayClientUseItem) create(PacketType.Play.Client.USE_ITEM,
                    Map.of("hand", hand, "sequence", 300L, "yaw", 12.5d, "pitch", -4d));
            assertSame(hand, packet.getHand());
            assertEquals(300, packet.getSequence());
            assertEquals(12.5f, packet.getYaw());
            assertEquals(-4f, packet.getPitch());
            assertEquals(PacketSide.CLIENT, packet.getPacketTypeData().getPacketType().getSide());
        }
    }

    @Test void constructsPacketsInEveryProtocolState() {
        assertEquals(PacketType.Status.Client.REQUEST, create(PacketType.Status.Client.REQUEST, Map.of()).getPacketTypeData().getPacketType());
        assertEquals(PacketType.Login.Server.SET_COMPRESSION, create(PacketType.Login.Server.SET_COMPRESSION, Map.of("threshold", 256L)).getPacketTypeData().getPacketType());
        assertEquals(PacketType.Configuration.Server.KEEP_ALIVE, create(PacketType.Configuration.Server.KEEP_ALIVE, Map.of("id", 99L)).getPacketTypeData().getPacketType());
        var handshake = create(PacketType.Handshaking.Client.HANDSHAKE,
                Map.of("protocol version", 767L, "server address", "localhost", "server port", 25565L,
                        "intention", com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake.ConnectionIntention.LOGIN));
        assertEquals(PacketType.Handshaking.Client.HANDSHAKE, handshake.getPacketTypeData().getPacketType());
    }

    @Test void supportsBossBarFieldsBeyondConstructorArguments() {
        UUID uuid = UUID.randomUUID();
        var packet = (WrapperPlayServerBossBar) create(PacketType.Play.Server.BOSS_BAR,
                Map.of("uuid", uuid, "action", WrapperPlayServerBossBar.Action.ADD,
                        "title", Component.text("Test"), "health", 0.75d));
        assertEquals(uuid, packet.getUUID());
        assertEquals(Component.text("Test"), packet.getTitle());
        assertEquals(0.75f, packet.getHealth());
    }

    @Test void acceptsTypedCollectionsAndEmptyLists() {
        var packet = (WrapperPlayServerPlayerInfoRemove) create(PacketType.Play.Server.PLAYER_INFO_REMOVE,
                Map.of("profile ids", new UUID[]{new UUID(0, 1), new UUID(0, 2)}));
        assertEquals(List.of(new UUID(0, 1), new UUID(0, 2)), packet.getProfileIds());
        var empty = (WrapperPlayServerPlayerInfoRemove) create(PacketType.Play.Server.PLAYER_INFO_REMOVE,
                Map.of("profile ids", new UUID[0]));
        assertTrue(empty.getProfileIds().isEmpty());
    }

    @Test void rawPayloadIsExplicitAndBytesAreChecked() {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.STOP_SOUND);
        assertNotNull(schema.getAccessor("raw data"));
        var packet = create(PacketType.Play.Server.STOP_SOUND, Map.of("raw data", new Number[]{0, 255, -1}));
        assertInstanceOf(PacketFieldRegistrar.RawPacket.class, packet);
        assertEquals(PacketType.Play.Server.STOP_SOUND, packet.getPacketTypeData().getPacketType());
        assertThrows(IllegalArgumentException.class, () -> create(PacketType.Play.Server.STOP_SOUND, Map.of("raw data", new Number[]{256})));
    }

    @Test void invalidValueReportsItsFieldInsteadOfSilentlyConstructing() {
        assertThrows(IllegalArgumentException.class, () -> create(PacketType.Play.Client.HELD_ITEM_CHANGE, Map.of("slot", "1")));
        assertThrows(IllegalArgumentException.class, () -> create(PacketType.Play.Client.HELD_ITEM_CHANGE, Map.of("slot", 1.5)));
        assertThrows(IllegalStateException.class, () -> create(PacketType.Play.Client.HELD_ITEM_CHANGE, Map.of()));
    }

    static PacketWrapper<?> create(PacketTypeCommon type, Map<String, Object> values) {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(type);
        return schema.constructor().apply(new ConstructionContext<>(type, schema.accessors(), values));
    }
}
