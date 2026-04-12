package br.com.lalurecf.infrastructure.dto.taxparameter;

/**
 * DTO com código e descrição de um parâmetro tributário, usado no retorno de listagens agrupadas.
 */
public record TaxParameterOption(String code, String description) {}
