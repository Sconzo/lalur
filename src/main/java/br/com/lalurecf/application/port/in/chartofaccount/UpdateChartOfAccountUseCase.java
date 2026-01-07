package br.com.lalurecf.application.port.in.chartofaccount;

import br.com.lalurecf.infrastructure.dto.chartofaccount.ChartOfAccountResponse;
import br.com.lalurecf.infrastructure.dto.chartofaccount.UpdateChartOfAccountRequest;

/**
 * Port IN: Use case para atualizar uma conta contábil (ChartOfAccount).
 *
 * <p>Permite editar campos da conta, EXCETO code e fiscalYear.
 */
public interface UpdateChartOfAccountUseCase {

  /**
   * Atualiza conta contábil existente.
   *
   * @param id ID da conta a atualizar
   * @param request novos dados da conta
   * @return conta atualizada
   */
  ChartOfAccountResponse execute(Long id, UpdateChartOfAccountRequest request);
}
