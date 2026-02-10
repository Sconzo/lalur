package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.planodecontas.CreatePlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.GetPlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.ListPlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.TogglePlanoDeContasStatusUseCase;
import br.com.lalurecf.application.port.in.planodecontas.UpdatePlanoDeContasUseCase;
import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.infrastructure.dto.mapper.PlanoDeContasDtoMapper;
import br.com.lalurecf.infrastructure.dto.planodecontas.CreatePlanoDeContasRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.UpdatePlanoDeContasRequest;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import java.time.Year;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service que implementa os Use Cases de PlanoDeContas (Plano de Contas).
 *
 * <p>Gerencia CRUD de contas contábeis com validações ECF e vinculação a Conta Referencial RFB.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanoDeContasService
    implements CreatePlanoDeContasUseCase,
        ListPlanoDeContasUseCase,
        GetPlanoDeContasUseCase,
        UpdatePlanoDeContasUseCase,
        TogglePlanoDeContasStatusUseCase {

  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final ContaReferencialRepositoryPort contaReferencialRepository;
  private final PlanoDeContasDtoMapper dtoMapper;

  @Override
  @Transactional
  public PlanoDeContasResponse execute(CreatePlanoDeContasRequest request) {
    log.info("Creating PlanoDeContas with code: {}", request.getCode());

    // Obter empresa do contexto
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    // Validar fiscal year
    validateFiscalYear(request.getFiscalYear());

    // Validar code não vazio
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw new IllegalArgumentException("Code cannot be empty");
    }

    // Validar contaReferencialId existe e está ACTIVE
    ContaReferencial contaReferencial =
        contaReferencialRepository
            .findById(request.getContaReferencialId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "ContaReferencial not found with id: " + request.getContaReferencialId()));

    if (contaReferencial.getStatus() != Status.ACTIVE) {
      throw new IllegalArgumentException(
          "ContaReferencial must be ACTIVE. Current status: " + contaReferencial.getStatus());
    }

    // Verificar unicidade (company + code + fiscalYear)
    Optional<PlanoDeContas> existing =
        planoDeContasRepository.findByCompanyIdAndCodeAndFiscalYear(
            companyId, request.getCode(), request.getFiscalYear());

    if (existing.isPresent()) {
      throw new IllegalArgumentException(
          String.format(
              "PlanoDeContas with code '%s' already exists for company %d and fiscal year %d",
              request.getCode(), companyId, request.getFiscalYear()));
    }

    // Criar conta
    PlanoDeContas account =
        PlanoDeContas.builder()
            .companyId(companyId)
            .code(request.getCode())
            .name(request.getName())
            .fiscalYear(request.getFiscalYear())
            .accountType(request.getAccountType())
            .contaReferencialId(request.getContaReferencialId())
            .classe(request.getClasse())
            .nivel(request.getNivel())
            .natureza(request.getNatureza())
            .afetaResultado(request.getAfetaResultado())
            .dedutivel(request.getDedutivel())
            .status(Status.ACTIVE)
            .build();

    PlanoDeContas saved = planoDeContasRepository.save(account);
    log.info("PlanoDeContas created successfully with id: {}", saved.getId());

    return dtoMapper.toResponse(saved, contaReferencial.getCodigoRfb());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PlanoDeContasResponse> execute(
      Integer fiscalYear,
      AccountType accountType,
      ClasseContabil classe,
      NaturezaConta natureza,
      String search,
      Boolean includeInactive,
      Pageable pageable) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info(
        "Listing PlanoDeContas for company: {}, fiscalYear: {}", companyId, fiscalYear);

    // Buscar todas contas da empresa
    Page<PlanoDeContas> accountsPage =
        planoDeContasRepository.findByCompanyId(companyId, pageable);

    // Filtrar por critérios
    var filteredAccounts =
        accountsPage.getContent().stream()
            .filter(
                acc -> {
                  // Filtro fiscal year
                  if (fiscalYear != null && !fiscalYear.equals(acc.getFiscalYear())) {
                    return false;
                  }

                  // Filtro account type
                  if (accountType != null && !accountType.equals(acc.getAccountType())) {
                    return false;
                  }

                  // Filtro classe
                  if (classe != null && !classe.equals(acc.getClasse())) {
                    return false;
                  }

                  // Filtro natureza
                  if (natureza != null && !natureza.equals(acc.getNatureza())) {
                    return false;
                  }

                  // Filtro search (code ou name)
                  if (search != null
                      && !search.trim().isEmpty()
                      && !acc.getCode().toLowerCase().contains(search.toLowerCase())
                      && !acc.getName().toLowerCase().contains(search.toLowerCase())) {
                    return false;
                  }

                  // Filtro status
                  if (includeInactive == null || !includeInactive) {
                    return acc.getStatus() == Status.ACTIVE;
                  }

                  return true;
                })
            .map(
                acc -> {
                  // Buscar código da conta referencial
                  String codigoRfb =
                      contaReferencialRepository
                          .findById(acc.getContaReferencialId())
                          .map(ContaReferencial::getCodigoRfb)
                          .orElse(null);
                  return dtoMapper.toResponse(acc, codigoRfb);
                })
            .toList();

    return new PageImpl<>(filteredAccounts, pageable, accountsPage.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public PlanoDeContasResponse execute(Long id) {
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Getting PlanoDeContas with id: {}", id);

    PlanoDeContas account =
        planoDeContasRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("PlanoDeContas not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!account.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "PlanoDeContas does not belong to company in context");
    }

    // Buscar código da conta referencial
    String codigoRfb =
        contaReferencialRepository
            .findById(account.getContaReferencialId())
            .map(ContaReferencial::getCodigoRfb)
            .orElse(null);

    return dtoMapper.toResponse(account, codigoRfb);
  }

  @Override
  @Transactional
  public PlanoDeContasResponse execute(Long id, UpdatePlanoDeContasRequest request) {
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Updating PlanoDeContas with id: {}", id);

    PlanoDeContas account =
        planoDeContasRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("PlanoDeContas not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!account.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "PlanoDeContas does not belong to company in context");
    }

    // Validar contaReferencialId existe e está ACTIVE
    ContaReferencial contaReferencial =
        contaReferencialRepository
            .findById(request.getContaReferencialId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "ContaReferencial not found with id: " + request.getContaReferencialId()));

    if (contaReferencial.getStatus() != Status.ACTIVE) {
      throw new IllegalArgumentException(
          "ContaReferencial must be ACTIVE. Current status: " + contaReferencial.getStatus());
    }

    // Atualizar campos (code e fiscalYear são imutáveis)
    account.setName(request.getName());
    account.setAccountType(request.getAccountType());
    account.setContaReferencialId(request.getContaReferencialId());
    account.setClasse(request.getClasse());
    account.setNivel(request.getNivel());
    account.setNatureza(request.getNatureza());
    account.setAfetaResultado(request.getAfetaResultado());
    account.setDedutivel(request.getDedutivel());

    PlanoDeContas updated = planoDeContasRepository.save(account);
    log.info("PlanoDeContas updated successfully with id: {}", updated.getId());

    return dtoMapper.toResponse(updated, contaReferencial.getCodigoRfb());
  }

  @Override
  @Transactional
  public ToggleStatusResponse execute(Long id, ToggleStatusRequest request) {
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Toggling status of PlanoDeContas with id: {} to {}", id, request.getStatus());

    PlanoDeContas account =
        planoDeContasRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("PlanoDeContas not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!account.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "PlanoDeContas does not belong to company in context");
    }

    // Alternar status
    account.setStatus(request.getStatus());
    planoDeContasRepository.save(account);

    String message =
        String.format(
            "PlanoDeContas '%s' status changed to %s", account.getName(), request.getStatus());

    log.info("PlanoDeContas status toggled successfully: {}", message);

    return ToggleStatusResponse.builder()
        .success(true)
        .message(message)
        .newStatus(request.getStatus())
        .build();
  }

  /**
   * Valida que fiscal year está no range permitido (2000 a ano atual + 1).
   *
   * @param fiscalYear ano fiscal a validar
   */
  private void validateFiscalYear(Integer fiscalYear) {
    int currentYear = Year.now().getValue();
    int maxYear = currentYear + 1;

    if (fiscalYear < 2000 || fiscalYear > maxYear) {
      throw new IllegalArgumentException(
          String.format("Fiscal year must be between 2000 and %d. Got: %d", maxYear, fiscalYear));
    }
  }
}
