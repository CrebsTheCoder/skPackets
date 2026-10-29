package org.crebs.skpackets.elements.team;

import com.github.shanebeee.skr.Registration;
import org.crebs.skpackets.elements.RegistrationModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.crebs.skpackets.elements.team.effects.EffTeamMembers;
import org.crebs.skpackets.elements.team.expressions.*;
import org.crebs.skpackets.elements.team.section.SecNewPacketTeam;

public record TeamModule(Registration registration) implements RegistrationModule {

    @Override
    public String name() {
        return "team";
    }

    @Override
    public void load(SkriptAddon addon) {
        register(registration,
                EffTeamMembers::register,
                SecNewPacketTeam::register,
                ExprPacketTeam::register,
                ExprTeamName::register,
                ExprTeamEntries::register,
                ExprTeamDisplayName::register,
                ExprTeamPrefix::register,
                ExprTeamSuffix::register,
                ExprTeamColor::register,
                ExprTeamNameTagVisibility::register,
                ExprTeamCollisionRule::register,
                ExprTeamFriendlyFire::register,
                ExprTeamSeeInvisible::register);
    }
}
