package br.com.lalurecf.infrastructure.dto.fiscalyear;

/**
 * DTO de resposta para seleção do ano fiscal de trabalho.
 */
public record SelectFiscalYearResponse(
    Boolean success,
    Integer fiscalYear,
    String message
) {
}
