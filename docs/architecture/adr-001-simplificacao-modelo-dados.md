# ADR-001: Simplificação do Modelo de Dados

## Status

**APROVADO** ✅

**Data Proposta:** 2025-10-25
**Data Aprovação:** 2025-10-25
**Autor:** Winston (Architect Agent)
**Aprovado por:** Equipe de Desenvolvimento

## Contexto

Durante o planejamento inicial do projeto LALUR V2 ECF (Épicos 01 e 02), foi desenhado um modelo de dados com as seguintes características:

### Modelo Original (v1.0)

1. **Hierarquia em TaxParameter**
   - Estrutura self-referential (parent/child)
   - Campos: `id`, `code`, `name`, `description`, `type`, `configuration` (JSON), `parent_id`, `children`
   - Suportava árvores de parâmetros (ex: IRPJ-BASE → IRPJ-ALIQUOTA-15, IRPJ-ADICIONAL-10)

2. **Relacionamento Company ↔ TaxParameter**
   - `@ManyToMany` gerenciado automaticamente pelo JPA
   - Tabela de junção `company_tax_parameters` criada implicitamente
   - **SEM campos de auditoria** na tabela associativa

3. **User e Company**
   - Estrutura básica conforme planejado

### Motivação para Mudança

Após análise de requisitos reais e simplicidade de implementação no MVP, identificamos:

1. **Hierarquia de parâmetros tributários é over-engineering para MVP:**
   - Parâmetros fiscais brasileiros (IRPJ, CSLL, alíquotas) são relativamente planos
   - Complexidade de queries recursivas não justificada
   - Overhead de manutenção alto (endpoints `/roots`, `/{id}/children`)

2. **Auditoria na associação Empresa ↔ Parâmetros é crítica:**
   - Necessidade de rastrear **quem e quando** associou cada parâmetro a empresa
   - Conformidade com auditorias fiscais e LGPD
   - Tabela associativa implícita do JPA não permite campos extras

3. **Nomenclatura de banco de dados:**
   - Preferência por snake_case nas tabelas (padrão PostgreSQL)
   - Melhor legibilidade em queries SQL diretas

## Decisão

**Simplificar o modelo de dados** com as seguintes mudanças:

### 1. Remover Hierarquia de TaxParameter

**Antes:**
```sql
CREATE TABLE tax_parameters (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    configuration JSONB,
    parent_id BIGINT REFERENCES tax_parameters(id),
    -- campos auditoria
);
```

**Depois:**
```sql
CREATE TABLE tb_parametros_tributarios (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,           -- ex: 'IRPJ', 'CSLL', 'GERAL'
    codigo VARCHAR(255) UNIQUE NOT NULL,
    descricao TEXT,
    status VARCHAR(20) NOT NULL,
    criado_por BIGINT,
    criado_em TIMESTAMP,
    atualizado_por BIGINT,
    atualizado_em TIMESTAMP
);
```

**Mudanças:**
- ✅ Removido: `parent_id`, `children`, `configuration` (JSON)
- ✅ Simplificado: Estrutura flat com `tipo` como categoria
- ✅ Renomeado: snake_case (tb_*, criado_por, etc.)

### 2. Criar Tabela Associativa Explícita com Auditoria

**Antes:**
```sql
-- Tabela implícita criada pelo JPA @ManyToMany
CREATE TABLE company_tax_parameters (
    company_id BIGINT NOT NULL,
    tax_parameter_id BIGINT NOT NULL,
    PRIMARY KEY (company_id, tax_parameter_id)
);
```

**Depois:**
```sql
CREATE TABLE tb_empresa_parametros_tributarios (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES tb_empresa(id),
    parametro_tributario_id BIGINT NOT NULL REFERENCES tb_parametros_tributarios(id),
    criado_por BIGINT,
    criado_em TIMESTAMP,
    UNIQUE (empresa_id, parametro_tributario_id)
);
```

**Mudanças:**
- ✅ Adicionado: `id` como PK (entidade própria)
- ✅ Adicionado: `criado_por`, `criado_em` (auditoria)
- ✅ Mantido: UNIQUE constraint (empresa_id, parametro_tributario_id)

### 3. Padronização de Nomenclatura

Todas as tabelas e colunas seguem **snake_case**:

