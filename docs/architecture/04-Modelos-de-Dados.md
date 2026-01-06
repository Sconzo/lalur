# 4. Modelos de Dados

> **Nota:** Estes modelos foram atualizados conforme [ADR-001: Simplificação do Modelo de Dados](adr-001-simplificacao-modelo-dados.md) ✅
>
> **Versão atual:** v2.0 (2025-10-25) - Modelo simplificado aprovado
>
> **Nomenclatura:** Campos Java em camelCase, colunas DB em snake_case (ex: `firstName` → `primeiro_nome`)

### User (Usuário)

**Tabela DB:** `tb_usuario`

**Propósito:** Representa usuários do sistema (ADMIN e CONTADOR) com autenticação JWT e controle de acesso baseado em roles.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `firstName` | `primeiro_nome` | String | Nome do usuário |
| `lastName` | `sobrenome` | String | Sobrenome do usuário |
| `email` | `email` | String (unique) | Email para login |
| `password` | `senha` | String | Hash BCrypt (strength 12) |
| `role` | `funcao` | String | 'ADMIN' ou 'CONTADOR' |
| `mustChangePassword` | `deve_mudar_senha` | Boolean | Forçar troca de senha (default: true) |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' (soft delete) |
| `createdAt` | `criado_em` | Timestamp | Auditoria - quando criado |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria - última atualização |
| `createdBy` | `criado_por` | Long | Auditoria - quem criou (FK User) |
| `updatedBy` | `atualizado_por` | Long | Auditoria - quem atualizou (FK User) |

**Relacionamentos:**
- Nenhum relacionamento direto (autenticação via email único)

**Decisões de Design:**
- Email como username (simplicidade, padrão da indústria)
- Nomenclatura snake_case no banco conforme ADR-001

---

### Company (Empresa)

**Tabela DB:** `tb_empresa`

**Propósito:** Representa empresas gerenciadas no sistema (multi-tenant). CONTADOR acessa apenas suas empresas, ADMIN gerencia todas.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `cnpj` | `cnpj` | String (14) | CNPJ sem formatação (unique) |
| `razaoSocial` | `razao_social` | String | Razão social oficial |
| `periodoContabil` | `periodo_contabil` | Date | Período contábil (data de corte para edições) |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' |
| `createdAt` | `criado_em` | Timestamp | Auditoria |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria |
| `createdBy` | `criado_por` | Long | Auditoria (FK User) |
| `updatedBy` | `atualizado_por` | Long | Auditoria (FK User) |

**Relacionamentos:**
- One-to-Many com ChartOfAccount
- One-to-Many com LancamentoContabil (substitui AccountingData)
- One-to-Many com ContaParteB
- One-to-Many com LancamentoParteB
- One-to-Many com FiscalMovement
- One-to-Many com TaxCalculationResult
- One-to-Many com EcfFile
- **Many-to-Many com TaxParameter** via `CompanyTaxParameter` (ADR-001)

**Decisões de Design:**
- Dados com competência anterior ao `periodoContabil` não podem ser editados (locked)
- CNPJ validado via BrasilAPI/ReceitaWS em criação
- Relacionamento com TaxParameter através de tabela explícita com auditoria (ADR-001)

---

### ChartOfAccount (Plano de Contas Contábil)

**Tabela DB:** `tb_plano_conta_contabil`

**Propósito:** Plano de contas contábil por empresa (estrutura flat, sem hierarquia parent/child para simplicidade), vinculado obrigatoriamente a Contas Referenciais RFB para compliance com estrutura oficial ECF.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `companyId` | `empresa_id` | Long | FK → tb_empresa (NOT NULL) |
| `contaReferencialId` | `conta_referencial_id` | Long | FK → tb_conta_referencial (NOT NULL) |
| `code` | `codigo` | String | Código da conta (ex: "1.01.01.001") |
| `name` | `nome` | String | Nome da conta (ex: "Caixa") |
| `accountType` | `tipo` | String | ATIVO, PASSIVO, PATRIMONIO_LIQUIDO, RECEITA, DESPESA, CUSTO, RESULTADO, COMPENSACAO, ATIVO_RETIFICADORA, PASSIVO_RETIFICADORA |
| `classe` | `classe` | String | Classe contábil ECF (ex: "ATIVO_CIRCULANTE", "RECEITA_BRUTA") |
| `nivel` | `nivel` | Integer | Nível hierárquico (1-5) para estruturação ECF |
| `natureza` | `natureza` | String | Natureza da conta: "DEVEDORA" ou "CREDORA" |
| `afetaResultado` | `afeta_resultado` | Boolean | Indica se conta afeta DRE/resultado |
| `dedutivel` | `dedutivel` | Boolean | Indica se despesa é dedutível fiscalmente |
| `fiscalYear` | `ano_fiscal` | Integer | Ano fiscal da conta (para versionamento anual) |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' |
| `createdAt` | `criado_em` | Timestamp | Auditoria |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria |
| `createdBy` | `criado_por` | Long | Auditoria (FK User) |
| `updatedBy` | `atualizado_por` | Long | Auditoria (FK User) |

