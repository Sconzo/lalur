package br.com.lalurecf.infrastructure.dto.company;

/**
 * Summary representation of a TaxParameter.
 * Used in Company responses to show associated tax parameters.
 *
 * @param id the tax parameter ID
 * @param codigo the unique tax parameter code
 * @param descricao the tax parameter description
 */
public record TaxParameterSummary(
    Long id,
    String codigo,
    String descricao
) {}
