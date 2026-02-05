package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.ExportLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.ImportLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.CreateLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.GetLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.ListLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.ToggleLancamentoContabilStatusUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.UpdateLancamentoContabilUseCase;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.CreateLancamentoContabilRequest;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.LancamentoContabilResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.UpdateLancamentoContabilRequest;
import br.com.lalurecf.infrastructure.dto.mapper.LancamentoContabilDtoMapper;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
 * REST Controller para gerenciamento de Lançamentos Contábeis.
 *
 * <p>Endpoints para importação e CRUD de lançamentos contábeis com partidas dobradas.
 *
 * <p>Todos endpoints requerem autenticação como CONTADOR e header X-Company-Id.
 */
@RestController
@RequestMapping("/lancamento-contabil")
@RequiredArgsConstructor
@Slf4j
public class LancamentoContabilController {

  private final ImportLancamentoContabilUseCase importLancamentoContabilUseCase;
  private final ExportLancamentoContabilUseCase exportLancamentoContabilUseCase;
  private final CreateLancamentoContabilUseCase createLancamentoContabilUseCase;
  private final ListLancamentoContabilUseCase listLancamentoContabilUseCase;
  private final GetLancamentoContabilUseCase getLancamentoContabilUseCase;
  private final UpdateLancamentoContabilUseCase updateLancamentoContabilUseCase;
  private final ToggleLancamentoContabilStatusUseCase toggleLancamentoContabilStatusUseCase;
  private final LancamentoContabilDtoMapper lancamentoContabilDtoMapper;

