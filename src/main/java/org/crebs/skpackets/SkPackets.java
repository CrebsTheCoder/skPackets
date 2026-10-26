package org.crebs.skpackets;

import com.github.shanebeee.skr.Registration;

import org.bukkit.plugin.java.JavaPlugin;
import org.crebs.skpackets.api.entity.PacketEntityManager;
import org.crebs.skpackets.api.simple.FakeBlockManager;
import org.crebs.skpackets.api.simple.FakeEquipmentManager;
import org.crebs.skpackets.api.simple.FakeInventoryManager;
import org.crebs.skpackets.api.team.PacketTeamManager;
import org.crebs.skpackets.elements.ElementCounts;
import org.crebs.skpackets.elements.ElementRegistration;
import org.crebs.skpackets.elements.ElementRuntime;

public final class SkPackets extends JavaPlugin {
    private static SkPackets instance;
    private org.crebs.skpackets.api.simple.PlayerSkinRefresh skinRefresh;
    private FakeInventoryManager fakeInventory;
    private FakeEquipmentManager fakeEquipment;

    public FakeEquipmentManager getFakeEquipment() {
        return fakeEquipment;
    }

    public FakeInventoryManager getFakeInventory() {
        return fakeInventory;
    }

    private PacketEntityManager packetEntities;
    private FakeBlockManager fakeBlocks;

    public FakeBlockManager getFakeBlocks() {
        return fakeBlocks;
    }

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
        fakeBlocks = new FakeBlockManager(this);
        fakeInventory = new FakeInventoryManager(this);
        skinRefresh = new org.crebs.skpackets.api.simple.PlayerSkinRefresh(this);
        fakeEquipment = new FakeEquipmentManager(this);
        registration = new Registration(getName(), true);
        ElementRegistration.register(registration);
        ElementCounts.log(registration, getServer().getConsoleSender());
    }

    @Override
    public void onDisable() {
        if (skinRefresh != null) {
            skinRefresh.close();
            skinRefresh = null;
        }
        if (fakeEquipment != null) {
            fakeEquipment.close();
            fakeEquipment = null;
        }
        if (fakeInventory != null) {
            fakeInventory.close();
            fakeInventory = null;
        }
        if (fakeBlocks != null) {
            fakeBlocks.close();
            fakeBlocks = null;
        }
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
