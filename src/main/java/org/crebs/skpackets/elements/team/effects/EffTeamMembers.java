package org.crebs.skpackets.elements.team.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.shanebeee.skr.Registration;

import org.bukkit.event.Event;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.elements.team.PacketTeam;
import org.crebs.skpackets.elements.team.TeamEntityEntry;

public final class EffTeamMembers extends Effect {
    private Expression<?> members;
    private Expression<PacketWrapper> team;
    private boolean remove;

    public static void register(Registration registration) {
        registration
                .newEffect(
                        EffTeamMembers.class,
                        "add %entities/packetentities/strings% to [the] (fake|packet) team"
                            + " %packet%",
                        "remove %entities/packetentities/strings% from [the] (fake|packet) team"
                            + " %packet%")
                .name("Packet Team Members")
                .description(
                        "Adds or removes players, entities, packet entities or entry strings from a"
                            + " packet team. Players use their names; other entities use UUIDs."
                            + " Adding moves the entry from its previous team definition. Removal"
                            + " only affects the specified team. Send the team packet to apply"
                            + " changes to viewers. The fake and packet prefixes are aliases.")
                .examples(
                        """
                        command /team-members:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "members"
                                add player to the packet team {_team}
                                send packet {_team} to player
                                wait 5 seconds
                                remove player from the packet team {_team}
                                send packet {_team} to player
                        """,
                        """
                        command /team-members-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "members"
                                add player to the fake team {_team}
                                send packet {_team} to player
                                wait 5 seconds
                                remove player from the fake team {_team}
                                send packet {_team} to player
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
        members = expressions[0];
        team = (Expression<PacketWrapper>) expressions[1];
        remove = pattern == 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        PacketWrapper value = team.getSingle(event);
        if (value == null) return;
        if (!(value instanceof PacketTeam packetTeam)) {
            Skript.error("Use a team made with 'a new fake team' or 'a new packet team'.");
            return;
        }
        var manager = SkPackets.getInstance().getPacketTeams();
        for (Object member : members.getArray(event)) {
            try {
                String entry =
                        member instanceof String text ? text : TeamEntityEntry.resolve(member);
                if (entry.isEmpty() || entry.length() > 40) {
                    throw new IllegalArgumentException(
                            "Team entries must contain 1 to 40 characters.");
                }
                if (remove) manager.remove(entry, packetTeam);
                else manager.assign(entry, packetTeam);
            } catch (IllegalArgumentException exception) {
                Skript.error(exception.getMessage());
            }
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return (remove ? "remove " : "add ")
                + members.toString(event, debug)
                + (remove ? " from packet team " : " to packet team ")
                + team.toString(event, debug);
    }
}
