package org.crebs.skpackets;

import com.github.shanebeee.skr.Registration;
import org.bukkit.plugin.java.JavaPlugin;
import org.crebs.skpackets.elements.ElementRegistration;

public final class SkPackets extends JavaPlugin {

    @Override
    public void onEnable() {
        Registration registration = new Registration(getName(), false);
        ElementRegistration.register(registration);
        getLogger().info("skPackets enabled!");
    }

}
