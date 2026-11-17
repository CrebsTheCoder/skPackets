package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.score.BlankScoreFormat;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;
import com.github.retrooper.packetevents.protocol.vector.vecdelta.LinearVecDelta;
import com.github.retrooper.packetevents.protocol.vector.vecdelta.VecDelta;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef;
import com.github.retrooper.packetevents.protocol.world.waypoint.*;
import net.kyori.adventure.text.Component;
import org.bukkit.util.Vector;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PacketAdditionalValuesTest {
    @BeforeAll static void initialize() { PacketEventsTestBootstrap.initialize(); }
    private Object build(Class<?> type, Map<String, Object> fields) {
        return PacketArguments.convert(new PacketValue(new PacketRecord(fields)), type, "test");
    }
    @Test void namedRegistryValuesRoundTripAndRejectUnknownKeys() {
        var cases = Map.<Class<?>, String>ofEntries(
                Map.entry(com.github.retrooper.packetevents.protocol.attribute.Attribute.class, "minecraft:max_health"),
                Map.entry(com.github.retrooper.packetevents.protocol.chat.ChatType.class, "minecraft:chat"),
                Map.entry(com.github.retrooper.packetevents.protocol.item.mapdecoration.MapDecorationType.class, "minecraft:player"),
                Map.entry(com.github.retrooper.packetevents.protocol.world.damagetype.DamageType.class, "minecraft:generic"),
                Map.entry(com.github.retrooper.packetevents.protocol.recipe.category.RecipeBookCategory.class, "minecraft:crafting_misc"),
                Map.entry(com.github.retrooper.packetevents.protocol.world.clock.WorldClock.class, "minecraft:overworld"),
                Map.entry(com.github.retrooper.packetevents.protocol.component.ComponentType.class, "minecraft:damage"),
                Map.entry(com.github.retrooper.packetevents.protocol.debug.DebugSubscription.class, "minecraft:bees"),
                Map.entry(com.github.retrooper.packetevents.protocol.particle.type.ParticleType.class, "minecraft:flame"),
                Map.entry(com.github.retrooper.packetevents.protocol.recipe.RecipeSerializer.class, "minecraft:crafting_shaped"),
                Map.entry(com.github.retrooper.packetevents.protocol.world.dimension.DimensionType.class, "minecraft:overworld"),
                Map.entry(com.github.retrooper.packetevents.protocol.dialog.Dialog.class, "minecraft:server_links"),
                Map.entry(com.github.retrooper.packetevents.protocol.chat.Parsers.Parser.class, "brigadier:integer"),
                Map.entry(com.github.retrooper.packetevents.protocol.entity.data.EntityDataType.class, "minecraft:int"),
                Map.entry(com.github.retrooper.packetevents.protocol.item.trimpattern.TrimPattern.class, "minecraft:coast"));
        cases.forEach((type, key) -> {
            assertEquals(String.class, PacketArguments.expressionType(type));
            assertEquals(key, PacketArguments.toSkript(PacketArguments.convert(key, type, "key")));
            var error = assertThrows(IllegalArgumentException.class,
                    () -> PacketArguments.convert("minecraft:does_not_exist", type, "entries.first.type"));
            assertTrue(error.getMessage().contains("entries.first.type"));
        });
    }
    @Test void buildsBlankAndFixedScoreFormats() {
        assertInstanceOf(BlankScoreFormat.class, build(ScoreFormat.class, Map.of()));
        var fixed = (ScoreFormat) build(ScoreFormat.class, Map.of("value", Component.text("Points")));
        assertEquals(Component.text("Points"), fixed.format(10));
    }
    @Test void buildsDimensionReferences() {
        assertEquals(2, ((DimensionTypeRef) build(DimensionTypeRef.class, Map.of("id", 2))).getId());
        assertEquals("minecraft:overworld", ((DimensionTypeRef) build(DimensionTypeRef.class,
                Map.of("name", org.bukkit.NamespacedKey.minecraft("overworld")))).getName().toString());
        assertThrows(IllegalArgumentException.class, () -> build(DimensionTypeRef.class, Map.of("id", 2, "name", "overworld")));
    }
    @Test void buildsAllWaypointVariants() {
        assertInstanceOf(EmptyWaypointInfo.class, build(WaypointInfo.class, Map.of()));
        assertEquals(12f, ((AzimuthWaypointInfo) build(WaypointInfo.class, Map.of("angle", 12))).getAngle());
        assertEquals(3, ((ChunkWaypointInfo) build(WaypointInfo.class, Map.of("chunk x", 3, "chunk z", -4))).getChunkX());
        assertEquals(64, ((Vec3iWaypointInfo) build(WaypointInfo.class, Map.of("position", new Vector(1,64,2)))).getPosition().getY());
    }
    @Test void buildsLinearAndSteppedMovement() {
        var path = (PositionPath) build(PositionPath.class, Map.of("end position", new Vector(1,2,3)));
        assertEquals(2, path.getEndPosition().getY());
        var step = new PacketValue(new PacketRecord(Map.of("vector", new Vector(3,4,5), "tick offset", 2)));
        var stepped = (PositionPath) build(PositionPath.class, Map.of("steps", new PacketValue[]{step}));
        assertEquals(5, stepped.getEndPosition().getZ());
        var delta = (LinearVecDelta) build(VecDelta.class, Map.of("dx", 0.5, "dy", 0, "dz", -0.5));
        assertEquals(0.5, delta.dx());
        assertEquals(-0.5, delta.dz());
        var deltaStep = new PacketValue(new PacketRecord(Map.of("ticks", 1, "dx", 4, "dy", 0, "dz", -4)));
        assertEquals(1, ((VecDelta) build(VecDelta.class, Map.of("steps", new PacketValue[]{deltaStep}))).getStepCount());
    }
    @Test void textValuesRoundTripAndReportInvalidFieldPaths() throws Exception {
        var instant = java.time.Instant.parse("2026-11-15T01:02:03.123456789Z");
        assertEquals(instant, PacketArguments.convert(PacketArguments.toSkript(instant), java.time.Instant.class, "timestamp"));
        var key = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        assertEquals(key, PacketArguments.convert(PacketArguments.toSkript(key), java.security.PublicKey.class, "public key"));
        var json = (com.google.gson.JsonObject) PacketArguments.convert("{\"description\":\"hello\"}", com.google.gson.JsonObject.class, "response");
        assertEquals("hello", json.get("description").getAsString());
        assertEquals(json, PacketArguments.convert(PacketArguments.toSkript(json), com.google.gson.JsonObject.class, "response"));
        for (var type : List.of(java.time.Instant.class, java.security.PublicKey.class, com.google.gson.JsonObject.class,
                com.github.retrooper.packetevents.protocol.nbt.NBT.class, com.github.retrooper.packetevents.protocol.nbt.NBTCompound.class)) {
            var error = assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert("[", type, "nested.value"));
            assertTrue(error.getMessage().contains("nested.value"));
        }
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert("[]", com.google.gson.JsonObject.class, "response"));
    }
    @Test void snbtWorksWithoutSkBeeAndRequiresCompoundsWhereAppropriate() {
        var compound = (com.github.retrooper.packetevents.protocol.nbt.NBTCompound) PacketArguments.convert(
                "{health:20,flags:[B;1b,0b]}", com.github.retrooper.packetevents.protocol.nbt.NBTCompound.class, "nbt");
        assertEquals(20, compound.getNumberTagValueOrThrow("health").intValue());
        assertEquals(compound, PacketArguments.convert(PacketArguments.toSkript(compound),
                com.github.retrooper.packetevents.protocol.nbt.NBTCompound.class, "nbt"));
        var scalar = PacketArguments.convert("42", com.github.retrooper.packetevents.protocol.nbt.NBT.class, "payload");
        assertInstanceOf(com.github.retrooper.packetevents.protocol.nbt.NBTInt.class, scalar);
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert("42",
                com.github.retrooper.packetevents.protocol.nbt.NBTCompound.class, "nbt"));
    }
    private PacketValue record(Map<String, Object> fields) { return new PacketValue(new PacketRecord(fields)); }
    @Test void explicitlySelectedVariantsBuildRecursiveRecipeDisplays() {
        var empty = record(Map.of("empty", true));
        var fuel = record(Map.of("any fuel", true));
        var composite = record(Map.of("composite", record(Map.of("contents", new PacketValue[]{empty, fuel}))));
        var display = (com.github.retrooper.packetevents.protocol.recipe.display.FurnaceRecipeDisplay) build(
                com.github.retrooper.packetevents.protocol.recipe.display.RecipeDisplay.class,
                Map.of("furnace", record(Map.of("ingredient", composite, "fuel", fuel, "result", empty,
                        "crafting station", empty, "duration", 200, "experience", 0.5))));
        assertEquals(200, display.getDuration());
        assertEquals(2, ((com.github.retrooper.packetevents.protocol.recipe.display.slot.CompositeSlotDisplay) display.getIngredient()).getContents().size());
        assertThrows(IllegalArgumentException.class, () -> build(com.github.retrooper.packetevents.protocol.recipe.display.slot.SlotDisplay.class, Map.of("empty", false)));
        assertThrows(IllegalArgumentException.class, () -> build(com.github.retrooper.packetevents.protocol.recipe.display.slot.SlotDisplay.class, Map.of("empty", true, "any fuel", true)));
    }
    @Test void buildsExplicitLegacyChatAndUnboundedVersionRange() {
        var message = (com.github.retrooper.packetevents.protocol.chat.message.ChatMessage) build(
                com.github.retrooper.packetevents.protocol.chat.message.ChatMessage.class,
                Map.of("legacy", record(Map.of("chat content", Component.text("hello"), "type", "minecraft:chat"))));
        assertEquals(Component.text("hello"), message.getChatContent());
        var range = (com.github.retrooper.packetevents.util.VersionRange) build(com.github.retrooper.packetevents.util.VersionRange.class, Map.of());
        assertTrue(range.contains(com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_21_11));
    }

    com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet<com.github.retrooper.packetevents.protocol.attribute.Attribute> attributes;
    @Test void mappedSetsKeepTheirElementTypes() throws Exception {
        var type = getClass().getDeclaredField("attributes").getGenericType();
        var set = (com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet<?>) PacketArguments.convert(
                record(Map.of("entities", new String[]{"minecraft:max_health"})), type, "attributes");
        assertEquals("minecraft:max_health", set.getEntities().getFirst().getName().toString());
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert(
                record(Map.of("entities", new String[]{"minecraft:generic"})), type, "attributes"));
        var tagged = (com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet<?>) PacketArguments.convert(
                record(Map.of("tag key", org.bukkit.NamespacedKey.minecraft("test"))), type, "attributes");
        assertEquals("minecraft:test", tagged.getTagKey().toString());
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert(
                record(Map.of("tag key", org.bukkit.NamespacedKey.minecraft("test"), "entities", new String[0])), type, "attributes"));
    }
    @Test void particleBuilderWritesAndReadsTypedData() {
        var particle = (com.github.retrooper.packetevents.protocol.particle.Particle<?>) build(
                com.github.retrooper.packetevents.protocol.particle.Particle.class,
                Map.of("type", "minecraft:dragon_breath", "data", record(Map.of("power", record(Map.of("power", 0.75))))));
        var buffer = io.netty.buffer.Unpooled.buffer();
        try {
            var wrapper = com.github.retrooper.packetevents.wrapper.PacketWrapper.createDummyWrapper(
                    com.github.retrooper.packetevents.protocol.player.ClientVersion.V_26_3);
            wrapper.setBuffer(buffer);
            com.github.retrooper.packetevents.protocol.particle.Particle.write(wrapper, particle);
            var decoded = com.github.retrooper.packetevents.protocol.particle.Particle.read(wrapper);
            assertEquals(particle.getType(), decoded.getType());
            assertEquals(0.75f, ((com.github.retrooper.packetevents.protocol.particle.data.ParticlePowerData) decoded.getData()).getPower());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void attributeModifiersCanBeProvidedAndDefaultListsAreIndependent() {
        var type = com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property.class;
        var first = (com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property)
                build(type, Map.of("attribute", "minecraft:max_health", "value", 40));
        var second = (com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property)
                build(type, Map.of("attribute", "minecraft:max_health", "value", 40));
        var modifier = record(Map.of("name", org.bukkit.NamespacedKey.minecraft("bonus"), "amount", 5, "operation", "addition"));
        var modified = (com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property)
                build(type, Map.of("attribute", "minecraft:max_health", "value", 40, "modifiers", new PacketValue[]{modifier}));
        assertEquals(1, modified.getModifiers().size());
        assertEquals(45, modified.calcValue());
        first.addModifier(modified.getModifiers().getFirst());
        assertTrue(second.getModifiers().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> build(type, Map.of("attribute", "minecraft:max_health")));
    }

}
