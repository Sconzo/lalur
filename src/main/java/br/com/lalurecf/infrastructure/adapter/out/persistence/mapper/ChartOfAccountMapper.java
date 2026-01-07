package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.ChartOfAccount;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ChartOfAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para conversão entre ChartOfAccountEntity e ChartOfAccount (domain).
 *
 * <p>Converte entidade JPA (infraestrutura) para/de modelo de domínio puro.
 */
@Mapper(componentModel = "spring")
public interface ChartOfAccountMapper {

  /**
   * Converte ChartOfAccountEntity para ChartOfAccount (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(source = "company.id", target = "companyId")
  @Mapping(source = "contaReferencial.id", target = "contaReferencialId")
  ChartOfAccount toDomain(ChartOfAccountEntity entity);

  /**
   * Converte ChartOfAccount (domain) para ChartOfAccountEntity.
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(source = "companyId", target = "company.id")
  @Mapping(source = "contaReferencialId", target = "contaReferencial.id")
  ChartOfAccountEntity toEntity(ChartOfAccount domain);

  /**
   * Atualiza uma ChartOfAccountEntity existente com dados do ChartOfAccount (domain).
   *
   * <p>Usado para operações de UPDATE preservando ID e campos de auditoria.
   *
   * @param domain modelo de domínio com dados atualizados
   * @param entity entidade JPA existente a ser atualizada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(source = "companyId", target = "company.id")
  @Mapping(source = "contaReferencialId", target = "contaReferencial.id")
  void updateEntity(
      ChartOfAccount domain, @org.mapstruct.MappingTarget ChartOfAccountEntity entity);
}
