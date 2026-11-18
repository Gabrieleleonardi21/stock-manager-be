#!/bin/bash
# Test di fase 4: autorizzazione (IDOR), validazione, regola sulla giacenza, concorrenza, import, cascata.
U=http://localhost:8080; O=http://localhost:5173; PW="passwordsicura123"
login_utente(){ # $1 = jar, $2 = email
  rm -f "$1"; curl -s -c "$1" -o /dev/null $U/api/auth/me
  local x=$(grep XSRF-TOKEN "$1" | awk '{print $7}')
  curl -s -b "$1" -c "$1" -H "Origin: $O" -H "X-XSRF-TOKEN: $x" -H "Content-Type: application/json" -d "{\"name\":\"U\",\"email\":\"$2\",\"password\":\"$PW\"}" -o /dev/null $U/api/auth/register
  curl -s -b "$1" -c "$1" -H "Origin: $O" -H "X-XSRF-TOKEN: $x" -H "Content-Type: application/json" -d "{\"email\":\"$2\",\"password\":\"$PW\"}" -o /dev/null $U/api/auth/login
}
req(){ # $1 jar, $2 metodo, $3 path, $4 body(opzionale)
  local x=$(grep XSRF-TOKEN "$1" | awk '{print $7}')
  if [ -n "$4" ]; then curl -s -b "$1" -H "Origin: $O" -H "X-XSRF-TOKEN: $x" -H "Content-Type: application/json" -X "$2" -d "$4" -w "\n%{http_code}" "$U$3"; else curl -s -b "$1" -H "Origin: $O" -H "X-XSRF-TOKEN: $x" -X "$2" -w "\n%{http_code}" "$U$3"; fi
}
code(){ tail -1 <<< "$1"; }
body(){ sed '$d' <<< "$1"; }
id(){ python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("id") or d.get("magazzinoId"))' <<< "$(body "$1")"; }
ok(){ if [ "$2" = "$3" ]; then echo "  PASS $1 ($2)"; else echo "  FAIL $1: atteso $3, ottenuto $2"; fi; }

