package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.chartofaccount.ImportChartOfAccountUseCase;
import br.com.lalurecf.application.port.out.ChartOfAccountRepositoryPort;
import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.ChartOfAccount;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ImportChartOfAccountResponse;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ImportChartOfAccountResponse.ChartOfAccountPreview;
import br.com.lalurecf.infrastructure.dto.chartofaccount.ImportChartOfAccountResponse.ImportError;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * Service para importação de plano de contas via arquivo CSV/TXT.
 *
 * <p>Valida cada linha, busca Conta Referencial RFB por código, e persiste ou retorna preview.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImportChartOfAccountService implements ImportChartOfAccountUseCase {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  private final ChartOfAccountRepositoryPort chartOfAccountRepository;
  private final ContaReferencialRepositoryPort contaReferencialRepository;

  @Override
  @Transactional
  public ImportChartOfAccountResponse importChartOfAccounts(
      MultipartFile file, Long companyId, Integer fiscalYear, boolean dryRun) {

    log.info(
        "Importing ChartOfAccount for company {} and fiscalYear {} (dryRun: {})",
        companyId,
        fiscalYear,
        dryRun);

    // Validar arquivo
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
    }

    List<ImportError> errors = new ArrayList<>();
    List<ChartOfAccountPreview> preview = dryRun ? new ArrayList<>() : null;
    List<ChartOfAccount> accountsToSave = new ArrayList<>();
    Set<String> processedCodes = new HashSet<>();
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
          ParsedAccountLine parsedLine = parseLine(record, lineNumber);

          // Verificar duplicata dentro do arquivo
          if (processedCodes.contains(parsedLine.code)) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Duplicate code in file: " + parsedLine.code)
                    .build());
            continue;
          }

          // Verificar duplicata no banco
          Optional<ChartOfAccount> existing =
              chartOfAccountRepository.findByCompanyIdAndCodeAndFiscalYear(
                  companyId, parsedLine.code, fiscalYear);
          if (existing.isPresent()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Account code '"
                            + parsedLine.code
                            + "' already exists for company/year")
                    .build());
            continue;
          }

          // Buscar Conta Referencial por código RFB
          Optional<ContaReferencial> contaReferencialOpt =
              contaReferencialRepository.findByCodigoRfb(parsedLine.contaReferencialCodigo);
          if (contaReferencialOpt.isEmpty()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Conta Referencial '"
                            + parsedLine.contaReferencialCodigo
                            + "' not found")
                    .build());
            continue;
          }

          ContaReferencial contaReferencial = contaReferencialOpt.get();
          if (contaReferencial.getStatus() != Status.ACTIVE) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Conta Referencial '"
                            + parsedLine.contaReferencialCodigo
                            + "' is not ACTIVE")
                    .build());
            continue;
          }

          // Criar ChartOfAccount
          ChartOfAccount account =
              ChartOfAccount.builder()
                  .companyId(companyId)
                  .code(parsedLine.code)
                  .name(parsedLine.name)
                  .fiscalYear(fiscalYear)
                  .accountType(parsedLine.accountType)
                  .contaReferencialId(contaReferencial.getId())
                  .classe(parsedLine.classe)
                  .nivel(parsedLine.nivel)
                  .natureza(parsedLine.natureza)
                  .afetaResultado(parsedLine.afetaResultado)
                  .dedutivel(parsedLine.dedutivel)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            preview.add(
                ChartOfAccountPreview.builder()
                    .code(parsedLine.code)
                    .name(parsedLine.name)
                    .fiscalYear(fiscalYear)
                    .accountType(parsedLine.accountType)
                    .contaReferencialCodigo(parsedLine.contaReferencialCodigo)
                    .classe(parsedLine.classe)
                    .nivel(parsedLine.nivel)
                    .natureza(parsedLine.natureza)
                    .afetaResultado(parsedLine.afetaResultado)
                    .dedutivel(parsedLine.dedutivel)
                    .build());
          } else {
            accountsToSave.add(account);
          }

          processedCodes.add(parsedLine.code);
          processedLines++;

        } catch (Exception e) {
          log.warn("Error processing line {}: {}", lineNumber, e.getMessage());
          errors.add(
              ImportError.builder().lineNumber(lineNumber).error(e.getMessage()).build());
        }
      }

      // Persistir se não for dry-run
      if (!dryRun && !accountsToSave.isEmpty()) {
        for (ChartOfAccount account : accountsToSave) {
          chartOfAccountRepository.save(account);
        }
      }

      // Montar response
      boolean success = errors.isEmpty();
      String message =
          dryRun
              ? String.format(
                  "Dry-run completed. %d accounts would be imported, %d errors found",
                  processedLines, errors.size())
              : String.format(
                  "Import completed. %d accounts imported, %d skipped",
                  processedLines, totalLines - processedLines);

      return ImportChartOfAccountResponse.builder()
          .success(success)
          .message(message)
          .totalLines(totalLines)
          .processedLines(processedLines)
          .skippedLines(totalLines - processedLines)
          .errors(errors)
          .preview(preview)
          .build();

    } catch (Exception e) {
      log.error("Error importing ChartOfAccount: {}", e.getMessage(), e);
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

  private ParsedAccountLine parseLine(CSVRecord record, int lineNumber) {
    // Validar campos obrigatórios
    String code = getField(record, "code", lineNumber);
    String name = getField(record, "name", lineNumber);
    String accountTypeStr = getField(record, "accountType", lineNumber);
    String contaReferencialCodigo = getField(record, "contaReferencialCodigo", lineNumber);
    String classeStr = getField(record, "classe", lineNumber);
    String nivelStr = getField(record, "nivel", lineNumber);
    String naturezaStr = getField(record, "natureza", lineNumber);
    String afetaResultadoStr = getField(record, "afetaResultado", lineNumber);
    String dedutivelStr = getField(record, "dedutivel", lineNumber);

    // Parse enums
    AccountType accountType = parseAccountType(accountTypeStr, lineNumber);
    ClasseContabil classe = parseClasseContabil(classeStr, lineNumber);
    NaturezaConta natureza = parseNaturezaConta(naturezaStr, lineNumber);

    // Parse nivel
    Integer nivel = parseNivel(nivelStr, lineNumber);

    // Parse booleans
    Boolean afetaResultado = parseBoolean(afetaResultadoStr, "afetaResultado", lineNumber);
    Boolean dedutivel = parseBoolean(dedutivelStr, "dedutivel", lineNumber);

    return new ParsedAccountLine(
        code,
        name,
        accountType,
        contaReferencialCodigo,
        classe,
        nivel,
        natureza,
        afetaResultado,
        dedutivel);
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
      throw new IllegalArgumentException("Field '" + fieldName + "' not found in CSV header");
    }
  }

  private AccountType parseAccountType(String value, int lineNumber) {
    try {
      return AccountType.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid accountType: '"
              + value
              + "'. Must be one of: "
              + String.join(", ", getAccountTypeValues()));
    }
  }

  private ClasseContabil parseClasseContabil(String value, int lineNumber) {
    try {
      return ClasseContabil.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid classe: '"
              + value
              + "'. Must be one of: "
              + String.join(", ", getClasseContabilValues()));
    }
  }

  private NaturezaConta parseNaturezaConta(String value, int lineNumber) {
    try {
      return NaturezaConta.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid natureza: '"
              + value
              + "'. Must be one of: "
              + String.join(", ", getNaturezaContaValues()));
    }
  }

  private Integer parseNivel(String value, int lineNumber) {
    try {
      int nivel = Integer.parseInt(value);
      if (nivel < 1 || nivel > 5) {
        throw new IllegalArgumentException("nivel must be between 1 and 5");
      }
      return nivel;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid nivel: '" + value + "'. Must be an integer");
    }
  }

  private Boolean parseBoolean(String value, String fieldName, int lineNumber) {
    String normalized = value.toLowerCase().trim();
    return switch (normalized) {
      case "true", "yes", "sim", "1" -> true;
      case "false", "no", "não", "nao", "0" -> false;
      default ->
          throw new IllegalArgumentException(
              "Invalid "
                  + fieldName
                  + ": '"
                  + value
                  + "'. Must be true/false/yes/no/sim/não");
    };
  }

  private List<String> getAccountTypeValues() {
    List<String> values = new ArrayList<>();
    for (AccountType type : AccountType.values()) {
      values.add(type.name());
    }
    return values;
  }

  private List<String> getClasseContabilValues() {
    List<String> values = new ArrayList<>();
    for (ClasseContabil classe : ClasseContabil.values()) {
      values.add(classe.name());
    }
    return values;
  }

  private List<String> getNaturezaContaValues() {
    List<String> values = new ArrayList<>();
    for (NaturezaConta natureza : NaturezaConta.values()) {
      values.add(natureza.name());
    }
    return values;
  }

  /**
   * DTO interno para armazenar dados parseados de uma linha CSV.
   */
  private record ParsedAccountLine(
      String code,
      String name,
      AccountType accountType,
      String contaReferencialCodigo,
      ClasseContabil classe,
      Integer nivel,
      NaturezaConta natureza,
      Boolean afetaResultado,
      Boolean dedutivel) {}
}
