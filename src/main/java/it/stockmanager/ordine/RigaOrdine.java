package it.stockmanager.ordine;

import java.math.BigDecimal;
import java.util.UUID;

/** Una riga della bozza: articolo, nome al momento della bozza, quantita' da ordinare. */
public record RigaOrdine(UUID oggettoId, String nome, BigDecimal quantita) {}