  /**
   * Importa lançamentos contábeis via arquivo CSV/TXT.
   *
   * <p>Formato esperado: contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento
   *
   * <p>Separador: auto-detectado (; ou ,)
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Contas devem existir no plano de contas da empresa/ano
   *   <li>Débito != Crédito
   *   <li>Data >= Período Contábil
   *   <li>Valor > 0
   * </ul>
   *
   * @param file arquivo CSV/TXT (max 50MB)
   * @param fiscalYear ano fiscal dos lançamentos (obrigatório)
   * @param dryRun se true, apenas retorna preview sem persistir (default: false)
   * @return relatório da importação
   */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ImportLancamentoContabilResponse> importLancamentos(
      @RequestParam("file") MultipartFile file,
      @RequestParam("fiscalYear") Integer fiscalYear,
      @RequestParam(value = "dryRun", required = false, defaultValue = "false") boolean dryRun) {

    log.info(
        "POST /api/v1/lancamento-contabil/import - fiscalYear: {}, dryRun: {}, file: {}",
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
    ImportLancamentoContabilResponse response =
        importLancamentoContabilUseCase.importLancamentos(file, companyId, fiscalYear, dryRun);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  /**
   * Exporta lançamentos contábeis para arquivo CSV.
   *
   * <p>Formato gerado: contaDebitoCode;contaDebitoName;contaCreditoCode;contaCreditoName;
   * data;valor;historico;numeroDocumento
   *
   * <p>Separador: ; (ponto e vírgula)
   *
   * <p>Ordenação: data ASC
   *
   * @param fiscalYear ano fiscal dos lançamentos (obrigatório)
   * @param dataInicio data inicial do filtro (opcional)
   * @param dataFim data final do filtro (opcional)
   * @return arquivo CSV para download
   */
  @GetMapping("/export")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> exportLancamentos(
      @RequestParam("fiscalYear") Integer fiscalYear,
      @RequestParam(value = "dataInicio", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(value = "dataFim", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim) {

    log.info(
        "GET /api/v1/lancamento-contabil/export - fiscalYear: {}, dataInicio: {}, dataFim: {}",
        fiscalYear,
        dataInicio,
        dataFim);

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

    // Executar exportação
    String csvContent =
        exportLancamentoContabilUseCase.exportLancamentos(
            companyId, fiscalYear, dataInicio, dataFim);

    // Preparar response com arquivo CSV
    String filename =
        String.format("lancamentos-contabeis-%d-%d.csv", companyId, fiscalYear);
    byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(csvBytes.length);

    return ResponseEntity.ok().headers(headers).body(csvBytes);
  }

  /**
   * Cria um novo lançamento contábil manual com partidas dobradas.
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Conta débito != Conta crédito
   *   <li>Data >= Período Contábil da empresa
   *   <li>Valor > 0
   *   <li>Contas devem pertencer à empresa no contexto
   * </ul>
   *
   * @param request dados do lançamento
   * @return lançamento criado
   */
  @PostMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<LancamentoContabilResponse> create(
      @Valid @RequestBody CreateLancamentoContabilRequest request) {

    log.info("POST /api/v1/lancamento-contabil - creating lancamento");

    // Converter DTO para domain
    LancamentoContabil lancamento =
        LancamentoContabil.builder()
            .contaDebitoId(request.getContaDebitoId())
            .contaCreditoId(request.getContaCreditoId())
            .data(request.getData())
            .valor(request.getValor())
            .historico(request.getHistorico())
            .numeroDocumento(request.getNumeroDocumento())
            .fiscalYear(request.getFiscalYear())
            .build();

    // Criar
    LancamentoContabil created = createLancamentoContabilUseCase.create(lancamento);

    // Converter para DTO
    LancamentoContabilResponse response = lancamentoContabilDtoMapper.toResponse(created);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista lançamentos contábeis da empresa com filtros e paginação.
   *
   * <p>Filtros disponíveis:
   *
   * <ul>
   *   <li>contaDebitoId - filtrar por conta de débito
   *   <li>contaCreditoId - filtrar por conta de crédito
   *   <li>data - filtrar por data específica
   *   <li>dataInicio / dataFim - filtrar por range de data
   *   <li>fiscalYear - filtrar por ano fiscal
   *   <li>includeInactive - incluir inativos (default: false)
   * </ul>
   *
   * @param contaDebitoId filtro por conta débito (opcional)
   * @param contaCreditoId filtro por conta crédito (opcional)
   * @param data filtro por data (opcional)
   * @param dataInicio filtro por range - início (opcional)
   * @param dataFim filtro por range - fim (opcional)
   * @param fiscalYear filtro por ano fiscal (opcional)
   * @param includeInactive incluir inativos (opcional)
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<Page<LancamentoContabilResponse>> list(
      @RequestParam(value = "contaDebitoId", required = false) Long contaDebitoId,
      @RequestParam(value = "contaCreditoId", required = false) Long contaCreditoId,
      @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate data,
      @RequestParam(value = "dataInicio", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(value = "dataFim", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim,
      @RequestParam(value = "fiscalYear", required = false) Integer fiscalYear,
      @RequestParam(value = "includeInactive", required = false) Boolean includeInactive,
      @PageableDefault(size = 100, sort = "data", direction = Sort.Direction.DESC)
          Pageable pageable) {

    log.info("GET /api/v1/lancamento-contabil - listing lancamentos");

    // Listar
    Page<LancamentoContabil> lancamentos =
        listLancamentoContabilUseCase.list(
            contaDebitoId,
            contaCreditoId,
            data,
            dataInicio,
            dataFim,
            fiscalYear,
            includeInactive,
            pageable);

    // Converter para DTO
    Page<LancamentoContabilResponse> response =
        lancamentos.map(lancamentoContabilDtoMapper::toResponse);

    return ResponseEntity.ok(response);
  }

  /**
   * Busca lançamento contábil por ID.
   *
   * @param id ID do lançamento
   * @return lançamento encontrado
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<LancamentoContabilResponse> getById(@PathVariable Long id) {

    log.info("GET /api/v1/lancamento-contabil/{} - getting lancamento by id", id);

    // Buscar
    LancamentoContabil lancamento = getLancamentoContabilUseCase.getById(id);

    // Converter para DTO
    LancamentoContabilResponse response = lancamentoContabilDtoMapper.toResponse(lancamento);

    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza lançamento contábil existente.
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Período Contábil: data original >= company.periodoContabil
   *   <li>Período Contábil: nova data >= company.periodoContabil
   *   <li>Conta débito != Conta crédito
   *   <li>Valor > 0
   * </ul>
   *
   * @param id ID do lançamento
   * @param request dados atualizados
   * @return lançamento atualizado
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<LancamentoContabilResponse> update(
      @PathVariable Long id, @Valid @RequestBody UpdateLancamentoContabilRequest request) {

    log.info("PUT /api/v1/lancamento-contabil/{} - updating lancamento", id);

    // Converter DTO para domain
    LancamentoContabil lancamento =
        LancamentoContabil.builder()
            .contaDebitoId(request.getContaDebitoId())
            .contaCreditoId(request.getContaCreditoId())
            .data(request.getData())
            .valor(request.getValor())
            .historico(request.getHistorico())
            .numeroDocumento(request.getNumeroDocumento())
            .build();

    // Atualizar
    LancamentoContabil updated = updateLancamentoContabilUseCase.update(id, lancamento);

    // Converter para DTO
    LancamentoContabilResponse response = lancamentoContabilDtoMapper.toResponse(updated);

    return ResponseEntity.ok(response);
  }

  /**
   * Alterna status do lançamento contábil (ACTIVE ↔ INACTIVE).
   *
   * <p>Validação: data >= company.periodoContabil (não pode alterar lançamentos de período
   * fechado).
   *
   * @param id ID do lançamento
   * @param request requisição de toggle (opcional, pode ser null)
   * @return lançamento com status atualizado
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @RequestBody(required = false) ToggleStatusRequest request) {

    log.info("PATCH /api/v1/lancamento-contabil/{}/status - toggling status", id);

    // Toggle status
    LancamentoContabil updated = toggleLancamentoContabilStatusUseCase.toggleStatus(id);

    // Converter para DTO
    ToggleStatusResponse response =
        ToggleStatusResponse.builder()
            .success(true)
            .newStatus(updated.getStatus())
            .message("Status toggled successfully")
            .build();

    return ResponseEntity.ok(response);
  }
}
