package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamDisplayName extends TeamPropertyExpression {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprTeamDisplayName.class, Object.class, "team display name of %packets%")
                .name("Team Displayname")
                .description(
                        "The display name of the scoreboard team. Send the team packet to apply"
                                + " changes to viewers.")
                .examples(
                        """
                        command /hologram-name:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set team display name of {_team} to "Red Team"
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.DISPLAY_NAME;
    }
}
