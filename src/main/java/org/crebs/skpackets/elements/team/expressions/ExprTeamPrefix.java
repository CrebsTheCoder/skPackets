package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamPrefix extends TeamPropertyExpression {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(ExprTeamPrefix.class, Object.class, "team prefix of %packets%")
                .name("Team Prefix")
                .description(
                        "The text shown before names of team members. Send the team packet to apply"
                                + " changes to viewers.")
                .examples(
                        """
                        command /red-squad-prefix:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set team prefix of {_team} to colored "&c[Red] "
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.PREFIX;
    }
}
