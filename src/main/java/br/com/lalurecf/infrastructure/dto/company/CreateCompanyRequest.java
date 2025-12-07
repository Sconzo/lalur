package br.com.lalurecf.infrastructure.dto.company;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para criação de empresa.
 *
 * <p>Contém todos os campos obrigatórios para cadastro de uma nova empresa no sistema.
 */
public record CreateCompanyRequest(

    @NotNull(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter exatamente 14 dígitos numéricos")
    String cnpj,

    @NotBlank(message = "Razão Social é obrigatória")
    @Size(max = 255, message = "Razão Social deve ter no máximo 255 caracteres")
    String razaoSocial,

    @NotNull(message = "Período Contábil é obrigatório")
    @PastOrPresent(message = "Período Contábil não pode ser no futuro")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate periodoContabil,

    @NotNull(message = "CNAE é obrigatório")
    Long cnaeParametroId,

    @NotNull(message = "Qualificação da Pessoa Jurídica é obrigatória")
    Long qualificacaoPjParametroId,

    @NotNull(message = "Natureza Jurídica é obrigatória")
    Long naturezaJuridicaParametroId,

    List<Long> outrosParametrosIds
) {
}
