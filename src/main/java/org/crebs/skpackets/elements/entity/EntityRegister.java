package org.crebs.skpackets.elements.entity;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.entity.effects.EffKillPacketEntity;
import org.crebs.skpackets.elements.entity.effects.EffPacketEntityViewers;
import org.crebs.skpackets.elements.entity.effects.EffRideFakeEntity;
import org.crebs.skpackets.elements.entity.effects.EffSpawnFakeEntity;
import org.crebs.skpackets.elements.entity.effects.EffTeleportPacketEntity;
import org.crebs.skpackets.elements.entity.expressions.ExprEntityId;
import org.crebs.skpackets.elements.entity.expressions.ExprFakeEntitiesAll;
import org.crebs.skpackets.elements.entity.expressions.ExprFakeEntityFromId;
import org.crebs.skpackets.elements.entity.expressions.ExprFakeEntityFromUuid;
import org.crebs.skpackets.elements.entity.expressions.entity.FakeEntityExpressions;
import org.crebs.skpackets.elements.entity.expressions.meta.MetaExpressions;
import org.crebs.skpackets.elements.entity.expressions.metadata.NativeMetadataExpressions;
import org.crebs.skpackets.elements.entity.expressions.prop.ExprVisibleFakeEntities;
import org.crebs.skpackets.elements.entity.expressions.prop.living.ExprFakeLivingEntityAttribute;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.crebs.skpackets.elements.entity.field.meta.MetaFieldRegistry;
import org.crebs.skpackets.elements.entity.section.SecExprNewFakeEntity;
import org.crebs.skpackets.elements.entity.section.SecExprNewMeta;
import org.crebs.skpackets.elements.entity.section.SecExprNewPacketEntity;
import org.crebs.skpackets.elements.entity.types.PacketEntity;

public final class EntityRegister {

    public static void register(Registration registration) {
        FakeEntityFieldRegistry.INSTANCE.registerAll();
        MetaFieldRegistry.INSTANCE.registerAll();
        registerTypes(registration);
        registerSections(registration);
        registerEffects(registration);
        registerExpressions(registration);
    }

    private static void registerSections(Registration registration) {
        SecExprNewFakeEntity.register(registration);
        SecExprNewMeta.register(registration);
        SecExprNewPacketEntity.register(registration);
    }

    private static void registerEffects(Registration registration) {
        EffRideFakeEntity.register(registration);
        EffSpawnFakeEntity.register(registration);
        EffKillPacketEntity.register(registration);
        EffPacketEntityViewers.register(registration);
        EffTeleportPacketEntity.register(registration);
    }

    private static void registerExpressions(Registration registration) {
        ExprFakeLivingEntityAttribute.register(registration);
        FakeEntityExpressions.register(registration);
        MetaExpressions.register(registration);
        ExprVisibleFakeEntities.register(registration);
        ExprFakeEntitiesAll.register(registration);
        ExprFakeEntityFromId.register(registration);
        ExprFakeEntityFromUuid.register(registration);
        ExprEntityId.register(registration);
        NativeMetadataExpressions.register(registration);
    }

    private static void registerTypes(Registration registration) {
        Types.register(registration);
        PacketEntity.register(registration);
    }
}
