package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityEntityId extends ExprPacketEntityField {
    public ExprPacketEntityEntityId() {
        super(PacketEntityField.ENTITY_ID);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityEntityId.class,
                        Object.class,
                        "[meta field] entity id of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Entity ID")
                .description("The numeric network entity ID. Read-only. Read-only.")
                .examples(
                        """
                        command /who-am-i:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: zombie
                                    location: location of player
                                    viewers: player
                                send "%meta field entity id of packet entity {_mob}%" to player
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
