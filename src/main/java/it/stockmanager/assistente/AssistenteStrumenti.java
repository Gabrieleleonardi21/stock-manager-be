package it.stockmanager.assistente;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolUnion;
import it.stockmanager.common.ApiException;
import it.stockmanager.magazzino.Magazzino;
import it.stockmanager.magazzino.MagazzinoRepository;
import it.stockmanager.movimento.Movimento;
import it.stockmanager.movimento.MovimentoRepository;
import it.stockmanager.movimento.Tipo;
import it.stockmanager.oggetto.Oggetto;
import it.stockmanager.oggetto.OggettoRepository;
import it.stockmanager.ordine.OrdineService;
import it.stockmanager.ordine.OrdineService.RichiestaRiga;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Strumenti dell'assistente: leggono i magazzini e gli articoli da riordinare, e preparano bozze d'ordine.
 * Tutto e' limitato ai dati dell'utente della sessione. Nessuno strumento invia ordini a fornitori.
 */
@Component
public class AssistenteStrumenti {

    private final MagazzinoRepository magazzini;
    private final OggettoRepository oggetti;
    private final MovimentoRepository movimenti;
    private final OrdineService ordini;
    private final ObjectMapper json;

    public AssistenteStrumenti(MagazzinoRepository magazzini, OggettoRepository oggetti, MovimentoRepository movimenti,
                               OrdineService ordini, ObjectMapper json) {
        this.magazzini = magazzini;
        this.oggetti = oggetti;
        this.movimenti = movimenti;
        this.ordini = ordini;
        this.json = json;
    }

    /** Definizioni degli strumenti, come le vede il modello. */
    public List<ToolUnion> definizioni() {
        return List.of(
                ToolUnion.ofTool(strumento("elenca_magazzini",
                        "Elenca i magazzini dell'utente, con id e nome.",
                        Map.of(), List.of())),
                ToolUnion.ofTool(strumento("articoli_da_riordinare",
                        "Elenca gli articoli di un magazzino sotto la scorta minima o esauriti, con giacenza e quantita' suggerita.",
                        Map.of("magazzinoId", Map.of("type", "string", "description", "id del magazzino")),
                        List.of("magazzinoId"))),
                ToolUnion.ofTool(strumento("prepara_ordine",
                        "Prepara una BOZZA d'ordine per un magazzino. La bozza non viene inviata a nessuno: serve a essere rivista dall'utente.",
                        Map.of(
                                "magazzinoId", Map.of("type", "string", "description", "id del magazzino"),
                                "righe", Map.of("type", "array", "description", "articoli da ordinare",
                                        "items", Map.of("type", "object",
                                                "properties", Map.of(
                                                        "oggettoId", Map.of("type", "string"),
                                                        "quantita", Map.of("type", "number")),
                                                "required", List.of("oggettoId", "quantita"))),
                                "nota", Map.of("type", "string", "description", "nota facoltativa per la bozza")),
                        List.of("magazzinoId", "righe"))));
    }

    /** Esegue uno strumento e restituisce il risultato come JSON. Gli errori tornano al modello come testo. */
    @Transactional
    public String esegui(UUID owner, String nome, Map<String, Object> input) {
        try {
            Object risultato = switch (nome) {
                case "elenca_magazzini" -> elencaMagazzini(owner);
                case "articoli_da_riordinare" -> articoliDaRiordinare(owner, uuid(input, "magazzinoId"));
                case "prepara_ordine" -> preparaOrdine(owner, input);
                default -> Map.of("errore", "strumento sconosciuto");
            };
            return json.writeValueAsString(risultato);
        } catch (ApiException e) {
            return "{\"errore\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        } catch (Exception e) {
            return "{\"errore\":\"richiesta non valida\"}";
        }
    }

    private List<Map<String, Object>> elencaMagazzini(UUID owner) {
        return magazzini.findByOwnerId(owner).stream()
                .map(m -> Map.<String, Object>of("id", m.getId().toString(), "nome", m.getNome(),
                        "luogo", m.getLuogo()))
                .toList();
    }

