package br.com.lalurecf.infrastructure.dto.taxparameter;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;

/**
 * DTO para resposta de parâmetro tributário.
 *
 * @param id ID do parâmetro
 * @param code código único
 * @param type tipo/categoria
 * @param description descrição
 * @param nature natureza do parâmetro (GLOBAL, MONTHLY, QUARTERLY)
 * @param status status (ACTIVE/INACTIVE)
 * @param createdAt data de criação
 * @param updatedAt data de última atualização
 */
public record TaxParameterResponse(
    Long id,
    String code,
    String type,
    String description,
    ParameterNature nature,
    Status status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
