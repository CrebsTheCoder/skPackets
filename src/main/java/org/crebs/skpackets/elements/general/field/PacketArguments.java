package org.crebs.skpackets.elements.general.field;

import ch.njol.skript.registrations.Classes;
import com.github.retrooper.packetevents.util.Vector2i;
import org.bukkit.util.Vector;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketArguments {
    private static final Map<Class<?>, Class<?>> PUBLIC_TYPES = new ConcurrentHashMap<>();

    private PacketArguments() {}

    public static Class<?> expressionType(Type type) {
        Class<?> raw = raw(type);
        if (raw == Optional.class) return expressionType(argument(type, 0));
        if (raw.isArray()) return arrayType(expressionType(raw.getComponentType()));
        if (Collection.class.isAssignableFrom(raw)) return arrayType(expressionType(argument(type, 0)));
        return PUBLIC_TYPES.computeIfAbsent(raw, PacketArguments::scalarType);
    }

    private static Class<?> scalarType(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) return Boolean.class;
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) return Number.class;
        if (type == String.class) return String.class;
        var codec = PacketValueCodecs.find(type);
        if (codec != null) return codec.skriptType();
        if (type.getName().startsWith("org.bukkit.") || type.getName().startsWith("ch.njol.skript.")
                || type.getName().startsWith("org.skriptlang.skript.")) return type;
        if (Classes.getExactClassInfo(type) != null) return type;
        if (type.isEnum() && !PacketValueTypes.managedByModule(type)) return String.class;
        if (type == Object.class || type == UUID.class || PacketValueTypes.managedByModule(type)) return type;
        return PacketValue.class;
    }

    public static Class<?> raw(Type type) {
        return switch (type) {
            case Class<?> value -> value;
            case ParameterizedType value -> raw(value.getRawType());
            case WildcardType value -> raw(value.getUpperBounds()[0]);
            case TypeVariable<?> value -> raw(value.getBounds()[0]);
            case GenericArrayType value -> arrayType(raw(value.getGenericComponentType()));
            default -> throw new IllegalArgumentException("Unsupported PacketEvents value type: " + type);
        };
    }

    public static Type argument(Type type, int index) {
        return type instanceof ParameterizedType value ? value.getActualTypeArguments()[index] : Object.class;
    }

    public static Object convert(Object value, Type type, String name) {
        if (value instanceof PacketValue wrapped) value = wrapped.value();
        Class<?> target = raw(type);
        if (target == Optional.class) {
            Object input = value instanceof Optional<?> optional ? optional.orElse(null) : value;
            return input == null ? Optional.empty() : Optional.of(convert(input, argument(type, 0), name));
        }
        if (value instanceof PacketRecord record) value = PacketRecords.construct(record, type, name);
        if (value == null) throw invalid(name, type, null);
        if (target.isArray()) return convertArray(value, type, name);
        if (Collection.class.isAssignableFrom(target)) return convertCollection(value, type, name);
        if (Map.class.isAssignableFrom(target)) return convertMap(value, type, name);
        if (value instanceof Number number && (target.isPrimitive() || Number.class.isAssignableFrom(target)))
            return PacketNumbers.convert(number, target, name);
        if (target == boolean.class && value instanceof Boolean) return value;
        if (target.isInstance(value)) return value;
        if (target.isEnum() && expressionType(target) == String.class && value instanceof String text)
            return convertEnum(text, target, name);
        if (target == Vector2i.class && value instanceof Vector vector)
            return new Vector2i(vector.getBlockX(), vector.getBlockZ());
        if (target == com.github.retrooper.packetevents.protocol.world.BlockFace.class && value instanceof org.bukkit.block.BlockFace face)
            return face == org.bukkit.block.BlockFace.SELF ? com.github.retrooper.packetevents.protocol.world.BlockFace.OTHER
                    : com.github.retrooper.packetevents.protocol.world.BlockFace.valueOf(face.name());
        var codec = PacketValueCodecs.find(target);
        if (codec != null) return codec.write(value, name);
        throw invalid(name, type, value);
    }

    private static Object convertArray(Object value, Type type, String name) {
        Object[] input = elements(value);
        Type component = type instanceof GenericArrayType array ? array.getGenericComponentType() : raw(type).getComponentType();
        Object result = Array.newInstance(raw(component), input.length);
        for (int i = 0; i < input.length; i++) Array.set(result, i, convert(input[i], component, name + "[" + i + "]"));
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object convertCollection(Object value, Type type, String name) {
        Class<?> target = raw(type);
        Type elementType = argument(type, 0);
        Collection result;
        if (target == EnumSet.class) {
            Class<?> element = raw(elementType);
            if (!element.isEnum()) throw invalid(name, type, value);
            result = EnumSet.noneOf((Class<? extends Enum>) element);
        } else {
            result = Set.class.isAssignableFrom(target) ? new LinkedHashSet<>() : new ArrayList<>();
        }
        for (Object element : elements(value)) result.add(convert(element, elementType, name + " entry"));
        if (!target.isInstance(result)) throw invalid(name, type, value);
        return result;
    }

    private static Object convertMap(Object value, Type type, String name) {
        if (!(value instanceof Map<?, ?> input)) throw invalid(name, type, value);
        Map<Object, Object> result = new LinkedHashMap<>();
        input.forEach((key, entry) -> result.put(convert(key, argument(type, 0), name + " key"),
                convert(entry, argument(type, 1), name + " value")));
        if (!raw(type).isInstance(result)) throw invalid(name, type, value);
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object convertEnum(String text, Class<?> type, String name) {
        try {
            return Enum.valueOf((Class) type, text.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            List<String> choices = Arrays.stream(type.getEnumConstants()).map(value -> enumName((Enum<?>) value)).toList();
            throw new IllegalArgumentException("Invalid value for packet field '" + name + "': '" + text + "'. Expected: " + choices);
        }
    }

    static Object toSkript(Object value) {
        if (value == null) return null;
        if (value instanceof Optional<?> optional) return toSkript(optional.orElse(null));
        if (value instanceof Collection<?> collection) return collection.stream().map(PacketArguments::toSkript).toArray();
        if (value.getClass().isArray()) return Arrays.stream(elements(value)).map(PacketArguments::toSkript).toArray();
        var codec = PacketValueCodecs.find(value.getClass());
        if (codec != null) return codec.read(value);
        if (value instanceof Enum<?> entry && expressionType(entry.getDeclaringClass()) == String.class) return enumName(entry);
        return expressionType(value.getClass()) == PacketValue.class ? new PacketValue(value) : value;
    }

    private static Object[] elements(Object value) {
        if (value instanceof Collection<?> collection) return collection.toArray();
        if (!value.getClass().isArray()) return new Object[]{value};
        Object[] result = new Object[Array.getLength(value)];
        for (int i = 0; i < result.length; i++) result[i] = Array.get(value, i);
        return result;
    }

    private static Class<?> arrayType(Class<?> component) {
        return Array.newInstance(component, 0).getClass();
    }

    private static String enumName(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    static IllegalArgumentException invalid(String name, Type type, Object value) {
        return new IllegalArgumentException("Packet field '" + name + "' requires " + type.getTypeName()
                + "; received " + (value == null ? "nothing" : value.getClass().getTypeName()));
    }
}
