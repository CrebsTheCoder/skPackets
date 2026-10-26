package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayShadowStrength extends ExprPacketEntityField {
    public ExprPacketEntityDisplayShadowStrength() {
        super(PacketEntityField.SHADOW_STRENGTH);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayShadowStrength.class,
                        Object.class,
                        "meta [field] display shadow strength of (fake|packet) entity"
                                + " %packetentities%")
                .name("Packet Entity - Display Shadow Strength")
                .description(
                        "The packet entity display shadow strength property. Supports set,"
                                + " delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-shadow-strength:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display shadow strength of packet entity {_mob} to 1
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
