package br.com.lalurecf.application.port.in.taxparametertype;

import br.com.lalurecf.infrastructure.dto.taxparametertype.TaxParameterTypeResponse;
import java.util.List;

/**
 * Use case para listagem de tipos de parâmetros tributários.
 *
 * <p>Retorna todos os tipos ativos ordenados por descrição.
 */
public interface ListTaxParameterTypesUseCase {

  /**
   * Lista todos os tipos de parâmetros tributários ativos.
   *
   * @return lista de tipos ordenados por descrição
   */
  List<TaxParameterTypeResponse> listAll();
}
