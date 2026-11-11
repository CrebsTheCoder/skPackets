package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.recipe.data.MerchantOffer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import org.crebs.skpackets.api.util.field.FieldAccessor;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public final class PacketRecords {
    private PacketRecords() {}
    public record Shape(List<FieldAccessor<Object, ?>> fields, Map<String, Object> defaults,
                        Function<Map<String, Object>, Object> factory) {}
    private static final Map<Class<?>, Shape> SHAPES = new HashMap<>();
    static {
        define(Equipment.class, List.of(
                field("slot", com.github.retrooper.packetevents.protocol.player.EquipmentSlot.class, false),
                field("item", com.github.retrooper.packetevents.protocol.item.ItemStack.class, false)), Map.of(),
                v -> new Equipment((com.github.retrooper.packetevents.protocol.player.EquipmentSlot)v.get("slot"),
                        (com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("item")));
        define(UserProfile.class, List.of(field("uuid", UUID.class, false), field("name", String.class, false)), Map.of(),
                v -> new UserProfile((UUID)v.get("uuid"), (String)v.get("name")));
        define(PlayerInfo.class, List.of(
                field("profile", UserProfile.class, false), field("listed", boolean.class, true),
                field("latency", int.class, true), field("game mode", com.github.retrooper.packetevents.protocol.player.GameMode.class, true),
                field("display name", net.kyori.adventure.text.Component.class, true), field("list order", int.class, true),
                field("show hat", boolean.class, true)),
                Map.of("listed", true, "latency", 0, "game mode", org.bukkit.GameMode.SURVIVAL, "list order", 0, "show hat", true),
                v -> new PlayerInfo((UserProfile)v.get("profile"), (boolean)v.get("listed"), (int)v.get("latency"),
                        (com.github.retrooper.packetevents.protocol.player.GameMode)v.get("game mode"),
                        (net.kyori.adventure.text.Component)v.get("display name"), null, (int)v.get("list order"), (boolean)v.get("show hat")));
        define(MerchantOffer.class, List.of(
                field("input", com.github.retrooper.packetevents.protocol.item.ItemStack.class, false),
                field("second input", com.github.retrooper.packetevents.protocol.item.ItemStack.class, true),
                field("output", com.github.retrooper.packetevents.protocol.item.ItemStack.class, false),
                field("uses", int.class, true), field("max uses", int.class, true), field("xp", int.class, true),
                field("special price", int.class, true), field("price multiplier", float.class, true), field("demand", int.class, true)),
                Map.of("uses", 0, "max uses", 12, "xp", 0, "special price", 0, "price multiplier", 0f, "demand", 0),
                v -> MerchantOffer.of((com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("input"),
                        (com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("second input"),
                        (com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("output"),
                        (int)v.get("uses"), (int)v.get("max uses"), (int)v.get("xp"), (int)v.get("special price"),
                        (float)v.get("price multiplier"), (int)v.get("demand")));
    }
    private static void define(Class<?> type, List<FieldAccessor<Object, ?>> fields, Map<String, Object> defaults,
                               Function<Map<String, Object>, Object> factory) {
        SHAPES.put(type, new Shape(List.copyOf(fields), Map.copyOf(defaults), factory));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static FieldAccessor<Object, ?> field(String name, Type nativeType, boolean optional) {
        return new FieldAccessor(name, new String[0], PacketArguments.expressionType(nativeType), optional, null, null, nativeType);
    }
    public static Type elementType(Type type) {
        Class<?> raw = PacketArguments.raw(type);
        if (raw == Optional.class) return elementType(PacketArguments.argument(type, 0));
        if (raw.isArray()) return raw.getComponentType();
        if (Collection.class.isAssignableFrom(raw)) return PacketArguments.argument(type, 0);
        return type;
    }
    public static boolean isList(Type type) {
        Class<?> raw = PacketArguments.raw(type);
        if (raw == Optional.class) return isList(PacketArguments.argument(type, 0));
        return raw.isArray() || Collection.class.isAssignableFrom(raw);
    }
    public static Shape shape(Type type) { return SHAPES.get(PacketArguments.raw(elementType(type))); }
    public static Object construct(PacketRecord record, Type type, String name) {
        Shape shape = SHAPES.get(PacketArguments.raw(type));
        if (shape == null) throw new IllegalArgumentException("Packet field '" + name + "' does not support nested entries.");
        Map<String, Object> values = new HashMap<>(shape.defaults());
        for (String key : record.fields().keySet()) {
            if (shape.fields().stream().noneMatch(f -> f.name().equals(key)))
                throw new IllegalArgumentException("Unknown nested packet field '" + name + "." + key + "'.");
        }
        values.putAll(record.fields());
        Map<String, Object> converted = new HashMap<>();
        for (var field : shape.fields()) {
            Object value = values.get(field.name());
            if (value == null && !field.isOptional()) throw new IllegalArgumentException("Missing packet field '" + name + "." + field.name() + "'.");
            if (value != null) converted.put(field.name(), PacketArguments.convert(value, field.nativeType(), name + "." + field.name()));
        }
        return shape.factory().apply(converted);
    }
}
