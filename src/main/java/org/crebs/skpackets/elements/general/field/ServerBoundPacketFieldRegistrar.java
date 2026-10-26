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
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSelectBundleItem;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;

import org.bukkit.block.sign.Side;
import org.bukkit.util.Vector;
import org.crebs.skpackets.api.util.ConversionUtil;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

public class ServerBoundPacketFieldRegistrar implements FieldRegistrar {

    @Override
    public void register() {
        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Client.SELECT_BUNDLE_ITEM,
                        WrapperPlayClientSelectBundleItem.class)
                .requiredField(
                        Number.class,
                        WrapperPlayClientSelectBundleItem::getSlotId,
                        (w, id) -> w.setSlotId(id.intValue()),
                        "slot id",
                        "id")
                .requiredField(
                        Number.class,
                        WrapperPlayClientSelectBundleItem::getSelectedItemIndex,
                        (w, index) -> w.setSelectedItemIndex(index.intValue()),
                        "selected item index",
                        "selected index",
                        "item index",
                        "index")
                .constructor(
                        values ->
                                new WrapperPlayClientSelectBundleItem(
                                        values.getRequired("slot id", Number.class).intValue(),
                                        values.getRequired("selected item index", Number.class)
                                                .intValue()))
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(
                        PacketType.Play.Client.INTERACT_ENTITY,
                        WrapperPlayClientInteractEntity.class)
                .requiredField(
                        Number.class,
                        WrapperPlayClientInteractEntity::getEntityId,
                        (w, id) -> w.setEntityId(id.intValue()),
                        "entity id",
                        "id")
                .requiredField(
                        InteractionHand.class,
                        WrapperPlayClientInteractEntity::getHand,
                        WrapperPlayClientInteractEntity::setHand,
                        "interaction hand",
                        "hand")
                .requiredField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getLocation()),
                        (w, newLoc) -> w.setLocation(ConversionUtil.toPeVectorD(newLoc)),
                        "location vector",
                        "location",
                        "loc")
                .requiredField(
                        Boolean.class,
                        w -> w.isSneaking().orElse(false),
                        WrapperPlayClientInteractEntity::setSneaking,
                        "sneaking state",
                        "sneaking")
                .constructor(
                        values ->
                                new WrapperPlayClientInteractEntity(
                                        values.getRequired("entity id", Number.class).intValue(),
                                        values.getRequired(
                                                "interaction hand", InteractionHand.class),
                                        ConversionUtil.toPeVectorD(
                                                values.getRequired(
                                                        "location vector", Vector.class)),
                                        values.getRequired("sneaking state", Boolean.class)))
                .build();

        PacketFieldRegistry.INSTANCE
                .builder(PacketType.Play.Client.UPDATE_SIGN, WrapperPlayClientUpdateSign.class)
                .requiredField(
                        Vector.class,
                        w -> ConversionUtil.toBukkitVector(w.getBlockPosition()),
                        (w, newPos) -> w.setBlockPosition(ConversionUtil.toPeVectorI(newPos)),
                        "location vector",
                        "location")
                .requiredField(
                        String[].class,
                        WrapperPlayClientUpdateSign::getTextLines,
                        WrapperPlayClientUpdateSign::setTextLines,
                        "sign lines",
                        "lines")
                .requiredField(
                        Side.class,
                        w -> w.isFrontText() ? Side.FRONT : Side.BACK,
                        (w, side) -> w.setFrontText(side == Side.FRONT),
                        "sign side",
                        "side")
                .constructor(
                        values ->
                                new WrapperPlayClientUpdateSign(
                                        ConversionUtil.toPeVectorI(
                                                values.getRequired(
                                                        "location vector", Vector.class)),
                                        values.getRequired("sign lines", String[].class),
                                        values.getRequired("sign side", Side.class) == Side.FRONT))
                .build();
    }
}
