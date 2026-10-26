package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayBackgroundColor extends ExprPacketEntityField {
    public ExprPacketEntityDisplayBackgroundColor() {
        super(PacketEntityField.BACKGROUND_COLOR);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayBackgroundColor.class,
                        Object.class,
                        "meta [field] display background color of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display Background Color")
                .description(
                        "The text-display background color, including alpha. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-background-color:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display background color of packet entity {_mob} to -65536
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
