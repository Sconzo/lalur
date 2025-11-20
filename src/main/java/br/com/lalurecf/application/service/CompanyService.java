package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.company.CreateCompanyUseCase;
import br.com.lalurecf.application.port.in.company.GetCompanyUseCase;
import br.com.lalurecf.application.port.in.company.ListCompaniesUseCase;
import br.com.lalurecf.application.port.in.company.ToggleCompanyStatusUseCase;
import br.com.lalurecf.application.port.in.company.UpdateCompanyUseCase;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.CompanyStatus;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.dto.company.CompanyDetailResponse;
import br.com.lalurecf.infrastructure.dto.company.CompanyResponse;
import br.com.lalurecf.infrastructure.dto.company.CreateCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.company.UpdateCompanyRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    ToggleCompanyStatusUseCase {

  private final CompanyJpaRepository companyRepository;

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
              "Já existe uma empresa ativa com o CNPJ: " + cnpj.getFormattedValue());
        });

    // Criar entidade
    CompanyEntity entity = new CompanyEntity();
    entity.setCnpj(cnpj.getValue());
    entity.setRazaoSocial(request.razaoSocial());
    entity.setCnae(request.cnae());
    entity.setQualificacaoPessoaJuridica(request.qualificacaoPessoaJuridica());
    entity.setNaturezaJuridica(request.naturezaJuridica());
    entity.setPeriodoContabil(request.periodoContabil());
    entity.setStatus(Status.ACTIVE);

    CompanyEntity saved = companyRepository.save(entity);
    log.info("Empresa criada com sucesso. ID: {}, CNPJ: {}", saved.getId(), cnpj.getValue());

    return toDetailResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CompanyResponse> list(
      String globalSearch,
      String cnpjFilter,
      String razaoSocialFilter,
      boolean includeInactive,
      Pageable pageable) {

    log.debug("Listando empresas - globalSearch: {}, cnpjFilter: {}, razaoSocialFilter: {}, "
            + "includeInactive: {}",
        globalSearch, cnpjFilter, razaoSocialFilter, includeInactive);

    Specification<CompanyEntity> spec = buildSpecification(
        globalSearch, cnpjFilter, razaoSocialFilter, includeInactive);

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

    // Atualizar campos (CNPJ é imutável)
    entity.setRazaoSocial(request.razaoSocial());
    entity.setCnae(request.cnae());
    entity.setQualificacaoPessoaJuridica(request.qualificacaoPessoaJuridica());
    entity.setNaturezaJuridica(request.naturezaJuridica());
    entity.setPeriodoContabil(request.periodoContabil());

    CompanyEntity updated = companyRepository.save(entity);
    log.info("Empresa atualizada com sucesso. ID: {}", id);

    return toDetailResponse(updated);
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
      String globalSearch,
      String cnpjFilter,
      String razaoSocialFilter,
      boolean includeInactive) {

    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Filtro de status (padrão: apenas ACTIVE)
      if (!includeInactive) {
        predicates.add(cb.equal(root.get("status"), Status.ACTIVE));
      }

      // Filtro global (busca em todos os campos)
      if (globalSearch != null && !globalSearch.isBlank()) {
        String search = "%" + globalSearch.toLowerCase() + "%";
        Predicate globalPredicate = cb.or(
            cb.like(cb.lower(root.get("cnpj")), search),
            cb.like(cb.lower(root.get("razaoSocial")), search),
            cb.like(cb.lower(root.get("cnae")), search),
            cb.like(cb.lower(root.get("qualificacaoPessoaJuridica")), search),
            cb.like(cb.lower(root.get("naturezaJuridica")), search)
        );
        predicates.add(globalPredicate);
      }

      // Filtro específico por CNPJ
      if (cnpjFilter != null && !cnpjFilter.isBlank()) {
        String cnpjSearch = "%" + cnpjFilter.replaceAll("[^0-9]", "") + "%";
        predicates.add(cb.like(root.get("cnpj"), cnpjSearch));
      }

      // Filtro específico por Razão Social
      if (razaoSocialFilter != null && !razaoSocialFilter.isBlank()) {
        String razaoSearch = "%" + razaoSocialFilter.toLowerCase() + "%";
        predicates.add(cb.like(cb.lower(root.get("razaoSocial")), razaoSearch));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Converte Entity para CompanyResponse (listagem).
   */
  private CompanyResponse toResponse(CompanyEntity entity) {
    return new CompanyResponse(
        entity.getId(),
        formatCnpj(entity.getCnpj()),
        CompanyStatus.fromStatus(entity.getStatus()),
        entity.getRazaoSocial(),
        entity.getCnae(),
        entity.getQualificacaoPessoaJuridica(),
        entity.getNaturezaJuridica()
    );
  }

  /**
   * Converte Entity para CompanyDetailResponse (detalhes).
   */
  private CompanyDetailResponse toDetailResponse(CompanyEntity entity) {
    return new CompanyDetailResponse(
        entity.getId(),
        formatCnpj(entity.getCnpj()),
        CompanyStatus.fromStatus(entity.getStatus()),
        entity.getRazaoSocial(),
        entity.getCnae(),
        entity.getQualificacaoPessoaJuridica(),
        entity.getNaturezaJuridica(),
        entity.getPeriodoContabil(),
        Collections.emptyList(), // TODO: Implementar quando TaxParameter estiver pronto
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
}
