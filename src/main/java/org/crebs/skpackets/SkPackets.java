package org.crebs.skpackets;

import com.github.shanebeee.skr.Registration;

import org.bukkit.plugin.java.JavaPlugin;
import org.crebs.skpackets.api.entity.PacketEntityManager;
import org.crebs.skpackets.api.team.PacketTeamManager;
import org.crebs.skpackets.elements.ElementCounts;
import org.crebs.skpackets.elements.ElementRegistration;
import org.crebs.skpackets.elements.ElementRuntime;

public final class SkPackets extends JavaPlugin {
    private static SkPackets instance;
    private PacketEntityManager packetEntities;
    private final PacketTeamManager packetTeams = new PacketTeamManager();

    public PacketTeamManager getPacketTeams() {
        return packetTeams;
    }

    private ElementRuntime elementRuntime;
    private Registration registration;

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

    public Registration getRegistration() {
        return registration;
    }

    @Override
    public void onEnable() {
        try {
            Class.forName("net.kyori.adventure.util.Buildable");
        } catch (ClassNotFoundException exception) {
            getLogger()
                    .severe(
                            "PacketEvents 2.14 requires Adventure 4. Use Paper 26.1.2; Adventure 5"
                                    + " servers are not supported yet.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        packetEntities = new PacketEntityManager(this);
        instance = this;
        getServer().getPluginManager().registerEvents(packetTeams, this);
        elementRuntime = new ElementRuntime(this);
        registration = new Registration(getName(), true);
        ElementRegistration.register(registration);
        ElementCounts.log(registration, getServer().getConsoleSender());
    }

    @Override
    public void onDisable() {
        if (packetEntities != null) {
            packetEntities.close();
        }
        if (elementRuntime != null) {
            elementRuntime.close();
            elementRuntime = null;
        }
        packetTeams.clear();
        packetEntities = null;
        registration = null;
        instance = null;
    }
}
