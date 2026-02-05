package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.lancamentoparteb.CreateLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.GetLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.ListLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.ToggleLancamentoParteBStatusUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.UpdateLancamentoParteBUseCase;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.CreateLancamentoParteBRequest;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.UpdateLancamentoParteBRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

/**
 * Controller para gerenciamento de Lançamentos da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Todos endpoints requerem role CONTADOR e header X-Company-Id (contexto de empresa).
 */
@RestController
@RequestMapping("/lancamento-parte-b")
@RequiredArgsConstructor
@Tag(
    name = "Lançamentos Parte B",
    description = "Gerenciamento de lançamentos da Parte B (e-Lalur/e-Lacs)")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBController {

  private final CreateLancamentoParteBUseCase createLancamentoParteBUseCase;
  private final ListLancamentoParteBUseCase listLancamentoParteBUseCase;
  private final GetLancamentoParteBUseCase getLancamentoParteBUseCase;
  private final UpdateLancamentoParteBUseCase updateLancamentoParteBUseCase;
  private final ToggleLancamentoParteBStatusUseCase toggleLancamentoParteBStatusUseCase;

  /**
   * Cria um novo lançamento da Parte B.
   *
   * @param request dados do lançamento a ser criado
   * @return lançamento criado
   */
  @PostMapping
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Criar lançamento Parte B",
      description =
          "Cria um novo lançamento fiscal da Parte B (e-Lalur/e-Lacs) "
              + "para a empresa no contexto (header X-Company-Id)")
  public ResponseEntity<LancamentoParteBResponse> createLancamentoParteB(
      @Valid @RequestBody CreateLancamentoParteBRequest request) {
    LancamentoParteBResponse response =
        createLancamentoParteBUseCase.createLancamentoParteB(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista lançamentos da Parte B com paginação e filtros.
   *
   * @param anoReferencia filtro por ano de referência (opcional)
   * @param mesReferencia filtro por mês de referência (opcional)
   * @param tipoApuracao filtro por tipo de apuração (opcional)
   * @param tipoAjuste filtro por tipo de ajuste (opcional)
   * @param includeInactive se deve incluir lançamentos inativos
   * @param pageable configuração de paginação
   * @return página de lançamentos Parte B
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Listar lançamentos Parte B",
      description =
          "Lista lançamentos da Parte B da empresa no contexto com filtros e paginação "
              + "(header X-Company-Id)")
  public ResponseEntity<Page<LancamentoParteBResponse>> listLancamentosParteB(
      @RequestParam(name = "ano_referencia", required = false) Integer anoReferencia,
      @RequestParam(name = "mes_referencia", required = false) Integer mesReferencia,
      @RequestParam(name = "tipo_apuracao", required = false) TipoApuracao tipoApuracao,
      @RequestParam(name = "tipo_ajuste", required = false) TipoAjuste tipoAjuste,
      @RequestParam(name = "include_inactive", required = false, defaultValue = "false")
          Boolean includeInactive,
      @PageableDefault(size = 100, sort = "anoReferencia", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<LancamentoParteBResponse> response =
        listLancamentoParteBUseCase.listLancamentosParteB(
            anoReferencia, mesReferencia, tipoApuracao, tipoAjuste, includeInactive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Obtém lançamento da Parte B por ID.
   *
   * @param id ID do lançamento
   * @return dados do lançamento
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Obter lançamento Parte B",
      description = "Obtém lançamento da Parte B por ID (header X-Company-Id)")
  public ResponseEntity<LancamentoParteBResponse> getLancamentoParteBById(@PathVariable Long id) {
    LancamentoParteBResponse response = getLancamentoParteBUseCase.getLancamentoParteBById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza dados de um lançamento da Parte B.
   *
   * @param id ID do lançamento
   * @param request dados atualizados
   * @return lançamento atualizado
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Atualizar lançamento Parte B",
      description = "Atualiza lançamento da Parte B. Requer header X-Company-Id")
  public ResponseEntity<LancamentoParteBResponse> updateLancamentoParteB(
      @PathVariable Long id, @Valid @RequestBody UpdateLancamentoParteBRequest request) {
    LancamentoParteBResponse response =
        updateLancamentoParteBUseCase.updateLancamentoParteB(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Altera status de um lançamento da Parte B.
   *
   * @param id ID do lançamento
   * @param request novo status
   * @return resposta com novo status
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Alternar status",
      description = "Altera status do lançamento entre ACTIVE e INACTIVE (header X-Company-Id)")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request) {
    ToggleStatusResponse response = toggleLancamentoParteBStatusUseCase.toggleStatus(id, request);
    return ResponseEntity.ok(response);
  }
}
