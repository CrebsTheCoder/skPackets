package org.crebs.skpackets.elements;

import com.github.shanebeee.skr.Registration;
import org.crebs.skpackets.elements.entity.effects.EffKillPacketEntity;
import org.crebs.skpackets.elements.entity.expressions.ExprEntityId;
import org.crebs.skpackets.elements.entity.types.PacketEntity;

public final class ElementRegistration {
    private ElementRegistration() {}

    public static void register(Registration registration) {
        registerEffects(registration);
        registerExpressions(registration);
        registerTypes(registration);
        registration.finalizeRegistration();
    }

    public static void registerEffects(Registration registration) {
        EffKillPacketEntity.register(registration);
    }

    public static void registerExpressions(Registration registration) {
        ExprEntityId.register(registration);
    }

    public static void registerTypes(Registration registration) {
        PacketEntity.register(registration);
    }
}
