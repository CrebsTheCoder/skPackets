package org.crebs.skpackets.elements.team;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.team.expressions.ExprPacketTeam;
import org.crebs.skpackets.elements.team.expressions.ExprTeamCollisionRule;
import org.crebs.skpackets.elements.team.expressions.ExprTeamColor;
import org.crebs.skpackets.elements.team.expressions.ExprTeamDisplayName;
import org.crebs.skpackets.elements.team.expressions.ExprTeamEntries;
import org.crebs.skpackets.elements.team.expressions.ExprTeamFriendlyFire;
import org.crebs.skpackets.elements.team.expressions.ExprTeamName;
import org.crebs.skpackets.elements.team.expressions.ExprTeamNameTagVisibility;
import org.crebs.skpackets.elements.team.expressions.ExprTeamPrefix;
import org.crebs.skpackets.elements.team.expressions.ExprTeamSeeInvisible;
import org.crebs.skpackets.elements.team.expressions.ExprTeamSuffix;
import org.crebs.skpackets.elements.team.section.SecNewPacketTeam;

public final class TeamRegister {

    public static void register(Registration registration) {
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
