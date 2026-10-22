package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayAlignment extends ExprPacketEntityField {
    public ExprPacketEntityDisplayAlignment() {
        super(PacketEntityField.ALIGNMENT);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayAlignment.class,
                        Object.class,
                        "[meta field] display alignment of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display Alignment")
                .description(
                        "The packet entity display alignment property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-alignment:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display alignment of packet entity {_mob} to "center"
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
