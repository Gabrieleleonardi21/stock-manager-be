package it.stockmanager.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rifiuta con 413 le richieste con corpo oltre 100 KB (2 MB per l'import), prima che arrivino ai controller.
 * Limite della sezione 5 del piano: protegge da corpi enormi e collezioni troppo grandi.
 * Nota: copre le richieste con Content-Length; i corpi "chunked" senza lunghezza sono
 * limitati dal server (Tomcat) e andranno verificati in fase di deploy.
 */
@Component
public class BodySizeFilter extends OncePerRequestFilter {

    static final long LIMITE_BYTE = 100L * 1024L;
    /** L'import di un magazzino intero e' piu' grande: limite proprio, solo per /api/import/. */
    static final long LIMITE_IMPORT_BYTE = 2L * 1024L * 1024L;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long limite = LIMITE_BYTE;
        if (req.getRequestURI().startsWith("/api/import/")) {
            limite = LIMITE_IMPORT_BYTE;
        }
        if (req.getContentLengthLong() > limite) {
            res.setStatus(413);
            res.setContentType("application/problem+json");
            res.getWriter().write("{\"title\":\"Richiesta troppo grande\",\"status\":413}");
            return;
        }
        chain.doFilter(req, res);
    }
}
