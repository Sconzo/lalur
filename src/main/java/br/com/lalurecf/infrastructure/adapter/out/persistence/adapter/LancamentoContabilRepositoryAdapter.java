package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.LancamentoContabilRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoContabilEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.LancamentoContabilMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.LancamentoContabilJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter que implementa LancamentoContabilRepositoryPort.
 *
 * <p>Responsável por:
 *
 * <ul>
 *   <li>Converter entre domain models e JPA entities
 *   <li>Resolver FKs (company, contaDebito, contaCredito)
 *   <li>Delegar operações ao JPA repository
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LancamentoContabilRepositoryAdapter implements LancamentoContabilRepositoryPort {

  private final LancamentoContabilJpaRepository jpaRepository;
  private final CompanyJpaRepository companyRepository;
  private final PlanoDeContasJpaRepository planoDeContasRepository;
  private final LancamentoContabilMapper mapper;

  @Override
  public LancamentoContabil save(LancamentoContabil lancamento) {
    log.debug("Saving LancamentoContabil for company: {}", lancamento.getCompanyId());

    // Converter para entity
    LancamentoContabilEntity entity =
        (lancamento.getId() != null)
            ? jpaRepository
                .findById(lancamento.getId())
                .orElseGet(() -> mapper.toEntity(lancamento))
            : mapper.toEntity(lancamento);

    // Resolver FK: company
    CompanyEntity company =
        companyRepository
            .findById(lancamento.getCompanyId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Company not found with id: " + lancamento.getCompanyId()));
    entity.setCompany(company);

    // Resolver FK: contaDebito
    PlanoDeContasEntity contaDebito =
        planoDeContasRepository
            .findById(lancamento.getContaDebitoId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Conta de débito not found with id: " + lancamento.getContaDebitoId()));
    entity.setContaDebito(contaDebito);

    // Resolver FK: contaCredito
    PlanoDeContasEntity contaCredito =
        planoDeContasRepository
            .findById(lancamento.getContaCreditoId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Conta de crédito not found with id: " + lancamento.getContaCreditoId()));
    entity.setContaCredito(contaCredito);

    // Copiar campos do domain para entity
    entity.setData(lancamento.getData());
    entity.setValor(lancamento.getValor());
    entity.setHistorico(lancamento.getHistorico());
    entity.setNumeroDocumento(lancamento.getNumeroDocumento());
    entity.setFiscalYear(lancamento.getFiscalYear());
    entity.setStatus(lancamento.getStatus() != null ? lancamento.getStatus() : Status.ACTIVE);

    // Salvar
    LancamentoContabilEntity saved = jpaRepository.save(entity);

    log.debug("LancamentoContabil saved with id: {}", saved.getId());
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<LancamentoContabil> findById(Long id) {
    log.debug("Finding LancamentoContabil by id: {}", id);
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<LancamentoContabil> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear) {
    log.debug(
        "Finding LancamentosContabeis by companyId: {} and fiscalYear: {}", companyId, fiscalYear);
    return jpaRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<LancamentoContabil> findByCompanyId(Long companyId) {
    log.debug("Finding all LancamentosContabeis by companyId: {}", companyId);
    return jpaRepository.findByCompanyId(companyId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Page<LancamentoContabil> findByCompanyId(Long companyId, Pageable pageable) {
    log.debug("Finding LancamentosContabeis by companyId: {} with pagination", companyId);
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }

  @Override
  public void deleteById(Long id) {
    log.debug("Soft deleting LancamentoContabil with id: {}", id);
    LancamentoContabilEntity entity =
        jpaRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("LancamentoContabil not found with id: " + id));
    entity.setStatus(Status.INACTIVE);
    jpaRepository.save(entity);
    log.debug("LancamentoContabil soft deleted with id: {}", id);
  }
}
