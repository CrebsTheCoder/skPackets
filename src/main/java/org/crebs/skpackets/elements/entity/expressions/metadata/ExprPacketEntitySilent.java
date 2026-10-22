package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntitySilent extends ExprPacketEntityField {
    public ExprPacketEntitySilent() {
        super(PacketEntityField.SILENT);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntitySilent.class,
                        Object.class,
                        "[meta field] silent of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Silent")
                .description(
                        "The packet entity silent property. Supports set, delete and reset;"
                                + " unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-silent:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field silent of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
