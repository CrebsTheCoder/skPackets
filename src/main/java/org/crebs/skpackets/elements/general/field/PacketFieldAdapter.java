package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.function.BiFunction;
import java.util.function.Function;

record PacketFieldAdapter(Class<?> skriptType,
        BiFunction<PacketWrapper<?>, Object, Object> reader,
        Function<Object, Object> writer,
        boolean acceptsNull) {

    PacketFieldAdapter(Class<?> skriptType, BiFunction<PacketWrapper<?>, Object, Object> reader,
                       Function<Object, Object> writer) {
        this(skriptType, reader, writer, false);
    }
}
