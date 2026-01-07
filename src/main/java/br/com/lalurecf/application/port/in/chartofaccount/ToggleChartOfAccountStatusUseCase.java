package br.com.lalurecf.application.port.in.chartofaccount;

import br.com.lalurecf.infrastructure.dto.chartofaccount.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ToggleStatusResponse;

/**
 * Port IN: Use case para alternar status de conta contábil (ChartOfAccount).
 *
 * <p>Permite ativar (ACTIVE) ou inativar (INACTIVE) uma conta.
 */
public interface ToggleChartOfAccountStatusUseCase {

  /**
   * Alterna status de conta contábil.
   *
   * @param id ID da conta
   * @param request novo status desejado
   * @return confirmação da operação
   */
  ToggleStatusResponse execute(Long id, ToggleStatusRequest request);
}