**Relacionamentos:**
- Many-to-One com Company
- **Many-to-One com ContaReferencial (obrigatório)**
- One-to-Many com LancamentoContabil (como contaDebito e contaCredito)
- One-to-Many com LancamentoParteB (opcional)

**Constraints:**
- `UNIQUE (empresa_id, codigo, ano_fiscal)` - códigos únicos por empresa por ano
- `FK empresa_id REFERENCES tb_empresa(id) ON DELETE CASCADE`
- `FK conta_referencial_id REFERENCES tb_conta_referencial(id) ON DELETE RESTRICT`

**Decisões de Design:**
- **Estrutura flat** (sem parent/child) para simplicidade de queries
- **Vínculo obrigatório com ContaReferencial** garante compliance com layout ECF oficial RFB
- **Campos ECF-specific** (classe, nivel, natureza, afetaResultado, dedutivel) necessários para geração correta da Parte M
- **Versionamento anual** via fiscalYear permite plano de contas diferente por ano
- **AccountType** permanece como categorização interna (não substituído por classe)
- **ON DELETE RESTRICT** em conta_referencial previne exclusão acidental de contas mestras em uso

---

### ContaReferencial (Conta Referencial RFB)

**Tabela DB:** `tb_conta_referencial`

**Propósito:** Tabela mestra de Contas Referenciais da Receita Federal Brasil (RFB) - estrutura oficial ECF. Gerenciada exclusivamente por usuários ADMIN. Garante que plano de contas contábil esteja em compliance com layout oficial SPED.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `codigoRfb` | `codigo_rfb` | String (unique) | Código oficial RFB (ex: "1.01.01", "3.01") |
| `descricao` | `descricao` | String | Descrição oficial da conta referencial |
| `anoValidade` | `ano_validade` | Integer (nullable) | Ano de validade (null = válido para todos anos) |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' |
| `createdAt` | `criado_em` | Timestamp | Auditoria |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria |
| `createdBy` | `criado_por` | Long | Auditoria (FK User - ADMIN only) |
| `updatedBy` | `atualizado_por` | Long | Auditoria (FK User - ADMIN only) |

**Relacionamentos:**
- One-to-Many com ChartOfAccount (conta contábil deve referenciar uma conta oficial)

**Constraints:**
- `UNIQUE (codigo_rfb, ano_validade)` - código RFB único por ano (suporta mudanças anuais no layout ECF)

**Decisões de Design:**
- **Tabela mestra global** - não vinculada a empresas específicas (todas empresas usam mesma referência RFB)
- **CRUD restrito a ADMIN** - apenas administradores podem adicionar/editar contas oficiais
- **anoValidade opcional** - permite versionamento quando RFB altera estrutura de contas entre anos fiscais
- **ON DELETE RESTRICT** implícito via ChartOfAccount FK - não pode deletar conta referencial se em uso

---

### LancamentoContabil (Lançamento Contábil)

**Tabela DB:** `tb_lancamentos_contabeis`

