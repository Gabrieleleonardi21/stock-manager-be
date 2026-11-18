package it.stockmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Timeout assoluto della sessione (sezione 2): oltre 12 ore la sessione viene chiusa
 * anche se l'utente e' sempre attivo. Il timeout di inattivita' (30 minuti) e' in application.yaml.
 */
@Component
public class SessionLifetimeFilter extends OncePerRequestFilter {

    /** Attributo di sessione con l'istante del login. */
    public static final String ATTRIBUTO_LOGIN = "stockmanager.loginAt";
    static final Duration DURATA_MASSIMA = Duration.ofHours(12);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        HttpSession sessione = req.getSession(false);
        if (sessione != null) {
            Instant login = (Instant) sessione.getAttribute(ATTRIBUTO_LOGIN);
            if (login != null && login.plus(DURATA_MASSIMA).isBefore(Instant.now())) {
                sessione.invalidate();
            }
        }
        chain.doFilter(req, res);
    }
}
