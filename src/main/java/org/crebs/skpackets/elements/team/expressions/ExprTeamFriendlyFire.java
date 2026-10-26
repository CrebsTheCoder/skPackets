package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamFriendlyFire extends TeamPropertyExpression<Object> {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprTeamFriendlyFire.class, Object.class, "(fake|packet) team friendly fire of %packets%")
                .name("Team Friendlyfire")
                .description(
                        "Whether friendly fire is enabled in the client team definition. This"
                            + " packet does not enforce server damage rules. Send the team packet"
                            + " to apply changes to viewers.")
                .examples(
                        """
                        command /red-squad-friendly-fire:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set packet team friendly fire of {_team} to false
                                send packet {_team} to player
                        """,
                        """
                        command /red-squad-friendly-fire-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                set fake team friendly fire of {_team} to false
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.FRIENDLY_FIRE;
    }
}
