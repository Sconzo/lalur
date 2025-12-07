package br.com.lalurecf.infrastructure.dto.taxparameter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para requisição de criação de parâmetro tributário.
 *
 * @param code código único do parâmetro (alfanumérico com hífens)
 * @param type tipo/categoria do parâmetro (ex: 'CNAE', 'QUALIFICACAO_PJ', 'NATUREZA_JURIDICA')
 * @param description descrição detalhada (opcional)
 */
public record CreateTaxParameterRequest(
    @NotBlank(message = "Código é obrigatório")
    @Pattern(
        regexp = "^[A-Z0-9-]+$",
        message = "Código deve conter apenas letras maiúsculas, números e hífens")
    String code,

    @NotBlank(message = "Tipo é obrigatório")
    String type,

    String description
) {}
