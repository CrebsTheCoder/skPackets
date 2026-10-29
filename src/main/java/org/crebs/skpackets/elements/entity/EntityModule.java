package org.crebs.skpackets.elements.entity;

import com.github.shanebeee.skr.Registration;
import org.crebs.skpackets.elements.RegistrationModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.crebs.skpackets.elements.entity.effects.*;
import org.crebs.skpackets.elements.entity.expressions.*;
import org.crebs.skpackets.elements.entity.expressions.entity.FakeEntityExpressions;
import org.crebs.skpackets.elements.entity.expressions.meta.MetaExpressions;
import org.crebs.skpackets.elements.entity.expressions.metadata.NativeMetadataExpressions;
import org.crebs.skpackets.elements.entity.expressions.prop.ExprVisibleFakeEntities;
import org.crebs.skpackets.elements.entity.expressions.prop.living.ExprFakeLivingEntityAttribute;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.crebs.skpackets.elements.entity.field.meta.MetaFieldRegistry;
import org.crebs.skpackets.elements.entity.section.SecExprNewFakeEntity;
import org.crebs.skpackets.elements.entity.section.SecExprNewMeta;
import org.crebs.skpackets.elements.entity.types.PacketEntity;

public record EntityModule(Registration registration) implements RegistrationModule {

    @Override
    public String name() {
        return "entity";
    }

    @Override
    public void load(SkriptAddon addon) {
        FakeEntityFieldRegistry.INSTANCE.registerAll();
        MetaFieldRegistry.INSTANCE.registerAll();
        register(registration,
                Types::register,
                PacketEntity::register,
                SecExprNewFakeEntity::register,
                SecExprNewMeta::register,
                EffRideFakeEntity::register,
                EffSpawnFakeEntity::register,
                EffKillPacketEntity::register,
                EffPacketEntityViewers::register,
                EffTeleportPacketEntity::register,
                ExprFakeLivingEntityAttribute::register,
                FakeEntityExpressions::register,
                MetaExpressions::register,
                ExprVisibleFakeEntities::register,
                ExprFakeEntitiesAll::register,
                ExprFakeEntityFromId::register,
                ExprFakeEntityFromUuid::register,
                ExprEntityId::register,
                ExprFakeEquipment::register,
                NativeMetadataExpressions::register);
    }
}
