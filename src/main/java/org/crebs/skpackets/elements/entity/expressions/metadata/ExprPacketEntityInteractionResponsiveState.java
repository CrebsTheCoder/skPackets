package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityInteractionResponsiveState extends ExprPacketEntityField {
    public ExprPacketEntityInteractionResponsiveState() {
        super(PacketEntityField.INTERACTION_RESPONSIVE);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityInteractionResponsiveState.class,
                        Object.class,
                        "[meta field] interaction responsive [state] of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Interaction Responsive State")
                .description(
                        "The packet entity interaction responsive state property. Supports"
                                + " set, delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-interaction-responsive-state:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: interaction
                                    location: location of player
                                    viewers: player
                                set meta field interaction responsive state of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
