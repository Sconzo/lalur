# Epic 2: Gestão de Empresas, Seleção de Contexto & Parâmetros Tributários

**Objetivo do Epic:**

Implementar o gerenciamento completo de empresas (CRUD), integração com API governamental para consulta de CNPJ, mecanismo de seleção de contexto de empresa para usuários CONTADOR (via header X-Company-Id), implementação do Período Contábil com bloqueio temporal de edições, e gestão de parâmetros tributários hierárquicos globais (apenas ADMIN). Este épico estabelece a base de dados mestre (empresas e parâmetros fiscais) necessária para todas as funcionalidades contábeis e tributárias subsequentes. Ao final, CONTADOR poderá trabalhar em múltiplas empresas alternando contexto, e ADMIN terá configurado os parâmetros tributários que serão aplicados às empresas.

---

## Story 2.1: Entidade Company e Repository

**Como** desenvolvedor,
**Eu quero** entidade Company com repository JPA implementando port,
**Para que** possamos persistir empresas no banco de dados com todos os campos obrigatórios.

### Acceptance Criteria

1. Entidade JPA `CompanyEntity` criada em `infrastructure/adapter/out/persistence/entity/` estendendo `BaseEntity`:
   - `@Column(nullable=false, unique=true, length=14) String cnpj` (apenas números, 14 dígitos)
   - `@Column(name="razao_social", nullable=false) String razaoSocial`
   - `@Column(nullable=false, length=7) String cnae` (formato: 0000-0/00)
   - `@Column(name="qualificacao_pessoa_juridica", nullable=false) String qualificacaoPessoaJuridica`
   - `@Column(name="natureza_juridica", nullable=false) String naturezaJuridica`
   - `@Column(name="periodo_contabil", nullable=false) LocalDate periodoContabil` (data de corte para edições)
   - **Nota:** Tabela de banco = `tb_empresa`, colunas em snake_case conforme ADR-001
   - **Nota:** Relacionamento com TaxParameter gerenciado via tabela explícita `tb_empresa_parametros_tributarios` (ver Story 2.8)
2. Value Object `CNPJ` criado em `domain/model/valueobject/`:
   - Validação de formato (14 dígitos)
   - Validação de dígitos verificadores
   - Método `format()` retornando string formatada (00.000.000/0000-00)
3. Interface `CompanyRepositoryPort` criada em `application/port/out/`:
   - `Optional<Company> findByCnpj(String cnpj)`
   - `Company save(Company company)`
   - `Optional<Company> findById(Long id)`
   - `Page<Company> findAll(Pageable pageable)`
4. Interface `CompanyJpaRepository` criada estendendo `JpaRepository<CompanyEntity, Long>`:
   - `Optional<CompanyEntity> findByCnpj(String cnpj)`
5. Classe `CompanyRepositoryAdapter` implementa `CompanyRepositoryPort`
6. Model `Company` (domain) criado em `domain/model/` como POJO puro
7. Mapper MapStruct `CompanyMapper` criado para conversão `CompanyEntity` ↔ `Company`
8. Teste de integração (TestContainers) valida:
   - Salvar empresa e recuperar por CNPJ
   - Unique constraint em CNPJ (tentativa de duplicata lança exception)
   - Validação de CNPJ (CNPJ inválido lança exception)
   - Soft delete funciona corretamente
   - Período Contábil é persistido corretamente

---

## Story 2.2: Integração com API de Consulta CNPJ

**Como** ADMIN,
**Eu quero** buscar dados de uma empresa por CNPJ via API governamental,
**Para que** os campos sejam preenchidos automaticamente sem digitação manual.

### Acceptance Criteria

1. Interface `CnpjSearchPort` criada em `application/port/out/`:
   - `Optional<CnpjData> searchByCnpj(String cnpj)`
   - Record `CnpjData` com: `cnpj`, `razaoSocial`, `cnae`, `qualificacaoPj`, `naturezaJuridica`
2. Classe `BrasilApiCnpjAdapter` implementa `CnpjSearchPort` em `infrastructure/adapter/out/external/`:
   - Usa `RestTemplate` ou `WebClient` para chamada HTTP
   - Endpoint: `https://brasilapi.com.br/api/cnpj/v1/{cnpj}`
   - Timeout configurado: 10 segundos
   - Retry 1x em caso de falha (usando `@Retryable` do Spring Retry ou manual)
3. DTO `BrasilApiCnpjResponse` criado para mapear resposta da API
4. Tratamento de erros:
   - CNPJ não encontrado (404) → retorna `Optional.empty()`
   - Timeout ou erro de rede → loga erro e retorna `Optional.empty()`
   - Response inválido → loga erro e retorna `Optional.empty()`
5. Cache de consultas bem-sucedidas usando Spring Cache (`@Cacheable`):
   - TTL: 24 horas
   - Cache name: "cnpj-data"
6. Endpoint `GET /api/v1/companies/search-cnpj/{cnpj}` criado (ADMIN only):
   - Valida formato do CNPJ (14 dígitos)
   - Chama `CnpjSearchPort.searchByCnpj()`
   - Response 200 OK com dados se encontrado
   - Response 404 Not Found se CNPJ não existe ou API falhou
   - Response 400 Bad Request se CNPJ formato inválido
   - Protegido com `@PreAuthorize("hasRole('ADMIN')")`
