package org.crebs.skpackets.elements.entity.types;

import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;

public final class PacketEntity {
    private PacketEntity() {}

    public static void register(Registration registration) {
        registration
                .newType(WrapperEntity.class, "packetentity")
                .user("(fake|packet) ?entit(y|ies)")
                .name("Packet Entity")
                .description(
                        "Fake entity and packet entity are aliases for the same virtual entity that"
                            + " exists only on the client side. It can be used to send packets to"
                            + " the client without actually spawning a real entity on the server.")
                .examples(
                        """
                        command /my-double:
                            trigger:
                                set {_mob} to a new packet entity:
                                    entity: player
                                    username: "Demo"
                                    skin: skin of player
                                    viewers: player
                                    location: location of player
                                    persistent: true
                                    meta field:
                                        sneaking: true
                                        pose: "sneaking" parsed as entity pose
                                wait 5 seconds
                                kill packet entity {_mob}
                        """)
                .parser(
                        new Parser<>() {
                            @Override
                            public boolean canParse(ParseContext context) {
                                return false;
                            }

                            @Override
                            public String toString(WrapperEntity entity, int flags) {
                                return "packet entity " + entity.getEntityId();
                            }

                            @Override
                            public String toVariableNameString(WrapperEntity entity) {
                                return "packetentity:" + entity.getUuid();
                            }
                        })
                .since("1.0.0")
                .register();
    }
}
