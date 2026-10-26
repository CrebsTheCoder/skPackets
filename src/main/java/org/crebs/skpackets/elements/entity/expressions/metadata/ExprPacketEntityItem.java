package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityItem extends ExprPacketEntityField {
    public ExprPacketEntityItem() {
        super(PacketEntityField.ITEM);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityItem.class,
                        Object.class,
                        "meta [field] item of (fake|packet) entity %packetentities%")
                .name("Packet Entity - Item")
                .description(
                        "The packet entity item property. Supports set, delete and reset;"
                                + " unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-item:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: dropped item
                                    location: location of player
                                    viewers: player
                                set meta field item of packet entity {_mob} to diamond
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
