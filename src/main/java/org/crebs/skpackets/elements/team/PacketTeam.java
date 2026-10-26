package org.crebs.skpackets.elements.team;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;

import java.util.List;

public final class PacketTeam extends WrapperPlayServerTeams {
    public PacketTeam(String name) {
        super(name, TeamMode.CREATE, TeamField.defaults(name), List.of());
    }
}