**Propósito:** Lançamentos contábeis com método de partidas dobradas (débito/crédito). Substitui AccountingData para permitir CRUD manual completo além de importação CSV. Cada lançamento representa uma transação contábil com conta de débito, conta de crédito e valor.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `companyId` | `empresa_id` | Long | FK → tb_empresa (NOT NULL) |
| `contaDebitoId` | `conta_debito_id` | Long | FK → tb_plano_conta_contabil (NOT NULL) |
| `contaCreditoId` | `conta_credito_id` | Long | FK → tb_plano_conta_contabil (NOT NULL) |
| `data` | `data` | Date | Data do lançamento |
| `valor` | `valor` | Decimal(19,2) | Valor da transação (sempre positivo) |
| `historico` | `historico` | Text | Descrição/histórico do lançamento |
| `numeroDocumento` | `numero_documento` | String (nullable) | Número do documento (NF, recibo, etc.) |
| `fiscalYear` | `ano_fiscal` | Integer | Ano fiscal do lançamento |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' |
| `createdAt` | `criado_em` | Timestamp | Auditoria |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria |
| `createdBy` | `criado_por` | Long | Auditoria (FK User) |
| `updatedBy` | `atualizado_por` | Long | Auditoria (FK User) |

**Relacionamentos:**
- Many-to-One com Company
- Many-to-One com ChartOfAccount (contaDebito - cada lançamento referencia 2 contas)
- Many-to-One com ChartOfAccount (contaCredito - cada lançamento referencia 2 contas)

**Constraints:**
- `FK empresa_id REFERENCES tb_empresa(id) ON DELETE CASCADE`
- `FK conta_debito_id REFERENCES tb_plano_conta_contabil(id) ON DELETE RESTRICT`
- `FK conta_credito_id REFERENCES tb_plano_conta_contabil(id) ON DELETE RESTRICT`
- `CHECK (valor > 0)` - valores sempre positivos
- `CHECK (conta_debito_id <> conta_credito_id)` - débito e crédito devem ser contas diferentes
- Index em `(empresa_id, data, ano_fiscal)` para queries de período

**Decisões de Design:**
- **Partidas dobradas puras** - cada lançamento tem exatamente 1 débito e 1 crédito (simplicidade)
- **Valor sempre positivo** - natureza (débito/crédito) determinada pelas FKs, não por sinais +/-
- **CRUD completo habilitado** - permite lançamentos manuais além de importação CSV
- **Validação de Período Contábil** - data do lançamento deve respeitar período contábil da empresa
- **Aggregation para saldos** - saldos de contas calculados via SUM de lançamentos (não armazenados)
- **ON DELETE RESTRICT** em contas - previne exclusão de conta se tiver lançamentos associados
- **historico obrigatório** - rastreabilidade e auditoria fiscal

---

### TaxParameter (Parâmetro Tributário)

**Tabela DB:** `tb_parametros_tributarios`

**Propósito:** Parâmetros tributários globais gerenciados por ADMIN (ex: tipos de ajustes IRPJ, CSLL).

**⚠️ Mudança ADR-001:** Estrutura simplificada (flat) - hierarquia parent/child removida.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `code` | `codigo` | String (unique) | Código único (ex: "IRPJ_ADICAO_01") |
| `type` | `tipo` | String | Categoria ('IRPJ', 'CSLL', 'GERAL', etc.) |
| `description` | `descricao` | Text | Descrição detalhada |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' |
| `createdAt` | `criado_em` | Timestamp | Auditoria |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria |
| `createdBy` | `criado_por` | Long | Auditoria (FK User) |
| `updatedBy` | `atualizado_por` | Long | Auditoria (FK User) |

**Campos Removidos (ADR-001):**
- ❌ `parent` / `children` (hierarquia self-referential)
- ❌ `configuration` (JSON complexo)
- ❌ `name` (redundante com description)
- ❌ `valueType`, `valueText`, `effectiveFrom`, `effectiveUntil` (over-engineering para MVP)

**Relacionamentos:**
- **Many-to-Many com Company** via `CompanyTaxParameter` (ADR-001)

**Decisões de Design:**
- **Estrutura flat** (sem hierarquia) - queries O(1), simplicidade (ADR-001)
- Campo `type` para categorização flexível (String livre, não enum)
- Associação com empresas através de tabela explícita com auditoria

---

### CompanyTaxParameter (Associação Empresa ↔ Parâmetro Tributário)

**Tabela DB:** `tb_empresa_parametros_tributarios`

**Propósito:** Tabela associativa explícita com auditoria que gerencia quais parâmetros tributários se aplicam a cada empresa.

