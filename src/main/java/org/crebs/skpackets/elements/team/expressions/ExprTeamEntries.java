package org.crebs.skpackets.elements.team.expressions;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.TeamField;

public final class ExprTeamEntries extends TeamPropertyExpression {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprTeamEntries.class, Object.class, "team entries of %packets%")
                .name("Team Entries")
                .description(
                        "The player names or entity UUID strings assigned to the team. Supports"
                            + " set, add, remove and reset. Send the team packet to apply changes"
                            + " to viewers.")
                .examples(
                        """
                        command /red-squad-entries:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "demo"
                                set team entries of {_team} to name of player
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    protected TeamField field() {
        return TeamField.ENTRIES;
    }
}
