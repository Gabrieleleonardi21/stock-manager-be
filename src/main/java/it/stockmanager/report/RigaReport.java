package it.stockmanager.report;

import java.math.BigDecimal;

/** Una riga del report: un articolo con giacenza iniziale, movimenti del mese e finale. */
public record RigaReport(
        String nome,
        String sku,
        String categoria,
        String unita,
        BigDecimal giacenzaIniziale,
        BigDecimal arrivi,
        BigDecimal prelievi,
        BigDecimal giacenzaFinale,
        BigDecimal valoreAcquisti,
        BigDecimal entrate) {}
