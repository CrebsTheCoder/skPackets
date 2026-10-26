package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayUseDefaultBackgroundState extends ExprPacketEntityField {
    public ExprPacketEntityDisplayUseDefaultBackgroundState() {
        super(PacketEntityField.DEFAULT_BACKGROUND);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayUseDefaultBackgroundState.class,
                        Object.class,
                        "meta [field] display use default background [state] of (fake|packet) entity %packetentities%")
                .name("Packet Entity - Display Use Default Background State")
                .description(
                        "The packet entity display use default background state property."
                                + " Supports set, delete and reset; unsupported entity types are"
                                + " rejected.")
                .examples(
                        """
                        command /hologram-use-default-background-state:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display use default background state of packet entity {_mob} to true
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
