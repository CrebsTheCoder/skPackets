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
            fieldStrings.add("- `" + names + ": " + typeName(accessor.expectedType()) + "` ("
                    + String.join("; ", attributes) + ")");
            Class<?> valueType = accessor.expectedType().isArray()
                    ? accessor.expectedType().getComponentType() : accessor.expectedType();
            Class<?> nativeType = org.crebs.skpackets.elements.general.field.PacketArguments.raw(
                    org.crebs.skpackets.elements.general.field.PacketRecords.elementType(accessor.nativeType()));
            boolean textEnum = valueType == String.class && nativeType.isEnum();
            List<String> literals = textEnum ? java.util.Arrays.stream(nativeType.getEnumConstants())
                    .map(v -> "\"" + ((Enum<?>)v).name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + "\"").toList()
                    : org.crebs.skpackets.elements.general.field.PacketValueTypes.literalNames(valueType);
            if (!literals.isEmpty()) {
                fieldStrings.add("  - field values: " + literals.stream()
                        .map(value -> "`" + value + "`").collect(java.util.stream.Collectors.joining(", ")));
            } else if (org.crebs.skpackets.elements.general.field.PacketRecords.shape(accessor.nativeType()) != null) {
                var shape = org.crebs.skpackets.elements.general.field.PacketRecords.shape(accessor.nativeType());
                fieldStrings.add("  - Supports a nested " + (accessor.expectedType().isArray() ? "list of sections" : "section")
                        + ": " + shape.fields().stream().map(f -> f.name() + (f.isOptional() ? " (optional)" : " (required)")).collect(java.util.stream.Collectors.joining(", ")));
            }
        }
        return String.join("\n", fieldStrings);
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
