package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoContabilEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository para Lançamento Contábil.
 *
 * <p>Fornece operações de persistência para LancamentoContabilEntity.
 */
@Repository
public interface LancamentoContabilJpaRepository
    extends JpaRepository<LancamentoContabilEntity, Long> {

  /**
   * Busca todos lançamentos de uma empresa em um ano fiscal específico.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de lançamentos
   */
  List<LancamentoContabilEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca todos lançamentos de uma empresa.
   *
   * @param companyId ID da empresa
   * @return lista de lançamentos
   */
  List<LancamentoContabilEntity> findByCompanyId(Long companyId);

  /**
   * Busca lançamentos de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  Page<LancamentoContabilEntity> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Deleta fisicamente todos os lançamentos de uma empresa em um determinado mês e ano.
   *
   * @param companyId ID da empresa
   * @param mes mês (1-12)
   * @param ano ano (ex: 2024)
   * @return quantidade de registros deletados
   */
  @Modifying
  @Query(
      "DELETE FROM LancamentoContabilEntity l "
          + "WHERE l.company.id = :companyId "
          + "AND FUNCTION('EXTRACT', 'MONTH', l.data) = :mes "
          + "AND FUNCTION('EXTRACT', 'YEAR', l.data) = :ano")
  int deleteByCompanyIdAndMesAndAno(
      @Param("companyId") Long companyId,
      @Param("mes") Integer mes,
      @Param("ano") Integer ano);

  /**
   * Conta lançamentos de uma empresa em um determinado mês e ano.
   *
   * @param companyId ID da empresa
   * @param mes mês (1-12)
   * @param ano ano (ex: 2024)
   * @return quantidade de registros
   */
  @Query(
      "SELECT COUNT(l) FROM LancamentoContabilEntity l "
          + "WHERE l.company.id = :companyId "
          + "AND FUNCTION('EXTRACT', 'MONTH', l.data) = :mes "
          + "AND FUNCTION('EXTRACT', 'YEAR', l.data) = :ano")
  int countByCompanyIdAndMesAndAno(
      @Param("companyId") Long companyId,
      @Param("mes") Integer mes,
      @Param("ano") Integer ano);
}
