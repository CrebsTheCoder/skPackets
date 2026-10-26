package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplaySeeThroughState extends ExprPacketEntityField {
    public ExprPacketEntityDisplaySeeThroughState() {
        super(PacketEntityField.SEE_THROUGH);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplaySeeThroughState.class,
                        Object.class,
                        "meta [field] display see through [state] of (fake|packet) entity %packetentities%")
                .name("Packet Entity - Display See Through State")
                .description(
                        "The packet entity display see through state property. Supports set,"
                                + " delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-see-through-state:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display see through state of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
