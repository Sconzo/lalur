package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.ParameterNature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * JPA Entity para Parâmetros Tributários.
 *
 * <p>Estrutura flat (sem hierarquia parent/child) conforme ADR-001 v2.0.
 * Tipos comuns: "CNAE", "QUALIFICACAO_PJ", "NATUREZA_JURIDICA", "IRPJ", "CSLL", "GERAL".
 *
 * <p>Constraint: codigo + tipo devem ser únicos em conjunto
 * (permite mesmo código para tipos diferentes).
 */
@Entity
@Table(
    name = "tb_parametros_tributarios",
    uniqueConstraints = @UniqueConstraint(columnNames = {"codigo", "tipo"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaxParameterEntity extends BaseEntity {

  @Column(name = "codigo", nullable = false, length = 100)
  private String codigo;

  @Column(name = "tipo", nullable = false, length = 50)
  private String tipo;

  @Column(name = "descricao", columnDefinition = "TEXT")
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(name = "natureza", nullable = false, length = 20)
  @Builder.Default
  private ParameterNature natureza = ParameterNature.GLOBAL;

}
