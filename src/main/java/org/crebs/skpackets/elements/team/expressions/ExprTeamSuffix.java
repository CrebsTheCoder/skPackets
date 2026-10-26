package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamSuffix extends TeamPropertyExpression<Object> {

    public static void register(Registration registration) {
        registration
                .newPropertyExpression(ExprTeamSuffix.class, Object.class, "(fake|packet) team suffix", "packets")
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
                                set packet team suffix of {_team} to ""
                                send packet {_team} to player
                        """,
                        """
                        command /red-squad-suffix-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                set fake team suffix of {_team} to ""
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