**Tabelas:**
- `tb_usuario` (User)
- `tb_empresa` (Company)
- `tb_parametros_tributarios` (TaxParameter)
- `tb_empresa_parametros_tributarios` (Company ↔ TaxParameter)
- `tb_valores_parametros_temporais` (Valores Temporais de Parâmetros) **✨ NOVA**

**Colunas (exemplo User):**
- `primeiro_nome` (firstName)
- `sobrenome` (lastName)
- `deve_mudar_senha` (mustChangePassword)
- `criado_por`, `criado_em`, `atualizado_por`, `atualizado_em`

## Modelo Completo Novo (DBML)

```dbml
Table tb_usuario {
  id integer [primary key]
  primeiro_nome varchar
  sobrenome varchar
  email varchar [unique]
  senha varchar
  funcao varchar  // 'ADMIN' ou 'CONTADOR'
  deve_mudar_senha bool
  status varchar
  criado_por integer
  criado_em timestamp
  atualizado_por integer
  atualizado_em timestamp
}

Table tb_empresa {
  id integer [primary key]
  cnpj varchar [unique]
  razao_social text
  periodo_contabil date
  status varchar
  criado_por integer
  criado_em timestamp
  atualizado_por integer
  atualizado_em timestamp
}

Table tb_parametros_tributarios {
  id integer [primary key]
  tipo varchar            // 'IRPJ', 'CSLL', 'GERAL'
  codigo varchar [unique]
  descricao text
  status varchar
  criado_por integer
  criado_em timestamp
  atualizado_por integer
  atualizado_em timestamp
}

Table tb_empresa_parametros_tributarios {
  id integer [primary key]
  empresa_id integer [ref: > tb_empresa.id]
  parametro_tributario_id integer [ref: > tb_parametros_tributarios.id]
  criado_por integer
  criado_em timestamp

  indexes {
    (empresa_id, parametro_tributario_id) [unique]
  }
}

Table tb_valores_parametros_temporais {
  id integer [primary key]
  empresa_parametros_tributarios_id integer [ref: > tb_empresa_parametros_tributarios.id]
  ano integer
  mes integer              // 1-12 para mensais, NULL para trimestrais
  trimestre integer        // 1-4 para trimestrais, NULL para mensais

  indexes {
    (empresa_parametros_tributarios_id, ano, mes, trimestre) [unique]
  }
}
```

**Nota sobre tb_valores_parametros_temporais:**
- **Propósito:** Armazena períodos em que determinada associação empresa↔parâmetro está ativa
- **Uso:** Para parâmetros que variam por período (ex: "Forma de Estimativa Mensal", "Tipo de Tributação")
- **Lógica:** Presença do registro = parâmetro ativo naquele período
- **Constraint:** Ou tem `mes` OU `trimestre`, nunca ambos

## Consequências

### Positivas ✅

1. **Simplicidade de Implementação**
   - Menos código: Não precisa de endpoints `/roots`, `/{id}/children`
   - Queries mais rápidas: Sem recursão, sem joins complexos
   - Menos testes: Redução de ~30% em testes de integração

2. **Auditoria Completa**
   - Rastreabilidade: Saber quem associou cada parâmetro a cada empresa
   - Conformidade: Atende auditorias fiscais e LGPD
   - Debugging: Facilita troubleshooting de configurações incorretas

3. **Performance**
   - Queries flat são O(1) vs recursivas O(n log n)
   - Índices mais eficientes sem hierarquia
   - Menor overhead de joins

4. **Manutenibilidade**
   - Modelo mental mais simples para novos desenvolvedores
   - Menos pontos de falha (sem validações de ciclos na hierarquia)
   - Evolução futura mais clara

### Negativas ❌

1. **Perda de Flexibilidade Hierárquica**
   - Se futuramente precisarmos de hierarquia, será necessário refatorar
   - **Mitigação:** Campo `tipo` pode ser expandido para pseudo-hierarquia (ex: "IRPJ.ALIQUOTA.BASE")

2. **Migração de Código Existente**
   - Épicos 01 e 02 precisam ser ajustados
   - **Mitigação:** Nenhum código implementado ainda, apenas planejamento

3. **Nomenclatura Mista (Java camelCase vs DB snake_case)**
   - Necessidade de mappers claros entre Entity fields e DB columns
   - **Mitigação:** MapStruct + JPA `@Column(name="...")` resolvem transparentemente

### Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Necessidade futura de hierarquia | Baixa | Médio | Campo `tipo` + refactor controlado |
| Confusão na nomenclatura | Baixa | Baixo | Documentação + code review rigoroso |
| Perda de auditoria em migrações | N/A | N/A | Não aplicável (sistema novo) |

## Impactos nos Épicos

### Epic 01: Fundação & Autenticação

**Impacto: MÍNIMO**

- ✅ **Story 1.3 (Auditoria):** Mantém campos de auditoria (createdBy, updatedBy) - **SEM MUDANÇAS**
- ✅ **Story 1.4 (User Entity):** Ajustar nomenclatura para snake_case:
  - `firstName` → mapear para coluna `primeiro_nome`
  - `lastName` → mapear para coluna `sobrenome`
  - `mustChangePassword` → `deve_mudar_senha`
- ✅ **Stories 1.5-1.11:** Sem impacto (lógica de auth independente de schema)

**Ações:**
1. Atualizar `@Column(name="...")` em `UserEntity`
2. Manter lógica de negócio inalterada

### Epic 02: Gestão de Empresas & Parâmetros

**Impacto: MODERADO A ALTO**

#### Story 2.1 (Company Entity)
- ✅ Ajustar nomenclatura: `razaoSocial` → `razao_social`, `periodoContabil` → `periodo_contabil`
- ✅ Remover campo `parametrosTributarios` (Many-to-Many) - será gerenciado pela tabela explícita

#### Story 2.6 (TaxParameter Entity) - **MUDANÇAS SIGNIFICATIVAS**

**Antes (Story 2.6 original):**
```java
@Entity
class TaxParameterEntity extends BaseEntity {
    String code;
    String name;
    String description;
    TaxParameterType type;
    String configuration; // JSON
    TaxParameterEntity parent;  // ❌ REMOVER
    List<TaxParameterEntity> children;  // ❌ REMOVER
}
```

**Depois (modelo simplificado):**
```java
@Entity
@Table(name = "tb_parametros_tributarios")
class TaxParameterEntity extends BaseEntity {
    @Column(name = "codigo", unique = true)
    String code;

    @Column(name = "tipo")  // ✅ NOVO
    String type;  // 'IRPJ', 'CSLL', 'GERAL'

    @Column(name = "descricao")
    String description;

    // ❌ REMOVER: configuration, parent, children
}
```

**Ações:**
1. Remover campos `parent`, `children`, `configuration`
2. Adicionar campo `type` (String, não enum) para flexibilidade
3. Atualizar todos `@Column(name="...")` para snake_case

#### Story 2.7 (CRUD TaxParameter) - **REMOVER ENDPOINTS**

**Endpoints a REMOVER:**
- ❌ `GET /api/v1/tax-parameters/roots` (sem hierarquia)
- ❌ `GET /api/v1/tax-parameters/{id}/children` (sem hierarquia)

**Validações a REMOVER:**
- ❌ Validação de parent existe e está ACTIVE
- ❌ Regra "não pode mudar pai se já tem filhos"

**Manter:**
- ✅ `POST /api/v1/tax-parameters` (criar parâmetro flat)
- ✅ `GET /api/v1/tax-parameters` (listar com filtro por `type`)
- ✅ `GET /api/v1/tax-parameters/{id}`
- ✅ `PUT /api/v1/tax-parameters/{id}`
- ✅ `PATCH /api/v1/tax-parameters/{id}/status`

#### Story 2.8 (Associação Empresa ↔ Parâmetros) - **NOVA ENTITY**

**CRIAR Nova Entity:**
```java
@Entity
@Table(name = "tb_empresa_parametros_tributarios")
class CompanyTaxParameterEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    Long id;

    @Column(name = "empresa_id")
    Long companyId;

    @Column(name = "parametro_tributario_id")
    Long taxParameterId;

    @Column(name = "criado_por")
    Long createdBy;

    @Column(name = "criado_em")
    LocalDateTime createdAt;

    // Unique constraint via @Table(uniqueConstraints = ...)
}
```

**Endpoints mantidos:**
- ✅ `PUT /api/v1/companies/{id}/tax-parameters` (associar parâmetros)
- ✅ `GET /api/v1/companies/{id}/tax-parameters` (listar parâmetros associados)

**Lógica ajustada:**
- Ao associar parâmetros, criar registros em `CompanyTaxParameterEntity` com auditoria
- Queries devem fazer JOIN triplo: Company → CompanyTaxParameter → TaxParameter

