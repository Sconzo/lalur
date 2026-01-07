package br.com.lalurecf.infrastructure.dto.chartofaccount;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação de conta contábil (ChartOfAccount).
 *
 * <p>Contém todos os campos ECF-specific necessários para cadastro de conta no plano de contas,
 * incluindo vinculação obrigatória a uma Conta Referencial RFB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChartOfAccountRequest {

  /** Código da conta contábil (ex: "1.1.01.001"). */
  @NotBlank(message = "Code is required")
  private String code;

  /** Nome da conta contábil (ex: "Caixa"). */
  @NotBlank(message = "Name is required")
  private String name;

  /** Ano fiscal da conta (ex: 2024). */
  @NotNull(message = "Fiscal year is required")
  @Min(value = 2000, message = "Fiscal year must be >= 2000")
  private Integer fiscalYear;

  /** Tipo da conta (ATIVO, PASSIVO, RECEITA, etc.). */
  @NotNull(message = "Account type is required")
  private AccountType accountType;

  /** ID da Conta Referencial RFB (FK obrigatória). */
  @NotNull(message = "Conta Referencial ID is required")
  private Long contaReferencialId;

  /** Classe contábil ECF (ATIVO_CIRCULANTE, RECEITA_BRUTA, etc.). */
  @NotNull(message = "Classe is required")
  private ClasseContabil classe;

  /** Nível hierárquico (1-5) para estruturação ECF. */
  @NotNull(message = "Nivel is required")
  @Min(value = 1, message = "Nivel must be between 1 and 5")
  @Max(value = 5, message = "Nivel must be between 1 and 5")
  private Integer nivel;

  /** Natureza da conta (DEVEDORA ou CREDORA). */
  @NotNull(message = "Natureza is required")
  private NaturezaConta natureza;

  /** Indica se a conta afeta o resultado (DRE). */
  @NotNull(message = "AfetaResultado is required")
  private Boolean afetaResultado;

  /** Indica se despesa/custo é dedutível fiscalmente. */
  @NotNull(message = "Dedutivel is required")
  private Boolean dedutivel;
}
