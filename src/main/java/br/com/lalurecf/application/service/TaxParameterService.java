package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.taxparameter.CreateTaxParameterUseCase;
import br.com.lalurecf.application.port.in.taxparameter.GetTaxParameterTypesUseCase;
import br.com.lalurecf.application.port.in.taxparameter.GetTaxParameterUseCase;
import br.com.lalurecf.application.port.in.taxparameter.ListTaxParametersUseCase;
import br.com.lalurecf.application.port.in.taxparameter.ToggleTaxParameterStatusUseCase;
import br.com.lalurecf.application.port.in.taxparameter.UpdateTaxParameterUseCase;
import br.com.lalurecf.application.port.out.TaxParameterRepositoryPort;
import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.TaxParameter;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.dto.FilterDropdown;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.taxparameter.CreateTaxParameterRequest;
import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterResponse;
import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterTypeGroup;
import br.com.lalurecf.infrastructure.dto.taxparameter.UpdateTaxParameterRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementando use cases de TaxParameter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxParameterService implements
    CreateTaxParameterUseCase,
    ListTaxParametersUseCase,
    GetTaxParameterUseCase,
    UpdateTaxParameterUseCase,
    ToggleTaxParameterStatusUseCase,
    GetTaxParameterTypesUseCase {

  private final TaxParameterRepositoryPort taxParameterRepository;

  @Override
  @Transactional
  public TaxParameterResponse create(CreateTaxParameterRequest request) {
    log.info("Criando parâmetro tributário com código: {}", request.code());

    // Verificar se código já existe
    taxParameterRepository.findByCode(request.code())
        .ifPresent(existing -> {
          log.warn("Tentativa de criar parâmetro com código duplicado: {}", request.code());
          throw new IllegalArgumentException(
              "Já existe um parâmetro tributário com o código: " + request.code());
        });

    // Criar domain model
    TaxParameter taxParameter = TaxParameter.builder()
        .code(request.code())
        .type(request.type())
        .description(request.description())
        .nature(request.nature())
        .status(Status.ACTIVE)
        .build();

    TaxParameter saved = taxParameterRepository.save(taxParameter);
    log.info("Parâmetro tributário criado com sucesso. ID: {}, Código: {}",
        saved.getId(), saved.getCode());

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TaxParameterResponse> list(
      String type,
      ParameterNature nature,
      String search,
      boolean includeInactive,
      Pageable pageable) {

    log.info("Listando parâmetros tributários. Type: {}, Nature: {}, Search: {}",
        type, nature, search);

    Specification<TaxParameterEntity> spec =
        buildSpecification(type, nature, search, includeInactive);
    Page<TaxParameter> page = taxParameterRepository.findAll(spec, pageable);

    return page.map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public TaxParameterResponse getById(Long id) {
    log.info("Buscando parâmetro tributário por ID: {}", id);

    TaxParameter taxParameter = taxParameterRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Parâmetro tributário não encontrado. ID: {}", id);
          return new EntityNotFoundException("Parâmetro tributário não encontrado com ID: " + id);
        });

    return toResponse(taxParameter);
  }

  @Override
  @Transactional
  public TaxParameterResponse update(Long id, UpdateTaxParameterRequest request) {
    log.info("Atualizando parâmetro tributário ID: {}", id);

    TaxParameter taxParameter = taxParameterRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Tentativa de atualizar parâmetro inexistente. ID: {}", id);
          return new EntityNotFoundException("Parâmetro tributário não encontrado com ID: " + id);
        });

    // Verificar se o novo código já existe em outro registro
    if (!taxParameter.getCode().equals(request.code())) {
      taxParameterRepository.findByCode(request.code())
          .ifPresent(existing -> {
            log.warn("Tentativa de alterar código para um já existente: {}", request.code());
            throw new IllegalArgumentException(
                "Já existe um parâmetro tributário com o código: " + request.code());
          });
    }

    // Atualizar campos
    TaxParameter updated = taxParameter.toBuilder()
        .code(request.code())
        .type(request.type())
        .description(request.description())
        .nature(request.nature())
        .build();

    TaxParameter saved = taxParameterRepository.save(updated);
    log.info("Parâmetro tributário atualizado com sucesso. ID: {}", id);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public ToggleStatusResponse toggleStatus(Long id, Status newStatus) {
    log.info("Alterando status do parâmetro tributário ID: {} para {}", id, newStatus);

    TaxParameter taxParameter = taxParameterRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Tentativa de alterar status de parâmetro inexistente. ID: {}", id);
          return new EntityNotFoundException("Parâmetro tributário não encontrado com ID: " + id);
        });

    Status oldStatus = taxParameter.getStatus();
    TaxParameter updated = taxParameter.toBuilder()
        .status(newStatus)
        .build();
    taxParameterRepository.save(updated);

    log.info("Status do parâmetro ID: {} alterado de {} para {}", id, oldStatus, newStatus);

    return new ToggleStatusResponse(
        true,
        "Status alterado com sucesso de " + oldStatus + " para " + newStatus,
        br.com.lalurecf.domain.model.CompanyStatus.fromStatus(newStatus)
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getTypes(String search) {
    log.info("Buscando tipos de parâmetros tributários. Search: {}", search);

    List<String> types = taxParameterRepository.findDistinctTypes();

    // Aplicar filtro de busca se fornecido
    if (search != null && !search.isBlank()) {
      String searchLower = search.toLowerCase();
      types = types.stream()
          .filter(type -> type.toLowerCase().contains(searchLower))
          .limit(100)
          .toList();
    } else {
      types = types.stream().limit(100).toList();
    }

    log.info("Encontrados {} tipos de parâmetros tributários", types.size());
    return types;
  }

  @Override
  public HashMap<String, TaxParameterTypeGroup> getTaxParametersForCompanyCreation() {

    List<TaxParameter> allParameters = taxParameterRepository.findTaxParametersOrderByType();

    HashMap<String, TaxParameterTypeGroup> map = new HashMap<>();
    String currentType = null;
    ParameterNature currentNature = null;
    List<FilterDropdown> parameterList = new ArrayList<>();

    for (TaxParameter parameter : allParameters) {
      if (!Objects.equals(parameter.getType(), currentType) && currentType != null) {
        map.put(currentType, new TaxParameterTypeGroup(currentNature, parameterList));
        parameterList = new ArrayList<>();
      }
      currentType = parameter.getType();
      currentNature = parameter.getNature();
      parameterList.add(new FilterDropdown(parameter.getId(), parameter.getDescription()));
    }

    if (currentType != null) {
      map.put(currentType, new TaxParameterTypeGroup(currentNature, parameterList));
    }

    return map;
  }

  /**
   * Constrói Specification para filtros dinâmicos.
   */
  private Specification<TaxParameterEntity> buildSpecification(
      String type,
      ParameterNature nature,
      String search,
      boolean includeInactive) {

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Filtro por tipo (categoria)
      if (type != null && !type.isBlank()) {
        predicates.add(criteriaBuilder.equal(root.get("tipo"), type));
      }

      // Filtro por natureza
      if (nature != null) {
        predicates.add(criteriaBuilder.equal(root.get("natureza"), nature));
      }

      // Busca em código e descrição
      if (search != null && !search.isBlank()) {
        String searchPattern = "%" + search.toLowerCase() + "%";
        Predicate codePredicate = criteriaBuilder.like(
            criteriaBuilder.lower(root.get("codigo")), searchPattern);
        Predicate descriptionPredicate = criteriaBuilder.like(
            criteriaBuilder.lower(root.get("descricao")), searchPattern);
        predicates.add(criteriaBuilder.or(codePredicate, descriptionPredicate));
      }

      // Filtro por status
      if (!includeInactive) {
        predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Converte domain model para DTO de resposta.
   */
  private TaxParameterResponse toResponse(TaxParameter taxParameter) {
    return new TaxParameterResponse(
        taxParameter.getId(),
        taxParameter.getCode(),
        taxParameter.getType(),
        taxParameter.getDescription(),
        taxParameter.getNature(),
        taxParameter.getStatus(),
        taxParameter.getCreatedAt(),
        taxParameter.getUpdatedAt()
    );
  }
}
