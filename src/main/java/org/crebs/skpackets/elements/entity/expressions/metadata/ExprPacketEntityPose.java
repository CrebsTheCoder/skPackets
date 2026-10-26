package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityPose extends ExprPacketEntityField {
    public ExprPacketEntityPose() {
        super(PacketEntityField.POSE);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityPose.class,
                        Object.class,
                        "meta [field] pose of (fake|packet) entity %packetentities%")
                .name("Packet Entity - Pose")
                .description(
                        "The pose name, such as standing, crouching or swimming. Supports set,"
                                + " delete and reset. The sneaking flag is separate from the pose.")
                .examples(
                        """
                        command /crouching-packet-player:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: player
                                    username: "Demo"
                                    skin: skin of player
                                    location: location of player
                                    viewers: player
                                set meta field sneaking of packet entity {_mob} to true
                                set meta field pose of packet entity {_mob} to "crouching"
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
