package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.UploadImportedEcfUseCase;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.dto.ecf.UploadImportedEcfResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por validar e armazenar o ECF Importado.
 *
 * <p>Valida extensão, tamanho, formato SPED e presença do bloco M.
 * Lê o conteúdo em ISO-8859-1 (LATIN-1) conforme padrão SPED ECF.
 * Persiste via upsert e rebaixa COMPLETE_ECF existente para DRAFT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcfUploadService implements UploadImportedEcfUseCase {

  private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

  private final EcfFileRepositoryPort ecfFileRepositoryPort;
  private final CompanyRepositoryPort companyRepositoryPort;

  /**
   * Valida e armazena o ECF importado em 8 passos conforme AC da Story 5.4.
   */
  @Override
  @Transactional
  public UploadImportedEcfResponse upload(
      byte[] fileContent, String originalFileName,
      Integer fiscalYear, Long companyId, String generatedBy) {

    log.info("Upload ECF Importado: companyId={}, fiscalYear={}, file={}",
        companyId, fiscalYear, originalFileName);

    // Passo 1: validar extensão
    if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".txt")) {
      throw new IllegalArgumentException("O arquivo deve ter extensão .txt");
    }

    // Passo 2: validar tamanho
    if (fileContent.length > MAX_FILE_SIZE_BYTES) {
      throw new IllegalArgumentException("O arquivo excede o tamanho máximo de 50MB");
    }

    // Passo 5: converter para String ISO-8859-1 (antes de validar formato)
    String content = new String(fileContent, StandardCharsets.ISO_8859_1);

    // Passo 3: validar formato SPED (primeiras 50 linhas)
    boolean validSped = content.lines()
        .limit(50)
        .filter(line -> !line.isBlank())
        .allMatch(line -> line.startsWith("|") && line.endsWith("|"));
    if (!validSped) {
      throw new IllegalArgumentException(
          "O arquivo não está no formato SPED (linhas devem iniciar e terminar com |)");
    }

    // Passo 4: validar presença do bloco M
    if (!content.contains("|M001|")) {
      throw new IllegalArgumentException(
          "O arquivo não contém bloco M (|M001| não encontrado)");
    }

    // Passo 6: construir EcfFile
    Company company = companyRepositoryPort.findById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + companyId));

    String cnpj = company.getCnpj() != null ? company.getCnpj().getValue() : companyId.toString();
    String fileName = String.format("ECF_Importado_%d_%s.txt", fiscalYear, cnpj);

    EcfFile ecfFile = EcfFile.builder()
        .fileType(EcfFileType.IMPORTED_ECF)
        .companyId(companyId)
        .fiscalYear(fiscalYear)
        .content(content)
        .fileName(fileName)
        .fileStatus(EcfFileStatus.DRAFT)
        .generatedAt(LocalDateTime.now())
        .generatedBy(generatedBy)
        .status(Status.ACTIVE)
        .build();

    // Passo 7: persistir (upsert)
    EcfFile saved = ecfFileRepositoryPort.saveOrReplace(ecfFile);
    log.info("ECF Importado salvo: id={}, fileName={}", saved.getId(), saved.getFileName());

    // Passo 8: rebaixar COMPLETE_ECF existente para DRAFT
    ecfFileRepositoryPort
        .findByCompanyAndFiscalYearAndType(companyId, fiscalYear, EcfFileType.COMPLETE_ECF)
        .filter(ecf -> ecf.getFileStatus() == EcfFileStatus.VALIDATED
            || ecf.getFileStatus() == EcfFileStatus.FINALIZED)
        .ifPresent(ecf -> {
          ecf.setFileStatus(EcfFileStatus.DRAFT);
          ecf.setValidationErrors(null);
          ecfFileRepositoryPort.saveOrReplace(ecf);
          log.info("COMPLETE_ECF {} rebaixado para DRAFT após upload ECF Importado",
              ecf.getId());
        });

    int lineCount = (int) content.lines().count();

    return new UploadImportedEcfResponse(
        true,
        "ECF Importado armazenado com sucesso",
        saved.getId(),
        saved.getFileName(),
        (long) fileContent.length,
        lineCount);
  }
}
