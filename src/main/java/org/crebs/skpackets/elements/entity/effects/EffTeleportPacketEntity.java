package org.crebs.skpackets.elements.entity.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.crebs.skpackets.SkPackets;

public class EffTeleportPacketEntity extends Effect {
    private Expression<WrapperEntity> entities;
    private Expression<Location> location;

    public static void register(Registration registration) {
        registration
                .newEffect(
                        EffTeleportPacketEntity.class,
                        "teleport packet entit(y|ies) %packetentities% to %location%")
                .name("Teleport Packet Entities")
                .description(
                        "Moves packet entities and updates the position and world used when"
                                + " restoring their viewers.")
                .examples(
                        "teleport packet entity {_mob} to {_loc}",
                        "teleport packet entities {_mobs::*} to location of player")
                .since("1.0.0")
                .register();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            Expression<?>[] expressions, int pattern, Kleenean delayed, ParseResult result) {
        entities = (Expression<WrapperEntity>) expressions[0];
        location = (Expression<Location>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Location target = location.getSingle(event);
        if (target == null) {
            return;
        }
        var manager = SkPackets.getInstance().getPacketEntities();
        for (WrapperEntity entity : entities.getArray(event)) {
            try {
                manager.teleport(entity, target);
            } catch (IllegalArgumentException exception) {
                Skript.error(exception.getMessage());
            }
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "teleport packet entities "
                + entities.toString(event, debug)
                + " to "
                + location.toString(event, debug);
    }
}
