package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntitySwimming extends ExprPacketEntityField {
    public ExprPacketEntitySwimming() {
        super(PacketEntityField.SWIMMING);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntitySwimming.class,
                        Object.class,
                        "[meta field] swimming of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Swimming")
                .description(
                        "The packet entity swimming property. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-swimming:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: player
                                    username: "Demo"
                                    skin: skin of player
                                    location: location of player
                                    viewers: player
                                set meta field swimming of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
