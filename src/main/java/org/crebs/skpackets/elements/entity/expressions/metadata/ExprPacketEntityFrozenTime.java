package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityFrozenTime extends ExprPacketEntityField {
    public ExprPacketEntityFrozenTime() {
        super(PacketEntityField.FROZEN_TIME);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityFrozenTime.class,
                        Object.class,
                        "[meta field] frozen (ticks|time) of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Frozen Time")
                .description(
                        "The duration the entity has been frozen in powdered snow. Supports set,"
                                + " delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-frozen-time:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field frozen time of packet entity {_mob} to 40
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
