package org.crebs.skpackets.elements.general.field;

import java.util.Map;

public record PacketRecord(Map<String, Object> fields) {
    public PacketRecord { fields = Map.copyOf(fields); }
}
