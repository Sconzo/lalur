package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.PlanoDeContas;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de PlanoDeContas (Plano de Contas Contábil).
 *
 * <p>Interface de repositório para contas contábeis de empresas por ano fiscal, vinculadas a
 * Contas Referenciais RFB.
 */
public interface PlanoDeContasRepositoryPort {

  /**
   * Salva uma conta contábil (create ou update).
   *
   * @param account conta contábil a salvar
   * @return conta contábil salva com ID gerado
   */
  PlanoDeContas save(PlanoDeContas account);

  /**
   * Busca conta contábil por ID.
   *
   * @param id ID da conta
   * @return Optional com conta se encontrada
   */
  Optional<PlanoDeContas> findById(Long id);

  /**
   * Busca todas contas contábeis de uma empresa para um ano fiscal.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de contas da empresa no ano especificado
   */
  List<PlanoDeContas> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca conta contábil por empresa, código e ano fiscal.
   *
   * @param companyId ID da empresa
   * @param code código da conta
   * @param fiscalYear ano fiscal
   * @return Optional com conta se encontrada
   */
  Optional<PlanoDeContas> findByCompanyIdAndCodeAndFiscalYear(
      Long companyId, String code, Integer fiscalYear);

  /**
   * Deleta conta contábil por ID.
   *
   * @param id ID da conta a deletar
   */
  void deleteById(Long id);

  /**
   * Busca todas contas contábeis de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de contas da empresa
   */
  Page<PlanoDeContas> findByCompanyId(Long companyId, Pageable pageable);
}