7. Teste de integração (mock da API externa) valida:
   - Consulta bem-sucedida retorna dados corretos
   - CNPJ não encontrado retorna 404
   - Timeout/falha de rede retorna 404
   - Cache funciona (segunda consulta não chama API novamente)
8. Teste manual com CNPJ real valida integração com BrasilAPI

---

## Story 2.3: CRUD de Empresas

**Como** ADMIN,
**Eu quero** criar, listar, visualizar, editar e inativar empresas,
**Para que** eu possa gerenciar as empresas cadastradas no sistema.

### Acceptance Criteria

1. Controller `CompanyController` criado com endpoints (todos ADMIN only):
   - `POST /api/v1/companies` - criar empresa
   - `GET /api/v1/companies` - listar todas empresas com paginação
   - `GET /api/v1/companies/{id}` - visualizar empresa
   - `PUT /api/v1/companies/{id}` - editar empresa
   - `PATCH /api/v1/companies/{id}/status` - alternar status da empresa (ativar/inativar)
2. DTOs criados: `CreateCompanyRequest`, `UpdateCompanyRequest`, `CompanyResponse`
3. `CreateCompanyRequest`:
   - `cnpj` (obrigatório, 14 dígitos)
   - `razaoSocial` (obrigatório)
   - `cnae` (obrigatório)
   - `qualificacaoPessoaJuridica` (obrigatório)
   - `naturezaJuridica` (obrigatório)
   - `periodoContabil` (obrigatório, formato ISO 8601: YYYY-MM-DD)
   - `parametrosTributariosIds` (lista de IDs - opcional)
4. `CompanyResponse` (para listagem):
   - `id`, `cnpj` (formatado 00.000.000/0000-00), `status`, `razaoSocial`, `cnae`, `qualificacaoPessoaJuridica`, `naturezaJuridica`
5. `CompanyDetailResponse` (para visualização individual):
   - Todos campos de `CompanyResponse` + `periodoContabil`, `parametrosTributarios` (lista simplificada), `createdAt`, `updatedAt`
6. Use cases implementados:
   - `CreateCompanyUseCase`: valida CNPJ, verifica duplicata, salva empresa
   - `ListCompaniesUseCase`: retorna todas empresas (apenas ADMIN acessa)
   - `GetCompanyUseCase`: retorna empresa por ID (apenas ADMIN acessa)
   - `UpdateCompanyUseCase`: permite editar empresa (exceto CNPJ) (apenas ADMIN)
   - `ToggleCompanyStatusUseCase`: alterna status entre ACTIVE e INACTIVE (apenas ADMIN)
7. Validações no `CreateCompanyUseCase`:
   - CNPJ válido (formato e dígitos verificadores)
   - CNPJ único (não pode já existir ACTIVE)
   - Período Contábil não pode ser no futuro
   - Parâmetros tributários devem existir (se fornecidos)
8. **Listagem de empresas** (`GET /api/v1/companies`):
   - **Colunas retornadas** (ordem):
     1. CNPJ (formatado: 00.000.000/0000-00)
     2. Status (ACTIVE/INACTIVE)
     3. Razão Social
     4. CNAE
     5. Qualificação da Pessoa Jurídica
     6. Natureza Jurídica
   - **Filtro Global** (`?globalSearch=texto`):
     - Busca em todos os campos da tabela simultaneamente (CNPJ, Razão Social, CNAE, Qualificação PJ, Natureza Jurídica)
     - Case insensitive
   - **Filtros Específicos com dropdown e busca interna**:
     - **Filtro por CNPJ** (`?cnpjFilter=00.000.000/0000-00`):
       - Endpoint auxiliar: `GET /api/v1/companies/filter-options/cnpj?search=texto`
       - Retorna lista de CNPJs únicos (formatados) que contêm o texto da busca
       - Frontend exibe dropdown com busca (typeahead)
       - Usuário pode buscar dentro do dropdown e selecionar CNPJ
     - **Filtro por Razão Social** (`?razaoSocialFilter=Nome Empresa`):
       - Endpoint auxiliar: `GET /api/v1/companies/filter-options/razao-social?search=texto`
       - Retorna lista de Razões Sociais únicas que contêm o texto da busca
       - Frontend exibe dropdown com busca (typeahead)
       - Usuário pode buscar dentro do dropdown e selecionar Razão Social
   - **Paginação**: `?page=0&size=20&sort=razaoSocial,asc`
   - **Filtro por status**: `?include_inactive=true` (padrão: apenas ACTIVE)
   - **Ordenação**: Suporta ordenação por qualquer coluna (`?sort={campo},{asc|desc}`)
9. **Endpoints auxiliares para filtros**:
   - `GET /api/v1/companies/filter-options/cnpj?search={texto}` (ADMIN only):
     - DTO Response: `FilterOptionsResponse` com `List<String> options` (lista de CNPJs únicos formatados)
     - Query `search` opcional - se fornecido, filtra CNPJs que contêm o texto
     - Retorna apenas CNPJs de empresas ACTIVE (a menos que `include_inactive=true`)
     - Máximo 100 resultados
     - Ordenado alfabeticamente
   - `GET /api/v1/companies/filter-options/razao-social?search={texto}` (ADMIN only):
     - DTO Response: `FilterOptionsResponse` com `List<String> options` (lista de Razões Sociais únicas)
     - Query `search` opcional - se fornecido, filtra Razões Sociais que contêm o texto
     - Retorna apenas de empresas ACTIVE (a menos que `include_inactive=true`)
     - Máximo 100 resultados
     - Ordenado alfabeticamente
