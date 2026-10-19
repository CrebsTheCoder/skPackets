package org.crebs.skpackets.elements.entity.types;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperEntityAttributes;
import me.tofaa.entitylib.wrapper.WrapperEntityEquipment;

public class PacketEntity {

    public static void register(Registration registration) {
        registration
                .newType(WrapperEntity.class, "packetentity")
                .name("Packet Entity")
                .description(
                        "A packet entity is a virtual entity that exists only on the client side."
                                + " It can be used to send packets to the client without actually"
                                + " spawning a real entity on the server.")
                .since("1.0.0")
                .register();

        registration
                .newType(WrapperEntityAttributes.class, "packetentityattributes")
                .name("Packet Entity Attributes")
                .description("Attributes of a packet entity, such as health, speed, etc.")
                .since("1.0.0")
                .register();

        registration
                .newType(WrapperEntityEquipment.class, "packetentityequipment")
                .name("Packet Entity Equipment")
                .description("Equipment of a packet entity, such as armor and held items.")
                .since("1.0.0")
                .register();
    }
}
