package org.crebs.skpackets.elements.general.field;

import java.util.function.Supplier;

record PacketFieldRule(String name, Supplier<?> defaultValue, PacketFieldAdapter adapter) {
    static final PacketFieldRule DEFAULT = new PacketFieldRule("", null, null);

    static PacketFieldRule named(String name) {
        return new PacketFieldRule(name, null, null);
    }

    PacketFieldRule defaulting(Supplier<?> value) {
        return new PacketFieldRule(name, value, adapter);
    }

    PacketFieldRule adapting(PacketFieldAdapter value) {
        return new PacketFieldRule(name, defaultValue, value);
    }
}
