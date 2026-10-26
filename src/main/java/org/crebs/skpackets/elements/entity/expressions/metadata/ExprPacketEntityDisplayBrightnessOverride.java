package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;

public final class ExprPacketEntityDisplayBrightnessOverride extends ExprPacketEntityField {
    public ExprPacketEntityDisplayBrightnessOverride() {
        super(PacketEntityField.BRIGHTNESS);
    }

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketEntityDisplayBrightnessOverride.class,
                        Object.class,
                        "meta [field] display brightness [override] of (fake|packet) entity %packetentities%")
                .name("Packet Entity - Display Brightness Override")
                .description(
                        "The packet entity display brightness override property. Supports"
                                + " set, delete and reset; unsupported entity types are rejected.")
                .examples(
                        """
                        command /hologram-brightness-override:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: text display
                                    location: location of player
                                    viewers: player
                                    meta field:
                                        display text: "Hello!"
                                set meta field display brightness override of packet entity {_mob} to 15728880
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
