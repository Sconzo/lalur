package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.fiscalyear.SelectFiscalYearUseCase;
import java.time.Year;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service que implementa os Use Cases de ano fiscal.
 *
 * <p>Valida o ano fiscal selecionado pelo CONTADOR antes de iniciar o trabalho.
 */
@Service
@Slf4j
public class FiscalYearService implements SelectFiscalYearUseCase {

  @Override
  public Integer selectFiscalYear(Integer fiscalYear) {
    log.info("Selecionando ano fiscal: {}", fiscalYear);

    int currentYear = Year.now().getValue();

    if (fiscalYear < 2000 || fiscalYear > currentYear) {
      throw new IllegalArgumentException(
          String.format(
              "Ano fiscal inválido: %d. Deve estar entre 2000 e %d.", fiscalYear, currentYear));
    }

    log.info("Ano fiscal selecionado com sucesso: {}", fiscalYear);
    return fiscalYear;
  }
}
