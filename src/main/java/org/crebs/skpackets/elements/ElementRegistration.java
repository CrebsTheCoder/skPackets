package org.crebs.skpackets.elements;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.entity.EntityRegister;
import org.crebs.skpackets.elements.general.GeneralElementRegistration;
import org.crebs.skpackets.elements.simple.SimpleElementRegistration;
import org.crebs.skpackets.elements.team.TeamRegister;

public final class ElementRegistration {

    public static void register(Registration registration) {
        GeneralElementRegistration.register(registration);
        SimpleElementRegistration.register(registration);
        EntityRegister.register(registration);
        TeamRegister.register(registration);
        registration.finalizeRegistration();
    }
}
