package it.stockmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Controllo dell'origine (sezione 3 del piano): sulle richieste che modificano dati,
 * l'header Origin deve essere quello del frontend, e i corpi devono essere JSON.
 * Si aggiunge al CSRF token: due barriere indipendenti.
 */
@Component
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> METODI_CHE_MODIFICANO = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final String originAmmessa;

    public OriginCheckFilter(@Value("${app.allowed-origin}") String originAmmessa) {
        this.originAmmessa = originAmmessa;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!METODI_CHE_MODIFICANO.contains(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }
        // Origin mancante o diversa: richiesta non proveniente dal frontend
        if (!originAmmessa.equals(req.getHeader("Origin"))) {
            rifiuta(res, 403, "Origine non consentita");
            return;
        }
        // Un corpo presente deve essere JSON (il tipo "form" aggira alcune protezioni del browser)
        boolean haCorpo = req.getContentLengthLong() > 0 || req.getHeader("Transfer-Encoding") != null;
        String tipo = req.getContentType();
        if (haCorpo && (tipo == null || !tipo.toLowerCase().startsWith("application/json"))) {
            rifiuta(res, 415, "Usa Content-Type application/json");
            return;
        }
        chain.doFilter(req, res);
    }

    private void rifiuta(HttpServletResponse res, int stato, String titolo) throws IOException {
        res.setStatus(stato);
        res.setContentType("application/problem+json");
        res.getWriter().write("{\"title\":\"" + titolo + "\",\"status\":" + stato + "}");
    }
}
