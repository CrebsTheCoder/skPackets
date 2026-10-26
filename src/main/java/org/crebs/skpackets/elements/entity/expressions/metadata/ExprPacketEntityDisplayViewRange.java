package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayViewRange extends ExprPacketEntityField {
    public ExprPacketEntityDisplayViewRange() {
        super(PacketEntityField.VIEW_RANGE);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayViewRange.class,
                        Object.class,
                        "meta [field] display view range of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display View Range")
                .description(
                        "The packet entity display view range property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-view-range:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display view range of packet entity {_mob} to 1
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
