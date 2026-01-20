package br.com.lalurecf.infrastructure.dto.lancamentocontabil;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para atualização de lançamento contábil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLancamentoContabilRequest {

  @NotNull(message = "Conta de débito é obrigatória")
  private Long contaDebitoId;

  @NotNull(message = "Conta de crédito é obrigatória")
  private Long contaCreditoId;

  @NotNull(message = "Data é obrigatória")
  private LocalDate data;

  @NotNull(message = "Valor é obrigatório")
  @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
  private BigDecimal valor;

  @NotBlank(message = "Histórico é obrigatório")
  @Size(max = 2000, message = "Histórico deve ter no máximo 2000 caracteres")
  private String historico;

  @Size(max = 100, message = "Número do documento deve ter no máximo 100 caracteres")
  private String numeroDocumento;
}
