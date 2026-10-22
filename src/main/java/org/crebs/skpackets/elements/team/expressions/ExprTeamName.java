package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamName extends TeamPropertyExpression {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(ExprTeamName.class, Object.class, "team name of %packets%")
                .name("Team Name")
                .description(
                        "The scoreboard identifier of the team. Changing it recreates the team for"
                                + " viewers when sent. Send the team packet to apply changes to"
                                + " viewers.")
                .examples(
                        """
                        command /red-squad-name:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set team name of {_team} to "red"
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.NAME;
    }
}
