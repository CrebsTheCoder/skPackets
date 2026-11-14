package org.crebs.skpackets.elements.general.field;

import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PacketBuilderSelectionTest {
    private FieldAccessor<Object, ?> field(String name, Class<?> type, boolean optional) {
        return new FieldAccessor<>(name, new String[0], Object.class, optional, null, null, type);
    }
    private PacketRecords.Shape shape(FieldAccessor<Object, ?>... fields) {
        return new PacketRecords.Shape(List.of(fields), Map.of(), v -> v);
    }
    @Test void selectsUsingSuppliedFieldsAndPrefersFewerOmissions() {
        var shortBuilder = shape(field("name", String.class, false));
        var longBuilder = shape(field("name", String.class, false), field("extra", String.class, true));
        var builders = List.of(shortBuilder, longBuilder);
        assertSame(shortBuilder, PacketValueDiscovery.select(builders, Set.of("name"), "value"));
        assertSame(longBuilder, PacketValueDiscovery.select(builders, Set.of("name", "extra"), "value"));
        assertThrows(IllegalArgumentException.class, () -> PacketValueDiscovery.select(builders, Set.of(), "value"));
        assertThrows(IllegalArgumentException.class, () -> PacketValueDiscovery.select(builders, Set.of("unknown"), "value"));
    }
    @Test void reportsAmbiguityWithTypesAndFieldPath() {
        var error = assertThrows(IllegalArgumentException.class, () -> PacketValueDiscovery.select(List.of(
                shape(field("value", String.class, false)), shape(field("value", int.class, false))),
                Set.of("value"), "entries.first"));
        assertTrue(error.getMessage().contains("Ambiguous"));
        assertTrue(error.getMessage().contains("entries.first"));
        assertTrue(error.getMessage().contains("java.lang.String"));
        assertTrue(error.getMessage().contains("int"));
    }
    @Test void suppliesEmptyOptionalAndRetainsNullableOmission() {
        var builder = shape(field("optional", Optional.class, true), field("nullable", String.class, true));
        var values = (Map<?, ?>) PacketRecords.construct(Map.of(), builder, "value");
        assertEquals(Optional.empty(), values.get("optional"));
        assertNull(values.get("nullable"));
        assertThrows(IllegalArgumentException.class, () -> PacketRecords.construct(Map.of(),
                shape(field("required", String.class, false)), "value"));
    }
    @Test void recognizesOnlyTopLevelNullableParameters() {
        var method = new org.objectweb.asm.tree.MethodNode();
        int reference = org.objectweb.asm.TypeReference.newFormalParameterReference(0).getValue();
        method.invisibleTypeAnnotations = new ArrayList<>();
        method.invisibleTypeAnnotations.add(new org.objectweb.asm.tree.TypeAnnotationNode(reference,
                null, "Lorg/jetbrains/annotations/Nullable;"));
        assertTrue(PacketConstructor.nullable(method, 0));
        assertFalse(PacketConstructor.nullable(method, 1));
        method.invisibleTypeAnnotations.clear();
        method.invisibleTypeAnnotations.add(new org.objectweb.asm.tree.TypeAnnotationNode(reference,
                org.objectweb.asm.TypePath.fromString("0;"), "Lorg/jetbrains/annotations/Nullable;"));
        assertFalse(PacketConstructor.nullable(method, 0));
    }
    @Test void discoversBothUserProfileSignatures() {
        var shape = PacketValueDiscovery.discover(com.github.retrooper.packetevents.protocol.player.UserProfile.class);
        var uuid = UUID.randomUUID();
        var profile = (com.github.retrooper.packetevents.protocol.player.UserProfile)
                shape.factory().apply(Map.of("uuid", uuid, "name", "Builder"));
        assertEquals(uuid, profile.getUUID());
        assertEquals("Builder", profile.getName());
    }
}
