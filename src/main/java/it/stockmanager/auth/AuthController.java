package it.stockmanager.auth;

import it.stockmanager.auth.AuthDto.LoginRequest;
import it.stockmanager.auth.AuthDto.RegisterRequest;
import it.stockmanager.auth.AuthDto.UserResponse;
import it.stockmanager.security.CurrentUsers;
import it.stockmanager.security.SessionLifetimeFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint di autenticazione. Nessun altro punto di login nel progetto (sezione 2). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UserRepository utenti;
    private final HttpSessionSecurityContextRepository contesto = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService auth, UserRepository utenti) {
        this.auth = auth;
        this.utenti = utenti;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registra(@Valid @RequestBody RegisterRequest r,
                                                 HttpServletRequest req) {
        User u = auth.registra(r, req.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(u));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest r,
                              HttpServletRequest req, HttpServletResponse res) {
        User u = auth.login(r, req.getRemoteAddr());
        // Sessione nuova al login: l'id di quella precedente non vale piu' (anti session fixation)
        HttpSession sessione = req.getSession(true);
        req.changeSessionId();
        sessione.setAttribute(SessionLifetimeFilter.ATTRIBUTO_LOGIN, Instant.now());

        Authentication autenticazione = new UsernamePasswordAuthenticationToken(
                u.getId().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(autenticazione);
        SecurityContextHolder.setContext(ctx);
        contesto.saveContext(ctx, req, res);
        return UserResponse.from(u);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        // Il logout invalida la sessione sul server, non solo il cookie
        HttpSession sessione = req.getSession(false);
        if (sessione != null) {
            sessione.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** Ripristino della sessione dopo il ricaricamento della pagina. */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        java.util.UUID id = CurrentUsers.require(authentication);
        return utenti.findById(id).map(UserResponse::from)
                .orElseThrow(() -> new org.springframework.security.authentication.InsufficientAuthenticationException("Accesso richiesto"));
    }
}
