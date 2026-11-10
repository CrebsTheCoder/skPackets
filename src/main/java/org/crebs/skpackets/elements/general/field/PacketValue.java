package org.crebs.skpackets.elements.general.field;

import java.util.Objects;

public record PacketValue(Object value) {
    public PacketValue { Objects.requireNonNull(value); }
}
