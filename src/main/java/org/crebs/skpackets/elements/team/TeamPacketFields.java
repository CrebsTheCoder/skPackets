package org.crebs.skpackets.elements.team;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;

import org.crebs.skpackets.api.util.field.FieldRegistrar;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;

public final class TeamPacketFields implements FieldRegistrar {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register() {
        var builder =
                PacketFieldRegistry.INSTANCE.builder(
                        PacketType.Play.Server.TEAMS, WrapperPlayServerTeams.class);
        for (TeamField field : TeamField.values()) {
            if (field == TeamField.NAME) {
                builder.requiredField(
                        String.class,
                        WrapperPlayServerTeams::getTeamName,
                        (packet, value) -> field.set(packet, value),
                        "name",
                        "team name");
            } else {
                builder.optionalField(
                        (Class) field.type(),
                        packet -> field.get(packet),
                        (packet, value) -> field.set(packet, value),
                        field.key());
            }
        }
        builder.constructor(
                        values -> {
                            var packet = TeamField.create(values.getRequired("name", String.class));
                            for (TeamField field : TeamField.values()) {
                                Object value = values.getOptional(field.key(), field.type());
                                if (value != null) {
                                    field.set(packet, value);
                                }
                            }
                            return packet;
                        })
                .build();
    }
}