**✨ Novidade ADR-001:** Entity própria com auditoria completa (substitui `@ManyToMany` automático do JPA).

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `companyId` | `empresa_id` | Long | FK → tb_empresa (NOT NULL) |
| `taxParameterId` | `parametro_tributario_id` | Long | FK → tb_parametros_tributarios (NOT NULL) |
| `createdBy` | `criado_por` | Long | Quem associou (FK User) |
| `createdAt` | `criado_em` | Timestamp | Quando foi associado |

**Constraints:**
- `UNIQUE (empresa_id, parametro_tributario_id)` - previne duplicatas
- `FK empresa_id REFERENCES tb_empresa(id) ON DELETE CASCADE`
- `FK parametro_tributario_id REFERENCES tb_parametros_tributarios(id) ON DELETE RESTRICT`

**Relacionamentos:**
- Many-to-One com Company
- Many-to-One com TaxParameter

**Decisões de Design (ADR-001):**
- **PK própria** (`id`) ao invés de PK composta - facilita ORM
- **Auditoria completa** - rastreabilidade de quem associou e quando
- **Sem `updatedAt`/`updatedBy`** - associações são imutáveis (delete + insert para alterar)
- **ON DELETE CASCADE** em company - se empresa for deletada, associações vão junto
- **ON DELETE RESTRICT** em tax_parameter - não pode deletar parâmetro se associado a empresa

**Exemplo de Uso:**
```java
// ADMIN associa parâmetros IRPJ a empresa
CompanyTaxParameter ctp1 = new CompanyTaxParameter();
ctp1.setCompanyId(10L);
ctp1.setTaxParameterId(5L); // "IRPJ_ADICAO_01"
ctp1.setCreatedBy(1L); // ADMIN user ID
ctp1.setCreatedAt(LocalDateTime.now());
repository.save(ctp1);
```

---

### ValorParametroTemporal (Valor Temporal de Parâmetro)

**Tabela DB:** `tb_valores_parametros_temporais`

**Propósito:** Armazena períodos (mensais ou trimestrais) em que determinada associação empresa↔parâmetro está ativa/selecionada. Usado para parâmetros como "Forma de Estimativa Mensal" e "Tipo de Tributação no Período" que variam ao longo do ano fiscal.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `empresaParametroId` | `empresa_parametros_tributarios_id` | Long | FK → tb_empresa_parametros_tributarios (NOT NULL) |
| `ano` | `ano` | Integer | Ano fiscal (ex: 2024) |
| `mes` | `mes` | Integer | Mês (1-12) se periodicidade mensal, NULL se trimestral |
| `trimestre` | `trimestre` | Integer | Trimestre (1-4) se periodicidade trimestral, NULL se mensal |

**Constraints:**
- `UNIQUE (empresa_parametros_tributarios_id, ano, mes, trimestre)` - previne duplicatas
- `CHECK (mes IS NOT NULL AND trimestre IS NULL) OR (mes IS NULL AND trimestre IS NOT NULL)` - exatamente um dos dois deve estar preenchido
- `FK empresa_parametros_tributarios_id REFERENCES tb_empresa_parametros_tributarios(id) ON DELETE CASCADE`

**Relacionamentos:**
- Many-to-One com CompanyTaxParameter (relacionamento chave)

**Decisões de Design (ADR-001):**
- **Simplicidade por presença**: A existência de um registro indica que o parâmetro está ativo naquele período
- **Sem campo "valor"**: O próprio FK `empresa_parametros_tributarios_id` carrega a informação de qual opção foi selecionada
- **Periodicidade flexível**: Suporta tanto valores mensais quanto trimestrais via campos mutuamente exclusivos
- **Cascade delete**: Se associação empresa↔parâmetro for removida, todos períodos são removidos automaticamente
- **Unicidade temporal**: Constraint garante que não há duplicatas para o mesmo período

**Exemplo de Uso:**
```java
// Empresa X escolheu "Lucro Real" como forma de tributação em Jan/2024
ValorParametroTemporal vpt = new ValorParametroTemporal();
vpt.setEmpresaParametroId(42L); // Associação já existe: Empresa X ↔ "Lucro Real"
vpt.setAno(2024);
vpt.setMes(1);
vpt.setTrimestre(null);
repository.save(vpt);

// Empresa Y trabalha com estimativa trimestral - 1º trimestre de 2024
ValorParametroTemporal vpt2 = new ValorParametroTemporal();
vpt2.setEmpresaParametroId(55L); // Associação: Empresa Y ↔ "Estimativa Trimestral"
vpt2.setAno(2024);
vpt2.setMes(null);
vpt2.setTrimestre(1);
repository.save(vpt2);
```

