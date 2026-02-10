package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.lancamentocontabil.CreateLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.GetLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.ListLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.ToggleLancamentoContabilStatusUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.UpdateLancamentoContabilUseCase;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoContabilRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.exception.BusinessRuleViolationException;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import br.com.lalurecf.infrastructure.validation.EnforcePeriodoContabil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service para gerenciar lançamentos contábeis (CRUD manual).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LancamentoContabilService
    implements CreateLancamentoContabilUseCase,
        ListLancamentoContabilUseCase,
        GetLancamentoContabilUseCase,
        UpdateLancamentoContabilUseCase,
        ToggleLancamentoContabilStatusUseCase {

  private final LancamentoContabilRepositoryPort lancamentoContabilRepository;
  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final CompanyRepositoryPort companyRepository;

  @Override
  @Transactional
  public LancamentoContabil create(LancamentoContabil lancamento) {
    log.info("Creating lançamento contábil for company context");

    // Validar company context
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new BusinessRuleViolationException(
          "Company context is required (X-Company-Id header missing)");
    }

    // Buscar company para validar Período Contábil
    final Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Company not found with id: " + companyId));

    // Validar campos obrigatórios
    validateMandatoryFields(lancamento);

    // Validar partidas dobradas (débito != crédito)
    if (lancamento.getContaDebitoId().equals(lancamento.getContaCreditoId())) {
      throw new BusinessRuleViolationException(
          "Debit and credit accounts must be different (partidas dobradas)");
    }

    // Validar valor > 0
    if (lancamento.getValor().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleViolationException("Valor must be greater than zero");
    }

    // Buscar e validar conta débito
    PlanoDeContas contaDebito =
        planoDeContasRepository
            .findById(lancamento.getContaDebitoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Conta débito not found with id: " + lancamento.getContaDebitoId()));

    if (!contaDebito.getCompanyId().equals(companyId)) {
      throw new BusinessRuleViolationException(
          "Conta débito does not belong to the company in context");
    }

    if (!contaDebito.getFiscalYear().equals(lancamento.getFiscalYear())) {
      throw new BusinessRuleViolationException(
          "Conta débito fiscal year does not match lancamento fiscal year");
    }

    // Buscar e validar conta crédito
    PlanoDeContas contaCredito =
        planoDeContasRepository
            .findById(lancamento.getContaCreditoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Conta crédito not found with id: " + lancamento.getContaCreditoId()));

    if (!contaCredito.getCompanyId().equals(companyId)) {
      throw new BusinessRuleViolationException(
          "Conta crédito does not belong to the company in context");
    }

    if (!contaCredito.getFiscalYear().equals(lancamento.getFiscalYear())) {
      throw new BusinessRuleViolationException(
          "Conta crédito fiscal year does not match lancamento fiscal year");
    }

    // Validar Período Contábil (data >= company.periodoContabil)
    if (company.getPeriodoContabil() != null
        && lancamento.getData().isBefore(company.getPeriodoContabil())) {
      throw new BusinessRuleViolationException(
          "Data must be >= Período Contábil ("
              + company.getPeriodoContabil()
              + "). Cannot create lancamentos for closed periods.");
    }

    // Preencher campos adicionais
    lancamento.setCompanyId(companyId);
    lancamento.setStatus(Status.ACTIVE);

    // Salvar
    LancamentoContabil saved = lancamentoContabilRepository.save(lancamento);
    log.info("Lançamento contábil created with id: {}", saved.getId());
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LancamentoContabil> list(
      Long contaDebitoId,
      Long contaCreditoId,
      LocalDate data,
      LocalDate dataInicio,
      LocalDate dataFim,
      Integer fiscalYear,
      Boolean includeInactive,
      Pageable pageable) {

    log.info("Listing lançamentos contábeis with filters");

    // Validar company context
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new BusinessRuleViolationException(
          "Company context is required (X-Company-Id header missing)");
    }

    // Aplicar filtros manualmente (simplificado - em produção usar Specification ou QueryDSL)
    List<LancamentoContabil> allLancamentos =
        lancamentoContabilRepository.findByCompanyId(companyId);

    // Filtrar por status
    List<LancamentoContabil> filtered = new ArrayList<>();
    for (LancamentoContabil lancamento : allLancamentos) {
      boolean matches = true;

      // Status filter
      if (includeInactive == null || !includeInactive) {
        if (lancamento.getStatus() != Status.ACTIVE) {
          matches = false;
        }
      }

      // Conta débito filter
      if (contaDebitoId != null && !lancamento.getContaDebitoId().equals(contaDebitoId)) {
        matches = false;
      }

      // Conta crédito filter
      if (contaCreditoId != null && !lancamento.getContaCreditoId().equals(contaCreditoId)) {
        matches = false;
      }

      // Data filter
      if (data != null && !lancamento.getData().equals(data)) {
        matches = false;
      }

      // Data range filter
      if (dataInicio != null && lancamento.getData().isBefore(dataInicio)) {
        matches = false;
      }
      if (dataFim != null && lancamento.getData().isAfter(dataFim)) {
        matches = false;
      }

      // FiscalYear filter
      if (fiscalYear != null && !lancamento.getFiscalYear().equals(fiscalYear)) {
        matches = false;
      }

      if (matches) {
        filtered.add(lancamento);
      }
    }

    // Aplicar paginação manualmente
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), filtered.size());
    List<LancamentoContabil> page = filtered.subList(start, end);

    return new PageImpl<>(page, pageable, filtered.size());
  }

  @Override
  @Transactional(readOnly = true)
  public LancamentoContabil getById(Long id) {
    log.info("Getting lançamento contábil by id: {}", id);

    // Validar company context
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new BusinessRuleViolationException(
          "Company context is required (X-Company-Id header missing)");
    }

    LancamentoContabil lancamento =
        lancamentoContabilRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Lançamento contábil not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!lancamento.getCompanyId().equals(companyId)) {
      throw new BusinessRuleViolationException(
          "Lançamento contábil does not belong to the company in context");
    }

    return lancamento;
  }

  @Override
  @Transactional
  @EnforcePeriodoContabil
  public LancamentoContabil update(Long id, LancamentoContabil lancamentoAtualizado) {
    log.info("Updating lançamento contábil with id: {}", id);

    // Validar company context
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new BusinessRuleViolationException(
          "Company context is required (X-Company-Id header missing)");
    }

    // Buscar lançamento existente
    LancamentoContabil existing =
        lancamentoContabilRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Lançamento contábil not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!existing.getCompanyId().equals(companyId)) {
      throw new BusinessRuleViolationException(
          "Lançamento contábil does not belong to the company in context");
    }

    // Buscar company para validar Período Contábil
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Company not found with id: " + companyId));

    // Validar Período Contábil ANTES de permitir edição (data original)
    if (company.getPeriodoContabil() != null
        && existing.getData().isBefore(company.getPeriodoContabil())) {
      throw new BusinessRuleViolationException(
          "Cannot update lancamento with data < Período Contábil ("
              + company.getPeriodoContabil()
              + "). Period is closed.");
    }

    // Validar campos obrigatórios
    validateMandatoryFields(lancamentoAtualizado);

    // Validar partidas dobradas
    if (lancamentoAtualizado.getContaDebitoId().equals(lancamentoAtualizado.getContaCreditoId())) {
      throw new BusinessRuleViolationException(
          "Debit and credit accounts must be different (partidas dobradas)");
    }

    // Validar valor > 0
    if (lancamentoAtualizado.getValor().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleViolationException("Valor must be greater than zero");
    }

    // Buscar e validar conta débito
    PlanoDeContas contaDebito =
        planoDeContasRepository
            .findById(lancamentoAtualizado.getContaDebitoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Conta débito not found with id: "
                            + lancamentoAtualizado.getContaDebitoId()));

    if (!contaDebito.getCompanyId().equals(companyId)) {
      throw new BusinessRuleViolationException(
          "Conta débito does not belong to the company in context");
    }

    // Buscar e validar conta crédito
    PlanoDeContas contaCredito =
        planoDeContasRepository
            .findById(lancamentoAtualizado.getContaCreditoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Conta crédito not found with id: "
                            + lancamentoAtualizado.getContaCreditoId()));

    if (!contaCredito.getCompanyId().equals(companyId)) {
      throw new BusinessRuleViolationException(
          "Conta crédito does not belong to the company in context");
    }

    // Validar Período Contábil para NOVA data
    if (company.getPeriodoContabil() != null
        && lancamentoAtualizado.getData().isBefore(company.getPeriodoContabil())) {
      throw new BusinessRuleViolationException(
          "Data must be >= Período Contábil ("
              + company.getPeriodoContabil()
              + "). Cannot update to closed period.");
    }

    // Atualizar campos
    existing.setContaDebitoId(lancamentoAtualizado.getContaDebitoId());
    existing.setContaCreditoId(lancamentoAtualizado.getContaCreditoId());
    existing.setData(lancamentoAtualizado.getData());
    existing.setValor(lancamentoAtualizado.getValor());
    existing.setHistorico(lancamentoAtualizado.getHistorico());
    existing.setNumeroDocumento(lancamentoAtualizado.getNumeroDocumento());

    // Salvar
    LancamentoContabil updated = lancamentoContabilRepository.save(existing);
    log.info("Lançamento contábil updated with id: {}", updated.getId());
    return updated;
  }

  @Override
  @Transactional
  @EnforcePeriodoContabil
  public LancamentoContabil toggleStatus(Long id) {
    log.info("Toggling status of lançamento contábil with id: {}", id);

    // Validar company context
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new BusinessRuleViolationException(
          "Company context is required (X-Company-Id header missing)");
    }

    // Buscar lançamento existente
    LancamentoContabil existing =
        lancamentoContabilRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Lançamento contábil not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!existing.getCompanyId().equals(companyId)) {
      throw new BusinessRuleViolationException(
          "Lançamento contábil does not belong to the company in context");
    }

    // Buscar company para validar Período Contábil
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Company not found with id: " + companyId));

    // Validar Período Contábil ANTES de permitir toggle
    if (company.getPeriodoContabil() != null
        && existing.getData().isBefore(company.getPeriodoContabil())) {
      throw new BusinessRuleViolationException(
          "Cannot toggle status of lancamento with data < Período Contábil ("
              + company.getPeriodoContabil()
              + "). Period is closed.");
    }

    // Alternar status
    Status newStatus = existing.getStatus() == Status.ACTIVE ? Status.INACTIVE : Status.ACTIVE;
    existing.setStatus(newStatus);

    // Salvar
    LancamentoContabil updated = lancamentoContabilRepository.save(existing);
    log.info("Lançamento contábil status toggled to {} for id: {}", newStatus, updated.getId());
    return updated;
  }

  private void validateMandatoryFields(LancamentoContabil lancamento) {
    if (lancamento.getContaDebitoId() == null) {
      throw new BusinessRuleViolationException("Conta débito is required");
    }
    if (lancamento.getContaCreditoId() == null) {
      throw new BusinessRuleViolationException("Conta crédito is required");
    }
    if (lancamento.getData() == null) {
      throw new BusinessRuleViolationException("Data is required");
    }
    if (lancamento.getValor() == null) {
      throw new BusinessRuleViolationException("Valor is required");
    }
    if (lancamento.getHistorico() == null || lancamento.getHistorico().trim().isEmpty()) {
      throw new BusinessRuleViolationException("Histórico is required");
    }
    if (lancamento.getFiscalYear() == null) {
      throw new BusinessRuleViolationException("FiscalYear is required");
    }
    if (lancamento.getFiscalYear() < 2000) {
      throw new BusinessRuleViolationException("FiscalYear must be >= 2000");
    }
  }
}
