package org.crebs.skpackets.elements.simple.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.crebs.skpackets.SkPackets;

public final class EffFakeInventorySlot extends Effect {
    private boolean persistent;
    private int action;
    private Expression<Number> slots;
    private Expression<ItemType> item;
    private Expression<Player> players;

    public static void register(Registration registration) {
        registration
                .newEffect(
                        EffFakeInventorySlot.class,
                        "make fake inventory slot[s] %numbers% (as|to) %itemtype% to %players%"
                                + " [persistent:with persistent]",
                        "reset fake inventory slot[s] %numbers% for %players%",
                        "(reset|clear) [all] fake inventory [slots] for %players%")
                .name("Fake Inventory Slots")
                .description(
                        "Displays fake items in raw slots of the current inventory view. Add 'with"
                            + " persistent' to keep overriding server updates and refresh after"
                            + " clicks. Without it, the display is one-time and replaces any"
                            + " existing persistent override for that slot. Player inventory: 0"
                            + " crafting result, 1-4 crafting inputs, 5-8 armor, 9-35 storage,"
                            + " 36-44 hotbar, 45 offhand. Container slots use their own numbering;"
                            + " wait one tick after opening. Player-inventory overrides last until"
                            + " disconnect; container overrides end on close. Items are visual"
                            + " only. Creative mode can request real item creation."
                            + " Reset slots to restore their current real items and remove persistence."
                            + " Reset/clear the whole fake inventory to remove all overrides.")
                .examples(
                        """
                        command /phantom-recipe:
                            trigger:
                                if gamemode of player is creative:
                                    send "Use survival or adventure to preview this." to player
                                    stop
                                close player's inventory
                                wait 1 tick
                                make fake inventory slots 1, 2, 3 and 4 as diamond to player with persistent
                                make fake inventory slot 0 as diamond block to player with persistent
                                send "Open your inventory to see the fake recipe." to player
                                wait 10 seconds
                                reset fake inventory slots 0, 1, 2, 3 and 4 for player
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
            players = (Expression<Player>) expressions[0];
        } else {
            slots = (Expression<Number>) expressions[0];
            if (action == 0) item = (Expression<ItemType>) expressions[1];
            players = (Expression<Player>) expressions[action == 0 ? 2 : 1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        var manager = SkPackets.getInstance().getFakeInventory();
        if (action == 2) {
            for (Player player : players.getArray(event)) manager.clear(player);
            return;
        }
        ItemStack display = null;
        if (action == 0) {
            ItemType type = item.getSingle(event);
            if (type == null) return;
            display = type.getRandom();
            if (display == null) return;
        }
        Number[] values = slots.getArray(event);
        for (Player player : players.getArray(event)) {
            for (Number value : values) {
                double number = value.doubleValue();
                if (!Double.isFinite(number)
                        || number != Math.rint(number)
                        || number < 0
                        || number > Integer.MAX_VALUE) {
                    Skript.error("Fake inventory slots require nonnegative whole numbers.");
                    continue;
                }
                try {
                    if (action == 1) manager.reset(player, value.intValue());
                    else manager.show(player, value.intValue(), display, persistent);
                } catch (IllegalArgumentException ex) {
                    Skript.error(ex.getMessage());
                }
            }
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return (action == 0 ? "make" : "reset") + " fake inventory slots for " + players.toString(event, debug);
    }
}
