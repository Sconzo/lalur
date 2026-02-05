package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.contareferencial.CreateContaReferencialUseCase;
import br.com.lalurecf.application.port.in.contareferencial.GetContaReferencialUseCase;
import br.com.lalurecf.application.port.in.contareferencial.ImportContaReferencialUseCase;
import br.com.lalurecf.application.port.in.contareferencial.ListContaReferencialUseCase;
import br.com.lalurecf.application.port.in.contareferencial.ToggleContaReferencialStatusUseCase;
import br.com.lalurecf.application.port.in.contareferencial.UpdateContaReferencialUseCase;
import br.com.lalurecf.infrastructure.dto.contareferencial.ContaReferencialResponse;
import br.com.lalurecf.infrastructure.dto.contareferencial.CreateContaReferencialRequest;
import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse;
import br.com.lalurecf.infrastructure.dto.contareferencial.UpdateContaReferencialRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Controller para gerenciamento de contas referenciais RFB.
 *
 * <p>Endpoints de escrita (POST, PUT, PATCH) protegidos para ADMIN apenas. Endpoints de leitura
 * (GET) acessíveis para ADMIN e CONTADOR.
 */
@RestController
@RequestMapping("/conta-referencial")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Contas Referenciais RFB",
    description = "Gerenciamento de contas referenciais da tabela mestra RFB")
public class ContaReferencialController {

  private final CreateContaReferencialUseCase createContaReferencialUseCase;
  private final ListContaReferencialUseCase listContaReferencialUseCase;
  private final GetContaReferencialUseCase getContaReferencialUseCase;
  private final UpdateContaReferencialUseCase updateContaReferencialUseCase;
  private final ToggleContaReferencialStatusUseCase toggleContaReferencialStatusUseCase;
  private final ImportContaReferencialUseCase importContaReferencialUseCase;

  /**
   * Cria uma nova conta referencial RFB.
   *
   * @param request dados da conta a ser criada
   * @return conta criada
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Criar conta referencial",
      description = "Cria uma nova conta na tabela mestra RFB (ADMIN apenas)")
  public ResponseEntity<ContaReferencialResponse> createContaReferencial(
      @Valid @RequestBody CreateContaReferencialRequest request) {
    ContaReferencialResponse response =
        createContaReferencialUseCase.createContaReferencial(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Importa contas referenciais via arquivo CSV/TXT.
   *
   * <p>Formato esperado: codigoRfb;descricao;anoValidade
   *
   * <p>Separador: ; ou , (detectado automaticamente)
   *
   * <p>Campos:
   *
   * <ul>
   *   <li>codigoRfb: código oficial RFB (obrigatório)
   *   <li>descricao: descrição da conta (obrigatório, max 1000 chars)
   *   <li>anoValidade: ano de validade (opcional, entre 2000 e ano atual + 5)
   * </ul>
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Combinação (codigoRfb + anoValidade) deve ser única
   *   <li>Arquivo máximo: 10MB
   *   <li>Detecta duplicatas no arquivo e no banco
   * </ul>
   *
   * @param file arquivo CSV/TXT
   * @param dryRun se true, apenas retorna preview sem persistir
   * @return relatório detalhado da importação
   */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Importar contas referenciais via CSV",
      description =
          "Importa múltiplas contas referenciais via arquivo CSV/TXT (ADMIN apenas). "
              + "Formato: codigoRfb;descricao;anoValidade")
  public ResponseEntity<ImportContaReferencialResponse> importContasReferenciais(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "dryRun", required = false, defaultValue = "false") boolean dryRun) {

    log.info("Importing contas referenciais (dryRun: {})", dryRun);

    if (file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      throw new IllegalArgumentException("File name cannot be empty");
    }

    if (!originalFilename.toLowerCase().endsWith(".csv")
        && !originalFilename.toLowerCase().endsWith(".txt")) {
      throw new IllegalArgumentException("File must be CSV or TXT format");
    }

    ImportContaReferencialResponse response =
        importContaReferencialUseCase.importContasReferenciais(file, dryRun);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  /**
   * Lista contas referenciais com paginação e filtros.
   *
   * @param search termo de busca para codigoRfb/descricao (opcional)
   * @param anoValidade filtro por ano de validade (opcional)
   * @param includeInactive se deve incluir contas inativas
   * @param pageable configuração de paginação
   * @return página de contas referenciais
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
  @Operation(
      summary = "Listar contas referenciais",
      description = "Lista contas referenciais com paginação e filtros (ADMIN e CONTADOR)")
  public ResponseEntity<Page<ContaReferencialResponse>> listContasReferenciais(
      @RequestParam(required = false) String search,
      @RequestParam(name = "ano_validade", required = false) Integer anoValidade,
      @RequestParam(name = "include_inactive", required = false, defaultValue = "false")
          Boolean includeInactive,
      @PageableDefault(size = 100, sort = "codigoRfb", direction = Sort.Direction.ASC)
          Pageable pageable) {
    Page<ContaReferencialResponse> response =
        listContaReferencialUseCase.listContasReferenciais(
            search, anoValidade, includeInactive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Obtém conta referencial por ID.
   *
   * @param id ID da conta
   * @return dados da conta
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
  @Operation(
      summary = "Obter conta referencial",
      description = "Obtém conta referencial por ID (ADMIN e CONTADOR)")
  public ResponseEntity<ContaReferencialResponse> getContaReferencialById(@PathVariable Long id) {
    ContaReferencialResponse response = getContaReferencialUseCase.getContaReferencialById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza dados de uma conta referencial.
   *
   * @param id ID da conta
   * @param request dados atualizados
   * @return conta atualizada
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Atualizar conta referencial",
      description =
          "Atualiza descrição e ano de validade (ADMIN apenas, codigoRfb não pode ser alterado)")
  public ResponseEntity<ContaReferencialResponse> updateContaReferencial(
      @PathVariable Long id, @Valid @RequestBody UpdateContaReferencialRequest request) {
    ContaReferencialResponse response =
        updateContaReferencialUseCase.updateContaReferencial(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Altera status de uma conta referencial.
   *
   * @param id ID da conta
   * @param request novo status
   * @return resposta com novo status
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Alternar status",
      description = "Altera status da conta entre ACTIVE e INACTIVE (ADMIN apenas)")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request) {
    ToggleStatusResponse response =
        toggleContaReferencialStatusUseCase.toggleStatus(id, request);
    return ResponseEntity.ok(response);
  }
}
