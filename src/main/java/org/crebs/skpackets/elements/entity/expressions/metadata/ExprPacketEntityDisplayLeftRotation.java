package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayLeftRotation extends ExprPacketEntityField {
    public ExprPacketEntityDisplayLeftRotation() {
        super(PacketEntityField.LEFT_ROTATION);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayLeftRotation.class,
                        Object.class,
                        "meta [field] display left rotation of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display Left Rotation")
                .description(
                        "The quaternion applied before the display scale. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-left-rotation:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set {_rotation} to meta field display left rotation of packet entity {_mob}
                                set meta field display left rotation of packet entity {_mob} to {_rotation}
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
