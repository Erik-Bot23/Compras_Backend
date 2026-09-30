package com.erikjarquin.compras.model.enums;

/**
 * Agrupación temporal para {@code GET /api/local/reports/trend}: agrega las ventas
 * por día, mes o año. Recibido como query param string en ReportController
 * (Spring lo convierte automáticamente a enum).
 */
public enum ReportGroup {
    DAY,
    MONTH,
    YEAR
}