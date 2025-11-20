package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.CompanyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use case para listagem de empresas.
 *
 * <p>Suporta:
 * <ul>
 *   <li>Filtro global (busca em todos os campos)</li>
 *   <li>Filtro por CNPJ</li>
 *   <li>Filtro por Razão Social</li>
 *   <li>Combinação de filtros</li>
 *   <li>Paginação e ordenação</li>
 *   <li>Incluir/excluir empresas inativas</li>
 * </ul>
 */
public interface ListCompaniesUseCase {

  /**
   * Lista empresas com filtros e paginação.
   *
   * @param globalSearch filtro global (busca em todos os campos)
   * @param cnpjFilter filtro por CNPJ específico
   * @param razaoSocialFilter filtro por Razão Social específica
   * @param includeInactive incluir empresas inativas (padrão: false)
   * @param pageable configuração de paginação e ordenação
   * @return página de empresas
   */
  Page<CompanyResponse> list(
      String globalSearch,
      String cnpjFilter,
      String razaoSocialFilter,
      boolean includeInactive,
      Pageable pageable
  );
}
