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

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public abstract class PacketSendOrReceiveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    protected final ProtocolPacketEvent event;
    private final PacketWrapper<?> wrapper;
    private final PacketListenerPriority priority;

    public PacketSendOrReceiveEvent(
            ProtocolPacketEvent event,
            PacketWrapper<?> wrapper,
            PacketListenerPriority priority,
            boolean isAsync) {
        super(isAsync);
        this.event = event;
        this.wrapper = wrapper;
        this.priority = priority;
    }

    public ProtocolPacketEvent getEvent() {
        return event;
    }

    public PacketWrapper<?> getWrapper() {
        return wrapper;
    }

    public PacketListenerPriority getPriority() {
        return priority;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public static class NettyPacketEvent extends PacketSendOrReceiveEvent implements Cancellable {
        public NettyPacketEvent(
                ProtocolPacketEvent event,
                PacketWrapper<?> wrapper,
                PacketListenerPriority priority) {
            super(event, wrapper, priority, !Bukkit.isPrimaryThread());
        }

        @Override
        public boolean isCancelled() {
            return event.isCancelled();
        }

        @Override
        public void setCancelled(boolean state) {
            event.setCancelled(state);
        }
    }

    public static class SyncPacketEvent extends PacketSendOrReceiveEvent {
        public SyncPacketEvent(
                ProtocolPacketEvent event,
                PacketWrapper<?> wrapper,
                PacketListenerPriority priority) {
            super(event, wrapper, priority, false);
        }
    }

    public static class AsyncPacketEvent extends PacketSendOrReceiveEvent {
        public AsyncPacketEvent(
                ProtocolPacketEvent event,
                PacketWrapper<?> wrapper,
                PacketListenerPriority priority) {
            super(event, wrapper, priority, true);
        }
    }
}
