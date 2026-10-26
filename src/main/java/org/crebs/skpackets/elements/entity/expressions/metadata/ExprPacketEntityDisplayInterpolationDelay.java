package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayInterpolationDelay extends ExprPacketEntityField {
    public ExprPacketEntityDisplayInterpolationDelay() {
        super(PacketEntityField.INTERPOLATION_DELAY);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayInterpolationDelay.class,
                        Object.class,
                        "meta [field] display interpolation delay of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display Interpolation Delay")
                .description(
                        "The packet entity display interpolation delay property. Supports"
                                + " set, delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-interpolation-delay:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display interpolation delay of packet entity {_mob} to 5
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
