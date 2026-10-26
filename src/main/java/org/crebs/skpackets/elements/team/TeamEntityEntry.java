package org.crebs.skpackets.elements.team;

import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.wrapper.WrapperPlayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.crebs.skpackets.SkPackets;

public final class TeamEntityEntry {
    private TeamEntityEntry() {}

    public static String resolve(Object entity) {
        if (entity instanceof Player player) {
            return player.getName();
        }
        if (entity instanceof Entity real) {
            return real.getUniqueId().toString();
        }
        if (entity instanceof WrapperPlayer player) {
            return player.getUsername();
        }
        if (entity instanceof me.tofaa.entitylib.wrapper.WrapperEntity fake) {
            return fake.getUuid().toString();
        }
        if (entity instanceof Number number) {
            double value = number.doubleValue();
            if (value != number.intValue()) {
                throw new IllegalArgumentException("Entity ID must be a 32-bit integer.");
            }
            int id = number.intValue();
            var nativeEntity =
                    SkPackets.getInstance().getPacketEntities().getEntity(id).orElse(null);
            if (nativeEntity != null) {
                return resolve(nativeEntity);
            }
            var fake = EntityLib.get().getEntity(id);
            if (fake != null) {
                return resolve(fake);
            }
            for (var world : Bukkit.getWorlds()) {
                for (Entity real : world.getEntities()) {
                    if (real.getEntityId() == id) {
                        return resolve(real);
                    }
                }
            }
            throw new IllegalArgumentException(
                    "Unknown entity ID "
                            + id
                            + ". Use a spawned entity or a packet entity; teams need its UUID or"
                            + " player name.");
        }
        throw new IllegalArgumentException(
                "Expected an entity ID, packet entity, fake entity or Bukkit entity.");
    }
}
