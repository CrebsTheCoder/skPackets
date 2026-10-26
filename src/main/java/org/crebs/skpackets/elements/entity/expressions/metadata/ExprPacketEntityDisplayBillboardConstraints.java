package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayBillboardConstraints extends ExprPacketEntityField {
    public ExprPacketEntityDisplayBillboardConstraints() {
        super(PacketEntityField.BILLBOARD);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayBillboardConstraints.class,
                        Object.class,
                        "meta [field] display billboard [constraints] of (fake|packet) entity %packetentities%")
                .name("Packet Entity - Display Billboard Constraints")
                .description(
                        "The packet entity display billboard constraints property. Supports"
                                + " set, delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /follow-my-view:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display billboard constraints of packet entity {_mob} to "center"
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
