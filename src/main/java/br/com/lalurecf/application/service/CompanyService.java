package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.company.CreateCompanyUseCase;
import br.com.lalurecf.application.port.in.company.GetCompanyUseCase;
import br.com.lalurecf.application.port.in.company.GetPeriodoContabilAuditUseCase;
import br.com.lalurecf.application.port.in.company.ListCompaniesUseCase;
import br.com.lalurecf.application.port.in.company.SelectCompanyUseCase;
import br.com.lalurecf.application.port.in.company.ToggleCompanyStatusUseCase;
import br.com.lalurecf.application.port.in.company.UpdateCompanyUseCase;
import br.com.lalurecf.application.port.in.company.UpdatePeriodoContabilUseCase;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.CompanyStatus;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyTaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PeriodoContabilAuditEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyTaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PeriodoContabilAuditJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.dto.company.CompanyDetailResponse;
import br.com.lalurecf.infrastructure.dto.company.CompanyResponse;
import br.com.lalurecf.infrastructure.dto.company.CreateCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.PeriodoContabilAuditResponse;
import br.com.lalurecf.infrastructure.dto.company.TaxParameterSummary;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.company.UpdateCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilResponse;
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
    GetPeriodoContabilAuditUseCase {

  private final CompanyJpaRepository companyRepository;
  private final TaxParameterJpaRepository taxParameterRepository;
  private final CompanyTaxParameterJpaRepository companyTaxParameterRepository;
  private final PeriodoContabilAuditJpaRepository periodoContabilAuditRepository;

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

    // Encontrar cada parâmetro pelo tipo
    TaxParameterSummary cnae = findParameterByType(parameters, "CNAE");
    TaxParameterSummary qualificacaoPj = findParameterByType(parameters, "QUALIFICACAO_PJ");
    TaxParameterSummary naturezaJuridica = findParameterByType(parameters, "NATUREZA_JURIDICA");

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

    // Encontrar os 3 parâmetros obrigatórios pelo tipo
    TaxParameterSummary cnae = findParameterByType(parameters, "CNAE");
    TaxParameterSummary qualificacaoPj = findParameterByType(parameters, "QUALIFICACAO_PJ");
    TaxParameterSummary naturezaJuridica = findParameterByType(parameters, "NATUREZA_JURIDICA");

    // Encontrar outros parâmetros (que não são os 3 obrigatórios)
    List<TaxParameterSummary> outrosParametros = parameters.stream()
        .filter(p -> !p.getTipo().equals("CNAE")
            && !p.getTipo().equals("QUALIFICACAO_PJ")
            && !p.getTipo().equals("NATUREZA_JURIDICA"))
        .map(p -> new TaxParameterSummary(p.getId(), p.getCodigo(), p.getDescricao()))
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
   * Cria uma associação entre empresa e parâmetro tributário.
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   */
  private void createTaxParameterAssociation(Long companyId, Long taxParameterId) {
    CompanyTaxParameterEntity association = new CompanyTaxParameterEntity();
    association.setCompanyId(companyId);
    association.setTaxParameterId(taxParameterId);
    association.setCreatedAt(java.time.LocalDateTime.now());
    companyTaxParameterRepository.save(association);
  }

  /**
   * Encontra um parâmetro tributário pelo tipo na lista fornecida.
   *
   * @param parameters lista de parâmetros
   * @param tipo tipo do parâmetro (ex: "CNAE", "QUALIFICACAO_PJ", "NATUREZA_JURIDICA")
   * @return TaxParameterSummary ou null se não encontrado
   */
  private TaxParameterSummary findParameterByType(
      List<TaxParameterEntity> parameters,
      String tipo) {

    return parameters.stream()
        .filter(p -> tipo.equals(p.getTipo()))
        .findFirst()
        .map(p -> new TaxParameterSummary(p.getId(), p.getCodigo(), p.getDescricao()))
        .orElse(null);
  }
}
