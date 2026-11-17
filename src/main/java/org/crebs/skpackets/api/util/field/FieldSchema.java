/*
 * MIT License
 *
 * Copyright (c) 2026 3add
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.crebs.skpackets.api.util.field;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record FieldSchema<K, O>(
        K type,
        List<FieldAccessor<O, ?>> accessors,
        @Nullable Function<ConstructionContext<K, O>, O> constructor) {
    public FieldAccessor<O, ?> getAccessor(String name) {
        for (FieldAccessor<O, ?> accessor : accessors) {
            if (accessor.matches(name)) {
                return accessor;
            }
        }
        return null;
    }

    public static <K, O> String describe(
            Iterable<FieldSchema<K, O>> schemas, java.util.function.Function<K, String> name) {
        StringBuilder description = new StringBuilder("### Available Packets and their fields\n\n");
        List<FieldSchema<K, O>> sorted = new ArrayList<>();
        schemas.forEach(sorted::add);
        sorted.sort(java.util.Comparator.comparing(schema -> name.apply(schema.type())));
        for (var schema : sorted) {
            String fields = schema.getReadableFields();
            description.append("* **").append(name.apply(schema.type())).append("**");
            if (fields.isEmpty()) {
                description.append(": no fields.\n\n");
            } else {
                description.append(" fields:\n\n  <details>\n  <summary>View fields</summary>\n\n");
                for (String line : fields.split("\n")) description.append("  ").append(line).append("\n");
                description.append("\n  </details>\n\n");
            }
        }
        return description.toString();
    }

    public String getReadableFields() {
        List<String> fieldStrings = new ArrayList<>();
        for (FieldAccessor<O, ?> accessor : accessors) {
            List<String> attributes = new ArrayList<>();
            attributes.add(accessor.isOptional() ? "optional" : "required");
            if (accessor.getter() == null) attributes.add("creation-only");
            String names = accessor.name() + (accessor.aliases().length == 0 ? "" : "|" + String.join("|", accessor.aliases()));
            fieldStrings.add("- `" + names + ": " + fieldTypeName(accessor.nativeType(), accessor.expectedType()) + "` ("
                    + String.join(": ", attributes) + ")");
            Class<?> valueType = accessor.expectedType().isArray()
                    ? accessor.expectedType().getComponentType() : accessor.expectedType();
            Class<?> nativeType = org.crebs.skpackets.elements.general.field.PacketArguments.raw(
                    org.crebs.skpackets.elements.general.field.PacketRecords.elementType(accessor.nativeType()));
            String hint = textInputHint(nativeType);
            if (valueType == String.class && hint != null) fieldStrings.add("  - " + hint);
            boolean textEnum = valueType == String.class && nativeType.isEnum();
            if (valueType == String.class && com.github.retrooper.packetevents.protocol.mapper.MappedEntity.class.isAssignableFrom(nativeType))
                fieldStrings.add("  - Use a quoted namespaced registry key: unknown keys are rejected.");
            List<String> literals = textEnum ? java.util.Arrays.stream(nativeType.getEnumConstants())
                    .map(v -> "\"" + ((Enum<?>)v).name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + "\"").toList()
                    : org.crebs.skpackets.elements.general.field.PacketValueTypes.literalNames(valueType);
            if (!literals.isEmpty()) {
                fieldStrings.add("  - field values: " + literals.stream()
                        .map(value -> "`" + value + "`").collect(java.util.stream.Collectors.joining(", ")));
            } else if (org.crebs.skpackets.elements.general.field.PacketRecords.shape(accessor.nativeType()) != null) {
                var shape = org.crebs.skpackets.elements.general.field.PacketRecords.shape(accessor.nativeType());
                fieldStrings.add("  - Supports a nested " + (accessor.expectedType().isArray() ? "list of sections" : "section")
                        + (shape.alternatives().isEmpty() ? ": " + shape.fields().stream().map(f -> f.name()
                        + (f.isOptional() ? " (optional)" : " (required)")).collect(java.util.stream.Collectors.joining(", "))
                        : ": choose one of the builders below."));
                describeNested(shape, fieldStrings, "    ", new java.util.HashSet<>(java.util.Set.of(accessor.nativeType())), 0);
                fieldStrings.add("  - Nested syntax template (replace variables with values of the types above):");
                fieldStrings.add("    ```skript");
                fieldStrings.add("    " + accessor.name() + ":");
                String indent = "        ";
                if (org.crebs.skpackets.elements.general.field.PacketRecords.isList(accessor.nativeType())) {
                    fieldStrings.add(indent + "first:");
                    indent += "    ";
                }
                var example = shape.alternatives().isEmpty() ? shape : shape.alternatives().getFirst();
                if (example.fields().isEmpty()) fieldStrings.add(indent + "# No entries: builds the empty variant.");
                for (var field : example.fields()) if (!field.isOptional())
                    fieldStrings.add(indent + field.name() + ": {_" + field.name().replace(' ', '_') + "}");
                fieldStrings.add("    ```");
            } else if (valueType == org.crebs.skpackets.elements.general.field.PacketValue.class) {
                fieldStrings.add("  - No nested builder: copy a compatible value from an existing packet or use reflect.");
            }
        }
        return String.join("\n", fieldStrings);
    }

    private static void describeNested(org.crebs.skpackets.elements.general.field.PacketRecords.Shape shape,
                                       List<String> lines, String indent, java.util.Set<java.lang.reflect.Type> visited, int depth) {
        var alternatives = shape.alternatives().isEmpty() ? List.of(shape) : shape.alternatives();
        if (alternatives.size() > 1)
            lines.add(indent + "Builders are alternatives: supply fields for one builder. Equally suitable choices are rejected.");
        int index = 0;
        for (var alternative : alternatives) {
            if (alternatives.size() > 1) lines.add(indent + "- Builder " + (++index) + ":");
            if (alternative.fields().isEmpty()) lines.add(indent + "  - No entries (empty variant).");
            for (var field : alternative.fields()) {
                String defaultValue = alternative.defaults().containsKey(field.name())
                        ? ": default: " + defaultText(alternative.defaults().get(field.name())) : "";
                if (org.crebs.skpackets.elements.general.field.PacketArguments.raw(field.nativeType()) == java.util.Optional.class)
                    defaultValue = ": default: empty optional";
                lines.add(indent + "  - `" + field.name() + ": " + fieldTypeName(field.nativeType(), field.expectedType())
                        + "` (" + (field.isOptional() ? "optional" : "required") + defaultValue + ")");
                Class<?> raw = org.crebs.skpackets.elements.general.field.PacketArguments.raw(
                        org.crebs.skpackets.elements.general.field.PacketRecords.elementType(field.nativeType()));
                String hint = textInputHint(raw);
                if (field.expectedType() == String.class && hint != null) lines.add(indent + "    - " + hint);
                if (raw.isEnum() && field.expectedType() == String.class)
                    lines.add(indent + "    - Values: " + java.util.Arrays.stream(raw.getEnumConstants())
                            .map(value -> "`\"" + ((Enum<?>) value).name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + "\"`")
                            .collect(java.util.stream.Collectors.joining(", ")));
                if (org.crebs.skpackets.elements.general.field.PacketArguments.expressionType(raw) == String.class
                        && com.github.retrooper.packetevents.protocol.mapper.MappedEntity.class.isAssignableFrom(raw))
                    lines.add(indent + "    - Use a quoted namespaced registry key: unknown keys are rejected.");
                if (depth >= 3 || visited.contains(field.nativeType())) continue;
                var nested = org.crebs.skpackets.elements.general.field.PacketRecords.shape(field.nativeType());
                if (nested != null) {
                    var next = new java.util.HashSet<>(visited);
                    next.add(field.nativeType());
                    describeNested(nested, lines, indent + "    ", next, depth + 1);
                } else if (field.expectedType() == org.crebs.skpackets.elements.general.field.PacketValue.class) {
                    lines.add(indent + "    - No nested builder: copy a compatible value from an existing packet or use reflect.");
                }
            }
        }
    }

    private static String textInputHint(Class<?> type) {
        return switch (type.getName()) {
            case "com.github.retrooper.packetevents.protocol.nbt.NBT" -> "Use SNBT text, for example `{value:1}` or `[1,2]`. SkBee is not required for text inputs.";
            case "com.github.retrooper.packetevents.protocol.nbt.NBTCompound" -> "Use SNBT compound text, for example `{value:1}`. Lists and scalar roots are rejected.";
            case "com.google.gson.JsonObject" -> "Use JSON object text, for example `{\"description\":\"Hello\"}`. Arrays and scalar roots are rejected.";
            case "java.time.Instant" -> "Use an ISO-8601 UTC timestamp, for example `2026-11-15T01:02:03Z`. Fractional seconds are preserved.";
            case "java.security.PublicKey" -> "Use a Base64-encoded X.509 RSA public key (without PEM headers).";
            default -> null;
        };
    }

    private static String defaultText(Object value) {
        if (value.getClass().isArray()) return java.lang.reflect.Array.getLength(value) == 0 ? "empty list" : "list";
        return String.valueOf(value);
    }

    private static String fieldTypeName(java.lang.reflect.Type nativeType, Class<?> expectedType) {
        var raw = org.crebs.skpackets.elements.general.field.PacketArguments.raw(nativeType);
        if (raw == java.util.BitSet.class) return "bit indices (numbers)";
        if (raw == byte[][].class) return "byte arrays";
        if (raw == java.util.Optional.class)
            return fieldTypeName(org.crebs.skpackets.elements.general.field.PacketArguments.argument(nativeType, 0), expectedType);
        if (java.util.Map.class.isAssignableFrom(raw)) {
            var key = org.crebs.skpackets.elements.general.field.PacketArguments.argument(nativeType, 0);
            var value = org.crebs.skpackets.elements.general.field.PacketArguments.argument(nativeType, 1);
            return "map of " + fieldTypeName(key, org.crebs.skpackets.elements.general.field.PacketArguments.expressionType(key))
                    + " to " + fieldTypeName(value, org.crebs.skpackets.elements.general.field.PacketArguments.expressionType(value));
        }
        if (expectedType == org.crebs.skpackets.elements.general.field.PacketValue.class)
            return org.crebs.skpackets.elements.general.field.PacketValueTypes.label(raw);
        if (expectedType.isArray() && expectedType.getComponentType() == org.crebs.skpackets.elements.general.field.PacketValue.class)
            return org.crebs.skpackets.elements.general.field.PacketValueTypes.label(
                    org.crebs.skpackets.elements.general.field.PacketArguments.raw(
                            org.crebs.skpackets.elements.general.field.PacketRecords.elementType(nativeType))) + " values";
        return typeName(expectedType);
    }

    private static String typeName(Class<?> type) {
        if (type.isArray()) return pluralTypeName(type.getComponentType());
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (Number.class.isAssignableFrom(type) || type.isPrimitive() && type != char.class && type != void.class)
            return "number";
        if (type == String.class || type == Character.class || type == char.class) return "text";
        return switch (type.getName()) {
            case "java.lang.Object" -> "object";
            case "java.lang.Class", "ch.njol.skript.classes.ClassInfo" -> "type";
            case "java.util.UUID" -> "uuid";
            case "org.bukkit.util.Vector" -> "vector";
            case "org.crebs.skpackets.api.general.Vector2" -> "vector2";
            case "org.crebs.skpackets.elements.general.field.PacketValue" -> "packet value";
            case "org.bukkit.NamespacedKey" -> "namespaced key";
            case "org.bukkit.Location" -> "location";
            case "org.bukkit.inventory.EquipmentSlot" -> "equipment slot";
            case "org.bukkit.inventory.ItemStack", "ch.njol.skript.aliases.ItemType" -> "item";
            case "org.bukkit.block.data.BlockData" -> "block data";
            case "org.bukkit.GameMode" -> "game mode";
            case "org.bukkit.Difficulty" -> "difficulty";
            case "org.bukkit.SoundCategory" -> "sound category";
            case "org.bukkit.boss.BarStyle" -> "boss bar style";
            case "org.bukkit.event.player.PlayerResourcePackStatusEvent$Status" -> "resource pack state";
            case "net.kyori.adventure.text.Component" -> "text component";
            case "ch.njol.skript.util.Direction" -> "direction";
            case "org.bukkit.inventory.MainHand" -> "main hand";
            case "org.bukkit.entity.Pose" -> "entity pose";
            case "org.skriptlang.skript.bukkit.entity.EntityData" -> "entity type";
            case "org.bukkit.potion.PotionEffectType" -> "potion effect type";
            case "ch.njol.skript.util.Color" -> "color";
            case "org.bukkit.block.sign.Side" -> "sign side";
            case "org.crebs.skpackets.api.entity.Skin" -> "skin";
            case "org.crebs.skpackets.api.entity.metadata.EntityMeta" -> "entity meta";
            case "me.tofaa.entitylib.wrapper.WrapperEntity" -> "packet entity";
            case "com.github.retrooper.packetevents.protocol.player.InteractionHand" -> "interaction hand";
            case "com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityType" -> "block entity type";
            case "com.github.retrooper.packetevents.wrapper.PacketWrapper" -> "packet";
            case "com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon" -> "packet type";
            default -> org.crebs.skpackets.elements.general.field.PacketValueTypes.label(type);
        };
    }

    private static String pluralTypeName(Class<?> type) {
        String singular = typeName(type);
        return switch (singular) {
            case "difficulty" -> "difficulties";
            case "sound category" -> "sound categories";
            case "block data", "entity meta" -> singular;
            case "number", "boolean", "text", "object", "type", "uuid", "vector", "location",
                    "equipment slot", "item", "game mode", "main hand", "entity pose",
                    "entity type", "potion effect type", "color", "sign side", "skin", "packet entity",
                    "interaction hand", "block entity type", "packet", "packet type" -> singular + "s";
            default -> singular + " values";
        };
    }
}
