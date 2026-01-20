package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaReferencialEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.ContaReferencialMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para ContaReferencial.
 *
 * <p>Implementa ContaReferencialRepositoryPort (hexagonal port OUT) usando Spring Data JPA como
 * tecnologia de persistência.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Converter entre domain model (ContaReferencial) e JPA entity (ContaReferencialEntity)
 *   <li>Delegar operações de persistência ao ContaReferencialJpaRepository
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ContaReferencialRepositoryAdapter implements ContaReferencialRepositoryPort {

  private final ContaReferencialJpaRepository jpaRepository;
  private final ContaReferencialMapper mapper;

  @Override
  public ContaReferencial save(ContaReferencial conta) {
    ContaReferencialEntity entity;

    if (conta.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity =
          jpaRepository
              .findById(conta.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "ContaReferencial not found with id: " + conta.getId()));
      mapper.updateEntity(conta, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(conta);
    }

    ContaReferencialEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<ContaReferencial> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<ContaReferencial> findByCodigoRfb(String codigoRfb) {
    return jpaRepository.findByCodigoRfb(codigoRfb).map(mapper::toDomain);
  }

  @Override
  public Optional<ContaReferencial> findByCodigoRfbAndAnoValidade(
      String codigoRfb, Integer anoValidade) {
    return jpaRepository
        .findByCodigoRfbAndAnoValidade(codigoRfb, anoValidade)
        .map(mapper::toDomain);
  }

  @Override
  public List<ContaReferencial> findByAnoValidade(Integer anoValidade) {
    return jpaRepository.findByAnoValidade(anoValidade).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Page<ContaReferencial> findByAnoValidade(Integer anoValidade, Pageable pageable) {
    return jpaRepository.findByAnoValidade(anoValidade, pageable).map(mapper::toDomain);
  }

  @Override
  public List<ContaReferencial> findAll() {
    return jpaRepository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Page<ContaReferencial> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable).map(mapper::toDomain);
  }

  @Override
  public Page<ContaReferencial> findByStatus(
      br.com.lalurecf.domain.enums.Status status, Pageable pageable) {
    return jpaRepository.findByStatus(status, pageable).map(mapper::toDomain);
  }

  @Override
  public Page<ContaReferencial> findByAnoValidadeAndStatus(
      Integer anoValidade, br.com.lalurecf.domain.enums.Status status, Pageable pageable) {
    return jpaRepository
        .findByAnoValidadeAndStatus(anoValidade, status, pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<ContaReferencial> findBySearchContaining(String search, Pageable pageable) {
    return jpaRepository
        .findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCase(
            search, search, pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<ContaReferencial> findBySearchContainingAndStatus(
      String search, br.com.lalurecf.domain.enums.Status status, Pageable pageable) {
    return jpaRepository
        .findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCaseAndStatus(
            search, status, pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<ContaReferencial> findBySearchContainingAndAnoValidade(
      String search, Integer anoValidade, Pageable pageable) {
    return jpaRepository
        .findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCaseAndAnoValidade(
            search, anoValidade, pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<ContaReferencial> findBySearchContainingAndAnoValidadeAndStatus(
      String search,
      Integer anoValidade,
      br.com.lalurecf.domain.enums.Status status,
      Pageable pageable) {
    return jpaRepository
        .findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCaseAndAnoValidadeAndStatus(
            search, anoValidade, status, pageable)
        .map(mapper::toDomain);
  }
}
