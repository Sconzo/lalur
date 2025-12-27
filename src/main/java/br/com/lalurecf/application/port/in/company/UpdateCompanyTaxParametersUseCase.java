package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersResponse;

/**
 * Use case para atualização de parâmetros tributários de uma empresa.
 *
 * <p>Permite associar/desassociar parâmetros tributários. A lista substitui completamente a
 * anterior (não acumula).
 */
public interface UpdateCompanyTaxParametersUseCase {

  /**
   * Atualiza a lista de parâmetros tributários associados a uma empresa.
   *
   * <p>IMPORTANTE: A lista fornecida substitui completamente a lista anterior.
   *
   * @param companyId ID da empresa
   * @param request lista de IDs dos parâmetros tributários
   * @return resposta com a lista atualizada de parâmetros
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   * @throws br.com.lalurecf.domain.exception.BusinessRuleViolationException se algum parâmetro não
   *     existir ou estiver INACTIVE
   */
  UpdateTaxParametersResponse updateTaxParameters(
      Long companyId, UpdateTaxParametersRequest request);
}
