package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityInteractionHeight extends ExprPacketEntityField {
    public ExprPacketEntityInteractionHeight() {
        super(PacketEntityField.INTERACTION_HEIGHT);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityInteractionHeight.class,
                        Object.class,
                        "meta [field] interaction height of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Interaction Height")
                .description(
                        "The packet entity interaction height property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-interaction-height:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: interaction
                                    location: location of player
                                    viewers: player
                                set meta field interaction height of packet entity {_mob} to 1
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
