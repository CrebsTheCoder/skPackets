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
package org.crebs.skpackets.elements.entity.effects;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EffRideFakeEntity extends Effect {

    public static void register(Registration reg) {
        reg.newEffect(
                        EffRideFakeEntity.class,
                        "(make|let|force) (fake|packet) [entit(y|ies)] %packetentities% [to]"
                                + " (ride|mount) %packetentity/livingentity%",
                        "(make|let|force) (fake|packet) [entit(y|ies)] %packetentities% [to]"
                                + " (dismount|(dismount|leave) as passenger[s]) (from|of)"
                                + " %packetentity/livingentity%")
                .name("Fake Entity - Ride")
                .description(
                        """
                        Make fake entities ride another entity (fake or real), or remove them as passengers.
                        **Note: Real entities are just a single packet being sent, when a player relogs, changes worlds or some other circumstances the passengers will be altered by the vanilla server.**
                        """)
                .examples(
                        """
                        command /floating-nameplate:
                            trigger:
                                set {_displayMeta} to text display meta data:
                                    display content: "Hello!"
                                set {_entity} to a new fake text display entity:
                                    location: location of player
                                    viewers: player
                                    meta: {_displayMeta}
                                set {_meta} to fake entity meta data of {_entity}
                                set meta display text of {_meta} to "Hello!"
                                set meta display billboard constraints of {_meta} to center
                                make fake entity {_entity} ride player
                                wait 5 seconds
                                make fake entity {_entity} dismount from player
                                kill fake entity {_entity}
                        """)
                .since("1.0.0")
                .register();
    }

    private Expression<WrapperEntity> passengerExpr;
    private Expression<Object> vehicleExpr;
    private boolean mount;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        this.passengerExpr = (Expression<WrapperEntity>) expressions[0];
        this.vehicleExpr = (Expression<Object>) expressions[1];
        mount = matchedPattern == 0;

        return true;
    }

    @Override
    protected void execute(Event event) {
        WrapperEntity[] passengers = passengerExpr.getAll(event);
        Object vehicle = vehicleExpr.getSingle(event);

        if (passengers == null || passengers.length == 0 || vehicle == null) {
            return;
        }

        if (vehicle instanceof WrapperEntity fakeVehicle) {
            for (WrapperEntity passenger : passengers) {
                if (mount) {
                    fakeVehicle.addPassenger(passenger);
                } else {
                    fakeVehicle.removePassenger(passenger);
                }
            }
            return;
        }

        if (vehicle instanceof Entity realVehicle) {
            int vehicleId = realVehicle.getEntityId();

            List<Entity> realPassengers = realVehicle.getPassengers();
            int[] passengerIds;

            if (mount) {
                passengerIds = new int[realPassengers.size() + passengers.length];
                for (int i = 0; i < realPassengers.size(); i++) {
                    passengerIds[i] = realPassengers.get(i).getEntityId();
                }
                for (int i = 0; i < passengers.length; i++) {
                    passengerIds[realPassengers.size() + i] = passengers[i].getEntityId();
                }
            } else {
                passengerIds = new int[realPassengers.size()];
                for (int i = 0; i < realPassengers.size(); i++) {
                    passengerIds[i] = realPassengers.get(i).getEntityId();
                }
            }

            WrapperPlayServerSetPassengers packet =
                    new WrapperPlayServerSetPassengers(vehicleId, passengerIds);

            for (WrapperEntity passenger : passengers) {
                passenger.sendPacketToViewers(packet);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String action = this.mount ? "make fake entities" : "remove fake entities";
        String passenger = this.passengerExpr.toString(event, debug);
        String relation = this.mount ? " ride " : " as passengers of ";
        String vehicle = this.vehicleExpr.toString(event, debug);
        return String.format("%s %s%s%s", action, passenger, relation, vehicle);
    }
}
