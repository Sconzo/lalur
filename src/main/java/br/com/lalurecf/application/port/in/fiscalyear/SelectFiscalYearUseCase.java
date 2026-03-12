package br.com.lalurecf.application.port.in.fiscalyear;

/**
 * Use case para seleção do ano fiscal de trabalho.
 *
 * <p>Valida que o ano informado é um ano fiscal legítimo (entre 2000 e o ano corrente)
 * antes de permitir sua seleção pelo CONTADOR.
 */
public interface SelectFiscalYearUseCase {

  /**
   * Valida e retorna o ano fiscal selecionado.
   *
   * @param fiscalYear ano fiscal a selecionar
   * @return o ano fiscal validado
   * @throws IllegalArgumentException se o ano for inválido
   */
  Integer selectFiscalYear(Integer fiscalYear);
}