**Query Exemplo - Listar períodos ativos de uma empresa:**
```sql
SELECT
    e.razao_social,
    tp.codigo,
    tp.descricao,
    vpt.ano,
    COALESCE(vpt.mes::text, 'T' || vpt.trimestre) as periodo
FROM tb_valores_parametros_temporais vpt
JOIN tb_empresa_parametros_tributarios ept ON vpt.empresa_parametros_tributarios_id = ept.id
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios tp ON ept.parametro_tributario_id = tp.id
WHERE e.id = 123
  AND vpt.ano = 2024
ORDER BY vpt.ano, COALESCE(vpt.mes, vpt.trimestre * 3);
```

---

### ContaParteB (Conta da Parte B - e-Lalur/e-Lacs)

**Tabela DB:** `tb_contas_parte_b`

**Propósito:** Contas específicas da Parte B (e-Lalur para IRPJ e e-Lacs para CSLL). Estas contas são usadas para lançamentos de ajustes fiscais que não fazem parte do plano de contas contábil regular, mas são necessárias para apuração de IRPJ/CSLL conforme legislação fiscal.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `companyId` | `empresa_id` | Long | FK → tb_empresa (NOT NULL) |
| `codigoConta` | `codigo_conta` | String | Código da conta Parte B (ex: "4.01.01") |
| `descricao` | `descricao` | String | Descrição da conta |
| `anoBase` | `ano_base` | Integer | Ano base de criação/referência |
| `dataVigenciaInicio` | `data_vigencia_inicio` | Date | Data início de vigência |
| `dataVigenciaFim` | `data_vigencia_fim` | Date (nullable) | Data fim de vigência (null = vigente) |
| `tipoTributo` | `tipo_tributo` | String | "IRPJ", "CSLL" ou "AMBOS" |
| `saldoInicial` | `saldo_inicial` | Decimal(19,2) | Saldo inicial da conta |
| `tipoSaldo` | `tipo_saldo` | String | "DEVEDOR" ou "CREDOR" |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' |
| `createdAt` | `criado_em` | Timestamp | Auditoria |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria |
| `createdBy` | `criado_por` | Long | Auditoria (FK User) |
| `updatedBy` | `atualizado_por` | Long | Auditoria (FK User) |

**Relacionamentos:**
- Many-to-One com Company
- One-to-Many com LancamentoParteB

**Constraints:**
- `UNIQUE (empresa_id, codigo_conta, ano_base)` - código único por empresa por ano
- `FK empresa_id REFERENCES tb_empresa(id) ON DELETE CASCADE`
- `CHECK (tipo_tributo IN ('IRPJ', 'CSLL', 'AMBOS'))`
- `CHECK (tipo_saldo IN ('DEVEDOR', 'CREDOR'))`

**Decisões de Design:**
- **Separação clara entre contas contábeis e fiscais** - ContaParteB não está no plano de contas contábil
- **Vigência temporal** - contas podem ter período de validade (comum em legislação fiscal)
- **Tipo tributo flexível** - conta pode ser específica de IRPJ, CSLL ou aplicável a ambos
- **Saldo inicial configurável** - importante para carry-forward de exercícios anteriores
- **Versionamento anual** - permite estrutura diferente por ano fiscal

---

### LancamentoParteB (Lançamento da Parte B)

**Tabela DB:** `tb_lancamentos_parte_b`

**Propósito:** Lançamentos de ajustes fiscais na Parte B (e-Lalur/e-Lacs). Registra adições, exclusões e outras movimentações fiscais vinculadas a contas contábeis, contas da Parte B ou ambas, referenciando parâmetros tributários que fundamentam cada ajuste.

**Atributos Principais:**

