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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2})
    void parsesAttributesWithOmittedOrExplicitlyEmptyModifiers(int variant) throws Exception {
        String text = """
                entity id: 1
                properties:
                    health:
                        attribute: "minecraft:max_health"
                        value: 40
                """ + (variant == 1 ? "        modifiers:\n" : "");
        if (variant == 2) text = text.replace("attribute:", "ATTRIBUTE:");
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.UPDATE_ATTRIBUTES);
        Map<String, Expression<?>> expressions = new HashMap<>();
        try (var skript = mockStatic(Skript.class);
             var parsers = mockConstruction(SkriptParser.class, (parser, context) -> {
            String input = (String) context.arguments().getFirst();
            Object value = input.equals("1") ? 1 : input.equals("40") ? 40 : input.replace("\"", "");
            doReturn(new SimpleLiteral<>(value, false)).when(parser).parseExpression(any(Class[].class));
        })) {
            skript.when(() -> Skript.error(anyString())).thenAnswer(invocation -> { throw new AssertionError((String) invocation.getArgument(0)); });
            var node = new Config(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertTrue(PacketFieldSections.parse(schema.accessors(), node, expressions, "attributes"));
            var packet = (com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes)
                    FieldSections.create(schema, PacketType.Play.Server.UPDATE_ATTRIBUTES, expressions,
                            mock(org.bukkit.event.Event.class), Map.of());
            assertNotNull(packet);
            var property = packet.getProperties().getFirst();
            assertEquals("minecraft:max_health", property.getAttribute().getName().toString());
            assertEquals(40, property.getValue());
            assertTrue(property.getModifiers().isEmpty());
        }
    }

    @Test void nestedAlternativeFieldsAreCaseInsensitive() throws Exception {
        var accessor = new org.crebs.skpackets.api.util.field.FieldAccessor<Object, PacketValue>(
                "dimension", new String[0], PacketValue.class, false, null, null,
                com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef.class);
        var expressions = new HashMap<String, Expression<?>>();
        try (var skript = mockStatic(Skript.class);
             var parsers = mockConstruction(SkriptParser.class, (parser, context) ->
                     doReturn(new SimpleLiteral<>(2, false)).when(parser).parseExpression(any(Class[].class)))) {
            skript.when(() -> Skript.error(anyString())).thenAnswer(invocation -> { throw new AssertionError((String) invocation.getArgument(0)); });
            var node = new Config(new ByteArrayInputStream("dimension:\n    ID: 2\n".getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertTrue(PacketFieldSections.parse(List.of(accessor), node, expressions, "test"));
            var dimension = (com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef)
                    PacketArguments.convert(expressions.get("dimension").getSingle(mock(org.bukkit.event.Event.class)), accessor.nativeType(), "dimension");
            assertEquals(2, dimension.getId());
        }
    }

    @Test void emptyOptionalMapValuesCanBeOmittedInNestedSections() throws Exception {
        var type = com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.class
                .getMethod("getHashedSlots").getGenericReturnType();
        var accessor = new org.crebs.skpackets.api.util.field.FieldAccessor<Object, PacketValue[]>(
                "slots", new String[0], PacketValue[].class, false, null, null, type);
        var expressions = new HashMap<String, Expression<?>>();
        try (var skript = mockStatic(Skript.class);
             var parsers = mockConstruction(SkriptParser.class, (parser, context) ->
                     doReturn(new SimpleLiteral<>(4, false)).when(parser).parseExpression(any(Class[].class)))) {
            skript.when(() -> Skript.error(anyString())).thenAnswer(invocation -> { throw new AssertionError((String) invocation.getArgument(0)); });
            var node = new Config(new ByteArrayInputStream("slots:\n    first:\n        key: 4\n".getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertTrue(PacketFieldSections.parse(List.of(accessor), node, expressions, "test"));
            assertEquals(Map.of(4, Optional.empty()), PacketArguments.convert(
                    expressions.get("slots").getArray(mock(org.bukkit.event.Event.class)), type, "slots"));
        }
    }

    @Test void parsesNamedRecipeVariantSections() throws Exception {
        String text = """
                display:
                    stonecutter:
                        input:
                            empty: true
                        result:
                            empty: true
                        crafting station:
                            any fuel: true
                """;
        var accessor = new org.crebs.skpackets.api.util.field.FieldAccessor<Object, PacketValue>(
                "display", new String[0], PacketValue.class, false, null, null,
                com.github.retrooper.packetevents.protocol.recipe.display.RecipeDisplay.class);
        var expressions = new HashMap<String, Expression<?>>();
        try (var skript = mockStatic(Skript.class);
             var parsers = mockConstruction(SkriptParser.class, (parser, context) ->
                     doReturn(new SimpleLiteral<>(true, false)).when(parser).parseExpression(any(Class[].class)))) {
            skript.when(() -> Skript.error(anyString())).thenAnswer(invocation -> { throw new AssertionError((String) invocation.getArgument(0)); });
            var node = new Config(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
            assertTrue(PacketFieldSections.parse(List.of(accessor), node, expressions, "recipe"));
            Object value = expressions.get("display").getSingle(mock(org.bukkit.event.Event.class));
            var display = (com.github.retrooper.packetevents.protocol.recipe.display.StonecutterRecipeDisplay)
                    PacketArguments.convert(value, accessor.nativeType(), "display");
            assertInstanceOf(com.github.retrooper.packetevents.protocol.recipe.display.slot.EmptySlotDisplay.class, display.getInput());
            assertInstanceOf(com.github.retrooper.packetevents.protocol.recipe.display.slot.AnyFuelSlotDisplay.class, display.getCraftingStation());
        }
    }

    @Test void openSignEditorAcceptsBothPositionNames() throws Exception {
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        assertSame(schema.getAccessor("block position"), schema.getAccessor("position"));
        assertTrue(schema.getReadableFields().contains("block position|position"));
        for (String name : List.of("block position", "position")) {
            Map<String, Expression<?>> expressions = new HashMap<>();
            try (var parsers = mockConstruction(SkriptParser.class, (parser, context) -> {
                String input = (String) context.arguments().getFirst();
                Object value = input.equals("position-value") ? new org.bukkit.util.Vector(2, 64, -3)
                        : org.bukkit.block.sign.Side.FRONT;
                doReturn(new SimpleLiteral<>(value, false)).when(parser).parseExpression(any(Class[].class));
            })) {
                String text = name + ": position-value\nsign side: front\n";
                var node = new Config(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "test.sk", true, true, ":").getMainNode();
                assertTrue(PacketFieldSections.parse(schema.accessors(), node, expressions, "open sign editor"));
                var packet = (com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor)
                        FieldSections.create(schema, PacketType.Play.Server.OPEN_SIGN_EDITOR, expressions, mock(org.bukkit.event.Event.class), Map.of());
                assertNotNull(packet);
                assertEquals(new com.github.retrooper.packetevents.util.Vector3i(2, 64, -3), packet.getPosition());
                assertTrue(packet.isFrontText());
            }
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
