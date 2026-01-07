package br.com.lalurecf.domain.enums;

/**
 * Classe contábil para estruturação ECF.
 *
 * <p>Classificação mais detalhada da conta conforme layout oficial da ECF (Escrituração Contábil
 * Fiscal).
 */
public enum ClasseContabil {
  /** Ativo Circulante. */
  ATIVO_CIRCULANTE,

  /** Ativo Não Circulante. */
  ATIVO_NAO_CIRCULANTE,

  /** Passivo Circulante. */
  PASSIVO_CIRCULANTE,

  /** Passivo Não Circulante. */
  PASSIVO_NAO_CIRCULANTE,

  /** Patrimônio Líquido. */
  PATRIMONIO_LIQUIDO,

  /** Receita Bruta. */
  RECEITA_BRUTA,

  /** Deduções da Receita. */
  DEDUCOES_RECEITA,

  /** Custos. */
  CUSTOS,

  /** Despesas Operacionais. */
  DESPESAS_OPERACIONAIS,

  /** Outras Receitas. */
  OUTRAS_RECEITAS,

  /** Outras Despesas. */
  OUTRAS_DESPESAS,

  /** Resultado Financeiro. */
  RESULTADO_FINANCEIRO
}
