package it.stockmanager.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;

/**
 * Utente corrente, sempre dalla sessione (sezione 4): mai dal body o dai parametri.
 * Il nome del principal e' l'id UUID dell'utente.
 */
public final class CurrentUsers {

    private CurrentUsers() {}

    public static UUID require(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Accesso richiesto");
        }
        return UUID.fromString(auth.getName());
    }
}
