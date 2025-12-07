package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * JPA Entity para Parâmetros Tributários.
 *
 * <p>Estrutura flat (sem hierarquia parent/child) conforme ADR-001 v2.0.
 * Tipos comuns: "CNAE", "QUALIFICACAO_PJ", "NATUREZA_JURIDICA", "IRPJ", "CSLL", "GERAL".
 */
@Entity
@Table(name = "tb_parametros_tributarios")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaxParameterEntity extends BaseEntity {

  @Column(name = "codigo", unique = true, nullable = false, length = 100)
  private String codigo;

  @Column(name = "tipo", nullable = false, length = 50)
  private String tipo;

  @Column(name = "descricao", columnDefinition = "TEXT")
  private String descricao;

}
