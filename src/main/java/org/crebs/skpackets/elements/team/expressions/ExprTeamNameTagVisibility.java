package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamNameTagVisibility extends TeamPropertyExpression<Object> {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprTeamNameTagVisibility.class,
                        Object.class,
                        "(fake|packet) team name tag visibility of %packets%")
                .name("Team Nametagvisibility")
                .description(
                        "Controls name tag visibility: always, never, hide for other teams, or hide"
                            + " for own team. Send the team packet to apply changes to viewers.")
                .examples(
                        """
                        command /red-squad-name-tag-visibility:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set packet team name tag visibility of {_team} to "always"
                                send packet {_team} to player
                        """,
                        """
                        command /red-squad-name-tag-visibility-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                set fake team name tag visibility of {_team} to "always"
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.NAME_TAG_VISIBILITY;
    }
}
