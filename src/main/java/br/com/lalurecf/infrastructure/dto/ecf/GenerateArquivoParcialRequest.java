package br.com.lalurecf.infrastructure.dto.ecf;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO para geração do Arquivo Parcial ECF.
 */
public class GenerateArquivoParcialRequest {

  /**Ano fiscal de referência (obrigatório). */
  @NotNull(message = "fiscalYear é obrigatório")
  private Integer fiscalYear;

  public GenerateArquivoParcialRequest() {
  }

  public GenerateArquivoParcialRequest(Integer fiscalYear) {
    this.fiscalYear = fiscalYear;
  }

  public Integer getFiscalYear() {
    return fiscalYear;
  }

  public void setFiscalYear(Integer fiscalYear) {
    this.fiscalYear = fiscalYear;
  }
}
