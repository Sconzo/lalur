package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.lancamentoparteb.ImportLancamentoParteBUseCase;
import br.com.lalurecf.application.port.out.ContaParteBRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.application.port.out.TaxParameterRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.domain.model.TaxParameter;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse.ImportError;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse.LancamentoParteBPreview;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
 * Service para importação de lançamentos da Parte B via arquivo CSV/TXT.
 *
 * <p>Formato CSV esperado (10 colunas):
 * mesReferencia;anoReferencia;tipoApuracao;tipoRelacionamento;contaContabilCode;
 * contaParteBCode;parametroTributarioCodigo;tipoAjuste;descricao;valor
 *
 * <p>Separador: auto-detectado (; ou ,)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ImportLancamentoParteBService implements ImportLancamentoParteBUseCase {

  private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
  private static final int EXPECTED_COLUMNS = 10;

  private final LancamentoParteBRepositoryPort lancamentoParteBRepository;
  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final ContaParteBRepositoryPort contaParteBRepository;
  private final TaxParameterRepositoryPort taxParameterRepository;

  @Override
  @Transactional
  public ImportLancamentoParteBResponse importLancamentos(
      MultipartFile file, Long companyId, boolean dryRun) {

    log.info(
        "Starting import of LancamentosParteB for company {} (dryRun: {})", companyId, dryRun);

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "File size exceeds maximum allowed (50MB). Current size: " + file.getSize() + " bytes");
    }

    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    List<ImportError> errors = new ArrayList<>();
    List<LancamentoParteB> lancamentosToSave = new ArrayList<>();
    List<LancamentoParteBPreview> previews = new ArrayList<>();
    int lineNumber = 0;
    int processedLines = 0;
    int skippedLines = 0;

    try (BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        CSVParser csvParser = createCsvParser(reader)) {

      for (CSVRecord record : csvParser) {
        lineNumber++;

        try {
          if (record.size() < EXPECTED_COLUMNS) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Missing required fields. Expected "
                            + EXPECTED_COLUMNS
                            + " columns, got "
                            + record.size())
                    .build());
            skippedLines++;
            continue;
          }

          // Extrair campos
          final String mesReferenciaStr = record.get(0).trim();
          final String anoReferenciaStr = record.get(1).trim();
          final String tipoApuracaoStr = record.get(2).trim();
          final String tipoRelacionamentoStr = record.get(3).trim();
          final String contaContabilCode = record.get(4).trim();
          final String contaParteBCode = record.get(5).trim();
          final String parametroTributarioCodigo = record.get(6).trim();
          final String tipoAjusteStr = record.get(7).trim();
          final String descricao = record.get(8).trim();
          final String valorStr = record.get(9).trim();

          // Validar campos obrigatórios não-nulos
          if (mesReferenciaStr.isEmpty()
              || anoReferenciaStr.isEmpty()
              || tipoApuracaoStr.isEmpty()
              || tipoRelacionamentoStr.isEmpty()
              || parametroTributarioCodigo.isEmpty()
              || tipoAjusteStr.isEmpty()
              || descricao.isEmpty()
              || valorStr.isEmpty()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "One or more required fields are empty"
                            + " (mesReferencia, anoReferencia, tipoApuracao, tipoRelacionamento,"
                            + " parametroTributarioCodigo, tipoAjuste, descricao, valor)")
                    .build());
            skippedLines++;
            continue;
          }

          // Parse mesReferencia
          int mesReferencia;
          try {
            mesReferencia = Integer.parseInt(mesReferenciaStr);
            if (mesReferencia < 1 || mesReferencia > 12) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("mesReferencia must be between 1 and 12, got: " + mesReferenciaStr)
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid mesReferencia format: " + mesReferenciaStr)
                    .build());
            skippedLines++;
            continue;
          }

          // Parse anoReferencia
          int anoReferencia;
          try {
            anoReferencia = Integer.parseInt(anoReferenciaStr);
            if (anoReferencia < 2000) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("anoReferencia must be >= 2000, got: " + anoReferenciaStr)
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid anoReferencia format: " + anoReferenciaStr)
                    .build());
            skippedLines++;
            continue;
          }

          // Parse tipoApuracao
          TipoApuracao tipoApuracao;
          try {
            tipoApuracao = TipoApuracao.valueOf(tipoApuracaoStr.toUpperCase());
          } catch (IllegalArgumentException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Invalid tipoApuracao: '"
                            + tipoApuracaoStr
                            + "'. Valid values: IRPJ, CSLL")
                    .build());
            skippedLines++;
            continue;
          }

          // Parse tipoRelacionamento
          TipoRelacionamento tipoRelacionamento;
          try {
            tipoRelacionamento = TipoRelacionamento.valueOf(tipoRelacionamentoStr.toUpperCase());
          } catch (IllegalArgumentException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Invalid tipoRelacionamento: '"
                            + tipoRelacionamentoStr
                            + "'. Valid values: CONTA_CONTABIL, CONTA_PARTE_B, AMBOS")
                    .build());
            skippedLines++;
            continue;
          }

          // Parse tipoAjuste
          TipoAjuste tipoAjuste;
          try {
            tipoAjuste = TipoAjuste.valueOf(tipoAjusteStr.toUpperCase());
          } catch (IllegalArgumentException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Invalid tipoAjuste: '"
                            + tipoAjusteStr
                            + "'. Valid values: ADICAO, EXCLUSAO")
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
                      .error("valor must be > 0")
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid valor format: " + valorStr)
                    .build());
            skippedLines++;
            continue;
          }

          // Validar parâmetro tributário por código
          Optional<TaxParameter> parametroOpt =
              taxParameterRepository.findByCode(parametroTributarioCodigo);
          if (parametroOpt.isEmpty()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Parâmetro tributário não encontrado com código: '"
                            + parametroTributarioCodigo
                            + "'")
                    .build());
            skippedLines++;
            continue;
          }
          TaxParameter parametro = parametroOpt.get();
          if (parametro.getStatus() != Status.ACTIVE) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Parâmetro tributário '"
                            + parametroTributarioCodigo
                            + "' não está ACTIVE. Status: "
                            + parametro.getStatus())
                    .build());
            skippedLines++;
            continue;
          }

          // Validar FKs condicionais conforme tipoRelacionamento
          Long contaContabilId = null;
          Long contaParteBId = null;

          switch (tipoRelacionamento) {
            case CONTA_CONTABIL:
              if (contaContabilCode.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "contaContabilCode é obrigatório quando"
                                + " tipoRelacionamento = CONTA_CONTABIL")
                        .build());
                skippedLines++;
                continue;
              }
              Optional<PlanoDeContas> contaContabilOpt =
                  planoDeContasRepository.findByCompanyIdAndCodeAndFiscalYear(
                      companyId, contaContabilCode, anoReferencia);
              if (contaContabilOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "Conta contábil '"
                                + contaContabilCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
                        .build());
                skippedLines++;
                continue;
              }
              contaContabilId = contaContabilOpt.get().getId();
              break;

            case CONTA_PARTE_B:
              if (contaParteBCode.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "contaParteBCode é obrigatório quando"
                                + " tipoRelacionamento = CONTA_PARTE_B")
                        .build());
                skippedLines++;
                continue;
              }
              Optional<ContaParteB> contaParteBOpt =
                  contaParteBRepository.findByCompanyIdAndCodigoContaAndAnoBase(
                      companyId, contaParteBCode, anoReferencia);
              if (contaParteBOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "Conta Parte B '"
                                + contaParteBCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
                        .build());
                skippedLines++;
                continue;
              }
              contaParteBId = contaParteBOpt.get().getId();
              break;

            case AMBOS:
              if (contaContabilCode.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "contaContabilCode é obrigatório quando tipoRelacionamento = AMBOS")
                        .build());
                skippedLines++;
                continue;
              }
              if (contaParteBCode.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "contaParteBCode é obrigatório quando tipoRelacionamento = AMBOS")
                        .build());
                skippedLines++;
                continue;
              }
              Optional<PlanoDeContas> contaContabilAmbosOpt =
                  planoDeContasRepository.findByCompanyIdAndCodeAndFiscalYear(
                      companyId, contaContabilCode, anoReferencia);
              if (contaContabilAmbosOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "Conta contábil '"
                                + contaContabilCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
                        .build());
                skippedLines++;
                continue;
              }
              Optional<ContaParteB> contaParteBambosOpt =
                  contaParteBRepository.findByCompanyIdAndCodigoContaAndAnoBase(
                      companyId, contaParteBCode, anoReferencia);
              if (contaParteBambosOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "Conta Parte B '"
                                + contaParteBCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
                        .build());
                skippedLines++;
                continue;
              }
              contaContabilId = contaContabilAmbosOpt.get().getId();
              contaParteBId = contaParteBambosOpt.get().getId();
              break;

            default:
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("tipoRelacionamento inválido: " + tipoRelacionamento)
                      .build());
              skippedLines++;
              continue;
          }

          // Montar domain object
          LancamentoParteB lancamento =
              LancamentoParteB.builder()
                  .companyId(companyId)
                  .mesReferencia(mesReferencia)
                  .anoReferencia(anoReferencia)
                  .tipoApuracao(tipoApuracao)
                  .tipoRelacionamento(tipoRelacionamento)
                  .contaContabilId(contaContabilId)
                  .contaParteBId(contaParteBId)
                  .parametroTributarioId(parametro.getId())
                  .tipoAjuste(tipoAjuste)
                  .descricao(descricao)
                  .valor(valor)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            previews.add(
                LancamentoParteBPreview.builder()
                    .mesReferencia(mesReferenciaStr)
                    .anoReferencia(anoReferenciaStr)
                    .tipoApuracao(tipoApuracaoStr)
                    .tipoRelacionamento(tipoRelacionamentoStr)
                    .contaContabilCode(contaContabilCode.isEmpty() ? null : contaContabilCode)
                    .contaParteBCode(contaParteBCode.isEmpty() ? null : contaParteBCode)
                    .parametroTributarioCodigo(parametroTributarioCodigo)
                    .tipoAjuste(tipoAjusteStr)
                    .descricao(descricao)
                    .valor(valorStr)
                    .build());
          } else {
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

      // Persistir se não for dry run
      if (!dryRun) {
        for (LancamentoParteB lancamento : lancamentosToSave) {
          lancamentoParteBRepository.save(lancamento);
        }
        log.info("Persisted {} lançamentos Parte B", lancamentosToSave.size());
      }

      boolean success = skippedLines == 0;
      String message =
          success
              ? String.format("Successfully processed %d lines", processedLines)
              : String.format("Processed %d lines with %d errors", processedLines, skippedLines);

      return ImportLancamentoParteBResponse.builder()
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
   * Cria CSVParser com auto-detecção de separador (; ou ,).
   *
   * @param reader BufferedReader do arquivo
   * @return CSVParser configurado
   */
  private CSVParser createCsvParser(BufferedReader reader) throws Exception {
    reader.mark(8192);

    String firstLine = reader.readLine();
    if (firstLine == null || firstLine.trim().isEmpty()) {
      throw new IllegalArgumentException("File is empty or has no header");
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
}
