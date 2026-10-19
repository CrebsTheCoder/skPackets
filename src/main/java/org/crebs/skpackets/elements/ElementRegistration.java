package org.crebs.skpackets.elements;

import com.github.shanebeee.skr.Registration;

import static org.crebs.skpackets.elements.entity.EntityRegister.registerEntitySyntax;

public final class ElementRegistration {

    public static void register(Registration registration) {
        registerEntitySyntax(registration);
    }
}
