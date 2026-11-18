package it.stockmanager.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;

/**
 * Errori in formato ProblemDetail (RFC 9457) per tutta l'API.
 * In produzione niente stack trace nelle risposte: il dettaglio resta solo nel log (sezione 8).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Validazione dei DTO fallita: si segnalano i campi, mai i valori inseriti. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validazione(MethodArgumentNotValidException ex) {
        List<String> campi = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField())
                .distinct()
                .toList();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dati non validi");
        pd.setProperty("campi", campi);
        return pd;
    }

    /** JSON malformato o non leggibile. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail jsonNonValido(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Richiesta non leggibile");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail tipoNonSupportato(HttpMediaTypeNotSupportedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Usa Content-Type application/json");
    }

    /** Id malformato nell'URL (es. non e' un UUID): 400, non 500. */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroNonValido(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parametro non valido");
    }

    /** Errori applicativi: lo stato e il messaggio sono quelli scelti dal service. */
    @ExceptionHandler(ApiException.class)
    ProblemDetail applicativo(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStato(), ex.getMessage());
    }

    /** Gli errori di sicurezza mantengono il loro stato, senza finire nel 500 generico. */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail nonAutenticato(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Accesso richiesto");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail negato(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Operazione non consentita");
    }

    /** Qualunque altro errore: risposta generica, dettaglio solo nel log. */
    @ExceptionHandler(Exception.class)
    ProblemDetail imprevisto(Exception ex) {
        log.error("Errore non gestito", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno");
    }
}
