package org.crebs.skpackets.elements.team;

import com.github.shanebeee.skr.Registration;
import org.crebs.skpackets.elements.team.expressions.*;
import org.crebs.skpackets.elements.team.section.SecNewPacketTeam;

public final class TeamRegister {

    public static void register(Registration registration) {
        org.crebs.skpackets.elements.team.effects.EffTeamMembers.register(registration);
        registerSections(registration);
        registerExpressions(registration);
    }

    private static void registerSections(Registration registration) {
        SecNewPacketTeam.register(registration);
    }

    private static void registerExpressions(Registration registration) {
        ExprPacketTeam.register(registration);
        ExprTeamName.register(registration);
        ExprTeamEntries.register(registration);
        ExprTeamDisplayName.register(registration);
        ExprTeamPrefix.register(registration);
        ExprTeamSuffix.register(registration);
        ExprTeamColor.register(registration);
        ExprTeamNameTagVisibility.register(registration);
        ExprTeamCollisionRule.register(registration);
        ExprTeamFriendlyFire.register(registration);
        ExprTeamSeeInvisible.register(registration);
    }
}