## Plano de Migração

### Fase 1: Atualização de Documentação ✅ COMPLETO
- [x] Criar ADR-001
- [x] Atualizar seção 04-Modelos-de-Dados.md
- [x] Atualizar seção 09-Database-Schema.md
- [x] Criar README.md em docs/architecture/
- [x] Fazer sharding da documentação (17 seções)

### Fase 2: Atualização dos Épicos ✅ COMPLETO
- [x] Revisar e ajustar Epic 01 (Stories 1.3, 1.4)
- [x] Revisar e ajustar Epic 02 (Stories 2.1, 2.6, 2.7, 2.8)
- [x] Criar seções de resumo de mudanças em ambos épicos
- [x] Adicionar referências ao ADR-001 em todas as stories afetadas

**Stories Atualizadas:**
- Epic 01 - Story 1.4: Nomenclatura snake_case
- Epic 02 - Story 2.1: Company com nomenclatura atualizada
- Epic 02 - Story 2.6: TaxParameter simplificado (sem hierarquia)
- Epic 02 - Story 2.7: CRUD sem endpoints de hierarquia
- Epic 02 - Story 2.8: Nova entity CompanyTaxParameter com auditoria

### Fase 3: Implementação (Próximo Passo)
- [ ] Implementar entities conforme novo modelo
- [ ] Criar migrations (se usar Flyway) ou executar DDL scripts
- [ ] Atualizar testes unitários e de integração
- [ ] Validar com time de desenvolvimento

## Alternativas Consideradas

### Alternativa 1: Manter Hierarquia com JSONB
**Descrição:** Usar campo `configuration` JSONB para armazenar hierarquia flexível

**Prós:**
- Flexibilidade total
- Queries rápidas com GIN index

**Contras:**
- Perda de type-safety
- Validações complexas
- Dificuldade em queries relacionais

**Decisão:** Rejeitada - Complexidade não justificada para MVP

### Alternativa 2: Hierarquia via Closure Table
**Descrição:** Tabela `tax_parameter_paths` com `ancestor_id`, `descendant_id`, `depth`

**Prós:**
- Queries recursivas O(1)
- Mantém type-safety

**Contras:**
- Overhead de manutenção (triggers)
- Over-engineering para MVP

**Decisão:** Rejeitada - Solução muito complexa para necessidade simples

### Alternativa 3: Híbrido (Hierarquia + Auditoria na Associação)
**Descrição:** Manter hierarquia em TaxParameter, adicionar auditoria na tabela associativa

**Prós:**
- Melhor dos dois mundos

**Contras:**
- Não resolve complexidade de hierarquia
- Duplica esforço de implementação

**Decisão:** Rejeitada - Simplicidade é preferível no MVP

## Decisão Final

✅ **ADOTAR** modelo simplificado (flat TaxParameter + tabela associativa explícita com auditoria)

## Aprovações

- [x] Tech Lead
- [x] Equipe de Desenvolvimento
- [x] Product Owner
- [x] **Validação em equipe realizada e aprovada** ✅

**Data de aprovação:** 2025-10-25
**Validação final:** 2025-10-25

## Referências

- [Epic 01: Fundação & Autenticação](../epics/epic-01-fundacao-autenticacao.md)
- [Epic 02: Gestão de Empresas & Parâmetros](../epics/epic-02-gestao-empresas-parametros.md)
- [Seção 04: Modelos de Dados](04-Modelos-de-Dados.md)
- [Seção 09: Database Schema](09-Database-Schema.md)
- [DBML Documentation](https://dbml.dbdiagram.io/docs/)

## Changelog do ADR

| Data | Versão | Mudança | Autor |
|------|--------|---------|-------|
| 2025-10-25 | 1.0 | Versão inicial | Winston (Architect Agent) |
| 2025-10-25 | 1.1 | Aprovado e validado pela equipe | Winston (Architect Agent) |
| 2025-10-25 | 1.2 | Fases 1 e 2 concluídas - Épicos atualizados | Winston (Architect Agent) |
| 2025-10-25 | 1.3 | Adicionada tb_valores_parametros_temporais | Winston (Architect Agent) |

---

**Assinatura Digital:** `adr-001-simplificacao-modelo-dados-v1.1-20251025-VALIDADO`
