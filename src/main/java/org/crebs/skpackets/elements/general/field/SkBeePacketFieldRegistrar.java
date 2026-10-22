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

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.shanebeestudios.skbee.api.nbt.NBTCompound;

import org.bukkit.util.Vector;
import org.crebs.skpackets.api.util.ConversionUtil;
import org.crebs.skpackets.api.util.SkBeeConversionUtil;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

public class SkBeePacketFieldRegistrar implements FieldRegistrar {

    @Override
    public void register() {

        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Server.BLOCK_ENTITY_DATA,
                        WrapperPlayServerBlockEntityData.class)
                .requiredField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getPosition()),
                        (w, vector) -> w.setPosition(ConversionUtil.toPeVectorI(vector)),
                        "block position",
                        "block pos",
                        "position",
                        "pos")
                .requiredField(
                        BlockEntityType.class,
                        WrapperPlayServerBlockEntityData::getBlockEntityType,
                        WrapperPlayServerBlockEntityData::setType,
                        "block entity type",
                        "entity type",
                        "type")
                .requiredField(
                        NBTCompound.class,
                        w -> SkBeeConversionUtil.toNbtApiNBTCompound(w.getNBT()),
                        (w, nbt) -> w.setNBT(SkBeeConversionUtil.toPeNBTCompound(nbt)),
                        "nbt compound",
                        "nbt",
                        "compound")
                .constructor(
                        values ->
                                new WrapperPlayServerBlockEntityData(
                                        ConversionUtil.toPeVectorI(
                                                values.getRequired("block position", Vector.class)),
                                        values.getRequired(
                                                "block entity type", BlockEntityType.class),
                                        SkBeeConversionUtil.toPeNBTCompound(
                                                values.getRequired(
                                                        "nbt compound", NBTCompound.class))))
                .build();
    }
}
