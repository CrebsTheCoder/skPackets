package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamColor extends TeamPropertyExpression {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(ExprTeamColor.class, Object.class, "team color of %packets%")
                .name("Team Color")
                .description(
                        "The team color name used for member names and glowing outlines. Send the"
                                + " team packet to apply changes to viewers.")
                .examples(
                        """
                        command /red-squad-color:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set team color of {_team} to "red"
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