| Campo Java | Coluna DB | Tipo | Descrição |
|------------|-----------|------|-----------|
| `id` | `id` | Long | PK, auto-increment |
| `companyId` | `empresa_id` | Long | FK → tb_empresa (NOT NULL) |
| `mesReferencia` | `mes_referencia` | Integer | Mês de referência (1-12) |
| `anoReferencia` | `ano_referencia` | Integer | Ano de referência |
| `tipoApuracao` | `tipo_apuracao` | String | "IRPJ" ou "CSLL" |
| `tipoRelacionamento` | `tipo_relacionamento` | String | "CONTA_CONTABIL", "CONTA_PARTE_B", "AMBOS" |
| `contaContabilId` | `conta_contabil_id` | Long (nullable) | FK → tb_plano_conta_contabil |
| `contaParteBId` | `conta_parte_b_id` | Long (nullable) | FK → tb_contas_parte_b |
| `parametroTributarioId` | `parametro_tributario_id` | Long | FK → tb_parametros_tributarios (NOT NULL) |
| `tipoAjuste` | `tipo_ajuste` | String | "ADICAO" ou "EXCLUSAO" |
| `descricao` | `descricao` | Text | Descrição do ajuste fiscal |
| `valor` | `valor` | Decimal(19,2) | Valor do ajuste (sempre positivo) |
| `status` | `status` | String | 'ACTIVE' ou 'INACTIVE' |
| `createdAt` | `criado_em` | Timestamp | Auditoria |
| `updatedAt` | `atualizado_em` | Timestamp | Auditoria |
| `createdBy` | `criado_por` | Long | Auditoria (FK User) |
| `updatedBy` | `atualizado_por` | Long | Auditoria (FK User) |

**Relacionamentos:**
- Many-to-One com Company
- Many-to-One com ChartOfAccount (opcional, conforme tipoRelacionamento)
- Many-to-One com ContaParteB (opcional, conforme tipoRelacionamento)
- Many-to-One com TaxParameter (obrigatório)

**Constraints:**
- `FK empresa_id REFERENCES tb_empresa(id) ON DELETE CASCADE`
- `FK conta_contabil_id REFERENCES tb_plano_conta_contabil(id) ON DELETE RESTRICT`
- `FK conta_parte_b_id REFERENCES tb_contas_parte_b(id) ON DELETE RESTRICT`
- `FK parametro_tributario_id REFERENCES tb_parametros_tributarios(id) ON DELETE RESTRICT`
- `CHECK (tipo_apuracao IN ('IRPJ', 'CSLL'))`
- `CHECK (tipo_relacionamento IN ('CONTA_CONTABIL', 'CONTA_PARTE_B', 'AMBOS'))`
- `CHECK (tipo_ajuste IN ('ADICAO', 'EXCLUSAO'))`
- `CHECK (valor > 0)` - valores sempre positivos
- **Validação condicional de FKs:**
  - Se `tipo_relacionamento = 'CONTA_CONTABIL'` então `conta_contabil_id NOT NULL` e `conta_parte_b_id IS NULL`
  - Se `tipo_relacionamento = 'CONTA_PARTE_B'` então `conta_parte_b_id NOT NULL` e `conta_contabil_id IS NULL`
  - Se `tipo_relacionamento = 'AMBOS'` então `conta_contabil_id NOT NULL` e `conta_parte_b_id NOT NULL`
- Index em `(empresa_id, ano_referencia, tipo_apuracao)` para queries de apuração

**Decisões de Design:**
- **Flexibilidade de vinculação** - lançamento pode estar vinculado a conta contábil, conta Parte B ou ambas
- **Fundamentação via parâmetro tributário** - cada ajuste deve referenciar base legal/fiscal (obrigatório)
- **Tipo apuracao separado** - permite apurações IRPJ e CSLL independentes
- **Validação condicional via CHECK constraints** - garante integridade referencial baseada em tipo de relacionamento
- **Valor sempre positivo** - natureza (adição/exclusão) determinada pelo campo tipoAjuste
- **Mesma estrutura de auditoria** - rastreabilidade completa de quem criou/modificou ajustes fiscais
- **ON DELETE RESTRICT** em contas/parâmetros - previne exclusão se houver lançamentos associados
- **Complementa FiscalMovement** - LancamentoParteB é estruturado para ECF, FiscalMovement permanece para adições/exclusões genéricas Lalur/Lacs

---

### FiscalMovement (Movimento Fiscal LALUR/LACS)

**Propósito:** Adições e exclusões LALUR (IRPJ) e LACS (CSLL) que ajustam lucro contábil.

