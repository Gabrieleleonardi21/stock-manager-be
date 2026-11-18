package it.stockmanager.security;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import it.stockmanager.common.BodySizeFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Sicurezza HTTP (sezioni 2, 3, 4 e 7 del piano).
 * Regola: tutto richiede sessione, tranne le rotte elencate una per una. Mai permitAll su prefissi.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** CSP definitiva (fase 6): tutto dalla stessa origine, font inclusi. Nessun dominio esterno. */
    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "font-src 'self'; img-src 'self' data:; connect-src 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'";

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt con costo 12 (convenzione del team, decisione del piano)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OriginCheckFilter originCheck,
                                                   SessionLifetimeFilter lifetime,
                                                   BodySizeFilter bodySize) throws Exception {
        http
            // Filtri custom: prima la dimensione del corpo, poi l'origine, poi la scadenza della sessione
            .addFilterBefore(bodySize, CsrfFilter.class)
            .addFilterBefore(originCheck, CsrfFilter.class)
            .addFilterAfter(lifetime, CsrfFilter.class)
            // CSRF attivo (decisione del team): token nel cookie XSRF-TOKEN, letto dal frontend
            // Per una SPA: il frontend rimanda il valore "piatto" del cookie nell'header X-XSRF-TOKEN
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()::handle))
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // File statici dell'app e rotte del frontend: non contengono dati, i dati restano dietro /api
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/fonts/**", "/logo.png",
                        "/favicon.ico", "/azienda", "/azienda/**", "/report", "/report/**").permitAll()
                .anyRequest().authenticated())
            // Risposte 401 senza dettagli, in formato problem+json
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(401);
                res.setContentType("application/problem+json");
                res.getWriter().write("{\"title\":\"Accesso richiesto\",\"status\":401}");
            }))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())
            .headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives(CSP))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .permissionsPolicy(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()")));
        return http.build();
    }

    /** Forza la scrittura del cookie XSRF-TOKEN: senza, il frontend non ha il token al primo invio. */
    static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                        jakarta.servlet.FilterChain chain) throws jakarta.servlet.ServletException, IOException {
            org.springframework.security.web.csrf.CsrfToken token =
                    (org.springframework.security.web.csrf.CsrfToken) req.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
            if (token != null) {
                token.getToken(); // la lettura del valore fa scrivere il cookie
            }
            chain.doFilter(req, res);
        }
    }
}
