package org.crebs.skpackets.elements.team.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.util.Kleenean;
import org.crebs.skpackets.elements.team.PacketTeam;
import com.github.shanebeee.skr.Registration;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.elements.team.TeamEntityEntry;

import java.util.ArrayList;

public final class ExprPacketTeam extends PropertyExpression<Object, PacketTeam> {

    public static void register(Registration registration) {
        registration
                .newPropertyExpression(
                        ExprPacketTeam.class,
                        PacketTeam.class,
                        "(fake|packet) team",
                        "numbers/packetentities/entities")
                .name("Packet Team Of Entity")
                .description(
                        "Gets the packet team assigned to entity IDs, fake entities or Bukkit"
                            + " entities. Use 'add player to packet team {_team}' to assign a team."
                            + " Delete/reset clears the assignment; send the old team again to"
                            + " remove the client entry. Unknown IDs are rejected.")
                .examples(
                        """
                        command /red-squad-expr-packet-team:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                add player to packet team {_team}
                                send packet {_team} to player
                                set {_assigned} to packet team of player
                        """,
                        """
                        command /red-squad-expr-packet-team-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                add player to fake team {_team}
                                send packet {_team} to player
                                set {_assigned} to fake team of player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result) {
        setExpr(expressions[0]);
        return true;
    }

    @Override
    protected PacketTeam[] get(Event event, Object[] source) {
        var teams = new ArrayList<PacketTeam>();
        for (Object entity : source) {
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
        return teams.toArray(PacketTeam[]::new);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        return mode == ChangeMode.DELETE || mode == ChangeMode.RESET
                ? new Class<?>[] {PacketTeam.class}
                : null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        if (mode != ChangeMode.DELETE && mode != ChangeMode.RESET) return;
        for (Object entity : getExpr().getArray(event)) {
            try {
                SkPackets.getInstance()
                        .getPacketTeams()
                        .assign(TeamEntityEntry.resolve(entity), null);
            } catch (IllegalArgumentException ex) {
                Skript.error(ex.getMessage());
            }
        }
    }

    @Override
    public boolean isSingle() {
        return getExpr().isSingle();
    }

    @Override
    public Class<? extends PacketTeam> getReturnType() {
        return PacketTeam.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "packet team of " + getExpr().toString(event, debug);
    }
}
