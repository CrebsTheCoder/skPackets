package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.common.client.WrapperCommonClientSettings;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequiredPacketFieldReadbackTest {
    @BeforeAll
    static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @Test
    void animationHandReadsTheValueUsedToConstructThePacket() {
        PacketTypeCommon type = PacketType.Play.Client.ANIMATION;
        var packet = PacketCreationCoverageTest.create(type, Map.of("interaction hand", InteractionHand.OFF_HAND));
        assertSame(InteractionHand.OFF_HAND, read(type, packet, "interaction hand"));
        write(type, packet, "interaction hand", InteractionHand.MAIN_HAND);
        assertSame(InteractionHand.MAIN_HAND, read(type, packet, "interaction hand"));
    }

    @Test
    void structureKeepJigsawsReadsTheBooleanUsedToConstructThePacket() {
        PacketTypeCommon type = PacketType.Play.Client.GENERATE_STRUCTURE;
        var packet = PacketCreationCoverageTest.create(type,
                Map.of("block position", new Vector(1, 2, 3), "levels", 7L, "keep jigsaws", true));
        assertEquals(true, read(type, packet, "keep jigsaws"));
        write(type, packet, "keep jigsaws", false);
        assertEquals(false, read(type, packet, "keep jigsaws"));
    }

    @Test
    void serverAbilitiesExposeGodAndCreativeModeAsBooleans() {
        PacketTypeCommon type = PacketType.Play.Server.PLAYER_ABILITIES;
        var packet = PacketCreationCoverageTest.create(type, Map.of("god mode", true, "flying", false,
                "flight allowed", true, "creative mode", true, "fly speed", 0.125d, "fov modifier", 0.25d));
        assertEquals(true, read(type, packet, "god mode"));
        assertEquals(true, read(type, packet, "creative mode"));
        write(type, packet, "god mode", false);
        write(type, packet, "creative mode", false);
        assertEquals(false, read(type, packet, "god mode"));
        assertEquals(false, read(type, packet, "creative mode"));
    }

    @Test
    void clientAbilitiesUnwrapOptionalBooleans() {
        PacketTypeCommon type = PacketType.Play.Client.PLAYER_ABILITIES;
        var packet = PacketCreationCoverageTest.create(type,
                Map.of("flying", true, "god mode", true, "creative mode", true));
        assertEquals(true, read(type, packet, "god mode"));
        assertEquals(true, read(type, packet, "creative mode"));
        write(type, packet, "god mode", false);
        write(type, packet, "creative mode", false);
        assertEquals(false, read(type, packet, "god mode"));
        assertEquals(false, read(type, packet, "creative mode"));
    }

    @Test
    void spectateUuidReadsTheActualTarget() {
        PacketTypeCommon type = PacketType.Play.Client.SPECTATE;
        UUID uuid = new UUID(12, 34);
        var packet = PacketCreationCoverageTest.create(type, Map.of("uuid", uuid));
        assertEquals(uuid, read(type, packet, "uuid"));
        UUID replacement = new UUID(56, 78);
        write(type, packet, "uuid", replacement);
        assertEquals(replacement, read(type, packet, "uuid"));
    }

    @Test
    void timeUpdateGameTimeReadsWorldAge() {
        PacketTypeCommon type = PacketType.Play.Server.TIME_UPDATE;
        var packet = PacketCreationCoverageTest.create(type,
                Map.of("game time", 1234567890123L, "time of day", 12345L, "tick time", true));
        assertEquals(1234567890123L, read(type, packet, "game time"));
        write(type, packet, "game time", 9876543210123L);
        assertEquals(9876543210123L, read(type, packet, "game time"));
    }

    @Test
    void windowIdReadsContainerIdAsANumber() {
        PacketTypeCommon type = PacketType.Play.Server.WINDOW_PROPERTY;
        var packet = PacketCreationCoverageTest.create(type,
                Map.of("window id", 15L, "id", 2L, "value", 99L));
        assertEquals(15, read(type, packet, "window id"));
        write(type, packet, "window id", 300L);
        assertEquals(300, read(type, packet, "window id"));
    }

    @Test
    void clientSettingsReadTheModernVisibilityEnumAndNativeHand() {
        PacketTypeCommon type = PacketType.Play.Client.CLIENT_SETTINGS;
        var packet = PacketCreationCoverageTest.create(type, Map.ofEntries(
                Map.entry("locale", "en_gb"), Map.entry("view distance", 17L),
                Map.entry("visibility", WrapperCommonClientSettings.ChatVisibility.SYSTEM),
                Map.entry("chat colors", false), Map.entry("visible skin section mask", 90L),
                Map.entry("hand", org.bukkit.inventory.MainHand.LEFT),
                Map.entry("text filtering enabled", true), Map.entry("allow server listings", true),
                Map.entry("particle status", WrapperCommonClientSettings.ParticleStatus.MINIMAL)));
        assertEquals("system", read(type, packet, "visibility"));
        assertSame(org.bukkit.inventory.MainHand.LEFT, read(type, packet, "hand"));
        assertEquals(true, read(type, packet, "allow server listings"));
        write(type, packet, "visibility", WrapperCommonClientSettings.ChatVisibility.HIDDEN);
        write(type, packet, "hand", org.bukkit.inventory.MainHand.RIGHT);
        write(type, packet, "allow server listings", false);
        assertEquals("hidden", read(type, packet, "visibility"));
        assertSame(org.bukkit.inventory.MainHand.RIGHT, read(type, packet, "hand"));
        assertEquals(false, read(type, packet, "allow server listings"));
    }

    @Test
    void positionFlagsReadTheByteMaskInsteadOfTheRelativeFlagObject() {
        PacketTypeCommon type = PacketType.Play.Server.PLAYER_POSITION_AND_LOOK;
        var packet = PacketCreationCoverageTest.create(type, Map.ofEntries(
                Map.entry("x", 1d), Map.entry("y", 2d), Map.entry("z", 3d),
                Map.entry("yaw", 10d), Map.entry("pitch", 20d), Map.entry("flags", 5L),
                Map.entry("teleport id", 7L), Map.entry("dismount vehicle", false)));
        assertEquals((byte) 5, read(type, packet, "flags"));
        write(type, packet, "flags", 9L);
        assertEquals((byte) 9, read(type, packet, "flags"));
    }

    @Test
    void explosionMotionKeepsDoublePrecisionAndSmallParticleCanBeRead() {
        PacketTypeCommon type = PacketType.Play.Server.EXPLOSION;
        var smallParticle = mock(com.github.retrooper.packetevents.protocol.particle.Particle.class);
        var largeParticle = mock(com.github.retrooper.packetevents.protocol.particle.Particle.class);
        Vector motion = new Vector(1.1234567890123, 2.9876543210987, -3.1234567890123);
        var packet = PacketCreationCoverageTest.create(type, Map.ofEntries(
                Map.entry("position", new Vector(1, 2, 3)), Map.entry("strength", 4d),
                Map.entry("records", new Vector[0]), Map.entry("player motion", motion),
                Map.entry("small particle", smallParticle), Map.entry("particle", largeParticle),
                Map.entry("block interaction", WrapperPlayServerExplosion.BlockInteraction.DESTROY_BLOCKS),
                Map.entry("explosion sound", mock(com.github.retrooper.packetevents.protocol.sound.Sound.class)),
                Map.entry("block particles", mock(com.github.retrooper.packetevents.protocol.util.WeightedList.class))));
        assertEquals(motion, read(type, packet, "player motion"));
        assertEquals(new PacketValue(smallParticle), read(type, packet, "small particle"));
        Vector replacement = new Vector(9.1234567890123, 8.9876543210987, -7.1234567890123);
        write(type, packet, "player motion", replacement);
        write(type, packet, "small particle", largeParticle);
        assertEquals(replacement, read(type, packet, "player motion"));
        assertEquals(new PacketValue(largeParticle), read(type, packet, "small particle"));
    }

    @Test
    void playerInfoCollectionsKeepTheirDeclaredArrayTypesAndAllElements() {
        PacketTypeCommon type = PacketType.Play.Server.PLAYER_INFO;
        var first = mock(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData.class);
        var second = mock(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData.class);
        var values = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData[]{first, second};
        var packet = PacketCreationCoverageTest.create(type, Map.of("action",
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                "player data list", values));
        assertArrayEquals(new PacketValue[]{new PacketValue(first), new PacketValue(second)}, (Object[]) read(type, packet, "player data list"));
    }

    @Test
    void modernPlayerInfoSupportsMultipleActionsAndNonemptyEntries() {
        PacketTypeCommon type = PacketType.Play.Server.PLAYER_INFO_UPDATE;
        var entry = mock(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo.class);
        var actions = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action[]{
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY};
        var entries = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo[]{entry};
        var packet = PacketCreationCoverageTest.create(type, Map.of("actions", actions, "entries", entries));
        assertArrayEquals(new String[]{"update listed", "update latency"}, (Object[]) read(type, packet, "actions"));
        assertArrayEquals(new PacketValue[]{new PacketValue(entry)}, (Object[]) read(type, packet, "entries"));
    }

    @Test
    void equivalentConstructorAndPropertySpellingsShareOneAccessor() {
        var abilities = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.PLAYER_ABILITIES);
        assertSame(abilities.getAccessor("god mode"), abilities.getAccessor("in god mode"));
        assertSame(abilities.getAccessor("creative mode"), abilities.getAccessor("in creative mode"));
        var time = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.TIME_UPDATE);
        assertSame(time.getAccessor("game time"), time.getAccessor("world age"));

    }

    @Test
    void onlyFieldsWithoutPublicPacketEventsGettersAreCreationOnly() {
        java.util.Set<String> missing = new java.util.TreeSet<>();
        for (var type : org.crebs.skpackets.api.general.packet.PacketTypeRegistry.getAllPackets()) {
            var schema = PacketFieldRegistry.INSTANCE.getSchema(type);
            for (var field : schema.accessors()) {
                if (field.getter() == null) missing.add(type.getName() + ":" + field.name());
            }
        }
        assertEquals(java.util.Set.of("ENTITY_EFFECT:flags", "WORLD_BORDER_WARNING_DELAY:delay"), missing);
    }

    @SuppressWarnings("unchecked")
    private static void write(PacketTypeCommon type, PacketWrapper<?> packet, String name, Object value) {
        var field = (org.crebs.skpackets.api.util.field.FieldAccessor<PacketWrapper<?>, Object>)
                PacketFieldRegistry.INSTANCE.getSchema(type).getAccessor(name);
        assertNotNull(field.setter(), "Public PacketEvents setter is not exposed: " + type + " / " + name);
        field.setter().accept(packet, value);
    }

    private static Object read(PacketTypeCommon type, PacketWrapper<?> packet, String name) {
        var field = PacketFieldRegistry.INSTANCE.getSchema(type).getAccessor(name);
        assertNotNull(field, name);
        Object value = field.getter().apply(packet);
        assertNotNull(value, "Constructor field has no readable value: " + type + " / " + name);
        assertTrue(field.expectedType().isInstance(value),
                () -> "Read value type differs from declared Skript type for " + type + " / " + name + ": " + value.getClass());
        return value;
    }
}

