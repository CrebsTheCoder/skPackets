package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayHeight extends ExprPacketEntityField {
    public ExprPacketEntityDisplayHeight() {
        super(PacketEntityField.DISPLAY_HEIGHT);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayHeight.class,
                        Object.class,
                        "meta [field] display height of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display Height")
                .description(
                        "The packet entity display height property. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-height:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display height of packet entity {_mob} to 1
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