10. Todos endpoints protegidos com `@PreAuthorize("hasRole('ADMIN')")`
11. DTO adicional `ToggleStatusRequest`: `status` (obrigatório, enum: ACTIVE ou INACTIVE)
12. DTO `ToggleStatusResponse`: `success` (boolean), `message`, `newStatus`
13. DTO `FilterOptionsResponse`: `List<String> options`
14. Teste valida:
    - ADMIN consegue criar empresa
    - CNPJ duplicado retorna 400 Bad Request
    - CNPJ inválido retorna 400 Bad Request
    - CONTADOR recebe 403 ao tentar acessar qualquer endpoint de CRUD
    - ADMIN vê todas empresas na listagem com colunas corretas
    - Filtro global busca em todos os campos simultaneamente
    - Endpoint `/filter-options/cnpj` retorna lista de CNPJs únicos
    - Endpoint `/filter-options/cnpj?search=12345` filtra CNPJs que contêm "12345"
    - Endpoint `/filter-options/razao-social?search=Acme` filtra razões sociais que contêm "Acme"
    - Filtro `cnpjFilter` filtra listagem por CNPJ específico
    - Filtro `razaoSocialFilter` filtra listagem por Razão Social específica
    - Filtros podem ser combinados (globalSearch + cnpjFilter + razaoSocialFilter)
    - Toggle status: ACTIVE → INACTIVE funciona
    - Toggle status: INACTIVE → ACTIVE funciona
    - Empresa inativada não aparece na listagem padrão
    - Empresa inativada aparece com include_inactive=true
    - Ordenação por qualquer coluna funciona corretamente

---

## Story 2.4: Seleção de Empresa (Contexto CONTADOR e ADMIN)

**Como** CONTADOR ou ADMIN,
**Eu quero** selecionar uma empresa em um dropdown para trabalhar,
**Para que** todas minhas operações sejam feitas no contexto da empresa selecionada (obrigatório para CONTADOR, opcional para ADMIN).

### Acceptance Criteria

1. Endpoint `GET /api/v1/companies/my-companies` criado (autenticado):
   - Retorna lista de todas empresas ACTIVE (disponível para ADMIN e CONTADOR)
   - DTO `CompanyListItemResponse`: `id`, `cnpj` (formatado), `razaoSocial`
2. Endpoint `POST /api/v1/companies/select-company` criado (autenticado):
   - DTO `SelectCompanyRequest`: `companyId` (obrigatório)
   - DTO `SelectCompanyResponse`: `success`, `companyId`, `companyName`, `message`
   - Disponível para ADMIN e CONTADOR
3. Use case `SelectCompanyUseCase` implementado:
   - Valida que empresa existe e está ACTIVE
   - Retorna confirmação de seleção
4. Implementação do contexto via **header `X-Company-Id`**:
   - Usuário (ADMIN ou CONTADOR) pode enviar `X-Company-Id: {id}` em requisições após seleção
   - Classe `CompanyContextFilter` (implements Filter) criada:
     - Extrai `X-Company-Id` do header (se presente)
     - Valida que empresa existe e está ACTIVE
     - Armazena no `ThreadLocal` (`CompanyContext.setCurrentCompanyId(id)`)
     - Limpa ThreadLocal após request (`finally` block)
5. Classe utilitária `CompanyContext` criada:
   - `static void setCurrentCompanyId(Long id)`
   - `static Long getCurrentCompanyId()`
   - `static void clear()`
   - Usa `ThreadLocal<Long>` internamente
6. Validação automática em repositories:
   - Repositories de entidades relacionadas a empresas filtram automaticamente por `CompanyContext.getCurrentCompanyId()` quando header está presente
   - Exemplo: `ChartOfAccountRepository.findAll()` retorna apenas contas da empresa no contexto
7. Comportamento por role:
   - **CONTADOR**: `X-Company-Id` é **obrigatório** para acessar recursos de empresa (dados contábeis, plano de contas, etc.)
   - **ADMIN**: `X-Company-Id` é **opcional** - pode usar o header (dropdown) ou acessar via path param nos endpoints de CRUD
8. Response 404 Not Found se empresa no header não existe ou está INACTIVE
9. Response 400 Bad Request se `X-Company-Id` ausente e usuário é CONTADOR tentando acessar recurso de empresa
10. Endpoint `GET /api/v1/companies/current-company` retorna empresa selecionada atualmente (autenticado, se header presente)
11. Teste valida:
    - CONTADOR e ADMIN conseguem listar todas empresas ACTIVE via `/my-companies`
    - CONTADOR consegue selecionar qualquer empresa ACTIVE
    - ADMIN consegue selecionar qualquer empresa ACTIVE
    - Tentativa de selecionar empresa INACTIVE retorna 404
    - Tentativa de selecionar empresa inexistente retorna 404
    - Header `X-Company-Id` é validado em requisições subsequentes
    - ThreadLocal é limpo corretamente após cada request
    - CONTADOR é bloqueado de acessar recursos sem header (400)
    - ADMIN pode usar header ou acessar via path param

---

## Story 2.5: Período Contábil e Bloqueio Temporal

**Como** ADMIN,
**Eu quero** definir e editar o Período Contábil de uma empresa,
**Para que** dados com competência anterior ao Período Contábil fiquem bloqueados para edição.

### Acceptance Criteria

