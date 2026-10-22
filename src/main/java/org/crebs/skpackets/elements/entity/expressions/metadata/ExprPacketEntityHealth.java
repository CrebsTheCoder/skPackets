package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityHealth extends ExprPacketEntityField {
    public ExprPacketEntityHealth() {
        super(PacketEntityField.HEALTH);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityHealth.class,
                        Object.class,
                        "[meta field] health of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Health")
                .description(
                        "The living entity health metadata value. Supports set, delete and reset;"
                                + " unsupported entity types are rejected.")
                .examples(
                        """
                        command /healthy-mob:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field health of packet entity {_mob} to 20
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
