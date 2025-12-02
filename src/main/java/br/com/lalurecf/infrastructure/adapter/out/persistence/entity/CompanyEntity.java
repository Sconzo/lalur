package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing a Company (Empresa).
 * Extends BaseEntity for auditing and soft delete.
 * Table name and columns follow snake_case convention (ADR-001).
 */
@Entity
@Table(name = "tb_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEntity extends BaseEntity {

  @Column(nullable = false, unique = true, length = 14)
  private String cnpj;

  @Column(name = "razao_social", nullable = false)
  private String razaoSocial;

  @Column(nullable = false, length = 7)
  private String cnae;

  @Column(name = "qualificacao_pessoa_juridica", nullable = false)
  private String qualificacaoPessoaJuridica;

  @Column(name = "natureza_juridica", nullable = false)
  private String naturezaJuridica;

  @Column(name = "periodo_contabil", nullable = false)
  private LocalDate periodoContabil;
}
