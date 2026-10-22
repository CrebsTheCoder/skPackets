package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityGravity extends ExprPacketEntityField {
    public ExprPacketEntityGravity() {
        super(PacketEntityField.GRAVITY);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityGravity.class,
                        Object.class,
                        "[meta field] gravity of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Gravity")
                .description(
                        "The packet entity gravity property. Supports set, delete and reset;"
                                + " unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-gravity:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field gravity of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
