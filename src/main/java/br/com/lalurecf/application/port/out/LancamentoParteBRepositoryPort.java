package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.LancamentoParteB;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de LancamentoParteB.
 *
 * <p>Interface de repositório para Lançamentos da Parte B (e-Lalur/e-Lacs), vinculados a
 * empresas específicas.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface LancamentoParteBRepositoryPort {

  /**
   * Salva um lançamento Parte B (create ou update).
   *
   * @param lancamento lançamento Parte B a salvar
   * @return lançamento Parte B salvo com ID gerado
   */
  LancamentoParteB save(LancamentoParteB lancamento);

  /**
   * Busca lançamento Parte B por ID.
   *
   * @param id ID do lançamento
   * @return Optional com lançamento se encontrado
   */
  Optional<LancamentoParteB> findById(Long id);

  /**
   * Busca todos lançamentos Parte B de uma empresa em um ano de referência.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @return lista de lançamentos da empresa no ano especificado
   */
  List<LancamentoParteB> findByCompanyIdAndAnoReferencia(Long companyId, Integer anoReferencia);

  /**
   * Busca todos lançamentos Parte B de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de lançamentos da empresa
   */
  Page<LancamentoParteB> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Busca lançamentos por empresa, ano e mês de referência.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @param mesReferencia mês de referência
   * @return lista de lançamentos
   */
  List<LancamentoParteB> findByCompanyIdAndAnoReferenciaAndMesReferencia(
      Long companyId, Integer anoReferencia, Integer mesReferencia);

  /**
   * Deleta lançamento por ID.
   *
   * @param id ID do lançamento
   */
  void deleteById(Long id);
}
