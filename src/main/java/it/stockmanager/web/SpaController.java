package it.stockmanager.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rotte dell'app React (fase 6): il server restituisce sempre index.html e lascia il percorso al router del browser.
 * Le rotte /api e /actuator non passano di qui.
 */
@Controller
public class SpaController {

    @GetMapping({"/azienda", "/azienda/**", "/report", "/report/**"})
    public String app() {
        return "forward:/index.html";
    }
}
