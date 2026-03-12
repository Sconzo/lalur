package br.com.lalurecf.infrastructure.dto.ecf;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO para geração do ECF Completo.
 */
public class GenerateCompleteEcfRequest {

  /**Ano fiscal de referência (obrigatório). */
  @NotNull(message = "fiscalYear é obrigatório")
  private Integer fiscalYear;

  public GenerateCompleteEcfRequest() {
  }

  public GenerateCompleteEcfRequest(Integer fiscalYear) {
    this.fiscalYear = fiscalYear;
  }

  public Integer getFiscalYear() {
    return fiscalYear;
  }

  public void setFiscalYear(Integer fiscalYear) {
    this.fiscalYear = fiscalYear;
  }
}
