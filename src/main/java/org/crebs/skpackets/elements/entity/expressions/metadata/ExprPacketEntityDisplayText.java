package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayText extends ExprPacketEntityField {
    public ExprPacketEntityDisplayText() {
        super(PacketEntityField.TEXT);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayText.class,
                        Object.class,
                        "[meta field] display text of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display Text")
                .description(
                        "The text component shown by a text display. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hello-hologram:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display text of packet entity {_mob} to "Hello!"
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
