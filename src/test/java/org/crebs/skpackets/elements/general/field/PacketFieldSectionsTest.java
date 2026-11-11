package org.crebs.skpackets.elements.general.field;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleLiteral;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.api.util.field.FieldSections;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketFieldSectionsTest {
    @Test void fixedInvalidEnumRejectsTheSectionDuringLoading() throws Exception {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.BOSS_BAR);
        var uuid = new UUID(1, 2);
        try (var skript = mockStatic(Skript.class);
             var parsers = mockConstruction(SkriptParser.class, (parser, context) -> {
                 String input = (String)context.arguments().getFirst();
                 Object value = input.equals("uuid-value") ? uuid : "update";
                 doReturn(new SimpleLiteral<>(value, false)).when(parser).parseExpression(any(Class[].class));
             })) {
            var text = "uuid: uuid-value\naction: \"update\"\n";
            var node = new Config(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertFalse(PacketFieldSections.parse(schema.accessors(), node, new HashMap<>(), "boss bar"));
            skript.verify(() -> Skript.error(contains("Invalid value for packet field 'action': 'update'")));
        }
    }

    @BeforeAll static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @Test void parsesNestedListAndProfileThenBuildsActualPlayerInfoPacket() throws Exception {
        UUID uuid = new UUID(1, 2);
        String text = """
                actions: "update latency"
                entries:
                    first:
                        profile:
                            uuid: uuid-value
                            name: "Example"
                        latency: 42
                """;
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.PLAYER_INFO_UPDATE);
        Map<String, Expression<?>> expressions = new HashMap<>();
        try (var parsers = mockConstruction(SkriptParser.class, (parser, context) -> {
            String input = (String)context.arguments().getFirst();
            Object value = input.equals("uuid-value") ? uuid : input.equals("42") ? 42 : input.replace("\"", "");
            doReturn(new SimpleLiteral<>(value, false)).when(parser).parseExpression(any(Class[].class));
        })) {
            var node = new Config(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertTrue(PacketFieldSections.parse(schema.accessors(), node, expressions, "player info"));
            var packet = (WrapperPlayServerPlayerInfoUpdate) FieldSections.create(schema, PacketType.Play.Server.PLAYER_INFO_UPDATE, expressions, mock(org.bukkit.event.Event.class), Map.of());
            assertNotNull(packet);
            assertEquals(EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY), packet.getActions());
            assertEquals(1, packet.getEntries().size());
            assertEquals(uuid, packet.getEntries().getFirst().getProfileId());
            assertEquals(42, packet.getEntries().getFirst().getLatency());
        }
    }

    @Test void rejectsBothCanonicalNameAndAliasInsteadOfSilentlyIgnoringOne() throws Exception {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.PLAYER_ABILITIES);
        String text = "god mode: true\nin god mode: false\n";
        try (var skript = mockStatic(Skript.class)) {
            var node = new Config(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertFalse(PacketFieldSections.parse(schema.accessors(), node, new HashMap<>(), "player abilities"));
            skript.verify(() -> Skript.error(contains("Duplicate entries")));
        }
    }

    @Test void rejectsNestedSectionForUnsupportedComplexShape() throws Exception {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.CAMERA);
        try (var skript = mockStatic(Skript.class)) {
            var node = new Config(new ByteArrayInputStream("camera id:\n    value: 1\n".getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertFalse(PacketFieldSections.parse(schema.accessors(), node, new HashMap<>(), "camera"));
            skript.verify(() -> Skript.error(contains("does not support nested entries")));
        }
    }
}
