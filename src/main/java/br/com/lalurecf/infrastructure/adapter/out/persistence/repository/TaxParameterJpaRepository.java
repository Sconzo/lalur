package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA para TaxParameter.
 *
 * <p>Métodos de consulta por código (único) e por tipo (categoria).
 */
@Repository
public interface TaxParameterJpaRepository
    extends JpaRepository<TaxParameterEntity, Long> {

  /**
   * Busca parâmetro tributário por código único.
   *
   * @param codigo código do parâmetro
   * @return Optional contendo o parâmetro se encontrado
   */
  Optional<TaxParameterEntity> findByCodigo(String codigo);

  /**
   * Busca parâmetros tributários por tipo/categoria.
   *
   * @param tipo tipo do parâmetro (ex: "CNAE", "QUALIFICACAO_PJ", "NATUREZA_JURIDICA")
   * @return lista de parâmetros do tipo especificado
   */
  List<TaxParameterEntity> findByTipo(String tipo);

  /**
   * Busca parâmetros tributários por IDs e tipo específico.
   * Usado para validar que os IDs fornecidos são do tipo correto.
   *
   * @param ids lista de IDs
   * @param tipo tipo esperado
   * @return lista de parâmetros que correspondem aos IDs E ao tipo
   */
  @Query("SELECT t FROM TaxParameterEntity t WHERE t.id IN :ids AND t.tipo = :tipo")
  List<TaxParameterEntity> findByIdInAndTipo(
      @Param("ids") List<Long> ids,
      @Param("tipo") String tipo);

}