1. Endpoint `PUT /api/v1/companies/{id}/periodo-contabil` criado (ADMIN only):
   - DTO `UpdatePeriodoContabilRequest`: `novoPeriodoContabil` (obrigatório, formato ISO 8601)
   - DTO `UpdatePeriodoContabilResponse`: `success`, `message`, `periodoContabilAnterior`, `periodoContabilNovo`
2. Use case `UpdatePeriodoContabilUseCase` implementado:
   - Valida que nova data não é no futuro
   - Valida que nova data é posterior à data atual (não pode retroagir)
   - Registra alteração em log de auditoria dedicado
   - Atualiza campo `periodoContabil` da empresa
3. Entidade de auditoria `PeriodoContabilAuditEntity` criada:
   - `companyId`, `periodoContabilAnterior`, `periodoContabilNovo`, `changedBy` (email), `changedAt` (timestamp)
   - Não extends BaseEntity (não tem soft delete)
4. Repository `PeriodoContabilAuditRepository` criado para persistir logs
5. Endpoint `GET /api/v1/companies/{id}/periodo-contabil/audit` lista histórico de alterações (ADMIN only)
7. Implementação do bloqueio temporal:
   - Interface `TemporalEntity` criada com método `LocalDate getCompetencia()`
   - Entidades com competência implementam `TemporalEntity`
   - Annotation customizada `@EnforcePeriodoContabil` criada
   - Aspect `PeriodoContabilAspect` intercepta operações de update/delete:
     - Verifica se entidade implementa `TemporalEntity`
     - Compara `entity.getCompetencia()` com `company.getPeriodoContabil()`
     - Se `competencia < periodoContabil`, lança `PeriodoContabilViolationException`
8. Exception `PeriodoContabilViolationException` criada:
   - Mensagem: "Não é possível editar dados com competência anterior ao Período Contábil ({data})"
   - HTTP 400 Bad Request
9. Validação em endpoints de edição:
   - Endpoints que editam dados com competência chamam validação antes de salvar
   - Operações de leitura sempre permitidas (modo read-only)
10. Teste valida:
    - ADMIN consegue atualizar Período Contábil
    - CONTADOR recebe 403 ao tentar atualizar Período Contábil
    - Nova data não pode ser no futuro (400)
    - Nova data não pode retroagir (400)
    - Histórico de alterações é registrado corretamente
    - Tentativa de editar dado com competência < Período Contábil retorna 400
    - Leitura de dados antigos sempre permitida

---

## Story 2.6: Entidade TaxParameter e Repository

**Como** desenvolvedor,
**Eu quero** entidade TaxParameter simplificada (estrutura flat),
**Para que** ADMIN possa criar parâmetros tributários organizados por tipo/categoria.

**Nota:** Esta story foi simplificada conforme [ADR-001](../architecture/adr-001-simplificacao-modelo-dados.md) - hierarquia parent/child foi removida.

### Acceptance Criteria

1. Entidade JPA `TaxParameterEntity` criada estendendo `BaseEntity`:
   - `@Column(name="codigo", unique=true, nullable=false) String code` (código único identificador)
   - `@Column(name="tipo", nullable=false) String type` (categoria: 'IRPJ', 'CSLL', 'GERAL', etc.)
   - `@Column(name="descricao", columnDefinition="TEXT") String description` (descrição detalhada)
   - **Nota:** Tabela de banco = `tb_parametros_tributarios`, colunas em snake_case
   - **Removido (ADR-001):** `configuration` (JSON), `parent`, `children` (hierarquia)
2. Interface `TaxParameterRepositoryPort` criada em `application/port/out/`:
   - `Optional<TaxParameter> findByCode(String code)`
   - `TaxParameter save(TaxParameter taxParameter)`
   - `Optional<TaxParameter> findById(Long id)`
   - `List<TaxParameter> findAll()`
   - `List<TaxParameter> findByType(String type)` (busca por categoria)
3. Interface `TaxParameterJpaRepository` criada estendendo `JpaRepository<TaxParameterEntity, Long>`:
   - `Optional<TaxParameterEntity> findByCode(String code)`
   - `List<TaxParameterEntity> findByType(String type)`
4. Classe `TaxParameterRepositoryAdapter` implementa `TaxParameterRepositoryPort`
5. Model `TaxParameter` (domain) criado como POJO puro
6. Mapper MapStruct `TaxParameterMapper` criado
7. Teste de integração valida:
   - Salvar parâmetro com tipo/categoria
   - Buscar parâmetros por tipo (ex: todos do tipo 'IRPJ')
   - Unique constraint em code (duplicata lança exception)
   - Soft delete funciona corretamente

---

## Story 2.7: CRUD de Parâmetros Tributários (ADMIN apenas)

**Como** ADMIN,
**Eu quero** criar, listar, visualizar, editar e inativar parâmetros tributários,
**Para que** eu possa configurar os parâmetros que serão aplicados às empresas.

**Nota:** Esta story foi simplificada conforme [ADR-001](../architecture/adr-001-simplificacao-modelo-dados.md) - endpoints de hierarquia foram removidos.

### Acceptance Criteria

1. Controller `TaxParameterController` criado com endpoints (todos ADMIN only):
   - `POST /api/v1/tax-parameters` - criar parâmetro
   - `GET /api/v1/tax-parameters` - listar parâmetros com paginação
   - `GET /api/v1/tax-parameters/{id}` - visualizar parâmetro
   - `PUT /api/v1/tax-parameters/{id}` - editar parâmetro
   - `PATCH /api/v1/tax-parameters/{id}/status` - alternar status do parâmetro (ativar/inativar)
   - **Removido (ADR-001):** ~~`GET /api/v1/tax-parameters/roots`~~ (sem hierarquia)
   - **Removido (ADR-001):** ~~`GET /api/v1/tax-parameters/{id}/children`~~ (sem hierarquia)
