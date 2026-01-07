package br.com.lalurecf.application.port.in.chartofaccount;

import br.com.lalurecf.infrastructure.dto.chartofaccount.ChartOfAccountResponse;

/**
 * Port IN: Use case para buscar uma conta contábil (ChartOfAccount) por ID.
 *
 * <p>Valida que a conta pertence à empresa no contexto.
 */
public interface GetChartOfAccountUseCase {

  /**
   * Busca conta contábil por ID.
   *
   * @param id ID da conta
   * @return conta encontrada
   */
  ChartOfAccountResponse execute(Long id);
}
