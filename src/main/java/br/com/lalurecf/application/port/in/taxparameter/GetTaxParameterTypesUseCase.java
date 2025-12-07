package br.com.lalurecf.application.port.in.taxparameter;

import java.util.List;

/**
 * Use case para buscar tipos/categorias distintos de parâmetros tributários.
 *
 * <p>Usado para popular dropdowns de filtros.
 */
public interface GetTaxParameterTypesUseCase {

  /**
   * Busca tipos únicos de parâmetros tributários com filtro opcional.
   *
   * @param search texto de busca (opcional)
   * @return lista de tipos únicos ordenados (limitado a 100 resultados)
   */
  List<String> getTypes(String search);
}
