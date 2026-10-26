package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityInteractionWidth extends ExprPacketEntityField {
    public ExprPacketEntityInteractionWidth() {
        super(PacketEntityField.INTERACTION_WIDTH);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityInteractionWidth.class,
                        Object.class,
                        "meta [field] interaction width of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Interaction Width")
                .description(
                        "The packet entity interaction width property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-interaction-width:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: interaction
                                    location: location of player
                                    viewers: player
                                set meta field interaction width of packet entity {_mob} to 1
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
