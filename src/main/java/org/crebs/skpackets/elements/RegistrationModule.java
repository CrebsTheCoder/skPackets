package org.crebs.skpackets.elements;

import com.github.shanebeee.skr.Registration;
import org.skriptlang.skript.addon.AddonModule;

public interface RegistrationModule extends AddonModule {

    default void register(Registration registration, Registrar... registrars) {
        for (Registrar registrar : registrars) {
            registrar.register(registration);
        }
    }

    @FunctionalInterface
    interface Registrar {
        void register(Registration registration);
    }
}
