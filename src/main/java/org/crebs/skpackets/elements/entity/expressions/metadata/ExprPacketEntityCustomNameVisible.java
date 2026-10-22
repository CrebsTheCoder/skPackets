package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityCustomNameVisible extends ExprPacketEntityField {
    public ExprPacketEntityCustomNameVisible() {
        super(PacketEntityField.CUSTOM_NAME_VISIBLE);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityCustomNameVisible.class,
                        Object.class,
                        "[meta field] custom name visible of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Custom Name Visible")
                .description(
                        "The packet entity custom name visible property. Supports set,"
                                + " delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /private-preview:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                set meta field custom name visible of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
