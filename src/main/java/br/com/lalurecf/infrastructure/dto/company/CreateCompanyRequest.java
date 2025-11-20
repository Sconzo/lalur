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

    @NotBlank(message = "CNAE é obrigatório")
    @Size(max = 20, message = "CNAE deve ter no máximo 20 caracteres")
    String cnae,

    @NotBlank(message = "Qualificação da Pessoa Jurídica é obrigatória")
    @Size(max = 100, message = "Qualificação PJ deve ter no máximo 100 caracteres")
    String qualificacaoPessoaJuridica,

    @NotBlank(message = "Natureza Jurídica é obrigatória")
    @Size(max = 100, message = "Natureza Jurídica deve ter no máximo 100 caracteres")
    String naturezaJuridica,

    @NotNull(message = "Período Contábil é obrigatório")
    @PastOrPresent(message = "Período Contábil não pode ser no futuro")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate periodoContabil,

    List<Long> parametrosTributariosIds
) {
}
