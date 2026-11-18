package it.stockmanager.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accesso agli utenti: solo metodi derivati, mai query costruite con stringhe (sezione 1 di SECURITY.md). */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
