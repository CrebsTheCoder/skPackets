package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityId extends ExprPacketEntityField {
    public ExprPacketEntityId() {
        super(PacketEntityField.ENTITY_ID);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityId.class,
                        Object.class,
                        "[meta field] id of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - ID")
                .description("The packet entity id property. Read-only.")
                .examples(
                        """
                        command /who-am-i:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                send "%meta field id of packet entity {_mob}%" to player
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
