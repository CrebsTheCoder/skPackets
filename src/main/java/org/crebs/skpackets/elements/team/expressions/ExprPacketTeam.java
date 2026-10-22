package org.crebs.skpackets.elements.team.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.shanebeee.skr.Registration;

import org.bukkit.event.Event;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.elements.team.PacketTeam;
import org.crebs.skpackets.elements.team.TeamEntityEntry;

import java.util.ArrayList;

public final class ExprPacketTeam extends SimpleExpression<PacketWrapper> {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprPacketTeam.class,
                        PacketWrapper.class,
                        "packet team of %numbers/packetentities/entities%")
                .name("Packet Team Of Entity")
                .description(
                        "Gets or assigns a packet team to entity IDs, native packet entities,"
                            + " compatible fake entities or Bukkit entities. Assignment adds the"
                            + " entity UUID (or player name) to the team and removes it from its"
                            + " previous definition. Send the team to apply it. Delete/reset clears"
                            + " the assignment; send the old team again to remove the client entry."
                            + " Unknown IDs are rejected.")
                .examples(
                        """
                        command /red-squad-expr-packet-team:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set packet team of player to {_team}
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    private Expression<?> entities;

    @Override
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result) {
        entities = expressions[0];
        return true;
    }

    @Override
    protected PacketWrapper[] get(Event event) {
        var teams = new ArrayList<PacketWrapper>();
        for (Object entity : entities.getArray(event)) {
            try {
                var team =
                        SkPackets.getInstance()
                                .getPacketTeams()
                                .get(TeamEntityEntry.resolve(entity));
                if (team != null) {
                    teams.add(team);
                }
            } catch (IllegalArgumentException ex) {
                Skript.error(ex.getMessage());
            }
        }
        return teams.toArray(PacketWrapper[]::new);
    }

    @Override
    public Class<?>[] acceptChange(ChangeMode mode) {
        return mode == ChangeMode.SET || mode == ChangeMode.DELETE || mode == ChangeMode.RESET
                ? new Class<?>[] {PacketWrapper.class}
                : null;
    }

    @Override
    public void change(Event event, Object[] delta, ChangeMode mode) {
        PacketTeam team = null;
        if (mode == ChangeMode.SET) {
            if (delta == null || delta.length != 1 || !(delta[0] instanceof PacketTeam value)) {
                Skript.error("Assign a team made with 'a new packet team'.");
                return;
            }
            team = value;
        }
        for (Object entity : entities.getArray(event)) {
            try {
                SkPackets.getInstance()
                        .getPacketTeams()
                        .assign(TeamEntityEntry.resolve(entity), team);
            } catch (IllegalArgumentException ex) {
                Skript.error(ex.getMessage());
            }
        }
    }

    @Override
    public boolean isSingle() {
        return entities.isSingle();
    }

    @Override
    public Class<? extends PacketWrapper> getReturnType() {
        return PacketWrapper.class;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "packet team of " + entities.toString(event, debug);
    }
}