    private List<Map<String, Object>> articoliDaRiordinare(UUID owner, UUID magazzinoId) {
        Magazzino m = magazzini.findByIdAndOwnerId(magazzinoId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Magazzino non trovato"));
        Map<UUID, BigDecimal> giacenze = giacenze(owner);
        List<Map<String, Object>> risultato = new ArrayList<>();
        for (Oggetto o : oggetti.findByOwnerId(owner)) {
            if (!o.getMagazzinoId().equals(m.getId())) {
                continue;
            }
            BigDecimal giacenza = giacenze.getOrDefault(o.getId(), BigDecimal.ZERO);
            boolean esaurito = giacenza.signum() <= 0;
            boolean sottoSoglia = giacenza.compareTo(o.getSogliaMin()) < 0;
            if (esaurito || sottoSoglia) {
                // suggerimento semplice: riportare la giacenza al doppio della scorta minima
                BigDecimal suggerita = o.getSogliaMin().multiply(BigDecimal.valueOf(2)).subtract(giacenza);
                if (suggerita.signum() < 0) {
                    suggerita = BigDecimal.ZERO;
                }
                Map<String, Object> riga = new LinkedHashMap<>();
                riga.put("oggettoId", o.getId().toString());
                riga.put("nome", o.getNome());
                riga.put("sku", o.getSku());
                riga.put("unita", o.getUnita());
                riga.put("giacenza", giacenza.stripTrailingZeros().toPlainString());
                riga.put("sogliaMin", o.getSogliaMin().stripTrailingZeros().toPlainString());
                riga.put("quantitaSuggerita", suggerita.stripTrailingZeros().toPlainString());
                String stato = "sotto la scorta minima";
                if (esaurito) {
                    stato = "esaurito";
                }
                riga.put("stato", stato);
                risultato.add(riga);
            }
        }
        return risultato;
    }

    private Map<String, Object> preparaOrdine(UUID owner, Map<String, Object> input) {
        UUID magazzinoId = uuid(input, "magazzinoId");
        List<RichiestaRiga> righe = new ArrayList<>();
        Object elenco = input.get("righe");
        if (elenco instanceof List<?> lista) {
            for (Object voce : lista) {
                if (voce instanceof Map<?, ?> mappa) {
                    righe.add(new RichiestaRiga(uuid(castMap(mappa), "oggettoId"), numero(mappa.get("quantita"))));
                }
            }
        }
        Object nota = input.get("nota");
        String notaTesto = "";
        if (nota instanceof String s) {
            notaTesto = s;
        }
        var bozza = ordini.prepara(owner, magazzinoId, righe, notaTesto);
        return Map.of("bozzaId", bozza.id().toString(), "righe", bozza.righe().size(), "stato", bozza.stato());
    }

    /** Giacenza per articolo: arrivi meno prelievi, come nel resto dell'applicazione. */
    private Map<UUID, BigDecimal> giacenze(UUID owner) {
        Map<UUID, BigDecimal> g = new LinkedHashMap<>();
        for (Movimento mv : movimenti.findByOwnerId(owner)) {
            BigDecimal delta = mv.getQta();
            if (mv.getTipo() != Tipo.arrivo) {
                delta = delta.negate();
            }
            g.merge(mv.getOggettoId(), delta, BigDecimal::add);
        }
        return g;
    }

    private static Tool strumento(String nome, String descrizione, Map<String, Object> proprieta, List<String> obbligatori) {
        Tool.InputSchema.Properties.Builder props = Tool.InputSchema.Properties.builder();
        for (Map.Entry<String, Object> e : proprieta.entrySet()) {
            props.putAdditionalProperty(e.getKey(), JsonValue.from(e.getValue()));
        }
        return Tool.builder()
                .name(nome)
                .description(descrizione)
                .inputSchema(Tool.InputSchema.builder().properties(props.build()).required(obbligatori).build())
                .build();
    }

    private static UUID uuid(Map<String, Object> input, String chiave) {
        Object v = input.get(chiave);
        if (!(v instanceof String s)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parametro " + chiave + " mancante");
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parametro " + chiave + " non valido");
        }
    }

    private static Map<String, Object> castMap(Map<?, ?> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }

    private static BigDecimal numero(Object v) {
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Quantita' non valida");
    }
}
