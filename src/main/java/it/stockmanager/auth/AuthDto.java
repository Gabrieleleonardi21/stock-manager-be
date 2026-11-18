package it.stockmanager.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** DTO di autenticazione (sezione 5): limiti di lunghezza su ogni campo. */
public final class AuthDto {

    private AuthDto() {}

    /** Password: minimo 12 caratteri; massimo 72 byte, limite di BCrypt (controllato anche nel service). */
    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {}

    /** Risposta utente: mai password, hash o dati interni. */
    public record UserResponse(UUID id, String name, String email) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail());
        }
    }
}
