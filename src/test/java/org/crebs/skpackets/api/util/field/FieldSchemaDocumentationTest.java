package org.crebs.skpackets.api.util.field;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class FieldSchemaDocumentationTest {
    @Test
    void packetHeadingsContainCollapsibleFieldLists() {
        FieldSchema<String, Object> schema = new FieldSchema<>("clientbound example packet",
                List.of(field("id", Number.class, false, ignored -> 1)), null);
        String docs = FieldSchema.describe(List.of(schema), Function.identity());
        assertEquals("### Available Packets and their fields\n\n"
                + "* **clientbound example packet** fields:\n\n"
                + "  <details>\n  <summary>View fields</summary>\n\n"
                + "  - `id: number` (required)\n\n"
                + "  </details>\n\n", docs);
    }

    @Test
    void fieldTypesUseFriendlySkriptNamesAndRegisteredPacketLabels() {
        FieldSchema<String, Object> schema = new FieldSchema<>("example", List.of(
                field("id", Number.class, false, ignored -> 1),
                field("enabled", Boolean.class, false, ignored -> true),
                field("position", Vector.class, false, ignored -> new Vector()),
                field("slot", EquipmentSlot.class, false, ignored -> EquipmentSlot.HAND),
                field("item", org.bukkit.inventory.ItemStack.class, false, null),
                field("state", String.class, false, null).withNativeType(WrapperPlayServerBossBar.Action.class)), null);
        String docs = schema.getReadableFields();
        assertTrue(docs.contains("`id: number`"));
        assertTrue(docs.contains("`enabled: boolean`"));
        assertTrue(docs.contains("`position: vector`"));
        assertTrue(docs.contains("`slot: equipment slot`"));
        assertTrue(docs.contains("`item: item`"));
        assertTrue(docs.contains("`state: text`"));
        assertFalse(docs.matches("(?s).*(?:java|org|com|net|io|me)\\..*"));
        assertFalse(docs.contains("Number"));
        assertFalse(docs.contains("ItemStack"));
    }

    @Test
    void showsOptionalRequiredAliasesAndCreationOnlyFields() {
        FieldAccessor<Object, Number[]> accessor = new FieldAccessor<>(
                "ids", new String[]{"entity ids"}, Number[].class, true, null, null);
        FieldSchema<String, Object> schema = new FieldSchema<>("example", List.of(accessor), null);
        assertEquals("- `ids|entity ids: numbers` (optional: creation-only)", schema.getReadableFields());
    }

    @Test
    void emptyPacketsDoNotHaveEmptyDetailsSections() {
        FieldSchema<String, Object> schema = new FieldSchema<>("empty packet", List.of(), null);
        String docs = FieldSchema.describe(List.of(schema), Function.identity());
        assertEquals("### Available Packets and their fields\n\n* **empty packet**: no fields.\n\n", docs);
        assertFalse(docs.contains("<details>"));
        assertFalse(docs.contains("<summary>"));
    }

    @Test
    void packetOrderingIsIndependentOfRegistryIterationOrder() {
        FieldSchema<String, Object> alpha = new FieldSchema<>("alpha packet", List.of(), null);
        FieldSchema<String, Object> zeta = new FieldSchema<>("zeta packet",
                List.of(field("enabled", Boolean.class, true, ignored -> true)), null);
        String forward = FieldSchema.describe(List.of(alpha, zeta), Function.identity());
        String reversed = FieldSchema.describe(List.of(zeta, alpha), Function.identity());
        assertEquals(forward, reversed);
        assertTrue(forward.indexOf("**alpha packet**") < forward.indexOf("**zeta packet**"));
    }

    @Test
    void fieldsShowEnumValuesWithoutExamples() {
        FieldSchema<String, Object> schema = new FieldSchema<>("example", List.of(
                field("hand", com.github.retrooper.packetevents.protocol.player.InteractionHand.class, false, null),
                field("update type", String.class, false, null).withNativeType(com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetStructureBlock.UpdateType.class)), null);
        String docs = schema.getReadableFields();
        assertTrue(docs.contains("field values: `main hand`, `off hand`"));
        assertFalse(docs.contains("Example:"));
        assertTrue(docs.contains("field values: `\"update data\"`"));
    }

    @Test
    void complexMapFieldsDescribeTheirContentsAndNestedBuilder() throws Exception {
        var nativeType = com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkBiomes.class
                .getMethod("getChunks").getGenericReturnType();
        var accessor = field("chunks", org.crebs.skpackets.elements.general.field.PacketValue[].class, false, null)
                .withNativeType(nativeType);
        String docs = new FieldSchema<>("chunk biomes", List.of(accessor), null).getReadableFields();
        assertTrue(docs.contains("chunks: map of vector2 to packet chunk biome data"));
        assertTrue(docs.contains("key (required), value (required)"));
        assertFalse(docs.contains("packet value"));
    }

    @Test void nestedDocumentationIncludesTypesRequirementsDefaultsAndTemplates() {
        var accessor = field("entries", org.crebs.skpackets.elements.general.field.PacketValue[].class, false, null)
                .withNativeType(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo[].class);
        String docs = new FieldSchema<>("example", List.of(accessor), null).getReadableFields();
        assertTrue(docs.contains("`profile: packet user profile` (required)"));
        assertTrue(docs.contains("`uuid: uuid` (required)"));
        assertTrue(docs.contains("`latency: number` (optional: default: 0)"));
        assertTrue(docs.contains("```skript"));
        assertTrue(docs.contains("first:"));
    }
    @Test void alternativeBuildersDocumentRequirementsSeparately() {
        var accessor = field("dimension", org.crebs.skpackets.elements.general.field.PacketValue.class, false, null)
                .withNativeType(com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef.class);
        String docs = new FieldSchema<>("example", List.of(accessor), null).getReadableFields();
        assertTrue(docs.contains("Builder 1:"));
        assertTrue(docs.contains("Builder 2:"));
        assertTrue(docs.contains("`id: number` (required)"));
        assertTrue(docs.contains("`name: namespaced key` (required)"));
    }

    @Test void textInputsDescribeTheirFormats() {
        var schema = new FieldSchema<>("text values", List.of(
                field("nbt", String.class, false, null).withNativeType(com.github.retrooper.packetevents.protocol.nbt.NBTCompound.class),
                field("response", String.class, false, null).withNativeType(com.google.gson.JsonObject.class),
                field("timestamp", String.class, false, null).withNativeType(java.time.Instant.class),
                field("key", String.class, false, null).withNativeType(java.security.PublicKey.class)), null);
        String docs = schema.getReadableFields();
        assertTrue(docs.contains("SNBT compound text"));
        assertTrue(docs.contains("JSON object text"));
        assertTrue(docs.contains("ISO-8601 UTC timestamp"));
        assertTrue(docs.contains("Base64-encoded X.509 RSA"));
    }

    @Test void attributeModifiersDocumentAnOptionalEmptyDefault() {
        var accessor = field("properties", org.crebs.skpackets.elements.general.field.PacketValue[].class, false, null)
                .withNativeType(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property[].class);
        String docs = new FieldSchema<>("attributes", List.of(accessor), null).getReadableFields();
        assertTrue(docs.contains("optional: default: empty list"));
        assertFalse(docs.contains("modifiers: {_modifiers}"));
    }

    private static <T> FieldAccessor<Object, T> field(String name, Class<T> type, boolean optional,
                                                    Function<Object, T> getter) {
        return new FieldAccessor<>(name, new String[0], type, optional, getter, null);
    }
}
