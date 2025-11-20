APIs-Externas

### BrasilAPI (Primary CNPJ Lookup)

- **Propósito:** Buscar dados cadastrais de CNPJ (razão social, situação cadastral, endereço)
- **Documentação:** https://brasilapi.com.br/docs#tag/CNPJ
- **Base URL:** `https://brasilapi.com.br/api`
- **Autenticação:** Nenhuma (API pública)
- **Rate Limits:** Não documentado oficialmente (~100 req/min estimado)

**Endpoints Utilizados:**
- `GET /cnpj/v1/{cnpj}` - Buscar dados de CNPJ (14 dígitos sem formatação)

**Exemplo de Resposta:**
```json
{
  "cnpj": "19131243000197",
  "razao_social": "EMPRESAS TESTE LTDA",
  "nome_fantasia": "TESTE",
  "cnae_fiscal": 6204000,
  "cnae_fiscal_descricao": "Consultoria em tecnologia da informação",
  "situacao_cadastral": "ATIVA",
  "data_situacao_cadastral": "2005-11-03",
  "municipio": "SAO PAULO",
  "uf": "SP"
}
```

**Notas de Integração:**
- Timeout: 10 segundos (read timeout)
- Retry: 3 tentativas com exponential backoff (2s, 4s)
- Fallback: ReceitaWS em caso de timeout/503
- Cache: Redis 24h TTL (futuro - otimização)

---

### ReceitaWS API (Fallback CNPJ)

- **Propósito:** Fallback quando BrasilAPI falha/timeout
- **Documentação:** https://receitaws.com.br/api
- **Base URL:** `https://www.receitaws.com.br/v1`
- **Autenticação:** Nenhuma (API pública)
- **Rate Limits:** 3 requests/minuto (muito restritivo - por isso é fallback)

**Endpoints Utilizados:**
- `GET /cnpj/{cnpj}` - Buscar CNPJ

**Exemplo de Resposta:**
```json
{
  "cnpj": "19.131.243/0001-97",
  "nome": "EMPRESAS TESTE LTDA",
  "fantasia": "TESTE",
  "atividade_principal": [
    {
      "code": "62.04-0-00",
      "text": "Consultoria em tecnologia da informação"
    }
  ],
  "situacao": "ATIVA",
  "municipio": "SAO PAULO",
  "uf": "SP"
}
```

**Notas de Integração:**
- Timeout: 15 segundos (API mais lenta que BrasilAPI)
- Apenas usado em fallback (não primário devido a rate limits)
- Log WARN quando fallback ativado

---

### SPED PVA (Manual Validation)

- **Propósito:** Validação final do ECF gerado antes de transmissão oficial (software desktop Receita Federal)
- **Tipo:** Software desktop offline - SEM integração automatizada
- **Download:** https://www.gov.br/receitafederal/pt-br/assuntos/orientacao-tributaria/declaracoes-e-demonstrativos/ecf

**Processo Manual:**
1. Sistema gera ECF completo (.txt)
2. Usuário faz download do arquivo
3. Usuário abre SPED PVA (software desktop)
4. Usuário importa arquivo no PVA
5. PVA valida e aponta erros (se houver)
6. Usuário corrige dados no sistema, gera novo ECF
7. Repetir até PVA validar com sucesso
8. Usuário transmite via PVA (assinatura digital e-CPF/e-CNPJ)

**Validações Internas (Sistema):**
Sistema implementa ~80% das validações do PVA internamente:
- Campos obrigatórios preenchidos
- Formatos de registros corretos (pipe-delimited)
- Sequência de registros (M001 → M300 → M990)
- Somatórias de valores
- CNPJ válido

**Limitações:**
- Não valida assinatura digital (PVA)
- Não valida regras complexas da Receita (atualizadas frequentemente)
- Validação interna reduz ~80% dos erros, mas PVA é obrigatório para transmissão

---

