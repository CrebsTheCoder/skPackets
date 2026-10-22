package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityElytraFlying extends ExprPacketEntityField {
    public ExprPacketEntityElytraFlying() {
        super(PacketEntityField.ELYTRA_FLYING);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityElytraFlying.class,
                        Object.class,
                        "[meta field] elytra flying of [(fake|packet) entit(y|ies)]"
                                + " %packetentities%")
                .name("Packet Entity - Elytra Flying")
                .description(
                        "The packet entity elytra flying property. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /preview-elytra-flying:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: player
                                    username: "Demo"
                                    skin: skin of player
                                    location: location of player
                                    viewers: player
                                set meta field elytra flying of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
