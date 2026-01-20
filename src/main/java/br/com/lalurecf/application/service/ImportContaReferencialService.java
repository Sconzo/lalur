package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.contareferencial.ImportContaReferencialUseCase;
import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse;
import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse.ContaReferencialPreview;
import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse.ImportError;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service para importação de contas referenciais via arquivo CSV/TXT.
 *
 * <p>Valida cada linha, verifica duplicatas, e persiste ou retorna preview.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImportContaReferencialService implements ImportContaReferencialUseCase {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private static final int MAX_YEAR = Year.now().getValue() + 5;
  private static final int MIN_YEAR = 2000;

  private final ContaReferencialRepositoryPort contaReferencialRepository;

  @Override
  @Transactional
  public ImportContaReferencialResponse importContasReferenciais(
      MultipartFile file, boolean dryRun) {

    log.info("Importing ContaReferencial (dryRun: {})", dryRun);

    // Validar arquivo
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
    }

    List<ImportError> errors = new ArrayList<>();
    List<ContaReferencialPreview> preview = dryRun ? new ArrayList<>() : null;
    List<ContaReferencial> contasToSave = new ArrayList<>();
    Map<String, Integer> processedKeys = new HashMap<>();
    int totalLines = 0;
    int processedLines = 0;

    try (BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        CSVParser csvParser = createCsvParser(reader, file)) {

      for (CSVRecord record : csvParser) {
        totalLines++;
        int lineNumber = (int) record.getRecordNumber() + 1; // 1-based (conta header)

        try {
          // Parse linha
          ParsedContaReferencialLine parsedLine = parseLine(record, lineNumber);

          // Criar chave única (codigoRfb + anoValidade)
          String uniqueKey = createUniqueKey(parsedLine.codigoRfb, parsedLine.anoValidade);

          // Verificar duplicata dentro do arquivo
          if (processedKeys.containsKey(uniqueKey)) {
            int firstOccurrence = processedKeys.get(uniqueKey);
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Duplicate entry in file (codigoRfb='"
                            + parsedLine.codigoRfb
                            + "', anoValidade="
                            + parsedLine.anoValidade
                            + "). First occurrence at line "
                            + firstOccurrence)
                    .build());
            continue;
          }

          // Verificar duplicata no banco
          Optional<ContaReferencial> existing =
              contaReferencialRepository.findByCodigoRfbAndAnoValidade(
                  parsedLine.codigoRfb, parsedLine.anoValidade);
          if (existing.isPresent()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "ContaReferencial with codigoRfb='"
                            + parsedLine.codigoRfb
                            + "' and anoValidade="
                            + parsedLine.anoValidade
                            + " already exists")
                    .build());
            continue;
          }

          // Criar ContaReferencial
          ContaReferencial conta =
              ContaReferencial.builder()
                  .codigoRfb(parsedLine.codigoRfb)
                  .descricao(parsedLine.descricao)
                  .anoValidade(parsedLine.anoValidade)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            preview.add(
                ContaReferencialPreview.builder()
                    .codigoRfb(parsedLine.codigoRfb)
                    .descricao(parsedLine.descricao)
                    .anoValidade(parsedLine.anoValidade)
                    .build());
          } else {
            contasToSave.add(conta);
          }

          processedKeys.put(uniqueKey, lineNumber);
          processedLines++;

        } catch (Exception e) {
          log.warn("Error processing line {}: {}", lineNumber, e.getMessage());
          errors.add(
              ImportError.builder().lineNumber(lineNumber).error(e.getMessage()).build());
        }
      }

      // Persistir se não for dry-run
      if (!dryRun && !contasToSave.isEmpty()) {
        for (ContaReferencial conta : contasToSave) {
          contaReferencialRepository.save(conta);
        }
      }

      // Montar response
      boolean success = errors.isEmpty();
      String message =
          dryRun
              ? String.format(
                  "Dry-run completed. %d contas referenciais would be imported, %d errors found",
                  processedLines, errors.size())
              : String.format(
                  "Import completed. %d contas referenciais imported, %d skipped",
                  processedLines, totalLines - processedLines);

      return ImportContaReferencialResponse.builder()
          .success(success)
          .message(message)
          .totalLines(totalLines)
          .processedLines(processedLines)
          .skippedLines(totalLines - processedLines)
          .errors(errors)
          .preview(preview)
          .build();

    } catch (Exception e) {
      log.error("Error importing ContaReferencial: {}", e.getMessage(), e);
      throw new RuntimeException("Error importing file: " + e.getMessage(), e);
    }
  }

  private CSVParser createCsvParser(BufferedReader reader, MultipartFile file) throws Exception {
    // Detectar separador
    reader.mark(8192);
    String firstLine = reader.readLine();
    if (firstLine == null) {
      throw new IllegalArgumentException("File is empty or contains only header");
    }
    char delimiter = firstLine.contains(";") ? ';' : ',';
    reader.reset();

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

  private ParsedContaReferencialLine parseLine(CSVRecord record, int lineNumber) {
    // Validar campos obrigatórios
    String codigoRfb = getField(record, "codigoRfb", lineNumber);
    String descricao = getField(record, "descricao", lineNumber);

    // Campo opcional anoValidade
    Integer anoValidade = null;
    if (record.isMapped("anoValidade")) {
      String anoValidadeStr = record.get("anoValidade");
      if (anoValidadeStr != null && !anoValidadeStr.trim().isEmpty()) {
        anoValidade = parseAnoValidade(anoValidadeStr.trim(), lineNumber);
      }
    }

    // Validar tamanho da descrição
    if (descricao.length() > 1000) {
      throw new IllegalArgumentException(
          "Field 'descricao' exceeds maximum length of 1000 characters");
    }

    return new ParsedContaReferencialLine(codigoRfb, descricao, anoValidade);
  }

  private String getField(CSVRecord record, String fieldName, int lineNumber) {
    try {
      String value = record.get(fieldName);
      if (value == null || value.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Field '" + fieldName + "' is required but was empty");
      }
      return value.trim();
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("Mapping for")) {
        throw new IllegalArgumentException("Field '" + fieldName + "' not found in CSV header");
      }
      throw e;
    }
  }

  private Integer parseAnoValidade(String value, int lineNumber) {
    try {
      int ano = Integer.parseInt(value);
      if (ano < MIN_YEAR || ano > MAX_YEAR) {
        throw new IllegalArgumentException(
            "Invalid anoValidade: '"
                + value
                + "'. Must be between "
                + MIN_YEAR
                + " and "
                + MAX_YEAR);
      }
      return ano;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid anoValidade: '" + value + "'. Must be an integer");
    }
  }

  private String createUniqueKey(String codigoRfb, Integer anoValidade) {
    return codigoRfb + "|" + anoValidade;
  }

  /** DTO interno para armazenar dados parseados de uma linha CSV. */
  private record ParsedContaReferencialLine(
      String codigoRfb, String descricao, Integer anoValidade) {}
}
