package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.company.CreateCompanyUseCase;
import br.com.lalurecf.application.port.in.company.CreateTemporalValueUseCase;
import br.com.lalurecf.application.port.in.company.DeleteTemporalValueUseCase;
import br.com.lalurecf.application.port.in.company.GetCompanyTaxParametersTimelineUseCase;
import br.com.lalurecf.application.port.in.company.GetCompanyUseCase;
import br.com.lalurecf.application.port.in.company.GetPeriodoContabilAuditUseCase;
import br.com.lalurecf.application.port.in.company.ListCompaniesUseCase;
import br.com.lalurecf.application.port.in.company.ListCompanyTaxParametersUseCase;
import br.com.lalurecf.application.port.in.company.ListTemporalValuesUseCase;
import br.com.lalurecf.application.port.in.company.SelectCompanyUseCase;
import br.com.lalurecf.application.port.in.company.ToggleCompanyStatusUseCase;
import br.com.lalurecf.application.port.in.company.UpdateCompanyTaxParametersUseCase;
import br.com.lalurecf.application.port.in.company.UpdateCompanyUseCase;
import br.com.lalurecf.application.port.in.company.UpdatePeriodoContabilUseCase;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.CompanyStatus;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyTaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.EmpresaParametrosTributariosEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PeriodoContabilAuditEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ValorParametroTemporalEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyTaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PeriodoContabilAuditJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ValorParametroTemporalJpaRepository;
import br.com.lalurecf.infrastructure.dto.company.CompanyDetailResponse;
import br.com.lalurecf.infrastructure.dto.company.CompanyResponse;
import br.com.lalurecf.infrastructure.dto.company.CreateCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.CreateTemporalValueRequest;
import br.com.lalurecf.infrastructure.dto.company.PeriodicParameterRequest;
import br.com.lalurecf.infrastructure.dto.company.PeriodoContabilAuditResponse;
import br.com.lalurecf.infrastructure.dto.company.TaxParameterSummary;
import br.com.lalurecf.infrastructure.dto.company.TemporalValueInput;
import br.com.lalurecf.infrastructure.dto.company.TemporalValueResponse;
import br.com.lalurecf.infrastructure.dto.company.TimelineResponse;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.company.UpdateCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilResponse;
import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersRequestV2;
import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementando use cases de Company.
 *
 * <p>Implementa todos os 5 use cases: Create, List, Get, Update, Toggle Status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService implements
    CreateCompanyUseCase,
    ListCompaniesUseCase,
    GetCompanyUseCase,
    UpdateCompanyUseCase,
    ToggleCompanyStatusUseCase,
    SelectCompanyUseCase,
    UpdatePeriodoContabilUseCase,
    GetPeriodoContabilAuditUseCase,
    UpdateCompanyTaxParametersUseCase,
    ListCompanyTaxParametersUseCase,
    CreateTemporalValueUseCase,
    ListTemporalValuesUseCase,
    DeleteTemporalValueUseCase,
    GetCompanyTaxParametersTimelineUseCase {

  private final CompanyJpaRepository companyRepository;
  private final TaxParameterJpaRepository taxParameterRepository;
  private final CompanyTaxParameterJpaRepository companyTaxParameterRepository;
  private final PeriodoContabilAuditJpaRepository periodoContabilAuditRepository;
  private final ValorParametroTemporalJpaRepository valorParametroTemporalRepository;

  @Override
  @Transactional
  public CompanyDetailResponse create(CreateCompanyRequest request) {
    log.info("Criando nova empresa com CNPJ: {}", request.cnpj());

    // Validar CNPJ usando Value Object
    CNPJ cnpj = CNPJ.of(request.cnpj());

    // Verificar se já existe empresa ATIVA com mesmo CNPJ
    companyRepository.findByCnpjAndStatus(cnpj.getValue(), Status.ACTIVE)
        .ifPresent(existing -> {
          log.warn("Tentativa de criar empresa com CNPJ duplicado: {}", cnpj.getValue());
          throw new IllegalArgumentException(
              "Já existe uma empresa ativa com o CNPJ: " + cnpj.format());
        });

    // Validar os 3 parâmetros tributários obrigatórios
    validateRequiredTaxParameters(
        request.cnaeParametroId(),
        request.qualificacaoPjParametroId(),
        request.naturezaJuridicaParametroId()
    );

    // Criar entidade
    CompanyEntity entity = new CompanyEntity();
    entity.setCnpj(cnpj.getValue());
    entity.setRazaoSocial(request.razaoSocial());
    entity.setPeriodoContabil(request.periodoContabil());
    entity.setStatus(Status.ACTIVE);

    CompanyEntity saved = companyRepository.save(entity);
    log.info("Empresa criada com sucesso. ID: {}, CNPJ: {}", saved.getId(), cnpj.getValue());

    // Criar associações com os 3 parâmetros obrigatórios
    createTaxParameterAssociation(saved.getId(), request.cnaeParametroId());
    createTaxParameterAssociation(saved.getId(), request.qualificacaoPjParametroId());
    createTaxParameterAssociation(saved.getId(), request.naturezaJuridicaParametroId());

    // Criar associações com outros parâmetros opcionais
    if (request.outrosParametrosIds() != null) {
      for (Long parametroId : request.outrosParametrosIds()) {
        createTaxParameterAssociation(saved.getId(), parametroId);
      }
    }

    return toDetailResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CompanyResponse> list(
      String strSearch,
      List<String> cnpjFilters,
      List<String> razaoSocialFilters,
      boolean includeInactive,
      Pageable pageable) {

    log.info("Listando empresas - strSearch: {}, cnpjFilters: {}, razaoSocialFilters: {}, "
            + "includeInactive: {}",
        strSearch, cnpjFilters, razaoSocialFilters, includeInactive);

    Specification<CompanyEntity> spec = buildSpecification(
        strSearch, cnpjFilters, razaoSocialFilters, includeInactive);

    Page<CompanyEntity> entities = companyRepository.findAll(spec, pageable);

    return entities.map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyDetailResponse getById(Long id) {
    log.debug("Buscando empresa por ID: {}", id);

    CompanyEntity entity = companyRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Empresa não encontrada. ID: {}", id);
          return new EntityNotFoundException("Empresa não encontrada com ID: " + id);
        });

    return toDetailResponse(entity);
  }

  @Override
  @Transactional
  public CompanyDetailResponse update(Long id, UpdateCompanyRequest request) {
    log.info("Atualizando empresa ID: {}", id);

    CompanyEntity entity = companyRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Tentativa de atualizar empresa inexistente. ID: {}", id);
          return new EntityNotFoundException("Empresa não encontrada com ID: " + id);
        });

    // Validar os 3 parâmetros tributários obrigatórios
    validateRequiredTaxParameters(
        request.cnaeParametroId(),
        request.qualificacaoPjParametroId(),
        request.naturezaJuridicaParametroId()
    );

    // Atualizar campos (CNPJ é imutável)
    entity.setRazaoSocial(request.razaoSocial());
    entity.setPeriodoContabil(request.periodoContabil());

    // Remover todas as associações existentes
    companyTaxParameterRepository.deleteAllByCompanyId(id);

    // Criar associações com os 3 parâmetros obrigatórios
    createTaxParameterAssociation(id, request.cnaeParametroId());
    createTaxParameterAssociation(id, request.qualificacaoPjParametroId());
    createTaxParameterAssociation(id, request.naturezaJuridicaParametroId());

    // Criar associações com outros parâmetros opcionais
    if (request.outrosParametrosIds() != null) {
      for (Long parametroId : request.outrosParametrosIds()) {
        createTaxParameterAssociation(id, parametroId);
      }
    }

    CompanyEntity updated = companyRepository.save(entity);
    log.info("Empresa atualizada com sucesso. ID: {}", id);

    return toDetailResponse(updated);
  }

  @Override
  @Transactional
  public UpdatePeriodoContabilResponse update(
      Long companyId,
      UpdatePeriodoContabilRequest request) {

    log.info("Atualizando Período Contábil da empresa ID: {}", companyId);

    // Buscar empresa
    CompanyEntity company = companyRepository.findById(companyId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Empresa não encontrada com ID: " + companyId));

    LocalDate periodoAnterior = company.getPeriodoContabil();
    LocalDate periodoNovo = request.novoPeriodoContabil();

    // Validação 1: Nova data não pode ser no futuro
    if (periodoNovo.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException(
          "Período Contábil não pode ser uma data futura: " + periodoNovo);
    }

    // Validação 2: Nova data não pode retroagir (deve ser posterior à atual)
    if (periodoNovo.isBefore(periodoAnterior)) {
      throw new IllegalArgumentException(
          "Período Contábil não pode retroagir. Valor atual: " + periodoAnterior
              + ", Valor fornecido: " + periodoNovo);
    }

    // Validação 3: Nova data deve ser diferente da atual
    if (periodoNovo.equals(periodoAnterior)) {
      throw new IllegalArgumentException(
          "Período Contábil já está definido como: " + periodoNovo);
    }

    // Obter usuário autenticado
    String userEmail = getCurrentUserEmail();

    // Registrar em log de auditoria ANTES de atualizar
    PeriodoContabilAuditEntity audit = PeriodoContabilAuditEntity.builder()
        .companyId(companyId)
        .periodoContabilAnterior(periodoAnterior)
        .periodoContabilNovo(periodoNovo)
        .changedBy(userEmail)
        .changedAt(LocalDateTime.now())
        .build();

    periodoContabilAuditRepository.save(audit);
    log.info("Registro de auditoria criado para alteração de Período Contábil. "
        + "Company ID: {}, Anterior: {}, Novo: {}, Por: {}",
        companyId, periodoAnterior, periodoNovo, userEmail);

    // Atualizar empresa
    company.setPeriodoContabil(periodoNovo);
    companyRepository.save(company);

    log.info("Período Contábil atualizado com sucesso. "
        + "Company ID: {}, Anterior: {}, Novo: {}",
        companyId, periodoAnterior, periodoNovo);

    return new UpdatePeriodoContabilResponse(
        true,
        "Período Contábil atualizado com sucesso",
        periodoAnterior,
        periodoNovo
    );
  }

  @Override
  @Transactional
  public ToggleStatusResponse toggleStatus(Long id, CompanyStatus newStatus) {
    log.info("Alterando status da empresa ID: {} para {}", id, newStatus);

    CompanyEntity entity = companyRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Tentativa de alterar status de empresa inexistente. ID: {}", id);
          return new EntityNotFoundException("Empresa não encontrada com ID: " + id);
        });

    Status oldStatus = entity.getStatus();
    entity.setStatus(newStatus.toStatus());
    companyRepository.save(entity);

    log.info("Status da empresa ID: {} alterado de {} para {}", id, oldStatus, newStatus);

    return new ToggleStatusResponse(
        true,
        "Status alterado com sucesso de " + oldStatus + " para " + newStatus,
        newStatus
    );
  }

  /**
   * Constrói Specification para filtros dinâmicos.
   */
  private Specification<CompanyEntity> buildSpecification(
      String strSearch,
      List<String> cnpjFilters,
      List<String> razaoSocialFilters,
      boolean includeInactive) {

    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Filtro de status (padrão: apenas ACTIVE)
      if (!includeInactive) {
        predicates.add(cb.equal(root.get("status"), Status.ACTIVE));
      }

      // Filtro global (busca em CNPJ, Razão Social e Parâmetros Tributários)
      if (strSearch != null && !strSearch.isBlank()) {
        String search = "%" + strSearch.toLowerCase() + "%";

        // Subquery para buscar empresas com parâmetros tributários que correspondem
        var subquery = query.subquery(Long.class);
        var assocRoot = subquery.from(CompanyTaxParameterEntity.class);

        // Subquery aninhada para buscar IDs de parâmetros que correspondem ao filtro
        var paramSubquery = subquery.subquery(Long.class);
        var paramRoot = paramSubquery.from(TaxParameterEntity.class);
        paramSubquery.select(paramRoot.get("id"))
            .where(cb.or(
                cb.like(cb.lower(paramRoot.get("codigo")), search),
                cb.like(cb.lower(paramRoot.get("descricao")), search)
            ));

        subquery.select(assocRoot.get("companyId"))
            .where(cb.and(
                cb.equal(assocRoot.get("companyId"), root.get("id")),
                assocRoot.get("taxParameterId").in(paramSubquery)
            ));

        Predicate globalPredicate = cb.or(
            cb.like(cb.lower(root.get("cnpj")), search),
            cb.like(cb.lower(root.get("razaoSocial")), search),
            cb.exists(subquery)
        );
        predicates.add(globalPredicate);
      }

      // Filtro específico por CNPJ (lista com comparação exata)
      if (cnpjFilters != null && !cnpjFilters.isEmpty()) {
        predicates.add(root.get("cnpj").in(cnpjFilters));
      }

      // Filtro específico por Razão Social (lista com comparação exata)
      if (razaoSocialFilters != null && !razaoSocialFilters.isEmpty()) {
        predicates.add(root.get("razaoSocial").in(razaoSocialFilters));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Converte Entity para CompanyResponse (listagem).
   */
  private CompanyResponse toResponse(CompanyEntity entity) {
    // Buscar os 3 parâmetros tributários obrigatórios via JOIN
    List<CompanyTaxParameterEntity> associations =
        companyTaxParameterRepository.findByCompanyId(entity.getId());

    // Buscar os parâmetros tributários completos
    List<Long> parameterIds = associations.stream()
        .map(CompanyTaxParameterEntity::getTaxParameterId)
        .toList();

    List<TaxParameterEntity> parameters = parameterIds.isEmpty()
        ? Collections.emptyList()
        : taxParameterRepository.findAllById(parameterIds);

    // Criar mapa para lookup de informações de auditoria
    java.util.Map<Long, CompanyTaxParameterEntity> associationMap = associations.stream()
        .collect(Collectors.toMap(
            CompanyTaxParameterEntity::getTaxParameterId,
            assoc -> assoc));

    // Verificar quais parâmetros têm valores temporais
    java.util.Set<Long> associationsWithTemporalValues = associations.stream()
        .map(CompanyTaxParameterEntity::getId)
        .filter(assocId -> {
          List<ValorParametroTemporalEntity> valores =
              valorParametroTemporalRepository.findByEmpresaParametrosTributariosId(assocId);
          return !valores.isEmpty();
        })
        .collect(Collectors.toSet());

    // Criar mapa de taxParameterId -> associationId para lookup
    java.util.Map<Long, Long> taxParamToAssocId = associationMap.entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey(),
            e -> e.getValue().getId()));

    // Encontrar cada parâmetro pelo tipo
    TaxParameterSummary cnae = findParameterByType(
        parameters, associationMap, associationsWithTemporalValues, taxParamToAssocId, "CNAE");
    TaxParameterSummary qualificacaoPj = findParameterByType(
        parameters, associationMap, associationsWithTemporalValues, taxParamToAssocId,
        "QUALIFICACAO_PJ");
    TaxParameterSummary naturezaJuridica = findParameterByType(
        parameters, associationMap, associationsWithTemporalValues, taxParamToAssocId,
        "NATUREZA_JURIDICA");

    return new CompanyResponse(
        entity.getId(),
        formatCnpj(entity.getCnpj()),
        CompanyStatus.fromStatus(entity.getStatus()),
        entity.getRazaoSocial(),
        cnae,
        qualificacaoPj,
        naturezaJuridica
    );
  }

  /**
   * Converte Entity para CompanyDetailResponse (detalhes).
   */
  private CompanyDetailResponse toDetailResponse(CompanyEntity entity) {
    // Buscar todos os parâmetros tributários associados
    List<CompanyTaxParameterEntity> associations =
        companyTaxParameterRepository.findByCompanyId(entity.getId());

    // Buscar os parâmetros tributários completos
    List<Long> parameterIds = associations.stream()
        .map(CompanyTaxParameterEntity::getTaxParameterId)
        .toList();

    List<TaxParameterEntity> parameters = parameterIds.isEmpty()
        ? Collections.emptyList()
        : taxParameterRepository.findAllById(parameterIds);

    // Criar mapa para lookup de informações de auditoria
    java.util.Map<Long, CompanyTaxParameterEntity> associationMap = associations.stream()
        .collect(Collectors.toMap(
            CompanyTaxParameterEntity::getTaxParameterId,
            assoc -> assoc));

    // Verificar quais parâmetros têm valores temporais
    java.util.Set<Long> associationsWithTemporalValues = associations.stream()
        .map(CompanyTaxParameterEntity::getId)
        .filter(assocId -> {
          List<ValorParametroTemporalEntity> valores =
              valorParametroTemporalRepository.findByEmpresaParametrosTributariosId(assocId);
          return !valores.isEmpty();
        })
        .collect(Collectors.toSet());

    // Criar mapa de taxParameterId -> associationId para lookup
    java.util.Map<Long, Long> taxParamToAssocId = associationMap.entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey(),
            e -> e.getValue().getId()));

    // Encontrar os 3 parâmetros obrigatórios pelo tipo
    TaxParameterSummary cnae = findParameterByType(
        parameters, associationMap, associationsWithTemporalValues, taxParamToAssocId, "CNAE");
    TaxParameterSummary qualificacaoPj = findParameterByType(
        parameters, associationMap, associationsWithTemporalValues, taxParamToAssocId,
        "QUALIFICACAO_PJ");
    TaxParameterSummary naturezaJuridica = findParameterByType(
        parameters, associationMap, associationsWithTemporalValues, taxParamToAssocId,
        "NATUREZA_JURIDICA");

    // Encontrar outros parâmetros (que não são os 3 obrigatórios)
    List<TaxParameterSummary> outrosParametros = parameters.stream()
        .filter(p -> !p.getTipo().equals("CNAE")
            && !p.getTipo().equals("QUALIFICACAO_PJ")
            && !p.getTipo().equals("NATUREZA_JURIDICA"))
        .map(p -> {
          CompanyTaxParameterEntity assoc = associationMap.get(p.getId());
          String createdByEmail = "admin@example.com"; // TODO: buscar email do usuário
          Long assocId = taxParamToAssocId.get(p.getId());
          boolean hasTemporalValues = assocId != null
              && associationsWithTemporalValues.contains(assocId);
          return new TaxParameterSummary(
              p.getId(),
              p.getCodigo(),
              p.getTipo(),
              p.getDescricao(),
              assoc != null ? assoc.getCreatedAt() : null,
              createdByEmail,
              hasTemporalValues);
        })
        .toList();

    return new CompanyDetailResponse(
        entity.getId(),
        formatCnpj(entity.getCnpj()),
        CompanyStatus.fromStatus(entity.getStatus()),
        entity.getRazaoSocial(),
        entity.getPeriodoContabil(),
        cnae,
        qualificacaoPj,
        naturezaJuridica,
        outrosParametros,
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  /**
   * Formata CNPJ para exibição: 00.000.000/0000-00.
   */
  private String formatCnpj(String cnpj) {
    if (cnpj == null || cnpj.length() != 14) {
      return cnpj;
    }
    return String.format("%s.%s.%s/%s-%s",
        cnpj.substring(0, 2),
        cnpj.substring(2, 5),
        cnpj.substring(5, 8),
        cnpj.substring(8, 12),
        cnpj.substring(12, 14)
    );
  }

  /**
   * Seleciona uma empresa para trabalho do usuário.
   * Valida que empresa existe e está ACTIVE.
   *
   * @param companyId ID da empresa
   * @return Company domain model
   * @throws EntityNotFoundException se empresa não existe ou está inativa
   */
  @Override
  @Transactional(readOnly = true)
  public br.com.lalurecf.domain.model.Company selectCompany(Long companyId) {
    log.info("Selecionando empresa: companyId={}", companyId);

    CompanyEntity entity = companyRepository.findById(companyId)
        .orElseThrow(() -> {
          log.warn("Empresa não encontrada: companyId={}", companyId);
          return new EntityNotFoundException(
              "Empresa com ID " + companyId + " não encontrada");
        });

    if (!Status.ACTIVE.equals(entity.getStatus())) {
      log.warn("Tentativa de selecionar empresa inativa: companyId={}", companyId);
      throw new EntityNotFoundException(
          "Empresa com ID " + companyId + " está inativa");
    }

    log.info("Empresa selecionada com sucesso: companyId={}, razaoSocial={}",
        companyId, entity.getRazaoSocial());

    // Convert to domain model
    br.com.lalurecf.domain.model.Company company = new br.com.lalurecf.domain.model.Company();
    company.setId(entity.getId());
    company.setCnpj(CNPJ.of(entity.getCnpj()));
    company.setRazaoSocial(entity.getRazaoSocial());
    company.setPeriodoContabil(entity.getPeriodoContabil());
    company.setStatus(entity.getStatus());
    company.setCreatedBy(entity.getCreatedBy());
    company.setCreatedAt(entity.getCreatedAt());
    company.setUpdatedBy(entity.getUpdatedBy());
    company.setUpdatedAt(entity.getUpdatedAt());

    return company;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PeriodoContabilAuditResponse> getAuditHistory(Long companyId) {
    log.info("Buscando histórico de auditoria do Período Contábil para empresa ID: {}",
        companyId);

    // Validar que empresa existe
    if (!companyRepository.existsById(companyId)) {
      throw new EntityNotFoundException("Empresa não encontrada com ID: " + companyId);
    }

    List<PeriodoContabilAuditEntity> audits =
        periodoContabilAuditRepository.findByCompanyIdOrderByChangedAtDesc(companyId);

    return audits.stream()
        .map(audit -> new PeriodoContabilAuditResponse(
            audit.getId(),
            audit.getPeriodoContabilAnterior(),
            audit.getPeriodoContabilNovo(),
            audit.getChangedBy(),
            audit.getChangedAt()
        ))
        .collect(Collectors.toList());
  }

  /**
   * Atualiza parâmetros tributários de uma empresa (suporta periódicos e globais).
   *
   * <p>Suporta dois tipos de parâmetros:
   * <ul>
   *   <li><b>Globais:</b> Aplicam-se ao ano inteiro
   *   <li><b>Periódicos:</b> Requerem valores temporais (mês/trimestre específico)
   * </ul>
   *
   * <p>Operação atômica: remove todas as associações existentes e cria novas.
   *
   * @param companyId ID da empresa
   * @param request request contendo parâmetros globais e periódicos
   * @return response com lista atualizada de parâmetros
   */
  @Override
  @Transactional
  public UpdateTaxParametersResponse updateTaxParameters(
      Long companyId,
      UpdateTaxParametersRequestV2 request) {

    log.info("Atualizando parâmetros tributários para empresa ID: {}", companyId);

    // Validar que empresa existe
    CompanyEntity company = companyRepository.findById(companyId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Empresa não encontrada com ID: " + companyId));

    // Coletar todos os IDs de parâmetros (globais + periódicos)
    List<Long> globalIds = request.globalParameterIds() != null
        ? request.globalParameterIds()
        : Collections.emptyList();

    List<Long> periodicIds = request.periodicParameters() != null
        ? request.periodicParameters().stream()
            .map(PeriodicParameterRequest::taxParameterId)
            .collect(Collectors.toList())
        : Collections.emptyList();

    // Combinar e remover duplicatas
    List<Long> allParameterIds = new java.util.ArrayList<>();
    allParameterIds.addAll(globalIds);
    allParameterIds.addAll(periodicIds);
    allParameterIds = allParameterIds.stream().distinct().collect(Collectors.toList());

    // Validar que todos os parâmetros existem e estão ACTIVE
    for (Long parameterId : allParameterIds) {
      TaxParameterEntity parameter = taxParameterRepository.findById(parameterId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Parâmetro tributário não encontrado com ID: " + parameterId));

      if (parameter.getStatus() != Status.ACTIVE) {
        throw new IllegalArgumentException(
            "Não é permitido associar parâmetro INACTIVE. Parâmetro ID: " + parameterId);
      }
    }

    // Obter userId do SecurityContext para auditoria
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = authentication.getName();
    Long userId = 1L; // TODO: Buscar ID do usuário pelo email

    // Remover todas as associações existentes (incluindo valores temporais em cascata)
    companyTaxParameterRepository.deleteAllByCompanyId(companyId);

    LocalDateTime now = LocalDateTime.now();

    // Criar associações para parâmetros GLOBAIS (sem valores temporais)
    for (Long parameterId : globalIds) {
      CompanyTaxParameterEntity association = CompanyTaxParameterEntity.builder()
          .companyId(companyId)
          .taxParameterId(parameterId)
          .createdBy(userId)
          .createdAt(now)
          .build();
      companyTaxParameterRepository.save(association);
      log.debug("Parâmetro global associado: parameterId={}", parameterId);
    }

    // Criar associações para parâmetros PERIÓDICOS (com valores temporais)
    if (request.periodicParameters() != null) {
      for (PeriodicParameterRequest periodicParam : request.periodicParameters()) {
        Long parameterId = periodicParam.taxParameterId();

        // Criar associação empresa-parâmetro
        CompanyTaxParameterEntity association = CompanyTaxParameterEntity.builder()
            .companyId(companyId)
            .taxParameterId(parameterId)
            .createdBy(userId)
            .createdAt(now)
            .build();
        CompanyTaxParameterEntity savedAssociation =
            companyTaxParameterRepository.save(association);

        log.debug("Parâmetro periódico associado: parameterId={}", parameterId);

        // Criar valores temporais
        EmpresaParametrosTributariosEntity empresaParametro =
            findEmpresaParametroEntity(savedAssociation.getId());

        for (TemporalValueInput temporalInput : periodicParam.temporalValues()) {
          // Validar constraint XOR
          validatePeriodicityConstraint(temporalInput.mes(), temporalInput.trimestre());

          ValorParametroTemporalEntity temporalValue = ValorParametroTemporalEntity.builder()
              .empresaParametrosTributarios(empresaParametro)
              .ano(temporalInput.ano())
              .mes(temporalInput.mes())
              .trimestre(temporalInput.trimestre())
              .status(Status.ACTIVE)
              .build();

          valorParametroTemporalRepository.save(temporalValue);
          log.debug("Valor temporal criado: parameterId={}, ano={}, mes={}, trimestre={}",
              parameterId, temporalInput.ano(), temporalInput.mes(), temporalInput.trimestre());
        }
      }
    }

    // Buscar parâmetros associados para retorno
    List<TaxParameterSummary> taxParameters = listTaxParameters(companyId);

    log.info("Parâmetros tributários atualizados com sucesso para empresa ID: {}", companyId);

    return new UpdateTaxParametersResponse(
        true,
        "Parâmetros tributários atualizados com sucesso",
        taxParameters);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TaxParameterSummary> listTaxParameters(Long companyId) {
    log.info("Listando parâmetros tributários para empresa ID: {}", companyId);

    // Validar que empresa existe
    if (!companyRepository.existsById(companyId)) {
      throw new EntityNotFoundException("Empresa não encontrada com ID: " + companyId);
    }

    // Buscar associações
    List<CompanyTaxParameterEntity> associations =
        companyTaxParameterRepository.findByCompanyId(companyId);

    if (associations.isEmpty()) {
      return Collections.emptyList();
    }

    // Buscar os parâmetros tributários associados
    List<Long> parameterIds = associations.stream()
        .map(CompanyTaxParameterEntity::getTaxParameterId)
        .collect(Collectors.toList());

    List<TaxParameterEntity> parameters = taxParameterRepository.findAllById(parameterIds);

    // Verificar quais parâmetros têm valores temporais
    // Buscar todas as associações com valores temporais
    java.util.Set<Long> associationsWithTemporalValues = associations.stream()
        .map(CompanyTaxParameterEntity::getId)
        .filter(assocId -> {
          List<ValorParametroTemporalEntity> valores =
              valorParametroTemporalRepository.findByEmpresaParametrosTributariosId(assocId);
          return !valores.isEmpty();
        })
        .collect(Collectors.toSet());

    // Mapear para TaxParameterSummary com informações de auditoria
    return associations.stream()
        .map(assoc -> {
          TaxParameterEntity param = parameters.stream()
              .filter(p -> p.getId().equals(assoc.getTaxParameterId()))
              .findFirst()
              .orElse(null);

          if (param == null || param.getStatus() != Status.ACTIVE) {
            return null;
          }

          // TODO: Buscar email do usuário pelo ID
          String createdByEmail = "admin@example.com";

          boolean hasTemporalValues = associationsWithTemporalValues.contains(assoc.getId());

          return new TaxParameterSummary(
              param.getId(),
              param.getCodigo(),
              param.getTipo(),
              param.getDescricao(),
              assoc.getCreatedAt(),
              createdByEmail,
              hasTemporalValues);
        })
        .filter(summary -> summary != null)
        .collect(Collectors.toList());
  }

  /**
   * Obtém o email do usuário autenticado do contexto de segurança.
   *
   * @return email do usuário
   */
  private String getCurrentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return "SYSTEM";
    }
    return authentication.getName();
  }

  /**
   * Valida se os 3 parâmetros tributários obrigatórios existem e são dos tipos corretos.
   *
   * @param cnaeId ID do parâmetro CNAE
   * @param qualificacaoPjId ID do parâmetro Qualificação PJ
   * @param naturezaJuridicaId ID do parâmetro Natureza Jurídica
   * @throws IllegalArgumentException se algum parâmetro não existe ou não é do tipo correto
   */
  private void validateRequiredTaxParameters(
      Long cnaeId,
      Long qualificacaoPjId,
      Long naturezaJuridicaId) {

    // Validar CNAE
    TaxParameterEntity cnae = taxParameterRepository.findById(cnaeId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Parâmetro CNAE não encontrado com ID: " + cnaeId));

    if (!"CNAE".equals(cnae.getTipo())) {
      throw new IllegalArgumentException(
          "Parâmetro com ID " + cnaeId + " não é do tipo CNAE (tipo atual: "
              + cnae.getTipo() + ")");
    }

    // Validar Qualificação PJ
    TaxParameterEntity qualificacaoPj = taxParameterRepository.findById(qualificacaoPjId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Parâmetro Qualificação PJ não encontrado com ID: " + qualificacaoPjId));

    if (!"QUALIFICACAO_PJ".equals(qualificacaoPj.getTipo())) {
      throw new IllegalArgumentException(
          "Parâmetro com ID " + qualificacaoPjId
              + " não é do tipo QUALIFICACAO_PJ (tipo atual: "
              + qualificacaoPj.getTipo() + ")");
    }

    // Validar Natureza Jurídica
    TaxParameterEntity naturezaJuridica = taxParameterRepository.findById(naturezaJuridicaId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Parâmetro Natureza Jurídica não encontrado com ID: " + naturezaJuridicaId));

    if (!"NATUREZA_JURIDICA".equals(naturezaJuridica.getTipo())) {
      throw new IllegalArgumentException(
          "Parâmetro com ID " + naturezaJuridicaId
              + " não é do tipo NATUREZA_JURIDICA (tipo atual: "
              + naturezaJuridica.getTipo() + ")");
    }
  }

  /**
   * Cria uma associação entre empresa e parâmetro tributário com auditoria.
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   */
  private void createTaxParameterAssociation(Long companyId, Long taxParameterId) {
    // Obter userId do SecurityContext para auditoria
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Long userId = 1L; // TODO: Buscar ID do usuário pelo email

    CompanyTaxParameterEntity association = CompanyTaxParameterEntity.builder()
        .companyId(companyId)
        .taxParameterId(taxParameterId)
        .createdBy(userId)
        .createdAt(LocalDateTime.now())
        .build();
    companyTaxParameterRepository.save(association);
  }

  /**
   * Encontra um parâmetro tributário pelo tipo na lista fornecida.
   *
   * @param parameters lista de parâmetros
   * @param associationMap mapa de associações para buscar informações de auditoria
   * @param associationsWithTemporalValues set de IDs de associações com valores temporais
   * @param taxParamToAssocId mapa de taxParameterId para associationId
   * @param tipo tipo do parâmetro (ex: "CNAE", "QUALIFICACAO_PJ", "NATUREZA_JURIDICA")
   * @return TaxParameterSummary ou null se não encontrado
   */
  private TaxParameterSummary findParameterByType(
      List<TaxParameterEntity> parameters,
      java.util.Map<Long, CompanyTaxParameterEntity> associationMap,
      java.util.Set<Long> associationsWithTemporalValues,
      java.util.Map<Long, Long> taxParamToAssocId,
      String tipo) {

    return parameters.stream()
        .filter(p -> tipo.equals(p.getTipo()))
        .findFirst()
        .map(p -> {
          CompanyTaxParameterEntity assoc = associationMap.get(p.getId());
          String createdByEmail = "admin@example.com"; // TODO: buscar email do usuário
          Long assocId = taxParamToAssocId.get(p.getId());
          boolean hasTemporalValues = assocId != null
              && associationsWithTemporalValues.contains(assocId);
          return new TaxParameterSummary(
              p.getId(),
              p.getCodigo(),
              p.getTipo(),
              p.getDescricao(),
              assoc != null ? assoc.getCreatedAt() : null,
              createdByEmail,
              hasTemporalValues);
        })
        .orElse(null);
  }

  // ==================================================================================
  // Temporal Values Use Cases (Story 2.9)
  // ==================================================================================

  @Override
  @Transactional
  public TemporalValueResponse createTemporalValue(
      Long companyId,
      Long taxParameterId,
      CreateTemporalValueRequest request) {

    log.info(
        "Criando valor temporal para empresa ID: {}, parâmetro ID: {}, ano: {}",
        companyId,
        taxParameterId,
        request.ano());

    // Validar que associação empresa-parâmetro existe
    CompanyTaxParameterEntity association = findAssociation(companyId, taxParameterId);

    // Validar constraint XOR: exatamente UM campo preenchido
    validatePeriodicityConstraint(request.mes(), request.trimestre());

    // Buscar a entidade EmpresaParametrosTributarios correspondente
    // (precisamos do ID da entidade de associação com ManyToOne)
    EmpresaParametrosTributariosEntity empresaParametro =
        findEmpresaParametroEntity(association.getId());

    // Criar valor temporal
    ValorParametroTemporalEntity entity = ValorParametroTemporalEntity.builder()
        .empresaParametrosTributarios(empresaParametro)
        .ano(request.ano())
        .mes(request.mes())
        .trimestre(request.trimestre())
        .status(Status.ACTIVE)
        .build();

    ValorParametroTemporalEntity saved = valorParametroTemporalRepository.save(entity);

    log.info("Valor temporal criado com sucesso. ID: {}", saved.getId());

    return toTemporalValueResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemporalValueResponse> listTemporalValues(
      Long companyId,
      Long taxParameterId,
      Integer ano) {

    log.info(
        "Listando valores temporais para empresa ID: {}, parâmetro ID: {}, ano: {}",
        companyId,
        taxParameterId,
        ano);

    // Validar que associação empresa-parâmetro existe
    CompanyTaxParameterEntity association = findAssociation(companyId, taxParameterId);

    // Buscar a entidade EmpresaParametrosTributarios correspondente
    EmpresaParametrosTributariosEntity empresaParametro =
        findEmpresaParametroEntity(association.getId());

    // Buscar valores temporais
    List<ValorParametroTemporalEntity> entities;
    if (ano != null) {
      entities = valorParametroTemporalRepository
          .findByEmpresaParametrosTributariosIdAndAno(empresaParametro.getId(), ano);
    } else {
      entities = valorParametroTemporalRepository
          .findByEmpresaParametrosTributariosId(empresaParametro.getId());
    }

    return entities.stream()
        .map(this::toTemporalValueResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void deleteTemporalValue(Long companyId, Long taxParameterId, Long valorId) {
    log.info(
        "Deletando valor temporal ID: {} (empresa: {}, parâmetro: {})",
        valorId,
        companyId,
        taxParameterId);

    // Validar que associação empresa-parâmetro existe
    findAssociation(companyId, taxParameterId);

    // Buscar e deletar valor temporal
    ValorParametroTemporalEntity entity = valorParametroTemporalRepository
        .findById(valorId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Valor temporal não encontrado com ID: " + valorId));

    valorParametroTemporalRepository.delete(entity);

    log.info("Valor temporal deletado com sucesso. ID: {}", valorId);
  }

  @Override
  @Transactional(readOnly = true)
  public TimelineResponse getTimeline(Long companyId, Integer ano) {
    log.info("Obtendo timeline para empresa ID: {}, ano: {}", companyId, ano);

    // Validar que empresa existe
    companyRepository.findById(companyId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Empresa não encontrada com ID: " + companyId));

    // Buscar todos os valores temporais do ano com parâmetros carregados
    List<ValorParametroTemporalEntity> valores =
        valorParametroTemporalRepository.findByCompanyIdAndAnoWithParameters(companyId, ano);

    // Agrupar por tipo de parâmetro
    java.util.Map<String, List<TimelineResponse.ParameterTimeline>> timelineByType =
        new java.util.LinkedHashMap<>();

    // Agrupar valores por parâmetro (codigo)
    java.util.Map<String, List<ValorParametroTemporalEntity>> valuesByParameter =
        valores.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                v -> v.getEmpresaParametrosTributarios().getParametroTributario().getCodigo(),
                java.util.LinkedHashMap::new,
                java.util.stream.Collectors.toList()));

    // Para cada parâmetro, criar ParameterTimeline
    valuesByParameter.forEach((codigo, valorList) -> {
      if (!valorList.isEmpty()) {
        TaxParameterEntity param =
            valorList.get(0).getEmpresaParametrosTributarios().getParametroTributario();
        String tipo = param.getTipo();
        String descricao = param.getDescricao();

        // Coletar períodos formatados
        List<String> periodos = valorList.stream()
            .map(ValorParametroTemporalEntity::formatPeriodo)
            .collect(java.util.stream.Collectors.toList());

        // Criar timeline entry
        TimelineResponse.ParameterTimeline paramTimeline =
            new TimelineResponse.ParameterTimeline(codigo, descricao, periodos);

        // Adicionar ao grupo por tipo
        timelineByType.computeIfAbsent(tipo, k -> new java.util.ArrayList<>())
            .add(paramTimeline);
      }
    });

    log.info("Timeline construída: {} tipos, {} parâmetros",
        timelineByType.size(),
        valuesByParameter.size());

    return new TimelineResponse(ano, timelineByType);
  }

  /**
   * Helper: Valida constraint XOR (exatamente UM campo preenchido).
   */
  private void validatePeriodicityConstraint(Integer mes, Integer trimestre) {
    boolean hasMonth = mes != null;
    boolean hasQuarter = trimestre != null;

    if (hasMonth == hasQuarter) { // Ambos null ou ambos preenchidos
      throw new IllegalArgumentException("Deve ter mes OU trimestre, nunca ambos ou nenhum");
    }
  }

  /**
   * Helper: Busca associação empresa-parâmetro.
   */
  private CompanyTaxParameterEntity findAssociation(Long companyId, Long taxParameterId) {
    return companyTaxParameterRepository
        .findByCompanyIdAndTaxParameterId(companyId, taxParameterId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Associação não encontrada entre empresa " + companyId
                + " e parâmetro " + taxParameterId));
  }

  /**
   * Helper: Busca entidade EmpresaParametrosTributarios pelo ID.
   *
   * <p>NOTA: Esta é uma solução temporária. Idealmente, deveríamos ter apenas uma entidade para
   * a tabela tb_empresa_parametros_tributarios.
   */
  private EmpresaParametrosTributariosEntity findEmpresaParametroEntity(Long associationId) {
    // Por enquanto, criar uma entidade sem buscar do banco
    // já que temos os IDs necessários
    return EmpresaParametrosTributariosEntity.builder()
        .id(associationId)
        .build();
  }

  /**
   * Helper: Converte entidade para DTO response.
   */
  private TemporalValueResponse toTemporalValueResponse(ValorParametroTemporalEntity entity) {
    return new TemporalValueResponse(
        entity.getId(),
        entity.getAno(),
        entity.getMes(),
        entity.getTrimestre(),
        entity.formatPeriodo());
  }
}
