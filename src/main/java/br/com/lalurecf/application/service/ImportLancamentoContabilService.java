package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ImportLancamentoContabilUseCase;
import br.com.lalurecf.application.port.out.ChartOfAccountRepositoryPort;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoContabilRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.ChartOfAccount;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse.ImportError;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse.LancamentoContabilPreview;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service para importação de lançamentos contábeis via arquivo CSV/TXT.
 *
 * <p>Implementa parsing com auto-detecção de separador, validação de partidas dobradas e validação
 * de Período Contábil.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImportLancamentoContabilService implements ImportLancamentoContabilUseCase {

  private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  private final LancamentoContabilRepositoryPort lancamentoContabilRepository;
  private final ChartOfAccountRepositoryPort chartOfAccountRepository;
  private final CompanyRepositoryPort companyRepository;

  @Override
  @Transactional
  public ImportLancamentoContabilResponse importLancamentos(
      MultipartFile file, Long companyId, Integer fiscalYear, boolean dryRun) {

    log.info(
        "Starting import of Lançamentos Contábeis for company {} and fiscalYear {} (dryRun: {})",
        companyId,
        fiscalYear,
        dryRun);

    // Validar tamanho do arquivo
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "File size exceeds maximum allowed (50MB). Current size: " + file.getSize() + " bytes");
    }

    // Validar arquivo não vazio
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    // Buscar company para validar Período Contábil
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found with id: " + companyId));

    List<ImportError> errors = new ArrayList<>();
    List<LancamentoContabil> lancamentosToSave = new ArrayList<>();
    List<LancamentoContabilPreview> previews = new ArrayList<>();
    int lineNumber = 0;
    int processedLines = 0;
    int skippedLines = 0;

    try (BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        CSVParser csvParser = createCsvParser(reader, file)) {

      for (CSVRecord record : csvParser) {
        lineNumber++;

        try {
          // Validar que record tem todos os campos esperados
          if (record.size() < 5) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Missing required fields. Expected at least 5 columns.")
                    .build());
            skippedLines++;
            continue;
          }

          // Extrair campos
          final String contaDebitoCode = record.get(0).trim();
          final String contaCreditoCode = record.get(1).trim();
          final String dataStr = record.get(2).trim();
          final String valorStr = record.get(3).trim();
          final String historico = record.get(4).trim();
          final String numeroDocumento = record.size() > 5 ? record.get(5).trim() : null;

          // Validar campos obrigatórios
          if (contaDebitoCode.isEmpty()
              || contaCreditoCode.isEmpty()
              || dataStr.isEmpty()
              || valorStr.isEmpty()
              || historico.isEmpty()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("One or more required fields are empty")
                    .build());
            skippedLines++;
            continue;
          }

          // Validar contas diferentes
          if (contaDebitoCode.equals(contaCreditoCode)) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Debit and credit accounts must be different")
                    .build());
            skippedLines++;
            continue;
          }

          // Buscar contas
          Optional<ChartOfAccount> contaDebitoOpt =
              chartOfAccountRepository.findByCompanyIdAndCodeAndFiscalYear(
                  companyId, contaDebitoCode, fiscalYear);
          if (contaDebitoOpt.isEmpty()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Account code '"
                            + contaDebitoCode
                            + "' not found for company/year")
                    .build());
            skippedLines++;
            continue;
          }

          Optional<ChartOfAccount> contaCreditoOpt =
              chartOfAccountRepository.findByCompanyIdAndCodeAndFiscalYear(
                  companyId, contaCreditoCode, fiscalYear);
          if (contaCreditoOpt.isEmpty()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Account code '"
                            + contaCreditoCode
                            + "' not found for company/year")
                    .build());
            skippedLines++;
            continue;
          }

          // Parse data
          LocalDate data;
          try {
            data = LocalDate.parse(dataStr, DATE_FORMATTER);
          } catch (DateTimeParseException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid date format. Expected YYYY-MM-DD, got: " + dataStr)
                    .build());
            skippedLines++;
            continue;
          }

          // Validar Período Contábil
          if (company.getPeriodoContabil() != null && data.isBefore(company.getPeriodoContabil())) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Data "
                            + dataStr
                            + " is before Período Contábil "
                            + company.getPeriodoContabil())
                    .build());
            skippedLines++;
            continue;
          }

          // Parse valor
          BigDecimal valor;
          try {
            valor = new BigDecimal(valorStr);
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("Invalid value: must be > 0")
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid value format: " + valorStr)
                    .build());
            skippedLines++;
            continue;
          }

          // Criar lançamento
          LancamentoContabil lancamento =
              LancamentoContabil.builder()
                  .companyId(companyId)
                  .contaDebitoId(contaDebitoOpt.get().getId())
                  .contaCreditoId(contaCreditoOpt.get().getId())
                  .data(data)
                  .valor(valor)
                  .historico(historico)
                  .numeroDocumento(numeroDocumento)
                  .fiscalYear(fiscalYear)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            // Adicionar ao preview
            previews.add(
                LancamentoContabilPreview.builder()
                    .contaDebitoCode(contaDebitoCode)
                    .contaCreditoCode(contaCreditoCode)
                    .data(dataStr)
                    .valor(valorStr)
                    .historico(historico)
                    .numeroDocumento(numeroDocumento)
                    .build());
          } else {
            // Adicionar para persistir
            lancamentosToSave.add(lancamento);
          }

          processedLines++;

        } catch (Exception e) {
          log.error("Error processing line {}: {}", lineNumber, e.getMessage(), e);
          errors.add(
              ImportError.builder()
                  .lineNumber(lineNumber)
                  .error("Unexpected error: " + e.getMessage())
                  .build());
          skippedLines++;
        }
      }

      // Persistir lançamentos se não for dry run
      if (!dryRun) {
        for (LancamentoContabil lancamento : lancamentosToSave) {
          lancamentoContabilRepository.save(lancamento);
        }
        log.info("Persisted {} lançamentos contábeis", lancamentosToSave.size());
      }

      // Montar resposta
      boolean success = skippedLines == 0;
      String message =
          success
              ? String.format("Successfully processed %d lines", processedLines)
              : String.format(
                  "Processed %d lines with %d errors", processedLines, skippedLines);

      return ImportLancamentoContabilResponse.builder()
          .success(success)
          .message(message)
          .totalLines(lineNumber)
          .processedLines(processedLines)
          .skippedLines(skippedLines)
          .errors(errors)
          .preview(dryRun ? previews : null)
          .build();

    } catch (Exception e) {
      log.error("Error during import: {}", e.getMessage(), e);
      throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
    }
  }

  /**
   * Cria CSVParser com auto-detecção de separador.
   *
   * @param reader BufferedReader do arquivo
   * @param file arquivo original para mensagens de erro
   * @return CSVParser configurado
   */
  private CSVParser createCsvParser(BufferedReader reader, MultipartFile file) throws Exception {
    // Marcar posição inicial para poder resetar
    reader.mark(8192);

    // Ler primeira linha para detectar separador
    String firstLine = reader.readLine();
    if (firstLine == null || firstLine.trim().isEmpty()) {
      throw new IllegalArgumentException("File is empty or has no header");
    }

    // Detectar separador (priorizar ; sobre ,)
    char delimiter = firstLine.contains(";") ? ';' : ',';

    // Resetar reader para início
    reader.reset();

    // Criar CSVFormat
    CSVFormat format =
        CSVFormat.DEFAULT
            .builder()
            .setDelimiter(delimiter)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

    return new CSVParser(reader, format);
  }
}
