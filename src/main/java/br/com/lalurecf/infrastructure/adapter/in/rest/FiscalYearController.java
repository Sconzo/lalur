package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.fiscalyear.SelectFiscalYearUseCase;
import br.com.lalurecf.infrastructure.dto.fiscalyear.SelectFiscalYearRequest;
import br.com.lalurecf.infrastructure.dto.fiscalyear.SelectFiscalYearResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para seleção do ano fiscal de trabalho.
 *
 * <p>Permite ao CONTADOR selecionar o ano fiscal antes de iniciar os lançamentos.
 * O ano selecionado é validado e retornado para o front-end armazenar e usar
 * em todas as operações subsequentes.
 */
@RestController
@RequestMapping("/fiscal-year")
@RequiredArgsConstructor
@Slf4j
public class FiscalYearController {

  private final SelectFiscalYearUseCase selectFiscalYearUseCase;

  /**
   * Seleciona o ano fiscal de trabalho.
   *
   * <p>Valida que o ano é entre 2000 e o ano corrente.
   *
   * @param request ano fiscal a selecionar
   * @return ano fiscal validado com mensagem de confirmação
   */
  @PostMapping("/select")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<SelectFiscalYearResponse> selectFiscalYear(
      @Valid @RequestBody SelectFiscalYearRequest request) {

    log.info("POST /api/v1/fiscal-year/select - fiscalYear: {}", request.fiscalYear());

    try {
      Integer fiscalYear = selectFiscalYearUseCase.selectFiscalYear(request.fiscalYear());

      SelectFiscalYearResponse response = new SelectFiscalYearResponse(
          true,
          fiscalYear,
          "Ano fiscal " + fiscalYear + " selecionado com sucesso"
      );

      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Ano fiscal inválido: {}", e.getMessage());

      SelectFiscalYearResponse response = new SelectFiscalYearResponse(
          false,
          request.fiscalYear(),
          e.getMessage()
      );

      return ResponseEntity.badRequest().body(response);
    }
  }
}
