package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayRightRotation extends ExprPacketEntityField {
    public ExprPacketEntityDisplayRightRotation() {
        super(PacketEntityField.RIGHT_ROTATION);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayRightRotation.class,
                        Object.class,
                        "meta [field] display right rotation of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display Right Rotation")
                .description(
                        "The quaternion applied after the display scale. Supports set, delete and"
                                + " reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-right-rotation:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set {_rotation} to meta field display right rotation of packet entity {_mob}
                                set meta field display right rotation of packet entity {_mob} to {_rotation}
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
