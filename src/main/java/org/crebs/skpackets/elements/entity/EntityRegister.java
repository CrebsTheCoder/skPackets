package org.crebs.skpackets.elements.entity;

import com.github.shanebeee.skr.Registration;
import org.crebs.skpackets.elements.entity.effects.EffKillPacketEntity;
import org.crebs.skpackets.elements.entity.effects.EffPacketEntityViewers;
import org.crebs.skpackets.elements.entity.effects.EffTeleportPacketEntity;
import org.crebs.skpackets.elements.entity.expressions.ExprEntityId;
import org.crebs.skpackets.elements.entity.expressions.ExprPacketEntityField;
import org.crebs.skpackets.elements.entity.section.SecExprNewPacketEntity;
import org.crebs.skpackets.elements.entity.types.PacketEntity;

public final class EntityRegister {

    public static void registerEntitySyntax(Registration registration) {
        registerTypes(registration);
        SecExprNewPacketEntity.register(registration);
        registerEffects(registration);
        registerExpressions(registration);
        registration.finalizeRegistration();
    }

    public static void registerEffects(Registration registration) {
        EffKillPacketEntity.register(registration);
        EffPacketEntityViewers.register(registration);
        EffTeleportPacketEntity.register(registration);
    }

    public static void registerExpressions(Registration registration) {
        ExprEntityId.register(registration);
        ExprPacketEntityField.register(registration);
    }

    public static void registerTypes(Registration registration) {
        PacketEntity.register(registration);
    }

}
