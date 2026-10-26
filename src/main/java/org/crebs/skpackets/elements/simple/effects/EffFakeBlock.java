package org.crebs.skpackets.elements.simple.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.shanebeee.skr.Registration;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.crebs.skpackets.SkPackets;

public final class EffFakeBlock extends Effect {
    private Expression<Location> locations;
    private Expression<BlockData> data;
    private Expression<Player> viewers;
    private boolean persistent;
    private int mode;

    public static void register(Registration registration) {
        registration
                .newEffect(
                        EffFakeBlock.class,
                        "make fake blocks at %locations% as %blockdata% to %players%"
                            + " [persistent:with persistent]",
                        "reset fake blocks at %locations% for %players%",
                        "clear [all] fake blocks for %players%")
                .name("Fake Blocks")
                .description(
                        "Shows client-side block data without changing the world. Persistence"
                            + " defaults to false. Persistent blocks block outgoing updates to their positions"
                            + " and are reapplied after chunk updates and reconnects until reset or cleared; they are"
                            + " not saved across server restarts. Reset restores current real block"
                            + " data. Clear removes every managed fake block for the players. These"
                            + " blocks have no server-side collision or drops. Use block data such"
                            + " as gold block.")
                .examples(
                        """
                        command /golden-floor:
                            trigger:
                                set {_floor} to location of block below player
                                make fake blocks at {_floor} as gold block to player with persistent
                                wait 20 seconds
                                reset fake blocks at {_floor} for player
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
        mode = pattern;
        if (mode == 2) viewers = (Expression<Player>) expressions[0];
        else {
            locations = (Expression<Location>) expressions[0];
            if (mode == 0) {
                data = (Expression<BlockData>) expressions[1];
                viewers = (Expression<Player>) expressions[2];
                persistent = result.hasTag("persistent");
            } else viewers = (Expression<Player>) expressions[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        var manager = SkPackets.getInstance().getFakeBlocks();
        BlockData state = mode == 0 ? data.getSingle(event) : null;
        if (mode == 0 && state == null) return;
        boolean keep = persistent;
        Location[] positions = mode == 2 ? new Location[0] : locations.getArray(event);
        for (Player viewer : viewers.getArray(event)) {
            if (mode == 2) {
                manager.clear(viewer);
                continue;
            }
            for (Location location : positions) {
                try {
                    if (mode == 0) manager.show(viewer, location, state, keep);
                    else manager.reset(viewer, location);
                } catch (IllegalArgumentException ex) {
                    Skript.error(ex.getMessage());
                }
            }
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "manage fake blocks for " + viewers.toString(event, debug);
    }
}
