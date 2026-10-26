package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntitySneakingState extends ExprPacketEntityField {
    public ExprPacketEntitySneakingState() {
        super(PacketEntityField.SNEAKING);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntitySneakingState.class,
                        Object.class,
                        "[meta field] sneaking [state] of [(fake|packet) entit(y|ies)] %packetentities%")
                .name("Packet Entity - Sneaking State")
                .description(
                        "Controls the sneaking metadata flag. Supports set, delete and reset;"
                                + " unsupported entity types are rejected.")
                .examples(
                        """
                        command /crouching-clone:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: player
                                    username: "Demo"
                                    skin: skin of player
                                    location: location of player
                                    viewers: player
                                set meta field sneaking state of packet entity {_mob} to true
                                set meta pose of {_mob} to "crouching"
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
