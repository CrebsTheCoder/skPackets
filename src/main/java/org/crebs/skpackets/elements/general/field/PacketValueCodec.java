package org.crebs.skpackets.elements.general.field;

import java.util.function.BiFunction;
import java.util.function.Function;

record PacketValueCodec<P, S>(
        Class<P> packetType,
        Class<S> skriptType,
        Function<P, S> reader,
        BiFunction<S, String, P> writer) {

    static <P, S> PacketValueCodec<P, S> of(Class<P> packetType, Class<S> skriptType,
                                           Function<P, S> reader, Function<S, P> writer) {
        return new PacketValueCodec<>(packetType, skriptType, reader, (value, name) -> writer.apply(value));
    }

    Object read(Object value) {
        return reader.apply(packetType.cast(value));
    }

    Object write(Object value, String name) {
        if (!skriptType.isInstance(value)) throw PacketArguments.invalid(name, packetType, value);
        return writer.apply(skriptType.cast(value), name);
    }
}
