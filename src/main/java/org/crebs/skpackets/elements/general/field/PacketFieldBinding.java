package org.crebs.skpackets.elements.general.field;

import org.crebs.skpackets.api.general.packet.PacketAccessManager;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.crebs.skpackets.api.util.field.BaseFieldRegistry;
import org.crebs.skpackets.api.util.field.ConstructionContext;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.BiConsumer;

record PacketFieldBinding(
        String name,
        List<String> aliases,
        Type nativeType,
        boolean optional,
        boolean nullable,
        Function<Object, Object> getter,
        BiConsumer<Object, Object> setter,
        PacketFieldRule rule) {

    static PacketFieldBinding parameter(Class<?> wrapper, PacketConstructor.Argument argument, List<Method> methods) {
        var rule = PacketFieldRules.find(wrapper, argument.name());
        String property = PacketReflection.property(wrapper, argument.name());
        Method getter = PacketReflection.getter(methods, property, argument.type());
        Method setter = PacketReflection.setter(methods, property, PacketArguments.raw(argument.type()));
        if (getter == null) getter = PacketReflection.getter(methods, argument.name(), argument.type());
        if (setter == null) setter = PacketReflection.setter(methods, argument.name(), PacketArguments.raw(argument.type()));

        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(rule.name().isEmpty() ? PacketReflection.fieldName(argument.name()) : rule.name());
        names.add(PacketReflection.fieldName(property));
        if (getter != null) names.add(PacketReflection.fieldName(getter.getName().replaceFirst("^(get|is|has)", "")));
        String name = names.removeFirst();
        boolean optional = argument.nullable() || PacketArguments.raw(argument.type()) == Optional.class || rule.defaultValue() != null;
        return new PacketFieldBinding(name, List.copyOf(names), argument.type(), optional, argument.nullable(), reader(getter), writer(setter), rule);
    }

    static PacketFieldBinding extra(Method getter, Method setter) {
        String name = PacketReflection.fieldName(setter.getName().substring(3));
        return new PacketFieldBinding(name, List.of(), setter.getGenericParameterTypes()[0], true, false, reader(getter), writer(setter), PacketFieldRule.DEFAULT);
    }

    private static Function<Object, Object> reader(Method getter) {
        return getter == null ? null : PacketAccessManager.reader(getter);
    }

    private static BiConsumer<Object, Object> writer(Method setter) {
        return setter == null ? null : PacketAccessManager.writer(setter);
    }

    Class<?> skriptType() {
        return rule.adapter() == null ? PacketArguments.expressionType(nativeType) : rule.adapter().skriptType();
    }

    List<String> names() {
        List<String> names = new ArrayList<>();
        names.add(name);
        names.addAll(aliases);
        return names;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void register(BaseFieldRegistry.Builder builder) {
        java.util.function.Function read = getter == null ? null : (Object packet) -> read((PacketWrapper<?>) packet);
        java.util.function.BiConsumer write = setter == null ? null : (Object packet, Object value) -> write((PacketWrapper<?>) packet, value);
        if (optional) builder.optionalField(skriptType(), read, write, names().toArray(String[]::new));
        else builder.requiredField(skriptType(), read, write, names().toArray(String[]::new));
        builder.nativeType(nativeType);
    }

    Object constructorValue(ConstructionContext<PacketTypeCommon, ?> values) {
        Object value = optional ? values.getOptional(name, Object.class) : values.getRequired(name, Object.class);
        if (value == null && rule.defaultValue() != null) value = rule.defaultValue().get();
        return value == null && nullable ? null : encode(value);
    }

    Object read(PacketWrapper<?> packet) {
        Object value = getter.apply(packet);
        if (rule.adapter() != null) return rule.adapter().reader().apply(packet, value);
        return typedArray(PacketArguments.toSkript(value), skriptType());
    }

    void write(PacketWrapper<?> packet, Object value) {
        setter.accept(packet, encode(value));
    }

    private Object encode(Object value) {
        if (rule.adapter() != null && (rule.adapter().skriptType().isInstance(value)
                || value == null && rule.adapter().acceptsNull())) value = rule.adapter().writer().apply(value);
        return PacketArguments.convert(value, nativeType, name);
    }

    private static Object typedArray(Object value, Class<?> type) {
        if (value == null || !type.isArray() || type.isInstance(value)) return value;
        Object result = Array.newInstance(type.getComponentType(), Array.getLength(value));
        for (int i = 0; i < Array.getLength(value); i++) Array.set(result, i, typedArray(Array.get(value, i), type.getComponentType()));
        return result;
    }
}
