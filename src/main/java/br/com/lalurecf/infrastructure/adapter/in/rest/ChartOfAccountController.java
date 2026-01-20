package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.chartofaccount.CreateChartOfAccountUseCase;
import br.com.lalurecf.application.port.in.chartofaccount.GetChartOfAccountUseCase;
import br.com.lalurecf.application.port.in.chartofaccount.ImportChartOfAccountUseCase;
import br.com.lalurecf.application.port.in.chartofaccount.ListChartOfAccountsUseCase;
import br.com.lalurecf.application.port.in.chartofaccount.ToggleChartOfAccountStatusUseCase;
import br.com.lalurecf.application.port.in.chartofaccount.UpdateChartOfAccountUseCase;
import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ChartOfAccountResponse;
import br.com.lalurecf.infrastructure.dto.chartofaccount.CreateChartOfAccountRequest;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ImportChartOfAccountResponse;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.chartofaccount.UpdateChartOfAccountRequest;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller para gerenciamento de Plano de Contas (ChartOfAccount).
 *
 * <p>Endpoints para CRUD de contas contábeis com validações ECF e vinculação a Conta Referencial
 * RFB.
 *
 * <p>Todos endpoints requerem autenticação como CONTADOR e header X-Company-Id.
 */
@RestController
@RequestMapping("/api/v1/chart-of-accounts")
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountController {

  private final CreateChartOfAccountUseCase createChartOfAccountUseCase;
  private final ListChartOfAccountsUseCase listChartOfAccountsUseCase;
  private final GetChartOfAccountUseCase getChartOfAccountUseCase;
  private final UpdateChartOfAccountUseCase updateChartOfAccountUseCase;
  private final ToggleChartOfAccountStatusUseCase toggleChartOfAccountStatusUseCase;
  private final ImportChartOfAccountUseCase importChartOfAccountUseCase;

  /**
   * Cria uma nova conta contábil.
   *
   * @param request dados da conta a criar
   * @return conta criada
   */
  @PostMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ChartOfAccountResponse> create(
      @Valid @RequestBody CreateChartOfAccountRequest request) {
    log.info("POST /api/v1/chart-of-accounts - Creating chart of account");
    ChartOfAccountResponse response = createChartOfAccountUseCase.execute(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Importa plano de contas via arquivo CSV/TXT.
   *
   * <p>Formato esperado:
   * code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
   *
   * <p>Separador: ; ou , (detectado automaticamente)
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>contaReferencialCodigo deve existir e estar ACTIVE
   *   <li>nivel deve estar entre 1 e 5
   *   <li>Combinação (company + code + fiscalYear) deve ser única
   * </ul>
   *
   * @param file arquivo CSV/TXT (max 10MB)
   * @param fiscalYear ano fiscal das contas (obrigatório)
   * @param dryRun se true, apenas retorna preview sem persistir (default: false)
   * @return relatório da importação
   */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ImportChartOfAccountResponse> importChartOfAccounts(
      @RequestParam("file") MultipartFile file,
      @RequestParam("fiscalYear") Integer fiscalYear,
      @RequestParam(value = "dryRun", required = false, defaultValue = "false") boolean dryRun) {

    log.info(
        "POST /api/v1/chart-of-accounts/import - fiscalYear: {}, dryRun: {}, file: {}",
        fiscalYear,
        dryRun,
        file.getOriginalFilename());

    // Obter empresa do contexto
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    // Validar fiscal year
    if (fiscalYear == null) {
      throw new IllegalArgumentException("Fiscal year is required");
    }

    // Executar importação
    ImportChartOfAccountResponse response =
        importChartOfAccountUseCase.importChartOfAccounts(file, companyId, fiscalYear, dryRun);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  /**
   * Lista contas contábeis com filtros e paginação.
   *
   * @param fiscalYear filtro por ano fiscal (opcional)
   * @param accountType filtro por tipo de conta (opcional)
   * @param classe filtro por classe contábil (opcional)
   * @param natureza filtro por natureza (opcional)
   * @param search busca em code e name (opcional)
   * @param includeInactive incluir contas inativas (default: false)
   * @param pageable configuração de paginação
   * @return página de contas
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<Page<ChartOfAccountResponse>> list(
      @RequestParam(required = false) Integer fiscalYear,
      @RequestParam(required = false) AccountType accountType,
      @RequestParam(required = false) ClasseContabil classe,
      @RequestParam(required = false) NaturezaConta natureza,
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "false") Boolean includeInactive,
      @PageableDefault(size = 100, sort = "code", direction = Sort.Direction.ASC)
          Pageable pageable) {
    log.info("GET /api/v1/chart-of-accounts - Listing chart of accounts");
    Page<ChartOfAccountResponse> response =
        listChartOfAccountsUseCase.execute(
            fiscalYear, accountType, classe, natureza, search, includeInactive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Busca conta contábil por ID.
   *
   * @param id ID da conta
   * @return conta encontrada
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ChartOfAccountResponse> getById(@PathVariable Long id) {
    log.info("GET /api/v1/chart-of-accounts/{} - Getting chart of account", id);
    ChartOfAccountResponse response = getChartOfAccountUseCase.execute(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza conta contábil existente.
   *
   * <p>Não permite editar code e fiscalYear (campos imutáveis).
   *
   * @param id ID da conta a atualizar
   * @param request novos dados da conta
   * @return conta atualizada
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ChartOfAccountResponse> update(
      @PathVariable Long id, @Valid @RequestBody UpdateChartOfAccountRequest request) {
    log.info("PUT /api/v1/chart-of-accounts/{} - Updating chart of account", id);
    ChartOfAccountResponse response = updateChartOfAccountUseCase.execute(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Alterna status de conta contábil (ACTIVE/INACTIVE).
   *
   * @param id ID da conta
   * @param request novo status desejado
   * @return confirmação da operação
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request) {
    log.info("PATCH /api/v1/chart-of-accounts/{}/status - Toggling status", id);
    ToggleStatusResponse response = toggleChartOfAccountStatusUseCase.execute(id, request);
    return ResponseEntity.ok(response);
  }
}
