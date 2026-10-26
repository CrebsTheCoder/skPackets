package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayScale extends ExprPacketEntityField {
    public ExprPacketEntityDisplayScale() {
        super(PacketEntityField.SCALE);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayScale.class,
                        Object.class,
                        "meta [field] display scale of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display Scale")
                .description(
                        "The display transformation scale vector. Supports set, delete and reset;"
                                + " unsupported entity types are rejected.")
                .examples(
                        """
                        command /giant-hologram:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display scale of packet entity {_mob} to vector(2, 2, 2)
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
