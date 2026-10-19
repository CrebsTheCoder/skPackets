package org.crebs.skpackets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;

import org.bukkit.plugin.java.JavaPlugin;
import org.crebs.skpackets.api.PacketEntityManager;
import org.crebs.skpackets.elements.ElementRegistration;

public final class SkPackets extends JavaPlugin {
    private static SkPackets instance;
    private PacketEntityManager packetEntities;

    public static SkPackets getInstance() {
        if (instance == null) {
            throw new IllegalStateException("skPackets is not enabled");
        }
        return instance;
    }

    public PacketEntityManager getPacketEntities() {
        if (packetEntities == null) {
            throw new IllegalStateException("skPackets is not enabled");
        }
        return packetEntities;
    }

    @Override
    public void onEnable() {
        EntityLib.init(
                new APIConfig(PacketEvents.getAPI(), new SpigotEntityLibPlatform(this))
                        .disableBStats());
        packetEntities = new PacketEntityManager(this);
        instance = this;
        Registration registration = new Registration(getName(), false);
        ElementRegistration.register(registration);
        getLogger().info("skPackets enabled!");
    }

    @Override
    public void onDisable() {
        if (packetEntities != null) {
            packetEntities.close();
        }
        EntityLib.shutdown();
        packetEntities = null;
        instance = null;
    }
}
