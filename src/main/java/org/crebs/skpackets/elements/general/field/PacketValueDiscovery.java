package org.crebs.skpackets.elements.general.field;

import org.crebs.skpackets.api.util.field.FieldAccessor;
import java.lang.reflect.*;
import java.util.*;

final class PacketValueDiscovery {
    private PacketValueDiscovery() {}

    static PacketRecords.Shape discover(Class<?> type) {
        if (!type.getName().startsWith("com.github.retrooper.packetevents.")
                || type.isEnum() || type.isInterface() || Modifier.isAbstract(type.getModifiers())) return null;
        List<Executable> candidates = new ArrayList<>();
        for (Constructor<?> constructor : type.getConstructors())
            if (!constructor.isAnnotationPresent(Deprecated.class)) candidates.add(constructor);
        for (Method method : type.getMethods())
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == type
                    && !method.isAnnotationPresent(Deprecated.class)
                    && (method.getName().startsWith("create") || method.getName().equals("of"))) candidates.add(method);
        candidates.removeIf(value -> value.getParameterCount() == 0
                || Arrays.stream(value.getParameters()).anyMatch(parameter -> !parameter.isNamePresent()
                    || parameter.getType() == type
                    || parameter.getType().getSimpleName().endsWith("Event")
                    || parameter.getType().getSimpleName().equals("ClientVersion")
                    || parameter.getType().getSimpleName().equals("ServerVersion")));
        candidates.sort(Comparator.<Executable>comparingInt(value -> (int) Arrays.stream(value.getGenericParameterTypes())
                .filter(t -> PacketArguments.expressionType(t) == PacketValue.class).count())
                .thenComparing(Comparator.comparingInt(Executable::getParameterCount).reversed())
                .thenComparing(Executable::toGenericString));
        if (candidates.isEmpty()) return null;
        Executable selected = candidates.getFirst();
        List<FieldAccessor<Object, ?>> fields = new ArrayList<>();
        for (Parameter parameter : selected.getParameters())
            fields.add(field(PacketReflection.fieldName(parameter.getName()), parameter.getParameterizedType()));
        Map<String, Object> defaults = new HashMap<>();
        for (var field : fields) {
            Class<?> raw = PacketArguments.raw(field.nativeType());
            if (raw == BitSet.class) defaults.put(field.name(), new Number[0]);
            else if (raw.isArray()) defaults.put(field.name(), java.lang.reflect.Array.newInstance(
                    PacketArguments.expressionType(raw).getComponentType(), 0));
        }
        return new PacketRecords.Shape(List.copyOf(fields), defaults, values -> {
            Object[] args = fields.stream().map(field -> values.get(field.name())).toArray();
            try {
                Object result = selected instanceof Constructor<?> constructor ? constructor.newInstance(args)
                        : ((Method) selected).invoke(null, args);
                return result;
            } catch (ReflectiveOperationException exception) {
                Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
                throw new IllegalArgumentException("Cannot build " + PacketValueTypes.label(type) + ": " + cause.getMessage(), cause);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static FieldAccessor<Object, ?> field(String name, Type type) {
        return new FieldAccessor(name, new String[0], PacketArguments.expressionType(type), PacketArguments.raw(type) == BitSet.class || PacketArguments.raw(type).isArray(), null, null, type);
    }
}
