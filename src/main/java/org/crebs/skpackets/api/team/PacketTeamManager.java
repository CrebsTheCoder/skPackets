package org.crebs.skpackets.api.team;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;

import org.crebs.skpackets.elements.team.PacketTeam;

import java.util.*;
import java.util.function.Consumer;

public final class PacketTeamManager implements org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        forget(event.getPlayer().getUniqueId());
    }

    private record Sent(String name, Set<String> entries) {}

    private final Map<UUID, Map<PacketTeam, Sent>> viewers = new HashMap<>();
    private final Set<PacketTeam> teams = Collections.newSetFromMap(new WeakHashMap<>());

    public synchronized void track(PacketTeam team) {
        teams.add(team);
    }

    public synchronized PacketTeam get(String entry) {
        return teams.stream()
                .filter(team -> team.getPlayers().contains(entry))
                .findFirst()
                .orElse(null);
    }

    public synchronized void assign(String entry, PacketTeam team) {
        for (PacketTeam previous : teams) {
            if (previous != team) {
                if (previous.getPlayers().contains(entry)) {
                    var remaining = new LinkedHashSet<>(previous.getPlayers());
                    remaining.remove(entry);
                    previous.setPlayers(remaining);
                }
            }
        }
        if (team != null) {
            track(team);
            var entries = new LinkedHashSet<>(team.getPlayers());
            entries.add(entry);
            team.setPlayers(entries);
        }
    }

    public synchronized void remove(String entry, PacketTeam team) {
        var remaining = new LinkedHashSet<>(team.getPlayers());
        if (remaining.remove(entry)) team.setPlayers(remaining);
    }

    public synchronized void send(
            UUID viewer, PacketTeam team, Consumer<WrapperPlayServerTeams> send) {
        track(team);
        Map<PacketTeam, Sent> known =
                viewers.computeIfAbsent(viewer, ignored -> new IdentityHashMap<>());
        Sent previous = known.get(team);
        String name = team.getTeamName();
        Set<String> entries = new LinkedHashSet<>(team.getPlayers());

        if (previous != null && !previous.name().equals(name)) {
            send.accept(packet(previous.name(), TeamMode.REMOVE, null, List.of()));
            known.remove(team);
            previous = null;
        }

        for (var iterator = known.entrySet().iterator(); iterator.hasNext(); ) {
            var other = iterator.next();
            if (other.getKey() != team && other.getValue().name().equals(name)) {
                previous = other.getValue();
                iterator.remove();
            }
        }
        if (previous == null) {
            send.accept(packet(name, TeamMode.CREATE, team.getTeamInfo().orElseThrow(), entries));
        } else {
            send.accept(packet(name, TeamMode.UPDATE, team.getTeamInfo().orElseThrow(), List.of()));
            Set<String> removed = new LinkedHashSet<>(previous.entries());
            removed.removeAll(entries);
            if (!removed.isEmpty()) {
                send.accept(packet(name, TeamMode.REMOVE_ENTITIES, null, removed));
            }
            Set<String> added = new LinkedHashSet<>(entries);
            added.removeAll(previous.entries());
            if (!added.isEmpty()) {
                send.accept(packet(name, TeamMode.ADD_ENTITIES, null, added));
            }
        }

        known.replaceAll(
                (other, state) -> {
                    if (other == team || Collections.disjoint(state.entries(), entries)) {
                        return state;
                    }
                    Set<String> remaining = new LinkedHashSet<>(state.entries());
                    remaining.removeAll(entries);
                    return new Sent(state.name(), Set.copyOf(remaining));
                });
        known.put(team, new Sent(name, Set.copyOf(entries)));
    }

    private static WrapperPlayServerTeams packet(
            String name,
            TeamMode mode,
            WrapperPlayServerTeams.ScoreBoardTeamInfo info,
            Collection<String> entries) {
        var copy =
                info == null
                        ? null
                        : new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                                info.getLegacyDisplayName(),
                                info.getLegacyPrefix(),
                                info.getLegacySuffix(),
                                info.getTagVisibility(),
                                info.getCollisionRule(),
                                info.getColor(),
                                info.getOptionData());
        return new WrapperPlayServerTeams(name, mode, copy, new ArrayList<>(entries));
    }

    public synchronized void forget(UUID viewer) {
        viewers.remove(viewer);
    }

    public synchronized void clear() {
        viewers.clear();
        teams.clear();
    }
}
