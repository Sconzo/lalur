package br.com.lalurecf.infrastructure.dto.planodecontas;

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
 * DTO para atualização de conta contábil (PlanoDeContas).
 *
 * <p>Permite editar campos da conta, EXCETO code e fiscalYear que são imutáveis após criação.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlanoDeContasRequest {

  /** Nome da conta contábil (ex: "Caixa"). */
  @NotBlank(message = "Name is required")
  private String name;

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
