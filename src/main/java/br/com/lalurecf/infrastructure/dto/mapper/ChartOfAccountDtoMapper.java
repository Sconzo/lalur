package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.ChartOfAccount;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ChartOfAccountResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre ChartOfAccount (domain) e DTOs.
 *
 * <p>Converte objetos de domínio para DTOs de resposta.
 */
@Component
public class ChartOfAccountDtoMapper {

  /**
   * Converte ChartOfAccount domain para ChartOfAccountResponse DTO.
   *
   * @param account objeto de domínio
   * @param contaReferencialCodigo código da Conta Referencial RFB
   * @return DTO de resposta
   */
  public ChartOfAccountResponse toResponse(ChartOfAccount account, String contaReferencialCodigo) {
    if (account == null) {
      return null;
    }

    return ChartOfAccountResponse.builder()
        .id(account.getId())
        .code(account.getCode())
        .name(account.getName())
        .fiscalYear(account.getFiscalYear())
        .accountType(account.getAccountType())
        .contaReferencialId(account.getContaReferencialId())
        .contaReferencialCodigo(contaReferencialCodigo)
        .classe(account.getClasse())
        .nivel(account.getNivel())
        .natureza(account.getNatureza())
        .afetaResultado(account.getAfetaResultado())
        .dedutivel(account.getDedutivel())
        .status(account.getStatus())
        .createdAt(account.getCreatedAt())
        .updatedAt(account.getUpdatedAt())
        .build();
  }
}
