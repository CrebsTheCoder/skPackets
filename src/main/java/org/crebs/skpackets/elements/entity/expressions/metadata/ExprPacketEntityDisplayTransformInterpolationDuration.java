package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayTransformInterpolationDuration
        extends ExprPacketEntityField {
    public ExprPacketEntityDisplayTransformInterpolationDuration() {
        super(PacketEntityField.TRANSFORM_DURATION);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayTransformInterpolationDuration.class,
                        Object.class,
                        "[meta field] display transform interpolation duration of [(fake|packet)"
                                + " entit(y|ies)] %packetentities%")
                .name("Packet Entity - Display Transform Interpolation Duration")
                .description(
                        "The packet entity display transform interpolation duration property."
                                + " Supports set, delete and reset; unsupported entity types are"
                                + " rejected.")
                .examples(
                        """
                        command /hologram-transform-interpolation-duration:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display transform interpolation duration of packet entity {_mob} to 5
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
