package br.com.lalurecf.infrastructure.dto.importschema;

import java.util.List;

/**
 * Descreve o formato completo de um arquivo CSV de importação.
 *
 * <p>Retornado pelos endpoints GET /*/import/schema para documentar
 * ao front-end o separador, presença de cabeçalho, total de colunas
 * e a definição de cada campo.
 */
public record ImportSchemaResponse(
    int totalColumns,
    String separator,
    boolean hasHeader,
    List<ImportFieldSchema> fields
) {
}
