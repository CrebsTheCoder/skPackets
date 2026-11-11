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
        assertEquals("- `ids|entity ids: numbers` (optional; creation-only)", schema.getReadableFields());
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

    private static <T> FieldAccessor<Object, T> field(String name, Class<T> type, boolean optional,
                                                    Function<Object, T> getter) {
        return new FieldAccessor<>(name, new String[0], type, optional, getter, null);
    }
}
