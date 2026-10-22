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
package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.util.Vector;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.general.EntityTracker;
import org.crebs.skpackets.api.util.ConversionUtil;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

import java.util.Arrays;

public class ClientBoundPacketFieldRegistrar implements FieldRegistrar {

    @Override
    public void register() {
        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Server.ENTITY_VELOCITY,
                        WrapperPlayServerEntityVelocity.class)
                .requiredField(
                        Number.class,
                        WrapperPlayServerEntityVelocity::getEntityId,
                        (w, id) -> w.setEntityId(id.intValue()),
                        "entity id",
                        "id")
                .requiredField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getVelocity()),
                        (w, vector) -> w.setVelocity(ConversionUtil.toPeVectorD(vector)),
                        "velocity vector",
                        "vector",
                        "velocity")
                .constructor(
                        values ->
                                new WrapperPlayServerEntityVelocity(
                                        values.getRequired("entity id", Number.class).intValue(),
                                        ConversionUtil.toPeVectorD(
                                                values.getRequired(
                                                        "velocity vector", Vector.class))))
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Server.GAME_TEST_HIGHLIGHT_POS,
                        WrapperPlayServerGameTestHighlightPos.class)
                .requiredField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getAbsolutePos()),
                        (w, vector) -> w.setAbsolutePos(ConversionUtil.toPeVectorI(vector)),
                        "absolute position",
                        "absolute pos",
                        "abs position",
                        "abs pos",
                        "position",
                        "pos")
                .optionalField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getRelativePos()),
                        (w, vector) -> w.setRelativePos(ConversionUtil.toPeVectorI(vector)),
                        "relative position",
                        "relative pos",
                        "rel position",
                        "rel pos")
                .constructor(
                        values ->
                                new WrapperPlayServerGameTestHighlightPos(
                                        ConversionUtil.toPeVectorI(
                                                values.getRequired(
                                                        "absolute position", Vector.class)),
                                        ConversionUtil.toPeVectorI(
                                                values.getRequired(
                                                        "relative position", Vector.class))))
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Server.ENTITY_METADATA,
                        WrapperPlayServerEntityMetadata.class)
                .requiredField(
                        Number.class,
                        WrapperPlayServerEntityMetadata::getEntityId,
                        (w, id) -> w.setEntityId(id.intValue()),
                        "entity id",
                        "id")
                .requiredField(
                        EntityMeta.class,
                        w -> {
                            EntityType type = EntityTracker.getType(w.getEntityId());
                            if (type == null) {
                                throw new IllegalStateException(
                                        "Failed to find entity type of entity with id "
                                                + w.getEntityId());
                            }

                            EntityMeta meta = EntityMeta.createMeta(w.getEntityId(), type);
                            meta.readPacket(w);
                            return meta;
                        },
                        (packet, meta) -> packet.setEntityMetadata(meta.entityData()),
                        "entity metadata",
                        "entity meta",
                        "metadata",
                        "meta")
                .constructor(
                        values ->
                                new WrapperPlayServerEntityMetadata(
                                        values.getRequired("entity id", Number.class).intValue(),
                                        values.getRequired("entity metadata", EntityMeta.class)
                                                .entityData()))
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(PacketType.Play.Server.BLOCK_CHANGE, WrapperPlayServerBlockChange.class)
                .requiredField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getBlockPosition()),
                        (w, vector) -> w.setBlockPosition(ConversionUtil.toPeVectorI(vector)),
                        "block position",
                        "block pos",
                        "position",
                        "pos")
                .requiredField(
                        BlockData.class,
                        w -> SpigotConversionUtil.toBukkitBlockData(w.getBlockState()),
                        (w, blockData) ->
                                w.setBlockState(
                                        SpigotConversionUtil.fromBukkitBlockData(blockData)),
                        "block state",
                        "state",
                        "block data",
                        "data")
                .constructor(
                        values ->
                                new WrapperPlayServerBlockChange(
                                        ConversionUtil.toPeVectorI(
                                                values.getRequired("block position", Vector.class)),
                                        SpigotConversionUtil.fromBukkitBlockData(
                                                values.getRequired(
                                                        "block state", BlockData.class))))
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Server.OPEN_SIGN_EDITOR,
                        WrapperPlayServerOpenSignEditor.class)
                .requiredField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getPosition()),
                        (w, vector) -> w.setPosition(ConversionUtil.toPeVectorI(vector)),
                        "block position",
                        "block pos",
                        "position",
                        "pos")
                .requiredField(
                        Side.class,
                        w -> w.isFrontText() ? Side.FRONT : Side.BACK,
                        (w, side) -> w.setFrontText(side == Side.FRONT),
                        "sign side",
                        "side")
                .constructor(
                        values ->
                                new WrapperPlayServerOpenSignEditor(
                                        ConversionUtil.toPeVectorI(
                                                values.getRequired("block position", Vector.class)),
                                        values.getRequired("sign side", Side.class) == Side.FRONT))
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(PacketType.Play.Server.CLOSE_WINDOW, WrapperPlayServerCloseWindow.class)
                .constructor(k -> new WrapperPlayServerCloseWindow())
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Server.DESTROY_ENTITIES,
                        WrapperPlayServerDestroyEntities.class)
                .requiredField(
                        Number[].class,
                        w -> Arrays.stream(w.getEntityIds()).boxed().toArray(Number[]::new),
                        (w, ids) ->
                                w.setEntityIds(
                                        Arrays.stream(ids).mapToInt(Number::intValue).toArray()),
                        "entity ids",
                        "ids")
                .constructor(
                        values ->
                                new WrapperPlayServerDestroyEntities(
                                        Arrays.stream(
                                                        values.getRequired(
                                                                "entity ids", Number[].class))
                                                .mapToInt(Number::intValue)
                                                .toArray()))
                .build();
    }
}
