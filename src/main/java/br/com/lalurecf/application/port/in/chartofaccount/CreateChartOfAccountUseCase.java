package br.com.lalurecf.application.port.in.chartofaccount;

import br.com.lalurecf.infrastructure.dto.chartofaccount.ChartOfAccountResponse;
import br.com.lalurecf.infrastructure.dto.chartofaccount.CreateChartOfAccountRequest;

/**
 * Port IN: Use case para criar uma conta contábil (ChartOfAccount).
 *
 * <p>Valida empresa via CompanyContext, valida contaReferencialId, verifica unicidade e persiste.
 */
public interface CreateChartOfAccountUseCase {

  /**
   * Cria uma nova conta contábil para a empresa no contexto.
   *
   * @param request dados da conta a criar
   * @return conta criada
   */
  ChartOfAccountResponse execute(CreateChartOfAccountRequest request);
}
