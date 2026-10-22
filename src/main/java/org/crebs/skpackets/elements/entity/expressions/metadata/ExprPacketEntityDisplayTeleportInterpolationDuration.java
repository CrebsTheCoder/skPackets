package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayTeleportInterpolationDuration
        extends ExprPacketEntityField {
    public ExprPacketEntityDisplayTeleportInterpolationDuration() {
        super(PacketEntityField.TELEPORT_DURATION);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayTeleportInterpolationDuration.class,
                        Object.class,
                        "[meta field] display teleport interpolation duration of [packet"
                                + " entit(y|ies)] %packetentities%")
                .name("Packet Entity - Display Teleport Interpolation Duration")
                .description(
                        "The packet entity display teleport interpolation duration property."
                                + " Supports set, delete and reset; unsupported entity types are"
                                + " rejected.")
                .examples(
                        """
                        command /hologram-teleport-interpolation-duration:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display teleport interpolation duration of packet entity {_mob} to 5
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
