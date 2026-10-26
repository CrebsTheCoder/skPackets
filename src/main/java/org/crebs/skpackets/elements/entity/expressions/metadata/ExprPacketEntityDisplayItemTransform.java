package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayItemTransform extends ExprPacketEntityField {
    public ExprPacketEntityDisplayItemTransform() {
        super(PacketEntityField.ITEM_TRANSFORM);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayItemTransform.class,
                        Object.class,
                        "[meta field] display item transform of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Display Item Transform")
                .description(
                        "The packet entity display item transform property. Supports set,"
                                + " delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-item-transform:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: item display
                                    location: location of player
                                    viewers: player
                                set meta field display item of packet entity {_mob} to diamond sword
                                set meta field display item transform of packet entity {_mob} to "gui"
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
