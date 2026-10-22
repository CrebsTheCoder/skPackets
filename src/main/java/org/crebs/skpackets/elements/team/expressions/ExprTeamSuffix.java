package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamSuffix extends TeamPropertyExpression {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(ExprTeamSuffix.class, Object.class, "team suffix of %packets%")
                .name("Team Suffix")
                .description(
                        "The text shown after names of team members. Send the team packet to apply"
                                + " changes to viewers.")
                .examples(
                        """
                        command /red-squad-suffix:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set team suffix of {_team} to ""
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.SUFFIX;
    }
}
