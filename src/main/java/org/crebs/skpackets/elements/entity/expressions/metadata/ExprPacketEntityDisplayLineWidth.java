package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayLineWidth extends ExprPacketEntityField {
    public ExprPacketEntityDisplayLineWidth() {
        super(PacketEntityField.LINE_WIDTH);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayLineWidth.class,
                        Object.class,
                        "[meta field] display line width of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display Line Width")
                .description(
                        "The maximum text-display line width before wrapping. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-line-width:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display line width of packet entity {_mob} to 200
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
