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

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import org.crebs.skpackets.api.util.LogUtil;
import org.crebs.skpackets.api.util.field.BaseFieldRegistry;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

import java.util.ArrayList;
import java.util.List;

public class PacketFieldRegistry extends BaseFieldRegistry<PacketTypeCommon, PacketWrapper<?>> {

    public static final PacketFieldRegistry INSTANCE = new PacketFieldRegistry();
    private final List<FieldRegistrar> registrars;

    private PacketFieldRegistry() {
        this.registrars = new ArrayList<>();

        this.registrars.addAll(
                List.of(
                        new ClientBoundPacketFieldRegistrar(),
                        new ServerBoundPacketFieldRegistrar(),
                        new org.crebs.skpackets.elements.team.TeamPacketFields()));

        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SkBee")) {
            return;
        }
        try {
            Class<?> nbtApiClass = Class.forName("com.shanebeestudios.skbee.api.nbt.NBTApi");
            boolean enabled = (boolean) nbtApiClass.getMethod("isEnabled").invoke(null);
            if (enabled) {
                this.registrars.add(new SkBeePacketFieldRegistrar());
                LogUtil.info("Hooked into SkBee NBT using NBT-API");
            }
        } catch (ClassNotFoundException ignored) {
            LogUtil.error(
                    "SkBee not found, PacketEventsSK elements depending on NBT will not be"
                            + " registered");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hook into SkBee NBT", e);
        }
    }

    @Override
    public List<FieldRegistrar> getRegistrars() {
        return registrars;
    }
}
