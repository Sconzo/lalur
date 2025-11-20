package br.com.lalurecf.infrastructure.dto.company;

import br.com.lalurecf.domain.model.CompanyStatus;

/**
 * DTO para resposta de listagem de empresas.
 *
 * <p>Contém apenas os campos essenciais para exibição em lista.
 */
public record CompanyResponse(
    Long id,
    String cnpj,  // Formatado: 00.000.000/0000-00
    CompanyStatus status,
    String razaoSocial,
    String cnae,
    String qualificacaoPessoaJuridica,
    String naturezaJuridica
) {
}
