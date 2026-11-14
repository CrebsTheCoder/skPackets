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
        candidates.sort(Comparator.comparing(Executable::toGenericString));
        if (candidates.isEmpty()) return null;
        List<PacketRecords.Shape> shapes = candidates.stream().map(PacketValueDiscovery::shape).toList();
        Map<String, FieldAccessor<Object, ?>> fields = new LinkedHashMap<>();
        for (var shape : shapes) for (var field : shape.fields())
            fields.putIfAbsent(field.name(), field(field.name(), field.nativeType(), true));
        return new PacketRecords.Shape(List.copyOf(fields.values()), Map.of(), values ->
                PacketRecords.construct(values, select(shapes, values.keySet(), type.getSimpleName()), type.getSimpleName()), shapes);
    }

    static PacketRecords.Shape select(List<PacketRecords.Shape> shapes, Set<String> supplied, String name) {
        List<PacketRecords.Shape> matches = shapes.stream().filter(shape -> {
            Set<String> names = new HashSet<>();
            for (var field : shape.fields()) {
                names.add(field.name());
                if (!field.isOptional() && !supplied.contains(field.name())) return false;
            }
            return names.containsAll(supplied);
        }).toList();
        // Prefer the builder that needs the fewest omitted optional inputs.
        int size = matches.stream().mapToInt(shape -> shape.fields().size()).min().orElse(-1);
        matches = matches.stream().filter(shape -> shape.fields().size() == size).toList();
        if (matches.size() != 1) throw new IllegalArgumentException(
                (matches.isEmpty() ? "No matching" : "Ambiguous") + " nested builder for '" + name
                + "' with fields " + new TreeSet<>(supplied) + ". Available builders: "
                + shapes.stream().map(shape -> shape.fields().stream().map(field -> field.name()
                    + ": " + field.nativeType().getTypeName() + (field.isOptional() ? " (optional)" : " (required)")).toList()).toList());
        return matches.getFirst();
    }

    private static PacketRecords.Shape shape(Executable selected) {
        List<FieldAccessor<Object, ?>> fields = new ArrayList<>();
        for (Parameter parameter : selected.getParameters())
            fields.add(field(PacketReflection.fieldName(parameter.getName()), parameter.getParameterizedType(),
                    nullable(selected, fields.size()) || parameter.getType() == Optional.class
                    || parameter.getType() == BitSet.class || parameter.getType().isArray()));
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
                throw new IllegalArgumentException("Cannot build " + PacketValueTypes.label(selected.getDeclaringClass()) + ": " + cause.getMessage(), cause);
            }
        });
    }

    private static boolean nullable(Executable executable, int index) {
        Class<?> owner = executable.getDeclaringClass();
        try (var stream = owner.getResourceAsStream("/" + owner.getName().replace('.', '/') + ".class")) {
            if (stream == null) throw new IllegalStateException("Cannot read " + owner.getName());
            var node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(node, org.objectweb.asm.ClassReader.SKIP_CODE
                    | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
            String descriptor = executable instanceof Constructor<?> constructor
                    ? org.objectweb.asm.Type.getConstructorDescriptor(constructor)
                    : org.objectweb.asm.Type.getMethodDescriptor((Method) executable);
            for (var method : node.methods) if (method.desc.equals(descriptor)
                    && method.name.equals(executable instanceof Constructor<?> ? "<init>" : executable.getName()))
                return PacketConstructor.nullable(method, index);
            return false;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read " + owner.getName(), exception);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static FieldAccessor<Object, ?> field(String name, Type type, boolean optional) {
        return new FieldAccessor(name, new String[0], PacketArguments.expressionType(type), optional, null, null, type);
    }
}
