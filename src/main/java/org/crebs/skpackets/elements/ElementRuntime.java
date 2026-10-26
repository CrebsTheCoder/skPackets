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
package org.crebs.skpackets.elements;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.api.general.EntityTracker;
import org.crebs.skpackets.api.general.PlayerSkinRegistry;
import org.crebs.skpackets.api.general.packet.PacketSendOrReceiveListener;
import org.crebs.skpackets.api.simple.ChatSessionListener;
import org.crebs.skpackets.api.simple.GlowingEntityListener;
import org.crebs.skpackets.api.simple.GlowingEntityManager;
import org.crebs.skpackets.api.simple.PlayerSkinListener;

import java.util.ArrayList;
import java.util.List;

public final class ElementRuntime implements AutoCloseable {
    private final List<PacketListenerCommon> listeners = new ArrayList<>();

    public ElementRuntime(SkPackets plugin) {
        EntityLib.init(
                new APIConfig(PacketEvents.getAPI(), new SpigotEntityLibPlatform(plugin))
                        .disableBStats());
        listeners.add(
                PacketEvents.getAPI()
                        .getEventManager()
                        .registerListener(new EntityTracker(), PacketListenerPriority.MONITOR));
        listeners.add(
                PacketEvents.getAPI()
                        .getEventManager()
                        .registerListener(
                                new GlowingEntityListener(), PacketListenerPriority.NORMAL));
        listeners.add(
                PacketEvents.getAPI()
                        .getEventManager()
                        .registerListener(
                                new ChatSessionListener(), PacketListenerPriority.MONITOR));
        listeners.add(
                PacketEvents.getAPI()
                        .getEventManager()
                        .registerListener(new PlayerSkinListener(), PacketListenerPriority.NORMAL));
        if (plugin.getServer().getServerConfig().isProxyOnlineMode()) {
            plugin.getServer().getPluginManager().registerEvents(new PlayerSkinRegistry(), plugin);
        }
    }

    @Override
    public void close() {
        PacketSendOrReceiveListener.close();
        for (var listener : listeners) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        }
        listeners.clear();
        EntityTracker.clear();
        GlowingEntityManager.clear();
        EntityLib.shutdown();
    }
}
