package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamSeeInvisible extends TeamPropertyExpression<Object> {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprTeamSeeInvisible.class,
                        Object.class,
                        "(fake|packet) team see invisible teammates of %packets%")
                .name("Team Seeinvisible")
                .description(
                        "Whether teammates can see invisible members of this team. Send the team"
                                + " packet to apply changes to viewers.")
                .examples(
                        """
                        command /red-squad-see-invisible:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set packet team see invisible teammates of {_team} to true
                                send packet {_team} to player
                        """,
                        """
                        command /red-squad-see-invisible-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                set fake team see invisible teammates of {_team} to true
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.SEE_INVISIBLE;
    }
}
