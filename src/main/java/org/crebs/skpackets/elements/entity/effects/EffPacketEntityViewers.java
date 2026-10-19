package org.crebs.skpackets.elements.entity.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.crebs.skpackets.SkPackets;

public class EffPacketEntityViewers extends Effect {
    private Expression<? extends OfflinePlayer> viewers;
    private Expression<WrapperEntity> entities;
    private boolean add;

    public static void register(Registration registration) {
        registration
                .newEffect(
                        EffPacketEntityViewers.class,
                        "add %players% to [the] viewers of packet entit(y|ies) %packetentities%",
                        "remove %offlineplayers% from [the] viewers of packet entit(y|ies)"
                                + " %packetentities%")
                .name("Packet Entity Viewers")
                .description(
                        "Adds online viewers or permanently removes viewers, including offline"
                                + " players. Updates persistence tracking. Removing the last viewer"
                                + " hides the entity without destroying it.")
                .examples(
                        "add player to viewers of packet entity {_mob}",
                        "remove player from viewers of packet entity {_mob}")
                .since("1.0.0")
                .register();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            Expression<?>[] expressions, int pattern, Kleenean delayed, ParseResult result) {
        viewers = (Expression<? extends OfflinePlayer>) expressions[0];
        entities = (Expression<WrapperEntity>) expressions[1];
        add = pattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        var manager = SkPackets.getInstance().getPacketEntities();
        OfflinePlayer[] targets = viewers.getArray(event);
        for (WrapperEntity entity : entities.getArray(event)) {
            for (OfflinePlayer viewer : targets) {
                try {
                    if (add) {
                        manager.addViewer(entity, (Player) viewer);
                    } else {
                        manager.removeViewer(entity, viewer.getUniqueId());
                    }
                } catch (IllegalArgumentException exception) {
                    Skript.error(exception.getMessage());
                }
            }
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return (add ? "add " : "remove ")
                + viewers.toString(event, debug)
                + (add ? " to viewers of " : " from viewers of ")
                + entities.toString(event, debug);
    }
}