2. DTOs criados: `CreateTaxParameterRequest`, `UpdateTaxParameterRequest`, `TaxParameterResponse`
3. `CreateTaxParameterRequest`:
   - `code` (obrigatório, único)
   - `type` (obrigatório - categoria: 'IRPJ', 'CSLL', 'GERAL', etc.)
   - `description` (opcional)
   - **Removido (ADR-001):** ~~`configuration`~~ (JSON), ~~`parentId`~~
4. `TaxParameterResponse`:
   - `id`, `code`, `type`, `description`, `status`, `createdAt`, `updatedAt`
   - **Removido (ADR-001):** ~~`configuration`~~, ~~`parentId`~~, ~~`parentName`~~, ~~`children`~~
5. Use cases implementados:
   - `CreateTaxParameterUseCase`: valida code único
   - `ListTaxParametersUseCase`: listagem flat com filtro por tipo
   - `GetTaxParameterUseCase`: retorna parâmetro individual
   - `UpdateTaxParameterUseCase`: permite editar (exceto code)
   - `ToggleTaxParameterStatusUseCase`: alterna status entre ACTIVE e INACTIVE
6. Validações:
   - Code deve ser alfanumérico com hífens (regex: `^[A-Z0-9-]+$`)
   - Code único (não pode duplicar)
   - Type obrigatório (String livre para flexibilidade)
   - **Removido (ADR-001):** ~~validação de parent~~, ~~validação de JSON configuration~~
7. Listagem suporta:
   - Paginação: `?page=0&size=50`
   - Filtro por tipo: `?type=IRPJ` (busca exata)
   - Busca: `?search=aliquota` (busca em code e description)
   - Filtro por status: `?include_inactive=true`
8. Todos endpoints protegidos com `@PreAuthorize("hasRole('ADMIN')")`
9. DTO adicional para toggle status: `ToggleStatusRequest` e `ToggleStatusResponse`
10. Teste valida:
    - ADMIN consegue criar parâmetro com tipo
    - Code duplicado retorna 400 Bad Request
    - Code com formato inválido retorna 400 Bad Request
    - CONTADOR recebe 403 ao tentar acessar endpoints
    - Listagem flat funciona corretamente
    - Filtro por tipo funciona
    - Toggle status: ACTIVE → INACTIVE funciona
    - Toggle status: INACTIVE → ACTIVE funciona
    - Parâmetro inativado não quebra relacionamentos com empresas
    - Parâmetro inativado não aparece na listagem padrão
    - Parâmetro inativado aparece com include_inactive=true

---

## Story 2.8: Associação de Parâmetros Tributários a Empresas

**Como** ADMIN,
**Eu quero** associar parâmetros tributários a uma empresa durante criação ou edição,
**Para que** os cálculos tributários usem os parâmetros corretos para cada empresa.

**Nota:** Esta story foi modificada conforme [ADR-001](../architecture/adr-001-simplificacao-modelo-dados.md) - tabela associativa explícita com auditoria.

### Acceptance Criteria

1. **Criar nova entidade JPA `CompanyTaxParameterEntity`** (ADR-001 - tabela associativa explícita com auditoria):
   ```java
   @Entity
   @Table(name = "tb_empresa_parametros_tributarios",
          uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "parametro_tributario_id"}))
   class CompanyTaxParameterEntity {
       @Id @GeneratedValue(strategy = IDENTITY)
       Long id;

       @Column(name = "empresa_id", nullable = false)
       Long companyId;

       @Column(name = "parametro_tributario_id", nullable = false)
       Long taxParameterId;

       @Column(name = "criado_por")
       Long createdBy;  // ID do usuário que associou

       @Column(name = "criado_em")
       LocalDateTime createdAt;
   }
   ```
2. **Criar repository para associação:**
   - Interface `CompanyTaxParameterJpaRepository` estendendo `JpaRepository<CompanyTaxParameterEntity, Long>`
   - Métodos:
     - `List<CompanyTaxParameterEntity> findByCompanyId(Long companyId)`
     - `void deleteByCompanyIdAndTaxParameterId(Long companyId, Long taxParameterId)`
     - `void deleteAllByCompanyId(Long companyId)` (para substituir lista completa)
3. Endpoint `PUT /api/v1/companies/{id}/tax-parameters` criado (ADMIN only):
   - DTO `UpdateTaxParametersRequest`: `taxParameterIds` (lista de IDs)
   - DTO `UpdateTaxParametersResponse`: `success`, `message`, `taxParameters` (lista aplicada)
   - Protegido com `@PreAuthorize("hasRole('ADMIN')")`
4. Use case `UpdateCompanyTaxParametersUseCase` implementado:
   - Valida que todos IDs existem e estão ACTIVE
   - **Lógica de substituição (ADR-001):**
     1. Busca associações atuais: `findByCompanyId(companyId)`
     2. Deleta todas associações antigas: `deleteAllByCompanyId(companyId)`
     3. Cria novas associações com auditoria:
        - `createdBy` = ID do usuário autenticado (extraído do SecurityContext)
        - `createdAt` = timestamp atual
   - Retorna lista atualizada
