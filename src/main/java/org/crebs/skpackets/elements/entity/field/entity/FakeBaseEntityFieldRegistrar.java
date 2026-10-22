/*
 * MIT License
 *
 * Copyright (c) 2026 3add
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.crebs.skpackets.elements.entity.field.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.util.field.ConstructionContext;
import org.crebs.skpackets.api.util.field.FieldRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class FakeBaseEntityFieldRegistrar implements FieldRegistrar {

    @Override
    public void register() {

        FakeEntityFieldRegistry.INSTANCE
                .builder(EntityTypes.ENTITY, WrapperEntity.class)
                .requiredField(
                        Player[].class,
                        w ->
                                org.crebs.skpackets.SkPackets.getInstance()
                                        .getPacketEntities()
                                        .getViewers(w)
                                        .stream()
                                        .map(Bukkit::getPlayer)
                                        .filter(Objects::nonNull)
                                        .toArray(Player[]::new),
                        FakeBaseEntityFieldRegistrar::setViewers,
                        "entity viewers",
                        "viewers")
                .requiredField(
                        org.bukkit.Location.class,
                        w ->
                                new org.bukkit.Location(
                                        Bukkit.getWorld(w.getWorld()),
                                        w.getX(),
                                        w.getY(),
                                        w.getZ(),
                                        w.getYaw(),
                                        w.getPitch()),
                        (w, newVec) ->
                                org.crebs.skpackets.SkPackets.getInstance()
                                        .getPacketEntities()
                                        .teleport(w, newVec),
                        "entity location",
                        "location")
                .optionalField(Number.class, WrapperEntity::getEntityId, null, "entity id", "id")
                .optionalField(
                        EntityMeta.class,
                        EntityMeta::of,
                        null,
                        "entity meta data",
                        "meta data",
                        "meta")
                .optionalField(
                        EntityType.class, WrapperEntity::getEntityType, null, "entity type", "type")
                .optionalField(UUID.class, WrapperEntity::getUuid, null, "entity uuid", "uuid")
                .optionalField(
                        Boolean.class,
                        w ->
                                org.crebs.skpackets.SkPackets.getInstance()
                                        .getPacketEntities()
                                        .isPersistent(w),
                        null,
                        "persistent")
                .constructor(
                        context -> {
                            CommonEntityData data = CommonEntityData.extract(context);

                            WrapperEntity entity =
                                    com.github.retrooper.packetevents.protocol.entity.type
                                                    .EntityTypes.isTypeInstanceOf(
                                                    context.getKey(),
                                                    com.github.retrooper.packetevents.protocol
                                                            .entity.type.EntityTypes.LIVINGENTITY)
                                            ? new me.tofaa.entitylib.wrapper.WrapperLivingEntity(
                                                    data.entityId(), data.uuid(), context.getKey())
                                            : new WrapperEntity(
                                                    data.entityId(), data.uuid(), context.getKey());
                            if (data.meta() != null) data.meta().applyTo(entity);

                            finalizeEntitySetup(entity, context);

                            return entity;
                        })
                .build();
    }

    public record CommonEntityData(int entityId, UUID uuid, @Nullable EntityMeta meta) {
        public static CommonEntityData extract(ConstructionContext<EntityType, ?> context) {
            EntityType type = context.getKey();

            UUID uuid = context.getOptional("entity uuid", UUID.class);
            if (uuid == null) {
                uuid = UUID.randomUUID();
            }

            EntityMeta meta = context.getOptional("entity meta data", EntityMeta.class);
            Number providedId = context.getOptional("entity id", Number.class);
            int entityId =
                    providedId == null
                            ? EntityLib.get().getPlatform().nextEntityId()
                            : providedId.intValue();
            return new CommonEntityData(entityId, uuid, meta);
        }
    }

    public static void finalizeEntitySetup(
            WrapperEntity entity, ConstructionContext<EntityType, ?> context) {
        Player[] viewers = context.getRequired("entity viewers", Player[].class);
        org.bukkit.Location location =
                context.getRequired("entity location", org.bukkit.Location.class);
        Boolean persistent = context.getOptional("persistent", Boolean.class);
        org.crebs.skpackets.SkPackets.getInstance()
                .getPacketEntities()
                .spawn(
                        entity,
                        location,
                        java.util.Arrays.asList(viewers),
                        persistent == null || persistent);
    }

    private static void setViewers(WrapperEntity w, Player[] viewers) {
        var manager = org.crebs.skpackets.SkPackets.getInstance().getPacketEntities();
        manager.getViewers(w).forEach(id -> manager.removeViewer(w, id));
        for (Player newViewer : viewers) {
            manager.addViewer(w, newViewer);
        }
    }
}
