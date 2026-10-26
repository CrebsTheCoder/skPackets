package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityGravityState extends ExprPacketEntityField {
    public ExprPacketEntityGravityState() {
        super(PacketEntityField.GRAVITY);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityGravityState.class,
                        Object.class,
                        "meta [field] gravity [state] of (fake|packet) entity %packetentities%")
                .name("Packet Entity - Gravity State")
                .description(
                        "True enables gravity; false disables it. Supports set, delete and reset;"
                                + " unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-gravity-state:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field gravity state of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
