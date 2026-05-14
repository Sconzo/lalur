package br.com.lalurecf.infrastructure.dto.importschema;

import java.util.List;

/**
 * Descreve o formato completo de um arquivo CSV de importação.
 *
 * <p>Retornado pelos endpoints GET /{tipo}/import/schema para documentar
 * ao front-end o separador, presença de cabeçalho, total de colunas
 * e a definição de cada campo.
 */
public record ImportSchemaResponse(
    int totalColumns,
    String separator,
    boolean hasHeader,
    List<ImportFieldSchema> fields
) {

  /** BOM UTF-8 para que o Excel detecte a codificação ao abrir o CSV. */
  private static final String UTF8_BOM = "﻿";

  /**
   * Renderiza o schema como CSV (separador ";") para o usuário visualizar em Excel.
   */
  public String toCsv() {
    StringBuilder sb = new StringBuilder();
    sb.append(UTF8_BOM);
    sb.append("campo;tipo;obrigatório;formato;valores permitidos;")
        .append("observação;tamanho máximo;exemplo\n");
    for (ImportFieldSchema f : fields) {
      String allowed = f.allowedValues() == null ? null : String.join(", ", f.allowedValues());
      String maxLen = f.maxLength() == null ? "" : f.maxLength().toString();
      sb.append(escape(f.name())).append(';')
          .append(escape(f.type())).append(';')
          .append(f.required() ? "sim" : "não").append(';')
          .append(escape(f.format())).append(';')
          .append(escape(allowed)).append(';')
          .append(escape(f.observation())).append(';')
          .append(maxLen).append(';')
          .append(escape(f.example())).append('\n');
    }
    return sb.toString();
  }

  private static String escape(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.indexOf(';') >= 0 || value.indexOf('"') >= 0
        || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
