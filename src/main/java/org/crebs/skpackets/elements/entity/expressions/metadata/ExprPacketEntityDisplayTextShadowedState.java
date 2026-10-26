package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayTextShadowedState extends ExprPacketEntityField {
    public ExprPacketEntityDisplayTextShadowedState() {
        super(PacketEntityField.TEXT_SHADOW);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayTextShadowedState.class,
                        Object.class,
                        "[meta field] display (shadowed [state]|text shadowed state) of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Display Text Shadowed State")
                .description(
                        "The packet entity display text shadowed state property. Supports"
                                + " set, delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-text-shadowed-state:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display text shadowed state of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
