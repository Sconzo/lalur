package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a Company (Empresa).
 * Pure POJO without framework dependencies (no Spring/JPA annotations).
 */
public class Company {

  private Long id;
  private CNPJ cnpj;
  private String razaoSocial;
  private String cnae;
  private String qualificacaoPessoaJuridica;
  private String naturezaJuridica;
  private LocalDate periodoContabil;
  private Status status;
  private String createdBy;
  private LocalDateTime createdAt;
  private String updatedBy;
  private LocalDateTime updatedAt;

  /**
   * Default constructor.
   */
  public Company() {
  }

  /**
   * Full constructor with all fields.
   */
  public Company(Long id, CNPJ cnpj, String razaoSocial, String cnae,
                 String qualificacaoPessoaJuridica, String naturezaJuridica,
                 LocalDate periodoContabil, Status status,
                 String createdBy, LocalDateTime createdAt,
                 String updatedBy, LocalDateTime updatedAt) {
    this.id = id;
    this.cnpj = cnpj;
    this.razaoSocial = razaoSocial;
    this.cnae = cnae;
    this.qualificacaoPessoaJuridica = qualificacaoPessoaJuridica;
    this.naturezaJuridica = naturezaJuridica;
    this.periodoContabil = periodoContabil;
    this.status = status;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.updatedBy = updatedBy;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CNPJ getCnpj() {
    return cnpj;
  }

  public void setCnpj(CNPJ cnpj) {
    this.cnpj = cnpj;
  }

  public String getRazaoSocial() {
    return razaoSocial;
  }

  public void setRazaoSocial(String razaoSocial) {
    this.razaoSocial = razaoSocial;
  }

  public String getCnae() {
    return cnae;
  }

  public void setCnae(String cnae) {
    this.cnae = cnae;
  }

  public String getQualificacaoPessoaJuridica() {
    return qualificacaoPessoaJuridica;
  }

  public void setQualificacaoPessoaJuridica(String qualificacaoPessoaJuridica) {
    this.qualificacaoPessoaJuridica = qualificacaoPessoaJuridica;
  }

  public String getNaturezaJuridica() {
    return naturezaJuridica;
  }

  public void setNaturezaJuridica(String naturezaJuridica) {
    this.naturezaJuridica = naturezaJuridica;
  }

  public LocalDate getPeriodoContabil() {
    return periodoContabil;
  }

  public void setPeriodoContabil(LocalDate periodoContabil) {
    this.periodoContabil = periodoContabil;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Company company = (Company) o;
    return Objects.equals(id, company.id) && Objects.equals(cnpj, company.cnpj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, cnpj);
  }

  @Override
  public String toString() {
    return "Company{"
        + "id=" + id
        + ", cnpj=" + cnpj
        + ", razaoSocial='" + razaoSocial + '\''
        + ", status=" + status
        + '}';
  }
}
