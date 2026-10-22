package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplaySeeThrough extends ExprPacketEntityField {
    public ExprPacketEntityDisplaySeeThrough() {
        super(PacketEntityField.SEE_THROUGH);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplaySeeThrough.class,
                        Object.class,
                        "[meta field] display see through of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display See Through")
                .description(
                        "The packet entity display see through property. Supports set,"
                                + " delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-see-through:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display see through of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
