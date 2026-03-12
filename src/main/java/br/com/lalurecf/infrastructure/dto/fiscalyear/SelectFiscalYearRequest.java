package br.com.lalurecf.infrastructure.dto.fiscalyear;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para seleção do ano fiscal de trabalho.
 */
public record SelectFiscalYearRequest(
    @NotNull(message = "Ano fiscal é obrigatório")
    @Min(value = 2000, message = "Ano fiscal deve ser >= 2000")
    Integer fiscalYear
) {
}
