package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.ChartOfAccountRepositoryPort;
import br.com.lalurecf.domain.model.ChartOfAccount;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ChartOfAccountEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.ChartOfAccountMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ChartOfAccountJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para ChartOfAccount.
 *
 * <p>Implementa ChartOfAccountRepositoryPort (hexagonal port OUT) usando Spring Data JPA como
 * tecnologia de persistência.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Converter entre domain model (ChartOfAccount) e JPA entity (ChartOfAccountEntity)
 *   <li>Delegar operações de persistência ao ChartOfAccountJpaRepository
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ChartOfAccountRepositoryAdapter implements ChartOfAccountRepositoryPort {

  private final ChartOfAccountJpaRepository jpaRepository;
  private final ChartOfAccountMapper mapper;

  @Override
  public ChartOfAccount save(ChartOfAccount account) {
    ChartOfAccountEntity entity;

    if (account.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity =
          jpaRepository
              .findById(account.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "ChartOfAccount not found with id: " + account.getId()));
      mapper.updateEntity(account, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(account);
    }

    ChartOfAccountEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<ChartOfAccount> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<ChartOfAccount> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear) {
    return jpaRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<ChartOfAccount> findByCompanyIdAndCodeAndFiscalYear(
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
  public Page<ChartOfAccount> findByCompanyId(Long companyId, Pageable pageable) {
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }
}
