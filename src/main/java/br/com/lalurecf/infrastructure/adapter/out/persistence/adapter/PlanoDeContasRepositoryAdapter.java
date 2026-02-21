package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.PlanoDeContasMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para PlanoDeContas.
 *
 * <p>Implementa PlanoDeContasRepositoryPort (hexagonal port OUT) usando Spring Data JPA como
 * tecnologia de persistência.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Converter entre domain model (PlanoDeContas) e JPA entity (PlanoDeContasEntity)
 *   <li>Delegar operações de persistência ao PlanoDeContasJpaRepository
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PlanoDeContasRepositoryAdapter implements PlanoDeContasRepositoryPort {

  private final PlanoDeContasJpaRepository jpaRepository;
  private final PlanoDeContasMapper mapper;
  private final CompanyJpaRepository companyJpaRepository;
  private final ContaReferencialJpaRepository contaReferencialJpaRepository;

  @Override
  public PlanoDeContas save(PlanoDeContas account) {
    PlanoDeContasEntity entity;

    if (account.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity =
          jpaRepository
              .findById(account.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "PlanoDeContas not found with id: " + account.getId()));
      mapper.updateEntity(account, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(account);
    }

    // Definir referências gerenciadas via getReferenceById para evitar instâncias transientes
    entity.setCompany(companyJpaRepository.getReferenceById(account.getCompanyId()));
    if (account.getContaReferencialId() != null) {
      entity.setContaReferencial(
          contaReferencialJpaRepository.getReferenceById(account.getContaReferencialId()));
    } else {
      entity.setContaReferencial(null);
    }

    PlanoDeContasEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<PlanoDeContas> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<PlanoDeContas> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear) {
    return jpaRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<PlanoDeContas> findByCompanyIdAndCodeAndFiscalYear(
      Long companyId, String code, Integer fiscalYear) {
    return jpaRepository
        .findByCompanyIdAndCodeAndFiscalYear(companyId, code, fiscalYear)
        .map(mapper::toDomain);
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public Page<PlanoDeContas> findByCompanyId(Long companyId, Pageable pageable) {
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }
}
