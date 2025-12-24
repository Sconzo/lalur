package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * JPA Entity para Valores de Parâmetros Temporais.
 *
 * <p>Representa períodos (mensais ou trimestrais) em que parâmetros tributários estão ativos
 * para uma empresa, conforme ADR-001.
 *
 * <p>Periodicidade:
 *
 * <ul>
 *   <li>Mensal: ano + mes preenchidos, trimestre NULL
 *   <li>Trimestral: ano + trimestre preenchidos, mes NULL
 * </ul>
 */
@Entity
@Table(
    name = "tb_valores_parametros_temporais",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_valores_temporais_periodo",
            columnNames = {"empresa_parametros_tributarios_id", "ano", "mes", "trimestre"}),
    indexes = {
      @Index(
          name = "idx_valores_temporais_empresa_param",
          columnList = "empresa_parametros_tributarios_id"),
      @Index(name = "idx_valores_temporais_ano", columnList = "ano"),
      @Index(name = "idx_valores_temporais_periodo", columnList = "ano, mes, trimestre")
    })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ValorParametroTemporalEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_parametros_tributarios_id", nullable = false)
  private EmpresaParametrosTributariosEntity empresaParametrosTributarios;

  @Column(name = "ano", nullable = false)
  private Integer ano;

  @Column(name = "mes")
  private Integer mes;

  @Column(name = "trimestre")
  private Integer trimestre;
}