5. Endpoint `GET /api/v1/companies/{id}/tax-parameters` lista parâmetros aplicados à empresa (ADMIN only):
   - Query deve fazer JOIN triplo:
     ```sql
     SELECT tp.* FROM tb_parametros_tributarios tp
     JOIN tb_empresa_parametros_tributarios ctp ON tp.id = ctp.parametro_tributario_id
     WHERE ctp.empresa_id = ? AND tp.status = 'ACTIVE'
     ```
6. Durante `CreateCompanyUseCase` (Story 2.3), parâmetros podem ser associados via `parametrosTributariosIds`:
   - Criar registros em `CompanyTaxParameterEntity` com auditoria
7. Validação: não permitir associar parâmetros INACTIVE
8. `CompanyResponse` (Story 2.3) inclui lista simplificada de parâmetros:
   - Cada item: `id`, `code`, `type`, `description`
   - **Novo (ADR-001):** Incluir `associatedAt` e `associatedBy` (email do usuário) para rastreabilidade
9. Teste valida:
   - ADMIN consegue associar parâmetros a empresa
   - Auditoria é registrada corretamente (`createdBy`, `createdAt`)
   - CONTADOR recebe 403 ao tentar associar parâmetros
   - IDs inválidos retornam 400 Bad Request
   - Parâmetros INACTIVE são rejeitados (400)
   - Listagem de parâmetros de empresa funciona com JOIN correto
   - Parâmetros são substituídos (não acumulados) em update
   - Unique constraint (empresa_id, parametro_tributario_id) previne duplicatas

---

## Story 2.9: Gestão de Valores Temporais de Parâmetros Tributários

**Como** ADMIN,
**Eu quero** definir valores temporais (mensais ou trimestrais) para parâmetros tributários de uma empresa,
**Para que** o sistema possa rastrear mudanças de parâmetros ao longo do ano fiscal (ex: "Lucro Real" em Jan-Fev, "Lucro Presumido" em Mar-Dez).

**Contexto:** Alguns parâmetros tributários variam ao longo do ano fiscal. Por exemplo, uma empresa pode optar por "Lucro Real" nos primeiros meses e depois mudar para "Lucro Presumido", ou ter diferentes "Formas de Estimativa" por trimestre. Esta story implementa o modelo temporal conforme [ADR-001](../architecture/adr-001-simplificacao-modelo-dados.md) v1.3.

### Acceptance Criteria

1. **Criar entidade JPA `ValorParametroTemporalEntity`** (ADR-001 v1.3 - valores temporais):
   ```java
   @Entity
   @Table(name = "tb_valores_parametros_temporais",
          uniqueConstraints = @UniqueConstraint(
              columnNames = {"empresa_parametros_tributarios_id", "ano", "mes", "trimestre"}))
   class ValorParametroTemporalEntity {
       @Id @GeneratedValue(strategy = IDENTITY)
       Long id;

       @Column(name = "empresa_parametros_tributarios_id", nullable = false)
       Long empresaParametroId;  // FK → tb_empresa_parametros_tributarios

       @Column(nullable = false)
       Integer ano;

       @Column
       Integer mes;  // 1-12 se mensal, NULL se trimestral

       @Column
       Integer trimestre;  // 1-4 se trimestral, NULL se mensal

       @PrePersist
       @PreUpdate
       private void validatePeriodicity() {
           boolean hasMonth = mes != null;
           boolean hasQuarter = trimestre != null;
           if (hasMonth == hasQuarter) {  // Ambos null ou ambos preenchidos
               throw new IllegalStateException("Deve ter mes OU trimestre, nunca ambos ou nenhum");
           }
           if (mes != null && (mes < 1 || mes > 12)) {
               throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
           }
           if (trimestre != null && (trimestre < 1 || trimestre > 4)) {
               throw new IllegalArgumentException("Trimestre deve estar entre 1 e 4");
           }
       }
   }
   ```

2. **Criar repository para valores temporais:**
   - Interface `ValorParametroTemporalJpaRepository` estendendo `JpaRepository<ValorParametroTemporalEntity, Long>`
   - Métodos customizados:
     - `List<ValorParametroTemporalEntity> findByEmpresaParametroId(Long empresaParametroId)`
     - `List<ValorParametroTemporalEntity> findByEmpresaParametroIdAndAno(Long empresaParametroId, Integer ano)`
     - `Optional<ValorParametroTemporalEntity> findByEmpresaParametroIdAndAnoAndMes(Long empresaParametroId, Integer ano, Integer mes)`
     - `Optional<ValorParametroTemporalEntity> findByEmpresaParametroIdAndAnoAndTrimestre(Long empresaParametroId, Integer ano, Integer trimestre)`

3. **Endpoint para definir valor temporal (ADMIN only):**
   - `POST /api/v1/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values`
   - Request DTO:
     ```json
     {
       "ano": 2024,
       "mes": 1,           // Ou null se trimestral
       "trimestre": null   // Ou 1-4 se trimestral
     }
     ```
   - Validações:
     - Valida que associação `companyId ↔ taxParameterId` existe em `tb_empresa_parametros_tributarios`
     - Valida que exatamente um dos campos (`mes` ou `trimestre`) está preenchido
     - Valida que não existe registro duplicado (unique constraint)
   - Response 201 Created com dados do registro criado
   - Response 400 Bad Request se validações falharem
   - Response 404 Not Found se associação não existir

