package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamDisplayName extends TeamPropertyExpression<Object> {

    public static void register(Registration registration) {
        registration
                .newPropertyExpression(
                        ExprTeamDisplayName.class, Object.class, "(fake|packet) team display name", "packets")
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
                                set packet team display name of {_team} to "Red Team"
                                send packet {_team} to player
                        """,
                        """
                        command /hologram-name-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                set fake team display name of {_team} to "Red Team"
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