**Atributos Principais:**
- `id`: Long - PK
- `companyId`: Long - FK Company
- `movementType`: MovementType enum - LALUR ou LACS
- `classification`: Classification enum - ADICAO ou EXCLUSAO
- `fiscalYear`: Integer - Ano fiscal (ex: 2024)
- `description`: String - Descrição do movimento
- `amount`: BigDecimal - Valor do ajuste
- `legalBasis`: String (optional) - Fundamentação legal
- `status`: Status enum

**Relacionamentos:**
- Many-to-One com Company

**Decisões de Design:**
- Separação LALUR/LACS via enum para queries eficientes
- `legalBasis` opcional mas recomendado para auditoria

---

### TaxCalculationResult (Resultado de Cálculo)

**Propósito:** Armazena resultados de cálculos IRPJ/CSLL com memória de cálculo detalhada.

**Atributos Principais:**
- `id`: Long - PK
- `companyId`: Long - FK Company
- `calculationType`: CalculationType enum - IRPJ ou CSLL
- `fiscalYear`: Integer
- `baseCalculationAmount`: BigDecimal - Lucro Real ou Base CSLL
- `totalTaxDue`: BigDecimal - Imposto total devido
- `calculationMemory`: String (JSON) - Memória de cálculo com `fiscalMovementIds` (REFINAMENTO v1.0 - rastreabilidade)
- `calculatedBy`: String - Email do usuário que executou
- `calculatedAt`: LocalDateTime
- `isOutdated`: Boolean - Flag de invalidação (eventos)
- `status`: Status enum

**Relacionamentos:**
- Many-to-One com Company

**Constraints:**
- Unique constraint (companyId + calculationType + fiscalYear)

**Decisões de Design:**
- `calculationMemory` JSON inclui `fiscalMovementIds` array (refinamento v1.0) para rastrear quais movimentos foram usados
- `isOutdated` flag atualizada via eventos quando FiscalMovements mudam

**Exemplo calculationMemory JSON:**
```json
{
  "calculationId": "calc-123-irpj-2024",
  "timestamp": "2024-01-15T14:30:00Z",
  "calculatedBy": "contador@empresa.com",
  "fiscalMovementIds": [101, 102, 105, 110],
  "steps": [
    {"step": 1, "description": "Lucro Líquido (base inicial)", "value": 500000.00},
    {"step": 2, "description": "Adições LALUR", "value": 50000.00},
    {"step": 3, "description": "Exclusões LALUR", "value": -20000.00},
    {"step": 4, "description": "Lucro Real", "value": 530000.00},
    {"step": 5, "description": "IRPJ 15%", "value": 79500.00},
    {"step": 6, "description": "IRPJ Adicional 10%", "value": 29000.00},
    {"step": 7, "description": "IRPJ Total", "value": 108500.00}
  ]
}
```

---

### EcfFile (Arquivo ECF)

**Propósito:** Representa arquivos ECF (importados Parte A, gerados Parte M, ou completos merged).

**Atributos Principais:**
- `id`: Long - PK
- `companyId`: Long - FK Company
- `fileType`: EcfFileType enum - IMPORTED_ECF, GENERATED_M_FILE, COMPLETE_ECF
- `fileName`: String
- `filePath`: String - Path no filesystem ou S3
- `fileSizeBytes`: Long
- `fiscalYear`: Integer
- `recordCount`: Integer - Número de linhas/registros
- `sourceImportedEcfId`: Long (FK opcional) - Referência ao ECF importado (REFINAMENTO v1.0 - obrigatório se COMPLETE_ECF)
- `sourceMFileId`: Long (FK opcional) - Referência ao M file gerado (REFINAMENTO v1.0 - obrigatório se COMPLETE_ECF)
- `validationStatus`: ValidationStatus enum - NOT_VALIDATED, VALID, INVALID
- `validationErrors`: String (JSON array) - Erros de validação
- `status`: Status enum

**Relacionamentos:**
- Many-to-One com Company
- Self-referential (sourceImportedEcfId, sourceMFileId)

**Constraints:**
- **CHECK constraint (refinamento v1.0):** Se `fileType = COMPLETE_ECF`, então `sourceImportedEcfId` e `sourceMFileId` DEVEM ser NOT NULL

**Decisões de Design:**
- Referências obrigatórias para COMPLETE_ECF garantem rastreabilidade
- `@PrePersist` validation adicional em JPA Entity

---

