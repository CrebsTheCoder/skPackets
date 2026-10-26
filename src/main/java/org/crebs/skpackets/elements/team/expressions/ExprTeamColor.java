package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamColor extends TeamPropertyExpression<ch.njol.skript.util.Color> {

    public static void register(Registration registration) {
        registration
                .newPropertyExpression(
                        ExprTeamColor.class,
                        ch.njol.skript.util.Color.class,
                        "(fake|packet) team color", "packets")
                .name("Team Color")
                .description(
                        "The Skript color used for member names and glowing outlines. Send the team"
                                + " packet to apply changes to viewers. RGB colors use the nearest"
                                + " supported team color.")
                .examples(
                        """
                        command /red-squad-color:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set packet team color of {_team} to red
                                send packet {_team} to player
                        """,
                        """
                        command /red-squad-color-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                set fake team color of {_team} to red
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.COLOR;
    }
}
