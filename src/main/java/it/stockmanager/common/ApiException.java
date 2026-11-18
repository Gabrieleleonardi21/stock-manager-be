package it.stockmanager.common;

import org.springframework.http.HttpStatus;

/** Errore applicativo con stato HTTP esplicito, reso in formato ProblemDetail dal gestore centrale. */
public class ApiException extends RuntimeException {

    private final HttpStatus stato;

    public ApiException(HttpStatus stato, String messaggio) {
        super(messaggio);
        this.stato = stato;
    }

    public HttpStatus getStato() {
        return stato;
    }
}
