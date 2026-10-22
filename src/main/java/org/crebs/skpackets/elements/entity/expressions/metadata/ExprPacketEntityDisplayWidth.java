package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayWidth extends ExprPacketEntityField {
    public ExprPacketEntityDisplayWidth() {
        super(PacketEntityField.DISPLAY_WIDTH);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayWidth.class,
                        Object.class,
                        "[meta field] display width of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display Width")
                .description(
                        "The packet entity display width property. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-width:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display width of packet entity {_mob} to 1
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
