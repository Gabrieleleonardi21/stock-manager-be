package it.stockmanager.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** Audit dei tentativi di accesso (sezione 8): esito, origine e istante. Mai la password. */
@Entity
@Table(name = "login_attempt")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(nullable = false)
    private boolean success;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LoginAttempt() {
        // richiesto da JPA
    }

    public LoginAttempt(String email, String ip, boolean success) {
        this.email = email;
        this.ip = ip;
        this.success = success;
    }
}
