package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayBlockData extends ExprPacketEntityField {
    public ExprPacketEntityDisplayBlockData() {
        super(PacketEntityField.BLOCK_DATA);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayBlockData.class,
                        Object.class,
                        "[meta field] display block (state|data) of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Display Block Data")
                .description(
                        "The packet entity display block data property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /floating-stone:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: block display
                                    location: location of player
                                    viewers: player
                                set meta field display block data of packet entity {_mob} to stone[]
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
