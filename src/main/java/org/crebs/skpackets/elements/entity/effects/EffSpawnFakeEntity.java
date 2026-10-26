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

import com.github.shanebeee.skr.Registration;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffSpawnFakeEntity extends Effect {

    public static void register(Registration reg) {
        reg.newEffect(
                        EffSpawnFakeEntity.class,
                        "spawn (fake|packet) [entit(y|ies)] %packetentities% at %location%")
                .name("Fake Entity - Spawn Fake Entity")
                .description(
                        "Spawn a packet entity at a location. An existing managed entity is moved."
                                + " To move use teleport.")
                .examples(
                        """
                        command /preview-spawn-fake-entity:
                            trigger:
                                set {_entity} to a new fake zombie entity:
                                    location: location of player
                                    viewers: player
                                wait 2 seconds
                                spawn fake entity {_entity} at location of player
                                wait 5 seconds
                                kill fake entity {_entity}
                        """)
                .since("1.0.0")
                .register();
    }

    Expression<WrapperEntity> entityExpr;
    Expression<Location> locationExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        this.entityExpr = (Expression<WrapperEntity>) expressions[0];
        this.locationExpr = (Expression<Location>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        WrapperEntity[] entities = this.entityExpr.getAll(event);
        Location location = this.locationExpr.getSingle(event);
        if (location == null) {
            return;
        }

        com.github.retrooper.packetevents.protocol.world.Location peLoc =
                SpigotConversionUtil.fromBukkitLocation(location);
        for (WrapperEntity entity : entities) {
            var manager = org.crebs.skpackets.SkPackets.getInstance().getPacketEntities();
            if (manager.getEntity(entity.getEntityId()).isPresent()) {
                manager.teleport(entity, location);
            } else {
                var viewers =
                        entity.getViewers().stream()
                                .map(org.bukkit.Bukkit::getPlayer)
                                .filter(java.util.Objects::nonNull)
                                .toList();
                manager.spawn(entity, location, viewers, true);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String entity = this.entityExpr.toString(event, debug);
        String location = this.locationExpr.toString(event, debug);
        return String.format("spawn fake %s at %s", entity, location);
    }
}
