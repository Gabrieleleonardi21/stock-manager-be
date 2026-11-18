package it.stockmanager.ordine;

import it.stockmanager.security.CurrentUsers;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

/** Elenco delle bozze d'ordine dell'utente, quelle preparate dall'assistente e in futuro dall'interfaccia. */
@RestController
@RequestMapping("/api/ordini")
public class OrdineController {

    private final OrdineRepository ordini;

    @Autowired
    public OrdineController(OrdineRepository ordini) {
        this.ordini = ordini;
    }

    @GetMapping
    public List<OrdineView> elenco(Authentication auth) {
        return ordini.findByOwnerIdOrderByCreatoIlDesc(CurrentUsers.require(auth)).stream()
                .map(OrdineView::from)
                .toList();
    }
}
