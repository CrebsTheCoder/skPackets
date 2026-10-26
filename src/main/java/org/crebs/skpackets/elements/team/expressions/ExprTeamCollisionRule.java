package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamCollisionRule extends TeamPropertyExpression<Object> {

    public static void register(Registration registration) {
        registration
                .newPropertyExpression(
                        ExprTeamCollisionRule.class,
                        Object.class,
                        "(fake|packet) team collision rule", "packets")
                .name("Team Collisionrule")
                .description(
                        "Controls collisions: always, never, push other teams, or push own team."
                                + " Send the team packet to apply changes to viewers.")
                .examples(
                        """
                        command /red-squad-collision-rule:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set packet team collision rule of {_team} to "never"
                                send packet {_team} to player
                        """,
                        """
                        command /red-squad-collision-rule-fake:
                            trigger:
                                set {_team} to a new fake team:
                                    name: "demo"
                                set fake team collision rule of {_team} to "never"
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.COLLISION_RULE;
    }
}
