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
package org.crebs.skpackets.api.general.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import org.bukkit.Bukkit;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive.ProcessType;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketSendOrReceiveListener implements PacketListener {

    private static final Map<
                    PacketListenerPriority, Map<PacketTypeCommon, Map<ProcessType, Integer>>>
            ACTIVE_LISTENERS = new ConcurrentHashMap<>();

    private static final Map<PacketListenerPriority, Map<ProcessType, Integer>> GLOBAL_LISTENERS =
            new ConcurrentHashMap<>();

    private static final Map<PacketTypeCommon, Constructor<? extends PacketWrapper<?>>>
            CONSTRUCTORS = new ConcurrentHashMap<>();

    private final PacketListenerPriority priority;
    private static final Map<PacketListenerPriority, PacketListenerCommon> REGISTERED =
            new EnumMap<>(PacketListenerPriority.class);

    public static synchronized void close() {
        REGISTERED.values().forEach(PacketEvents.getAPI().getEventManager()::unregisterListener);
        REGISTERED.clear();
        ACTIVE_LISTENERS.clear();
        GLOBAL_LISTENERS.clear();
        CONSTRUCTORS.clear();
    }

    private PacketSendOrReceiveListener(PacketListenerPriority priority) {
        this.priority = priority;
    }

    public static synchronized void registerListener(
            PacketTypeCommon type, ProcessType way, PacketListenerPriority priority) {
        if (type == null) {
            GLOBAL_LISTENERS
                    .computeIfAbsent(priority, k -> new ConcurrentHashMap<>())
                    .merge(way, 1, Integer::sum);
        } else {
            ACTIVE_LISTENERS
                    .computeIfAbsent(priority, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                    .merge(way, 1, Integer::sum);
        }

        REGISTERED.computeIfAbsent(
                priority,
                p ->
                        PacketEvents.getAPI()
                                .getEventManager()
                                .registerListener(new PacketSendOrReceiveListener(p), p));
    }

    public static synchronized void unregisterListener(
            PacketTypeCommon type, ProcessType way, PacketListenerPriority priority) {
        Map<PacketTypeCommon, Map<ProcessType, Integer>> specific = ACTIVE_LISTENERS.get(priority);
        Map<ProcessType, Integer> counts =
                type == null
                        ? GLOBAL_LISTENERS.get(priority)
                        : specific == null ? null : specific.get(type);
        if (counts == null) {
            return;
        }
        counts.computeIfPresent(way, (key, count) -> count == 1 ? null : count - 1);
        if (counts.isEmpty()) {
            if (type == null) {
                GLOBAL_LISTENERS.remove(priority);
            } else {
                specific.remove(type);
                if (specific.isEmpty()) {
                    ACTIVE_LISTENERS.remove(priority);
                }
            }
        }
        if (!GLOBAL_LISTENERS.containsKey(priority) && !ACTIVE_LISTENERS.containsKey(priority)) {
            PacketListenerCommon listener = REGISTERED.remove(priority);
            if (listener != null) {
                PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            }
        }
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent event) {
        trigger(event, priority);
    }

    @Override
    public void onPacketSend(@NonNull PacketSendEvent event) {
        trigger(event, priority);
    }

    private static void trigger(ProtocolPacketEvent event, PacketListenerPriority priority) {
        PacketTypeCommon type = event.getPacketType();

        Map<PacketTypeCommon, Map<ProcessType, Integer>> specificListeners =
                ACTIVE_LISTENERS.get(priority);
        Map<ProcessType, Integer> specificWays =
                specificListeners == null ? null : specificListeners.get(type);
        Map<ProcessType, Integer> globalWays = GLOBAL_LISTENERS.get(priority);
        boolean netty = listens(specificWays, globalWays, ProcessType.NETTY);
        boolean sync = listens(specificWays, globalWays, ProcessType.SYNC);
        boolean async = listens(specificWays, globalWays, ProcessType.ASYNC);
        if (!netty && !sync && !async) {
            return;
        }

        PacketWrapper<?> wrapper = getWrapper(type, event);
        if (wrapper == null) {
            return;
        }

        if (netty) {
            if (priority != PacketListenerPriority.MONITOR) {
                event.markForReEncode(true);
            }
            PacketSendOrReceiveEvent.NettyPacketEvent nettyEvent =
                    new PacketSendOrReceiveEvent.NettyPacketEvent(event, wrapper, priority);
            Bukkit.getPluginManager().callEvent(nettyEvent);

            if (nettyEvent.isCancelled()) {
                event.setCancelled(true);
            }
        }

        if (sync) {
            PacketSendOrReceiveEvent.SyncPacketEvent syncEvent =
                    new PacketSendOrReceiveEvent.SyncPacketEvent(event, wrapper, priority);
            Bukkit.getScheduler()
                    .runTask(
                            SkPackets.getInstance(),
                            () -> Bukkit.getPluginManager().callEvent(syncEvent));
        }

        if (async) {
            PacketSendOrReceiveEvent.AsyncPacketEvent asyncEvent =
                    new PacketSendOrReceiveEvent.AsyncPacketEvent(event, wrapper, priority);
            Bukkit.getScheduler()
                    .runTaskAsynchronously(
                            SkPackets.getInstance(),
                            () -> Bukkit.getPluginManager().callEvent(asyncEvent));
        }
    }

    private static PacketWrapper<?> getWrapper(PacketTypeCommon type, ProtocolPacketEvent event) {
        Class<? extends PacketWrapper<?>> clazz = type.getWrapperClass();
        if (clazz == null) {
            return null;
        }

        try {
            Constructor<? extends PacketWrapper<?>> constructor = CONSTRUCTORS.get(type);
            if (constructor == null) {
                constructor =
                        clazz.getConstructor(
                                type.getSide() == PacketSide.SERVER
                                        ? PacketSendEvent.class
                                        : PacketReceiveEvent.class);
                CONSTRUCTORS.putIfAbsent(type, constructor);
            }
            return constructor.newInstance(event);
        } catch (InvocationTargetException
                | InstantiationException
                | IllegalAccessException
                | NoSuchMethodError
                | NoSuchMethodException e) {
            throw new IllegalStateException("Couldn't create packet for: " + type, e);
        }
    }

    private static boolean listens(
            Map<ProcessType, Integer> specific,
            Map<ProcessType, Integer> global,
            ProcessType type) {
        return (specific != null && specific.containsKey(type))
                || (global != null && global.containsKey(type));
    }
}