4. **Endpoint para listar valores temporais de um parâmetro (ADMIN only):**
   - `GET /api/v1/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values?ano=2024`
   - Query parameter `ano` (opcional) - se fornecido, filtra por ano
   - Response 200 OK com array de períodos:
     ```json
     [
       {"id": 1, "ano": 2024, "mes": 1, "trimestre": null, "periodo": "Jan/2024"},
       {"id": 2, "ano": 2024, "mes": 2, "trimestre": null, "periodo": "Fev/2024"},
       {"id": 3, "ano": 2024, "mes": null, "trimestre": 1, "periodo": "1º Tri/2024"}
     ]
     ```
   - Campo `periodo` é formatado no backend para facilitar exibição

5. **Endpoint para deletar valor temporal (ADMIN only):**
   - `DELETE /api/v1/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values/{valorId}`
   - Response 204 No Content se deletado com sucesso
   - Response 404 Not Found se não existir

6. **Endpoint agregado: listar todos parâmetros com períodos ativos de uma empresa (ADMIN only):**
   - `GET /api/v1/companies/{companyId}/tax-parameters-timeline?ano=2024`
   - Query complexa com JOINs:
     ```sql
     SELECT
         tp.codigo, tp.descricao, tp.tipo,
         vpt.ano,
         vpt.mes,
         vpt.trimestre
     FROM tb_valores_parametros_temporais vpt
     JOIN tb_empresa_parametros_tributarios ept
         ON vpt.empresa_parametros_tributarios_id = ept.id
     JOIN tb_parametros_tributarios tp
         ON ept.parametro_tributario_id = tp.id
     WHERE ept.empresa_id = ?
       AND vpt.ano = ?
     ORDER BY tp.tipo, vpt.ano, COALESCE(vpt.mes, vpt.trimestre * 3);
     ```
   - Response agrupada por tipo de parâmetro:
     ```json
     {
       "ano": 2024,
       "timeline": {
         "FORMA_TRIBUTACAO_DE_LUCRO": [
           {"codigo": "0001", "descricao": "Lucro Real", "periodos": ["Jan/2024", "Fev/2024"]},
           {"codigo": "0002", "descricao": "Lucro Presumido", "periodos": ["Mar/2024", "Abr/2024"]}
         ],
         "FORMA_ESTIMATIVA_MENSAL": [
           {"codigo": "0011", "descricao": "Base Receita Bruta", "periodos": ["Jan/2024", "Fev/2024", "Mar/2024"]}
         ]
       }
     }
     ```

7. **Use Cases criados:**
   - `CreateTemporalValueUseCase`: valida associação existente e cria registro
   - `ListTemporalValuesUseCase`: busca valores com filtros
   - `DeleteTemporalValueUseCase`: remove valor temporal
   - `GetCompanyTaxParametersTimelineUseCase`: query agregada para visualização

8. **Atualizar `CompanyResponse` (Story 2.8):**
   - Incluir campo `hasTemporalValues: boolean` em cada parâmetro da lista
   - Se `true`, frontend sabe que deve buscar a timeline

9. **Validação de negócio:**
   - Não permitir criar valor temporal se associação `CompanyTaxParameter` não existir
   - Cascade delete: se associação for removida, valores temporais são deletados automaticamente (ON DELETE CASCADE no DDL)

10. **Testes de integração validam:**
    - Criação de valor mensal (mes preenchido, trimestre null)
    - Criação de valor trimestral (mes null, trimestre preenchido)
    - Erro ao tentar criar com ambos preenchidos ou ambos null
    - Erro ao tentar criar duplicata (unique constraint)
    - Listagem de valores temporais de um parâmetro
    - Listagem da timeline completa de uma empresa
    - Deleção de valor temporal
    - Cascade delete: ao remover associação, valores temporais são removidos
    - CONTADOR recebe 403 ao tentar qualquer operação (apenas ADMIN)
    - Validação de mês (1-12) e trimestre (1-4)

11. **Documentação:**
    - Adicionar exemplos de uso da API no Swagger/OpenAPI
    - Documentar casos de uso: "Como registrar que empresa mudou de Lucro Real para Presumido em Março"

---

## Story 2.10: Testes de Integração End-to-End do Epic 2

**Como** desenvolvedor,
**Eu quero** testes de integração que validem fluxos completos do Epic 2,
**Para que** tenhamos confiança de que todas funcionalidades estão integradas corretamente.

### Acceptance Criteria

1. Teste de integração: **Fluxo completo CONTADOR**
   - Login como CONTADOR
   - Listar todas empresas disponíveis via `/my-companies`
   - Selecionar qualquer empresa (`select-company`)
   - Enviar requisições com header `X-Company-Id`
   - Validar que contexto está correto
   - Tentar acessar CRUD de empresas → recebe 403
   - Tentar acessar recurso de empresa sem header → recebe 400
2a. Teste de integração: **Fluxo completo ADMIN com dropdown**
   - Login como ADMIN
   - Listar todas empresas via `/my-companies`
   - Selecionar qualquer empresa (`select-company`)
   - Enviar requisições com header `X-Company-Id`
   - Validar que contexto está correto
   - Acessar dados contábeis da empresa selecionada via contexto
2b. Teste de integração: **Fluxo completo ADMIN - Parâmetros Tributários**
   - Login como ADMIN
   - Criar parâmetro root (ex: "IRPJ-BASE")
   - Criar parâmetro filho (ex: "IRPJ-ALIQUOTA-15")
   - Criar outro filho (ex: "IRPJ-ADICIONAL-10")
   - Listar roots e validar hierarquia
   - Listar children de parâmetro pai
