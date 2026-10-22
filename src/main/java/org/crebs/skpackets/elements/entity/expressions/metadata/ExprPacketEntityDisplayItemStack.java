package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayItemStack extends ExprPacketEntityField {
    public ExprPacketEntityDisplayItemStack() {
        super(PacketEntityField.DISPLAY_ITEM);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayItemStack.class,
                        Object.class,
                        "[meta field] display item stack of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display Item Stack")
                .description(
                        "The packet entity display item stack property. Supports set, delete"
                                + " and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /floating-diamond:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: item display
                                    location: location of player
                                    viewers: player
                                set meta field display item stack of packet entity {_mob} to diamond
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
