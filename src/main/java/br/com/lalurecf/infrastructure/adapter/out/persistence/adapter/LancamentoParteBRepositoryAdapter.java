package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ChartOfAccountEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaParteBEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoParteBEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.LancamentoParteBMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ChartOfAccountJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaParteBJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.LancamentoParteBJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para LancamentoParteB.
 *
 * <p>Implementa LancamentoParteBRepositoryPort (hexagonal port OUT) usando Spring Data JPA como
 * tecnologia de persistência.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Converter entre domain model (LancamentoParteB) e JPA entity (LancamentoParteBEntity)
 *   <li>Delegar operações de persistência ao LancamentoParteBJpaRepository
 *   <li>Resolver relacionamentos com CompanyEntity, ChartOfAccountEntity, ContaParteBEntity e
 *       TaxParameterEntity
 * </ul>
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBRepositoryAdapter implements LancamentoParteBRepositoryPort {

  private final LancamentoParteBJpaRepository jpaRepository;
  private final CompanyJpaRepository companyJpaRepository;
  private final ChartOfAccountJpaRepository chartOfAccountJpaRepository;
  private final ContaParteBJpaRepository contaParteBJpaRepository;
  private final TaxParameterJpaRepository taxParameterJpaRepository;
  private final LancamentoParteBMapper mapper;

  @Override
  public LancamentoParteB save(LancamentoParteB lancamento) {
    LancamentoParteBEntity entity;

    if (lancamento.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity =
          jpaRepository
              .findById(lancamento.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "LancamentoParteB not found with id: " + lancamento.getId()));
      mapper.updateEntity(lancamento, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(lancamento);

      // Resolver relacionamento com Company
      CompanyEntity company =
          companyJpaRepository
              .findById(lancamento.getCompanyId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Company not found with id: " + lancamento.getCompanyId()));
      entity.setCompany(company);

      // Resolver relacionamento com ContaContabil (se informado)
      if (lancamento.getContaContabilId() != null) {
        ChartOfAccountEntity contaContabil =
            chartOfAccountJpaRepository
                .findById(lancamento.getContaContabilId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "ContaContabil not found with id: "
                                + lancamento.getContaContabilId()));
        entity.setContaContabil(contaContabil);
      }

      // Resolver relacionamento com ContaParteB (se informado)
      if (lancamento.getContaParteBId() != null) {
        ContaParteBEntity contaParteB =
            contaParteBJpaRepository
                .findById(lancamento.getContaParteBId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "ContaParteB not found with id: " + lancamento.getContaParteBId()));
        entity.setContaParteB(contaParteB);
      }

      // Resolver relacionamento com ParametroTributario (obrigatório)
      TaxParameterEntity parametroTributario =
          taxParameterJpaRepository
              .findById(lancamento.getParametroTributarioId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "ParametroTributario not found with id: "
                              + lancamento.getParametroTributarioId()));
      entity.setParametroTributario(parametroTributario);
    }

    LancamentoParteBEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<LancamentoParteB> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<LancamentoParteB> findByCompanyIdAndAnoReferencia(
      Long companyId, Integer anoReferencia) {
    return jpaRepository.findByCompanyIdAndAnoReferencia(companyId, anoReferencia).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Page<LancamentoParteB> findByCompanyId(Long companyId, Pageable pageable) {
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }

  @Override
  public List<LancamentoParteB> findByCompanyIdAndAnoReferenciaAndMesReferencia(
      Long companyId, Integer anoReferencia, Integer mesReferencia) {
    return jpaRepository
        .findByCompanyIdAndAnoReferenciaAndMesReferencia(companyId, anoReferencia, mesReferencia)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
  }
}
