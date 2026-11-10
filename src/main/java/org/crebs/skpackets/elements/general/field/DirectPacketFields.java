package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

final class DirectPacketFields implements FieldRegistrar {
    @Override public void register() {
        PacketFieldRegistry.INSTANCE.builder(PacketType.Play.Server.KEEP_ALIVE, WrapperPlayServerKeepAlive.class)
                .requiredField(Number.class, WrapperPlayServerKeepAlive::getId,
                        (packet, value) -> packet.setId(id(value)), "id")
                .nativeType(long.class)
                .constructor(values -> new WrapperPlayServerKeepAlive(id(values.getRequired("id", Number.class))))
                .build();
        PacketFieldRegistry.INSTANCE.builder(PacketType.Play.Client.KEEP_ALIVE, WrapperPlayClientKeepAlive.class)
                .requiredField(Number.class, WrapperPlayClientKeepAlive::getId,
                        (packet, value) -> packet.setId(id(value)), "id")
                .nativeType(long.class)
                .constructor(values -> new WrapperPlayClientKeepAlive(id(values.getRequired("id", Number.class))))
                .build();
    }

    private static long id(Number value) {
        return (long) PacketNumbers.convert(value, long.class, "id");
    }
}
