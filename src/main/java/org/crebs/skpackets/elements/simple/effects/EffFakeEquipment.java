package org.crebs.skpackets.elements.simple.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.shanebeee.skr.Registration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class EffFakeEquipment extends Effect {
    private Expression<EquipmentSlot> slot;
    private Expression<Entity> targets;
    private Expression<ItemType> item;
    private Expression<Player> viewers;
    private boolean persistent;
    private int action;

    public static void register(Registration registration) {
        registration
                .newEffect(
                        EffFakeEquipment.class,
                        "make fake equipment %equipmentslot% of %entities% as %itemtype% to %players%"
                                + " [persistent:with persistent]",
                        "reset fake equipment %equipmentslots% of %entities% for %players%",
                        "(reset|clear) [all] fake equipment of %entities% for %players%")
                .name("Fake Equipment")
                .description(
                        "Changes the equipment shown on real living entities for chosen viewers"
                            + " without changing inventories. Accepts player's held item or"
                            + " player's helmet. Uses Skript equipment slots, such as main hand and helmet slot."
                            + " Add 'with persistent' to keep the appearance across equipment updates,"
                            + " tracking changes and reconnects. Each viewer/target/slot stores only one item."
                            + " Overrides end when a non-player target unloads or dies, or the plugin stops."
                            + " Without the flag this is a one-time"
                            + " update and replaces the previous persistent override for that slot."
                            + " Reset restores current real equipment and removes the persistent overrides."
                            + " Does not change the owner's inventory or first-person held item.")
                .examples(
                        """
                        command /disguise-my-sword:
                            trigger:
                                make fake equipment main hand of player as diamond sword to all players with persistent
                                wait 10 seconds
                                reset fake equipment main hand of player for all players
                        """,
                        """
                        command /copy-my-helmet:
                            trigger:
                                set {_target} to target entity of player
                                if {_target} is not set:
                                    stop
                                make fake equipment helmet slot of {_target} as player's helmet to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result) {
        action = pattern;
        persistent = result.hasTag("persistent");
        if (action == 2) {
            targets = (Expression<Entity>) expressions[0];
            viewers = (Expression<Player>) expressions[1];
        } else {
            slot = (Expression<EquipmentSlot>) expressions[0];
            targets = (Expression<Entity>) expressions[1];
            if (action == 0) item = (Expression<ItemType>) expressions[2];
            viewers = (Expression<Player>) expressions[action == 0 ? 3 : 2];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        EquipmentSlot[] selected = action == 2 ? new EquipmentSlot[0] : slot.getArray(event);
        ItemStack shown = null;
        if (action == 0) {
            ItemType type = item.getSingle(event);
            if (type == null) return;
            shown = type.getRandom();
            if (shown == null) return;
        }
        var manager = org.crebs.skpackets.SkPackets.getInstance().getFakeEquipment();
        Player[] receivers = viewers.getArray(event);
        for (Entity target : targets.getArray(event)) {
            if (!(target instanceof LivingEntity living) || living.getEquipment() == null) {
                Skript.error("Displayed equipment requires a living entity with equipment.");
                continue;
            }
            for (Player viewer : receivers) {
                if (action == 2) manager.reset(viewer, living);
                else for (EquipmentSlot selectedSlot : selected) {
                    if (action == 1) manager.reset(viewer, living, selectedSlot);
                    else manager.show(viewer, living, selectedSlot, shown, persistent);
                }
            }
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return (action == 0 ? "make" : "reset")
                + " fake equipment "
                + (slot == null ? "all" : slot.toString(event, debug))
                + " of "
                + targets.toString(event, debug);
    }
}
