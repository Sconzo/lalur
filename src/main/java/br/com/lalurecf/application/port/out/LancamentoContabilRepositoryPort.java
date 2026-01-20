package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.LancamentoContabil;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de Lançamentos Contábeis.
 *
 * <p>Define as operações de persistência necessárias para gerenciar lançamentos contábeis
 * (partidas dobradas).
 *
 * <p>Segue padrão Hexagonal Architecture (Ports & Adapters).
 */
public interface LancamentoContabilRepositoryPort {

  /**
   * Salva um lançamento contábil.
   *
   * @param lancamento lançamento a ser salvo
   * @return lançamento salvo com ID gerado
   */
  LancamentoContabil save(LancamentoContabil lancamento);

  /**
   * Busca lançamento contábil por ID.
   *
   * @param id ID do lançamento
   * @return Optional contendo o lançamento se encontrado
   */
  Optional<LancamentoContabil> findById(Long id);

  /**
   * Busca todos lançamentos de uma empresa em um ano fiscal específico.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de lançamentos
   */
  List<LancamentoContabil> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca todos lançamentos de uma empresa.
   *
   * @param companyId ID da empresa
   * @return lista de lançamentos
   */
  List<LancamentoContabil> findByCompanyId(Long companyId);

  /**
   * Busca lançamentos de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  Page<LancamentoContabil> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Deleta um lançamento contábil.
   *
   * <p>Nota: Implementação deve usar soft delete via campo status.
   *
   * @param id ID do lançamento a deletar
   */
  void deleteById(Long id);
}
