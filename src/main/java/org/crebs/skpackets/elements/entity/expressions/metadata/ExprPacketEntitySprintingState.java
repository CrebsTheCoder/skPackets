package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntitySprintingState extends ExprPacketEntityField {
    public ExprPacketEntitySprintingState() {
        super(PacketEntityField.SPRINTING);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntitySprintingState.class,
                        Object.class,
                        "[meta field] sprinting [state] of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Sprinting State")
                .description(
                        "The packet entity sprinting state property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-sprinting-state:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: player
                                    username: "Demo"
                                    skin: skin of player
                                    location: location of player
                                    viewers: player
                                set meta field sprinting state of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
