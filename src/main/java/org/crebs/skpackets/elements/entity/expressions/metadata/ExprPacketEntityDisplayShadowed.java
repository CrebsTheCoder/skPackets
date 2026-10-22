package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayShadowed extends ExprPacketEntityField {
    public ExprPacketEntityDisplayShadowed() {
        super(PacketEntityField.TEXT_SHADOW);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayShadowed.class,
                        Object.class,
                        "[meta field] display shadowed of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display Shadowed")
                .description(
                        "The packet entity display shadowed property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-shadowed:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display shadowed of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
