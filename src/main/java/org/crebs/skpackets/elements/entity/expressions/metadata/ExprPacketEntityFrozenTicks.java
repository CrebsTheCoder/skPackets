package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityFrozenTicks extends ExprPacketEntityField {
    public ExprPacketEntityFrozenTicks() {
        super(PacketEntityField.FROZEN_TIME);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityFrozenTicks.class,
                        Object.class,
                        "[meta field] frozen ticks of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Frozen Ticks")
                .description(
                        "The packet entity frozen ticks property. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-frozen-ticks:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field frozen ticks of packet entity {_mob} to 40
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