3. Teste de integração: **Associação Empresa + Parâmetros**
   - ADMIN cria empresa
   - ADMIN cria 3 parâmetros tributários
   - ADMIN associa parâmetros à empresa
   - Buscar empresa e validar que parâmetros estão associados
   - Atualizar parâmetros (substituir lista)
   - Validar que lista foi substituída corretamente
4. Teste de integração: **Período Contábil e Bloqueio**
   - Criar empresa com Período Contábil = 2024-01-01
   - Criar dado contábil com competência 2023-12-31 (anterior)
   - Tentar editar dado antigo → deve falhar (400)
   - Ler dado antigo → deve funcionar
   - Atualizar Período Contábil para 2023-12-01
   - Tentar editar dado antigo novamente → deve funcionar
   - Validar que histórico de auditoria foi registrado
5. Teste de integração: **Row-level Security via X-Company-Id**
   - ADMIN cria empresa1 e empresa2
   - Login como CONTADOR
   - Listar empresas → deve ver empresa1 e empresa2
   - Selecionar empresa1 e enviar header X-Company-Id
   - Acessar recursos da empresa1 → sucesso
   - Tentar acessar recursos sem header → recebe 400
6. Teste de integração: **Consulta CNPJ com fallback**
   - Chamar endpoint `/search-cnpj` com CNPJ válido
   - Mock da API retorna dados → validar response 200
   - Mock da API retorna 404 → validar response 404
   - Mock da API timeout → validar response 404
   - Segunda chamada com mesmo CNPJ → validar que usou cache (não chamou API)
7. Todos testes usam TestContainers com PostgreSQL real
8. Todos testes limpam dados após execução (`@Transactional` com rollback)

---

## 📋 Resumo de Mudanças Arquiteturais (ADR-001)

**Referência:** [ADR-001: Simplificação do Modelo de Dados](../architecture/adr-001-simplificacao-modelo-dados.md)

Este épico foi **significativamente atualizado** para refletir a decisão arquitetural de simplificar o modelo de dados.

### Mudanças Aplicadas

#### Story 2.1 (Company Entity)
✅ **Nomenclatura ajustada para snake_case:**
- Tabela: `tb_empresa`
- `razaoSocial` → `razao_social`
- `periodoContabil` → `periodo_contabil`
- Removido relacionamento `@ManyToMany` direto com TaxParameter

#### Story 2.6 (TaxParameter Entity) - **MUDANÇAS SIGNIFICATIVAS**
❌ **Removido (hierarquia parent/child):**
- Campos `parent`, `children` (relacionamento self-referential)
- Campo `configuration` (JSON)
- Campo `name` (redundante com `description`)

✅ **Adicionado:**
- Campo `tipo` (String) para categorização ('IRPJ', 'CSLL', 'GERAL', etc.)
- Estrutura flat (sem hierarquia)
- Tabela: `tb_parametros_tributarios`

#### Story 2.7 (CRUD TaxParameter) - **ENDPOINTS REMOVIDOS**
❌ **Removidos:**
- `GET /api/v1/tax-parameters/roots`
- `GET /api/v1/tax-parameters/{id}/children`
- Validações de hierarquia (parent exists, prevent changing parent if has children)

✅ **Mantidos:**
- CRUD básico (POST, GET, PUT, PATCH)
- Listagem com filtro por `type`
- Busca por `code` e `description`

#### Story 2.8 (Associação Empresa ↔ Parâmetros) - **NOVA ENTITY**
✅ **Criada nova entidade `CompanyTaxParameterEntity`:**
- Tabela associativa **explícita** (não `@ManyToMany` automático)
- **Auditoria completa:** `criado_por`, `criado_em`
- Tabela: `tb_empresa_parametros_tributarios`
- PK própria (`id`)
- UNIQUE constraint (`empresa_id`, `parametro_tributario_id`)

### Impacto Geral

| Aspecto | Impacto | Detalhes |
|---------|---------|----------|
| **Complexidade** | 🔽 REDUZIDA | ~30% menos código (sem hierarquia) |
| **Performance** | 🚀 MELHORADA | Queries O(1) vs O(n log n) recursivas |
| **Auditoria** | 📊 MELHORADA | Rastreabilidade completa de associações |
| **Testes** | ✅ SIMPLIFICADOS | Menos casos de teste (sem hierarquia) |
| **Manutenibilidade** | ⬆️ AUMENTADA | Modelo mental mais simples |

### Mapeamento de Nomenclatura

**Java (camelCase) → PostgreSQL (snake_case):**

```java
// Company
razaoSocial          → razao_social
periodoContabil      → periodo_contabil
qualificacaoPj       → qualificacao_pessoa_juridica
naturezaJuridica     → natureza_juridica

// TaxParameter
code                 → codigo
type                 → tipo (NOVO campo)
description          → descricao

// CompanyTaxParameter (NOVA)
companyId            → empresa_id
taxParameterId       → parametro_tributario_id
createdBy            → criado_por
createdAt            → criado_em
```

### Próximas Ações na Implementação

1. ✅ Criar entities conforme novo modelo
2. ✅ Criar `CompanyTaxParameterEntity` e repository
3. ✅ Implementar endpoints sem hierarquia
4. ✅ Ajustar testes removendo casos de hierarquia
5. ✅ Validar auditoria em `tb_empresa_parametros_tributarios`
