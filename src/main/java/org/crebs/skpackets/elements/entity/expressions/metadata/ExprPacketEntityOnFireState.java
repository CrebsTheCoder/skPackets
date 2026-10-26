package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityOnFireState extends ExprPacketEntityField {
    public ExprPacketEntityOnFireState() {
        super(PacketEntityField.ON_FIRE);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityOnFireState.class,
                        Object.class,
                        "[meta field] on fire [state] of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - On Fire State")
                .description(
                        "The packet entity on fire state property. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-on-fire-state:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field on fire state of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
