package br.com.lalurecf.application.service;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela geração do conteúdo do bloco M do arquivo ECF.
 *
 * <p>Gera registros M030/M300/M305/M310 (IRPJ), M350/M355/M360 (CSLL) e
 * M400/M410/M405 (Contas da Parte B) a partir dos lançamentos cadastrados.
 *
 * <p>O método principal {@link #generateArquivoParcial} retorna o conteúdo do
 * bloco M (sem M001) incluindo M990 ao final, pronto para ser envolto pelo
 * montador do arquivo parcial (Story 5.3).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class PartMGeneratorService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

  private final LancamentoParteBRepositoryPort lancamentoRepo;
  private final ContaParteBRepositoryPort contaParteBRepo;
  private final PlanoDeContasRepositoryPort planoDeContasRepo;
  private final TaxParameterRepositoryPort taxParameterRepo;

  /**
   * Gera o conteúdo do bloco M para o arquivo parcial ECF.
   *
   * <p>Retorna todas as linhas M (Grupo1 IRPJ + Grupo2 CSLL + Grupo3 Parte B + M990)
   * como string com separador de linha. O chamador (assembler da Story 5.3) deve
   * adicionar a linha M001 antes deste conteúdo.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal de referência
   * @return conteúdo do bloco M terminando com M990
   * @throws IllegalArgumentException se não existirem lançamentos ACTIVE para o ano
   */
  @Transactional(readOnly = true)
  public String generateArquivoParcial(Long companyId, Integer fiscalYear) {
    List<LancamentoParteB> active =
        lancamentoRepo.findByCompanyIdAndAnoReferenciaAndStatus(
            companyId, fiscalYear, Status.ACTIVE);
    return generateArquivoParcial(active, fiscalYear);
  }

  /**
   * Variante que recebe a lista já filtrada, evitando nova consulta ao banco quando o chamador
   * já carregou os lançamentos.
   */
  public String generateArquivoParcial(List<LancamentoParteB> active, Integer fiscalYear) {
    if (active.isEmpty()) {
      throw new IllegalArgumentException(
          "Nenhum lançamento da Parte B ativo encontrado para o ano " + fiscalYear);
    }

    List<String> lines = new ArrayList<>();
    lines.addAll(generateGrupoIrpj(active, fiscalYear));
    lines.addAll(generateGrupoCsll(active, fiscalYear));
    lines.addAll(generateGrupo3ParteB(active));

    lines.add(String.format("|M990|%d|", lines.size() + 1));

    return String.join("\n", lines) + "\n";
  }

  /**
   * Gera linhas do Grupo 1 — IRPJ (M030/M300/M305/M310).
   * Package-private para facilitar testes unitários.
   */
  List<String> generateGrupoIrpj(List<LancamentoParteB> active, Integer fiscalYear) {
    return generateGrupo(active, fiscalYear, TipoApuracao.IRPJ, "M300", "M305", "M310");
  }

  /**
   * Gera linhas do Grupo 2 — CSLL (M030/M350/M355/M360).
   * Package-private para facilitar testes unitários.
   */
  List<String> generateGrupoCsll(List<LancamentoParteB> active, Integer fiscalYear) {
    return generateGrupo(active, fiscalYear, TipoApuracao.CSLL, "M350", "M355", "M360");
  }

  private List<String> generateGrupo(
      List<LancamentoParteB> allActive, Integer fiscalYear,
      TipoApuracao tipoApuracao, String regPai, String regFilhoParteB, String regFilhoContabil) {

    List<LancamentoParteB> filtered = allActive.stream()
        .filter(l -> l.getTipoApuracao() == tipoApuracao)
        .collect(Collectors.toList());

    if (filtered.isEmpty()) {
      return new ArrayList<>();
    }

    // Group by mesReferencia (sorted ascending)
    Map<Integer, List<LancamentoParteB>> byMes = new TreeMap<>(
        filtered.stream().collect(Collectors.groupingBy(LancamentoParteB::getMesReferencia)));

    List<String> lines = new ArrayList<>();

    for (Map.Entry<Integer, List<LancamentoParteB>> mesEntry : byMes.entrySet()) {
      int mes = mesEntry.getKey();
      List<LancamentoParteB> lancamentos = mesEntry.getValue();

      // M030
      LocalDate inicio = LocalDate.of(fiscalYear, mes, 1);
      LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
      String codigoApuracao = "A" + String.format("%02d", mes);
      lines.add(String.format("|M030|%s|%s|%s|",
          inicio.format(DATE_FORMAT), fim.format(DATE_FORMAT), codigoApuracao));

      // Group by parametroTributarioId (same code = same codigoEnquadramento)
      Map<Long, List<LancamentoParteB>> byParametro = lancamentos.stream()
          .collect(Collectors.groupingBy(
              LancamentoParteB::getParametroTributarioId,
              LinkedHashMap::new,
              Collectors.toList()));

      for (Map.Entry<Long, List<LancamentoParteB>> paramEntry : byParametro.entrySet()) {
        Long parametroId = paramEntry.getKey();
        List<LancamentoParteB> grupo = paramEntry.getValue();

        TaxParameter parametro = taxParameterRepo.findById(parametroId)
            .orElseThrow(() -> new IllegalArgumentException(
                "ParametroTributario não encontrado: " + parametroId));

        String codigoEnquadramento = parametro.getCode();
        String descricao = parametro.getDescription();
        TipoAjuste tipoAjuste = grupo.get(0).getTipoAjuste();
        String tipoAjusteStr = tipoAjuste == TipoAjuste.ADICAO ? "A" : "E";
        String indicador = determineIndicador(grupo);
        BigDecimal somaValores = grupo.stream()
            .map(LancamentoParteB::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String historico = grupo.get(0).getDescricao();

        lines.add(String.format("|%s|%s|%s|%s|%s|%s|%s|",
            regPai, codigoEnquadramento, descricao, tipoAjusteStr,
            indicador, formatValor(somaValores), historico));

        // Child records (M305/M355 and M310/M360) per lançamento
        for (LancamentoParteB lanc : grupo) {
          String dc = lanc.getTipoAjuste() == TipoAjuste.ADICAO ? "D" : "C";

          if (lanc.getTipoRelacionamento() == TipoRelacionamento.CONTA_PARTE_B
              || lanc.getTipoRelacionamento() == TipoRelacionamento.AMBOS) {
            ContaParteB conta = contaParteBRepo.findById(lanc.getContaParteBId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "ContaParteB não encontrada: " + lanc.getContaParteBId()));
            lines.add(String.format("|%s|%s|%s|%s|",
                regFilhoParteB, conta.getCodigoConta(), formatValor(lanc.getValor()), dc));
          }

          if (lanc.getTipoRelacionamento() == TipoRelacionamento.CONTA_CONTABIL
              || lanc.getTipoRelacionamento() == TipoRelacionamento.AMBOS) {
            PlanoDeContas plano = planoDeContasRepo.findById(lanc.getContaContabilId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "PlanoDeContas não encontrado: " + lanc.getContaContabilId()));
            lines.add(String.format("|%s|%s||%s|%s|",
                regFilhoContabil, plano.getCode(), formatValor(lanc.getValor()), dc));
          }
        }
      }
    }

    return lines;
  }

  /**
   * Gera linhas do Grupo 3 — Contas da Parte B (M400/M410/M405).
   *
   * <p>⚠️ Campos dos registros M400/M410/M405 são baseados em aproximação —
   * confirmar layout exato contra o manual oficial SPED ECF antes de homologar.
   *
   * <p>Package-private para facilitar testes unitários.
   */
  List<String> generateGrupo3ParteB(List<LancamentoParteB> active) {
    // Collect unique ContaParteB IDs referenced in lançamentos
    Set<Long> contaIdsUsadas = active.stream()
        .filter(l -> l.getContaParteBId() != null)
        .map(LancamentoParteB::getContaParteBId)
        .collect(Collectors.toSet());

    List<String> lines = new ArrayList<>();

    for (Long contaId : contaIdsUsadas) {
      ContaParteB conta = contaParteBRepo.findById(contaId)
          .orElseThrow(() -> new IllegalArgumentException(
              "ContaParteB não encontrada: " + contaId));

      List<LancamentoParteB> lancamentosDaConta = active.stream()
          .filter(l -> contaId.equals(l.getContaParteBId()))
          .collect(Collectors.toList());

      // M400 — natureza da conta (⚠️ confirmar campos no SPED ECF manual)
      lines.add(String.format("|M400|%s|%s|%s|",
          conta.getCodigoConta(), conta.getDescricao(), conta.getTipoTributo().name()));

      // M410 — lançamentos (⚠️ confirmar campos no SPED ECF manual)
      for (LancamentoParteB lanc : lancamentosDaConta) {
        String dc = lanc.getTipoAjuste() == TipoAjuste.ADICAO ? "D" : "C";
        lines.add(String.format("|M410|%s|%02d/%d|%s|%s|%s|%s|",
            conta.getCodigoConta(),
            lanc.getMesReferencia(), lanc.getAnoReferencia(),
            lanc.getTipoAjuste().name(),
            formatValor(lanc.getValor()), dc,
            lanc.getDescricao()));
      }

      // M405 — saldo da conta (⚠️ confirmar campos no SPED ECF manual)
      BigDecimal totalAdic = lancamentosDaConta.stream()
          .filter(l -> l.getTipoAjuste() == TipoAjuste.ADICAO)
          .map(LancamentoParteB::getValor)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal totalExcl = lancamentosDaConta.stream()
          .filter(l -> l.getTipoAjuste() == TipoAjuste.EXCLUSAO)
          .map(LancamentoParteB::getValor)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal saldoAnterior = conta.getSaldoInicial() != null
          ? conta.getSaldoInicial() : BigDecimal.ZERO;
      BigDecimal saldoAtual = saldoAnterior.add(totalAdic).subtract(totalExcl);

      lines.add(String.format("|M405|%s|%s|%s|%s|%s|",
          conta.getCodigoConta(),
          formatValor(saldoAnterior), formatValor(totalAdic),
          formatValor(totalExcl), formatValor(saldoAtual)));
    }

    return lines;
  }

  private String determineIndicador(List<LancamentoParteB> grupo) {
    boolean allContaParteB = grupo.stream()
        .allMatch(l -> l.getTipoRelacionamento() == TipoRelacionamento.CONTA_PARTE_B);
    boolean allContaContabil = grupo.stream()
        .allMatch(l -> l.getTipoRelacionamento() == TipoRelacionamento.CONTA_CONTABIL);

    if (allContaParteB) {
      return "1";
    }
    if (allContaContabil) {
      return "2";
    }
    return "3";
  }

  private String formatValor(BigDecimal valor) {
    if (valor == null) {
      return "0,00";
    }
    return String.format("%.2f", valor).replace(".", ",");
  }
}