TS=$(date +%s); login_utente /tmp/A.jar "a$TS@example.com"; login_utente /tmp/B.jar "b$TS@example.com"
echo "== Creazione dati (utente A)"
R=$(req /tmp/A.jar POST /api/magazzini '{"nome":"Centrale","luogo":"Roma","responsabile":"Gabriele"}'); ok "crea magazzino" "$(code "$R")" 201; MAG=$(id "$R")
R=$(req /tmp/A.jar POST /api/magazzini/$MAG/oggetti '{"nome":"Viti","sku":"V1","categoria":"Ricambi","unita":"pz","sogliaMin":5}'); ok "crea articolo" "$(code "$R")" 201; OGG=$(id "$R")
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"arrivo","data":"2026-10-05","qta":10,"prezzo":0.12,"nota":"Fornitore A"}'); ok "arrivo 10" "$(code "$R")" 201; MOV1=$(id "$R")
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"prelievo","data":"2026-10-06","qta":15,"nota":""}'); ok "prelievo oltre giacenza -> 400" "$(code "$R")" 400
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"prelievo","data":"2026-10-07","qta":4,"nota":"Linea 1"}'); ok "prelievo 4 ammesso" "$(code "$R")" 201
echo "== Autorizzazione: utente B non vede ne' tocca i dati di A (atteso 404)"
R=$(req /tmp/B.jar PUT /api/magazzini/$MAG '{"nome":"Hack","luogo":"","responsabile":""}'); ok "B modifica magazzino di A" "$(code "$R")" 404
R=$(req /tmp/B.jar DELETE /api/magazzini/$MAG); ok "B elimina magazzino di A" "$(code "$R")" 404
R=$(req /tmp/B.jar POST /api/magazzini/$MAG/oggetti '{"nome":"x","sku":"","categoria":"c","unita":"pz","sogliaMin":0}'); ok "B crea articolo nel magazzino di A" "$(code "$R")" 404
R=$(req /tmp/B.jar PUT /api/oggetti/$OGG '{"nome":"x","sku":"","categoria":"c","unita":"pz","sogliaMin":0}'); ok "B modifica articolo di A" "$(code "$R")" 404
R=$(req /tmp/B.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"arrivo","data":"2026-10-08","qta":1}'); ok "B registra movimento su articolo di A" "$(code "$R")" 404
R=$(req /tmp/B.jar DELETE /api/movimenti/$MOV1); ok "B elimina movimento di A" "$(code "$R")" 404
R=$(req /tmp/B.jar GET /api/azienda); ok "B legge il proprio albero" "$(code "$R")" 200; echo "$(body "$R")" | grep -q "Centrale" && echo "  FAIL B vede i dati di A" || echo "  PASS B non vede i dati di A"
echo "== Validazione (400)"
R=$(req /tmp/A.jar POST /api/magazzini '{"nome":"","luogo":"","responsabile":""}'); ok "nome vuoto" "$(code "$R")" 400
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"arrivo","data":"2026-10-09","qta":0}'); ok "qta zero" "$(code "$R")" 400
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"arrivo","data":"2026-10-09","qta":-3}'); ok "qta negativa" "$(code "$R")" 400
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"arrivo","data":"2026-10-09","qta":2,"prezzo":-1}'); ok "prezzo negativo" "$(code "$R")" 400
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"arrivo","data":"non-una-data","qta":2}'); ok "data non valida" "$(code "$R")" 400
R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"vendita","data":"2026-10-09","qta":2}'); ok "tipo sconosciuto" "$(code "$R")" 400
N=$(python3 -c 'print("n"*501)'); R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti "{\"tipo\":\"arrivo\",\"data\":\"2026-10-09\",\"qta\":2,\"nota\":\"$N\"}"); ok "nota oltre 500" "$(code "$R")" 400
R=$(req /tmp/A.jar DELETE /api/movimenti/non-un-uuid); ok "id non UUID nel percorso" "$(code "$R")" 400
echo "== Concorrenza: 5 prelievi simultanei di 4 su giacenza 6 (ne passa al massimo 1)"
for i in 1 2 3 4 5; do ( R=$(req /tmp/A.jar POST /api/oggetti/$OGG/movimenti '{"tipo":"prelievo","data":"2026-10-10","qta":4}'); code "$R" ) & done > /tmp/conc.txt; wait; sleep 1
echo "  esiti: $(sort /tmp/conc.txt | tr '\n' ' ')"; N201=$(grep -c "^201" /tmp/conc.txt); ok "prelievi riusciti (atteso 1)" "$N201" "1"
echo "== Import dal browser"
IMP='{"chiave":"local-abc","nome":"Deposito","luogo":"Milano","responsabile":"Luca","oggetti":[{"nome":"Cartone","sku":"C1","categoria":"Imballaggi","unita":"pz","sogliaMin":20,"movimenti":[{"tipo":"arrivo","data":"2026-11-01","qta":50,"prezzo":0.4,"nota":""},{"tipo":"prelievo","data":"2026-11-02","qta":10,"nota":""}]}]}'
R=$(req /tmp/A.jar POST /api/import/magazzini "$IMP"); ok "import nuovo" "$(code "$R")" 201; IMPID=$(id "$R")
R=$(req /tmp/A.jar POST /api/import/magazzini "$IMP"); ok "import ripetuto non duplica" "$(code "$R")" 200; ok "stesso id" "$(id "$R")" "$IMPID"
BAD='{"chiave":"local-bad","nome":"Errato","luogo":"","responsabile":"","oggetti":[{"nome":"X","sku":"","categoria":"c","unita":"pz","sogliaMin":0,"movimenti":[{"tipo":"prelievo","data":"2026-01-01","qta":5,"nota":""}]}]}'
R=$(req /tmp/A.jar POST /api/import/magazzini "$BAD"); ok "import incoerente rifiutato" "$(code "$R")" 400
R=$(req /tmp/A.jar GET /api/azienda); echo "$(body "$R")" | grep -q "Errato" && echo "  FAIL import parziale salvato" || echo "  PASS nessun salvataggio parziale"
echo "== Cascata"
R=$(req /tmp/A.jar DELETE /api/magazzini/$IMPID); ok "elimina magazzino importato" "$(code "$R")" 204
R=$(req /tmp/A.jar GET /api/azienda); echo "$(body "$R")" | grep -q "Deposito" && echo "  FAIL magazzino ancora presente" || echo "  PASS magazzino e movimenti rimossi"
echo "== Fine"
# Import di un magazzino grande (>100 KB ma <2 MB): deve passare; una rotta normale con lo stesso corpo deve dare 413.
GRANDE=$(python3 -c 'import json;mv=[{"tipo":"arrivo","data":"2026-01-01","qta":1,"prezzo":0.5,"nota":"nota di prova lunga per superare i cento kilobyte "*1} for _ in range(2000)];print(json.dumps({"chiave":"local-big-1","nome":"Grande","luogo":"","responsabile":"","oggetti":[{"nome":"Grande","sku":"","categoria":"c","unita":"pz","sogliaMin":0,"movimenti":mv}]}))')
echo "== Import grande"
R=$(req /tmp/A.jar POST /api/import/magazzini "$GRANDE"); ok "import sopra 100 KB accettato" "$(code "$R")" 201
PIC=$(python3 -c 'print("{\"nome\":\""+"x"*150000+"\"}")')
R=$(req /tmp/A.jar POST /api/magazzini "$PIC"); ok "rotta normale oltre 100 KB -> 413" "$(code "$R")" 413
echo "== Fine import grande"
# ---- Fase 5: report mensili
echo "== Report"
R=$(req /tmp/A.jar POST /api/magazzini '{"nome":"Report M","luogo":"","responsabile":""}'); MR=$(id "$R")
R=$(req /tmp/A.jar POST /api/magazzini/$MR/oggetti '{"nome":"Dado","sku":"D1","categoria":"c","unita":"pz","sogliaMin":1}'); OR=$(id "$R")
req /tmp/A.jar POST /api/oggetti/$OR/movimenti '{"tipo":"arrivo","data":"2026-10-03","qta":10,"prezzo":2,"nota":""}' >/dev/null
req /tmp/A.jar POST /api/oggetti/$OR/movimenti '{"tipo":"prelievo","data":"2026-10-20","qta":4,"nota":""}' >/dev/null
req /tmp/A.jar POST /api/oggetti/$OR/movimenti '{"tipo":"arrivo","data":"2026-11-05","qta":5,"prezzo":2,"nota":""}' >/dev/null
req /tmp/A.jar POST /api/oggetti/$OR/movimenti '{"tipo":"prelievo","data":"2026-11-09","qta":3,"nota":""}' >/dev/null
R=$(req /tmp/A.jar POST /api/magazzini/$MR/report '{"mese":"2026-11"}'); ok "genera report novembre" "$(code "$R")" 200; RP=$(id "$R")
ok "giacenza iniziale 6" "$(python3 -c 'import json,sys;print(float(json.load(sys.stdin)["righe"][0]["giacenzaIniziale"]))' <<< "$(body "$R")")" "6.0"
ok "giacenza finale 8" "$(python3 -c 'import json,sys;print(float(json.load(sys.stdin)["righe"][0]["giacenzaFinale"]))' <<< "$(body "$R")")" "8.0"
ok "valore acquisti 10" "$(python3 -c 'import json,sys;print(float(json.load(sys.stdin)["totValoreAcquisti"]))' <<< "$(body "$R")")" "10.0"
R=$(req /tmp/A.jar POST /api/magazzini/$MR/report '{"mese":"2026-11"}'); ok "rigenerare non duplica" "$(id "$R")" "$RP"
R=$(req /tmp/A.jar POST /api/magazzini/$MR/report '{"mese":"2026-13"}'); ok "mese non valido -> 400" "$(code "$R")" 400
R=$(req /tmp/A.jar POST /api/magazzini/$MR/report '{"mese":"2025-01"}'); ok "mese senza movimenti -> 400" "$(code "$R")" 400
R=$(req /tmp/A.jar GET "/api/report?pagina=0&dimensione=1000"); ok "elenco paginato" "$(code "$R")" 200
R=$(req /tmp/A.jar GET /api/report/$RP); ok "dettaglio con righe" "$(code "$R")" 200
R=$(req /tmp/A.jar GET /api/report/$RP/csv); ok "CSV" "$(code "$R")" 200
echo "$(body "$R")" | grep -q "Articolo;SKU" && echo "  PASS intestazione CSV" || echo "  FAIL intestazione CSV"
R=$(req /tmp/B.jar GET /api/report/$RP); ok "B legge report di A -> 404" "$(code "$R")" 404
R=$(req /tmp/B.jar GET /api/report/$RP/csv); ok "B esporta report di A -> 404" "$(code "$R")" 404
R=$(req /tmp/B.jar POST /api/magazzini/$MR/report '{"mese":"2026-11"}'); ok "B genera report sul magazzino di A -> 404" "$(code "$R")" 404
echo "== Formule nel CSV"
R=$(req /tmp/A.jar POST /api/magazzini/$MR/oggetti '{"nome":"=1+1","sku":"@SUM","categoria":"c","unita":"pz","sogliaMin":0}'); OF=$(id "$R")
req /tmp/A.jar POST /api/oggetti/$OF/movimenti '{"tipo":"arrivo","data":"2026-11-10","qta":1,"nota":""}' >/dev/null
R=$(req /tmp/A.jar POST /api/magazzini/$MR/report '{"mese":"2026-11"}'); RP2=$(id "$R")
R=$(req /tmp/A.jar GET /api/report/$RP2/csv); echo "$(body "$R")" | grep -q "'=1+1" && echo "  PASS formula neutralizzata" || echo "  FAIL formula non neutralizzata"
echo "== Import report legacy"
LEG='{"chiave":"local-rep-1","mese":"2026-09","azienda":"Vecchia","magazzinoNome":"Storico","autore":"Gabriele","righe":[{"nome":"Vite","sku":"V","categoria":"c","unita":"pz","giacenzaIniziale":1,"arrivi":2,"prelievi":0,"giacenzaFinale":3,"valoreAcquisti":4}]}'
R=$(req /tmp/A.jar POST /api/import/report "$LEG"); ok "import report legacy" "$(code "$R")" 200; LID=$(id "$R")
R=$(req /tmp/A.jar POST /api/import/report "$LEG"); ok "import report ripetuto non duplica" "$(id "$R")" "$LID"
echo "== Fine fase 5"
