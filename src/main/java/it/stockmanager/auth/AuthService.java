package it.stockmanager.auth;

import it.stockmanager.auth.AuthDto.LoginRequest;
import it.stockmanager.auth.AuthDto.RegisterRequest;
import it.stockmanager.common.ApiException;
import it.stockmanager.common.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registrazione e login (sezione 2 del piano).
 * Messaggi generici: non si rivela se un'email e' registrata.
 */
@Service
public class AuthService {

    private static final Duration FINESTRA_LOGIN = Duration.ofMinutes(15);
    // Limiti configurabili in application.yaml / .env: i valori di default sono quelli di produzione
    private final int maxLoginPerIp;
    private final int maxLoginPerAccount;
    private final int maxRegistrazioniPerIp;

    private final UserRepository utenti;
    private final LoginAttemptRepository tentativi;
    private final PasswordEncoder encoder;
    private final RateLimiter limiter;
    /** Hash finto: se l'email non esiste si esegue comunque un confronto BCrypt, cosi' i tempi non rivelano nulla. */
    private final String hashFinto;

    public AuthService(UserRepository utenti, LoginAttemptRepository tentativi,
                       PasswordEncoder encoder, RateLimiter limiter,
                       @Value("${app.limiti.login-per-ip:20}") int maxLoginPerIp,
                       @Value("${app.limiti.login-per-account:5}") int maxLoginPerAccount,
                       @Value("${app.limiti.registrazioni-per-ip-ora:5}") int maxRegistrazioniPerIp) {
        this.maxLoginPerIp = maxLoginPerIp;
        this.maxLoginPerAccount = maxLoginPerAccount;
        this.maxRegistrazioniPerIp = maxRegistrazioniPerIp;
        this.utenti = utenti;
        this.tentativi = tentativi;
        this.encoder = encoder;
        this.limiter = limiter;
        this.hashFinto = encoder.encode("password-fittizia-per-tempi-costanti");
    }

    @Transactional
    public User registra(RegisterRequest r, String ip) {
        if (!limiter.consenti("reg-ip:" + ip, maxRegistrazioniPerIp, Duration.ofHours(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Troppi tentativi, riprova più tardi");
        }
        String email = normalizza(r.email());
        // BCrypt accetta al massimo 72 byte: con caratteri multibyte il limite in caratteri non basta
        if (r.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password troppo lunga");
        }
        if (utenti.existsByEmail(email)) {
            // stesso messaggio generico di un errore qualsiasi: non si rivela che l'email esiste
            throw new ApiException(HttpStatus.BAD_REQUEST, "Registrazione non completata");
        }
        return utenti.save(new User(r.name().trim(), email, encoder.encode(r.password())));
    }

    /**
     * Verifica le credenziali. Lancia ApiException 401 con messaggio generico se non valgono.
     * Scrive sempre un record di audit (esito, IP, email; mai la password).
     */
    @Transactional
    public User login(LoginRequest r, String ip) {
        String email = normalizza(r.email());
        boolean ipOk = limiter.consenti("login-ip:" + ip, maxLoginPerIp, FINESTRA_LOGIN);
        boolean accountOk = limiter.consenti("login-acc:" + email, maxLoginPerAccount, FINESTRA_LOGIN);
        if (!ipOk || !accountOk) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Troppi tentativi, riprova più tardi");
        }

        User utente = utenti.findByEmail(email).orElse(null);
        String hash = hashFinto;
        if (utente != null) {
            hash = utente.getPasswordHash();
        }
        boolean ok = encoder.matches(r.password(), hash) && utente != null;

        tentativi.save(new LoginAttempt(email, ip, ok));
        if (!ok) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
        }
        limiter.reset("login-acc:" + email);
        return utente;
    }

    private static String normalizza(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
