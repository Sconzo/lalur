package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre LancamentoParteB (domain) e LancamentoParteBResponse (DTO).
 *
 * <p>Converte objetos de domínio para DTOs de resposta.
 */
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBDtoMapper {

  /**
   * Converte LancamentoParteB domain para LancamentoParteBResponse DTO.
   *
   * @param lancamento objeto de domínio
   * @return DTO de resposta
   */
  public LancamentoParteBResponse toResponse(LancamentoParteB lancamento) {
    if (lancamento == null) {
      return null;
    }

    return LancamentoParteBResponse.builder()
        .id(lancamento.getId())
        .mesReferencia(lancamento.getMesReferencia())
        .anoReferencia(lancamento.getAnoReferencia())
        .tipoApuracao(lancamento.getTipoApuracao())
        .tipoRelacionamento(lancamento.getTipoRelacionamento())
        .contaContabilId(lancamento.getContaContabilId())
        .contaParteBId(lancamento.getContaParteBId())
        .parametroTributarioId(lancamento.getParametroTributarioId())
        .tipoAjuste(lancamento.getTipoAjuste())
        .descricao(lancamento.getDescricao())
        .valor(lancamento.getValor())
        .status(lancamento.getStatus())
        .createdAt(lancamento.getCreatedAt())
        .updatedAt(lancamento.getUpdatedAt())
        .build();
  }
}
