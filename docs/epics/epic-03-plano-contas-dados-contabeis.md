# Epic 3: Plano de Contas & Dados Contábeis

**Objetivo do Epic:**

Implementar cadastro e importação de plano de contas contábeis (estrutura plana vinculada a Contas Referenciais RFB para compliance ECF), permitir importação/exportação e CRUD completo de lançamentos contábeis via CSV e interface com validação de partidas dobradas, garantir unicidade de contas por empresa/ano, implementar gestão de tabela mestra de Contas Referenciais RFB (ADMIN-only), cadastrar Contas da Parte B (e-Lalur/e-Lacs), e implementar CRUD completo de Lançamentos da Parte B com ajustes fiscais (adições/exclusões). Este épico estabelece a base de dados contábeis estruturada conforme layout oficial ECF/LALUR que alimentará posteriormente os cálculos de IRPJ/CSLL e preenchimento da ECF Parte M. Ao final, CONTADOR poderá importar/cadastrar plano de contas vinculado a estrutura RFB, lançamentos contábeis com partidas dobradas, contas fiscais da Parte B, e lançamentos fiscais com fundamentação tributária.

---

## Story 3.1: Entidade ChartOfAccount (Plano de Contas Contábil) e Repository

**Como** desenvolvedor,
**Eu quero** entidade ChartOfAccount com repository JPA implementando port, incluindo campos ECF-specific e FK obrigatória para ContaReferencial,
**Para que** possamos persistir contas contábeis de cada empresa por ano fiscal com estrutura plana vinculada à tabela mestra RFB.

### Acceptance Criteria

1. Entidade JPA `ChartOfAccountEntity` criada em `infrastructure/adapter/out/persistence/entity/` estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (empresa dona da conta)
   - `@ManyToOne @JoinColumn(name="conta_referencial_id", nullable=false) ContaReferencialEntity contaReferencial` (FK obrigatória para conta oficial RFB)
   - `@Column(nullable=false) String code` (código da conta, ex: "1.1.01.001")
   - `@Column(nullable=false) String name` (nome da conta, ex: "Caixa")
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal, ex: 2024)
   - `@Enumerated(STRING) AccountType accountType` (ATIVO, PASSIVO, PATRIMONIO_LIQUIDO, RECEITA, DESPESA, CUSTO, RESULTADO, COMPENSACAO, ATIVO_RETIFICADORA, PASSIVO_RETIFICADORA)
   - `@Enumerated(STRING) ClasseContabil classe` (ATIVO_CIRCULANTE, ATIVO_NAO_CIRCULANTE, PASSIVO_CIRCULANTE, PASSIVO_NAO_CIRCULANTE, PATRIMONIO_LIQUIDO, RECEITA_BRUTA, DEDUCOES_RECEITA, CUSTOS, DESPESAS_OPERACIONAIS, OUTRAS_RECEITAS, OUTRAS_DESPESAS, RESULTADO_FINANCEIRO)
   - `@Column(nullable=false) Integer nivel` (nível hierárquico 1-5 para estruturação ECF)
   - `@Enumerated(STRING) NaturezaConta natureza` (DEVEDORA, CREDORA)
   - `@Column(nullable=false) Boolean afetaResultado` (indica se conta afeta DRE/resultado)
   - `@Column(nullable=false) Boolean dedutivel` (indica se despesa é dedutível fiscalmente)
2. Enums criados:
   - `AccountType`: ATIVO, PASSIVO, PATRIMONIO_LIQUIDO, RECEITA, DESPESA, CUSTO, RESULTADO, COMPENSACAO, ATIVO_RETIFICADORA, PASSIVO_RETIFICADORA
   - `ClasseContabil`: ATIVO_CIRCULANTE, ATIVO_NAO_CIRCULANTE, PASSIVO_CIRCULANTE, PASSIVO_NAO_CIRCULANTE, PATRIMONIO_LIQUIDO, RECEITA_BRUTA, DEDUCOES_RECEITA, CUSTOS, DESPESAS_OPERACIONAIS, OUTRAS_RECEITAS, OUTRAS_DESPESAS, RESULTADO_FINANCEIRO
   - `NaturezaConta`: DEVEDORA, CREDORA
3. Constraint de unicidade: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code", "fiscal_year"}))`
   - Garante que não existam contas duplicadas para mesma empresa + código + ano
4. Interface `ChartOfAccountRepositoryPort` criada em `application/port/out/`:
   - `ChartOfAccount save(ChartOfAccount account)`
   - `Optional<ChartOfAccount> findById(Long id)`
   - `List<ChartOfAccount> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<ChartOfAccount> findByCompanyIdAndCodeAndFiscalYear(Long companyId, String code, Integer fiscalYear)`
   - `void deleteById(Long id)`
   - `Page<ChartOfAccount> findByCompanyId(Long companyId, Pageable pageable)`
5. Interface `ChartOfAccountJpaRepository` criada estendendo `JpaRepository<ChartOfAccountEntity, Long>`:
   - `List<ChartOfAccountEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<ChartOfAccountEntity> findByCompanyIdAndCodeAndFiscalYear(Long companyId, String code, Integer fiscalYear)`
6. Classe `ChartOfAccountRepositoryAdapter` implementa `ChartOfAccountRepositoryPort`
7. Model `ChartOfAccount` (domain) criado em `domain/model/` como POJO puro
8. Mapper MapStruct `ChartOfAccountMapper` criado
9. Teste de integração (TestContainers) valida:
   - Salvar conta com FK para ContaReferencial e recuperar por company + fiscalYear
   - Unique constraint funciona (duplicata lança exception)
   - FK constraint funciona (tentar salvar sem contaReferencialId válida lança exception)
   - Buscar conta por company + code + fiscalYear
   - Soft delete funciona corretamente
   - Listagem paginada por empresa
   - Campos ECF-specific (classe, nivel, natureza, afetaResultado, dedutivel) são persistidos e recuperados corretamente

---

## Story 3.2: CRUD de Plano de Contas (Manual)

**Como** CONTADOR,
**Eu quero** criar, listar, visualizar, editar e inativar contas contábeis manualmente com todos campos ECF-specific,
**Para que** eu possa cadastrar o plano de contas de uma empresa vinculando cada conta a uma Conta Referencial RFB oficial.

### Acceptance Criteria

1. Controller `ChartOfAccountController` criado com endpoints:
   - `POST /api/v1/chart-of-accounts` - criar conta (CONTADOR com header X-Company-Id)
   - `GET /api/v1/chart-of-accounts` - listar contas com paginação (CONTADOR com header)
   - `GET /api/v1/chart-of-accounts/{id}` - visualizar conta (CONTADOR com header)
   - `PUT /api/v1/chart-of-accounts/{id}` - editar conta (CONTADOR com header)
   - `PATCH /api/v1/chart-of-accounts/{id}/status` - alternar status da conta (ativar/inativar, CONTADOR com header)
2. DTOs criados: `CreateChartOfAccountRequest`, `UpdateChartOfAccountRequest`, `ChartOfAccountResponse`
3. `CreateChartOfAccountRequest`:
   - `code` (obrigatório, String)
   - `name` (obrigatório, String)
   - `fiscalYear` (obrigatório, Integer, ex: 2024)
   - `accountType` (obrigatório, enum)
   - `contaReferencialId` (obrigatório, Long - FK para ContaReferencial)
   - `classe` (obrigatório, enum ClasseContabil)
   - `nivel` (obrigatório, Integer, 1-5)
   - `natureza` (obrigatório, enum NaturezaConta)
   - `afetaResultado` (obrigatório, Boolean)
   - `dedutivel` (obrigatório, Boolean)
4. `ChartOfAccountResponse`:
   - `id`, `code`, `name`, `fiscalYear`, `accountType`, `contaReferencialId`, `contaReferencialCodigo`, `classe`, `nivel`, `natureza`, `afetaResultado`, `dedutivel`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateChartOfAccountUseCase`: valida company via `CompanyContext`, valida que contaReferencialId existe e está ACTIVE, verifica unicidade (company + code + year), salva
   - `ListChartOfAccountsUseCase`: retorna contas da empresa no contexto
   - `GetChartOfAccountUseCase`: retorna conta por ID (validando que pertence à empresa do contexto)
   - `UpdateChartOfAccountUseCase`: permite editar (exceto code e fiscalYear)
   - `ToggleChartOfAccountStatusUseCase`: alterna status entre ACTIVE e INACTIVE
6. Validações no `CreateChartOfAccountUseCase`:
   - Code não pode ser vazio ou conter apenas espaços
   - Fiscal year deve ser >= 2000 e <= ano atual + 1
   - contaReferencialId deve existir e estar ACTIVE
   - nivel deve estar entre 1 e 5
   - Combinação (company + code + fiscalYear) deve ser única
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=code,asc`
   - Filtro por ano: `?fiscalYear=2024`
   - Filtro por tipo: `?accountType=ATIVO`
   - Filtro por classe: `?classe=ATIVO_CIRCULANTE`
   - Filtro por natureza: `?natureza=DEVEDORA`
   - Busca: `?search=Caixa` (busca em code e name)
   - Filtro por status: `?include_inactive=true` (padrão: apenas ACTIVE)
8. Contexto de empresa obrigatório:
   - CONTADOR deve enviar header `X-Company-Id`
   - Endpoints retornam apenas contas da empresa selecionada
   - ADMIN pode usar header ou passar `?companyId={id}` como query param
9. DTO adicional `ToggleStatusRequest`:
   - `status` (obrigatório, enum: ACTIVE ou INACTIVE)
10. DTO `ToggleStatusResponse`:
    - `success` (boolean), `message`, `newStatus`
11. Teste valida:
    - CONTADOR consegue criar conta no contexto da empresa selecionada com todos campos ECF
    - Código duplicado para mesma empresa/ano retorna 400 Bad Request
    - contaReferencialId inexistente retorna 400
    - contaReferencialId INACTIVE retorna 400
    - nivel fora do range 1-5 retorna 400
    - CONTADOR sem header X-Company-Id recebe 400
    - Listagem retorna apenas contas da empresa no contexto
    - Edição não permite mudar code ou fiscalYear
    - Toggle status: ACTIVE → INACTIVE funciona
    - Toggle status: INACTIVE → ACTIVE funciona
    - Listagem com filtros (classe, natureza) funciona
    - Listagem com include_inactive=true retorna contas inativas

---

## Story 3.3: Importação de Plano de Contas via CSV/TXT

**Como** CONTADOR,
**Eu quero** importar plano de contas via arquivo CSV ou TXT com campos ECF-specific,
**Para que** eu possa carregar rapidamente centenas de contas de sistemas ERP externos já vinculadas a Contas Referenciais RFB.

### Acceptance Criteria

1. Endpoint `POST /api/v1/chart-of-accounts/import` criado (CONTADOR com header X-Company-Id):
   - Aceita `multipart/form-data` com arquivo CSV/TXT
   - Query param obrigatório: `?fiscalYear=2024`
   - Query param opcional: `?dryRun=true` (preview sem persistir)
2. DTO `ImportChartOfAccountRequest`:
   - `file` (MultipartFile, obrigatório)
   - `fiscalYear` (Integer, obrigatório)
   - `dryRun` (Boolean, default false)
3. DTO `ImportChartOfAccountResponse`:
   - `success` (boolean)
   - `message` (string)
   - `totalLines` (int - total de linhas no arquivo)
   - `processedLines` (int - linhas processadas com sucesso)
   - `skippedLines` (int - linhas puladas por erro)
   - `errors` (lista de objetos: `{lineNumber, error}`)
   - `preview` (lista de contas que seriam criadas - apenas se dryRun=true)
4. Formato do arquivo CSV/TXT esperado:
   - **Separador**: `;` (ponto e vírgula) ou `,` (vírgula) - detectado automaticamente
   - **Encoding**: UTF-8
   - **Header obrigatório** (primeira linha): `code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel`
   - **Exemplo**:
     ```
     code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
     1.1.01.001;Caixa;ATIVO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
     1.1.02.001;Bancos Conta Movimento;ATIVO;1.01.02;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
     3.1.01.001;Receita de Vendas;RECEITA;3.01;RECEITA_BRUTA;4;CREDORA;true;false
     ```
5. Use case `ImportChartOfAccountUseCase` implementado:
   - Valida que arquivo não está vazio (max 10MB)
   - Valida encoding UTF-8
   - Parse linha por linha usando biblioteca CSV (Apache Commons CSV ou OpenCSV)
   - Para cada linha:
     - Busca ContaReferencial por codigoRfb (coluna contaReferencialCodigo)
     - Se conta referencial não existe: registra erro e pula linha
     - Valida cada linha: campos obrigatórios, formato de accountType, classe, nivel (1-5), natureza, afetaResultado (true/false), dedutivel (true/false)
   - Se dryRun=false: persiste contas (transação atômica - falha em uma linha não impede as outras)
   - Se dryRun=true: retorna preview sem persistir
   - Ignora linhas duplicadas (mesmo code para mesma empresa/ano)
   - Retorna relatório detalhado
6. Validações por linha:
   - code não vazio
   - name não vazio
   - accountType deve ser um dos valores válidos do enum (case insensitive)
   - contaReferencialCodigo não vazio, deve existir na tabela tb_conta_referencial e estar ACTIVE
   - classe deve ser um dos valores válidos do enum (case insensitive)
   - nivel deve ser integer entre 1 e 5
   - natureza deve ser DEVEDORA ou CREDORA (case insensitive)
   - afetaResultado deve ser true/false/yes/no/sim/não (case insensitive)
   - dedutivel deve ser true/false/yes/no/sim/não (case insensitive)
7. Tratamento de erros:
   - Linha com campos faltando: registra erro e continua próxima linha
   - accountType inválido: registra erro e continua
   - contaReferencialCodigo não encontrado: registra erro `Conta Referencial '{codigo}' not found` e continua
   - classe inválido: registra erro e continua
   - nivel fora do range: registra erro `nivel must be between 1 and 5` e continua
   - Duplicata dentro do arquivo: registra warning e ignora duplicata
   - Duplicata com dados já existentes no DB: registra warning e ignora
8. Response:
   - 200 OK com relatório completo (mesmo se houve erros em linhas individuais)
   - 400 Bad Request se arquivo vazio, formato inválido, ou fiscalYear ausente
   - 413 Payload Too Large se arquivo > 10MB
9. Teste valida:
   - Importação com arquivo válido processa todas linhas
   - Dry run retorna preview sem persistir
   - Linha com campo faltando é registrada como erro
   - accountType inválido é registrado como erro
   - contaReferencialCodigo inexistente é registrado como erro
   - nivel fora do range é registrado como erro
   - Duplicatas são ignoradas com warning
   - Encoding UTF-8 é respeitado (caracteres acentuados)
   - Arquivo > 10MB retorna 413

---

## Story 3.4: Entidade ContaReferencial (Tabela Mestra RFB) e Repository

**Como** desenvolvedor,
**Eu quero** entidade ContaReferencial global (não vinculada a empresa),
**Para que** possamos persistir Contas Referenciais oficiais da Receita Federal Brasil que serão referenciadas por ChartOfAccount.

### Acceptance Criteria

1. Entidade JPA `ContaReferencialEntity` criada estendendo `BaseEntity`:
   - `@Column(nullable=false, unique=true) String codigoRfb` (código oficial RFB, ex: "1.01.01", "3.01")
   - `@Column(nullable=false, length=1000) String descricao` (descrição oficial da conta referencial)
   - `@Column Integer anoValidade` (ano de validade - nullable, null = válido para todos anos)
2. Constraint de unicidade: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"codigo_rfb", "ano_validade"}))`
   - Garante que não existam códigos duplicados por ano (suporta mudanças anuais no layout ECF)
3. Interface `ContaReferencialRepositoryPort` criada em `application/port/out/`:
   - `ContaReferencial save(ContaReferencial conta)`
   - `Optional<ContaReferencial> findById(Long id)`
   - `Optional<ContaReferencial> findByCodigoRfb(String codigoRfb)`
   - `List<ContaReferencial> findByAnoValidade(Integer anoValidade)`
   - `List<ContaReferencial> findAll()`
   - `Page<ContaReferencial> findAll(Pageable pageable)`
4. Interface `ContaReferencialJpaRepository` criada estendendo `JpaRepository<ContaReferencialEntity, Long>`:
   - `Optional<ContaReferencialEntity> findByCodigoRfb(String codigoRfb)`
   - `List<ContaReferencialEntity> findByAnoValidade(Integer anoValidade)`
5. Classe `ContaReferencialRepositoryAdapter` implementa `ContaReferencialRepositoryPort`
6. Model `ContaReferencial` (domain) criado em `domain/model/` como POJO puro
7. Mapper MapStruct `ContaReferencialMapper` criado
8. Teste de integração valida:
   - Salvar conta referencial e recuperar por codigoRfb
   - Unique constraint funciona (duplicata com mesmo codigo + ano lança exception)
   - Buscar por anoValidade
   - Conta com anoValidade=null pode coexistir com conta de mesmo código mas anoValidade específico
   - Soft delete funciona
   - Listagem paginada

---

## Story 3.5: CRUD de Contas Referenciais RFB (ADMIN-only)

**Como** ADMIN,
**Eu quero** criar, listar, visualizar, editar e inativar Contas Referenciais RFB,
**Para que** eu possa gerenciar a tabela mestra de contas oficiais da Receita Federal que serão usadas por todas empresas.

### Acceptance Criteria

1. Controller `ContaReferencialController` criado com endpoints:
   - `POST /api/v1/conta-referencial` - criar conta referencial (ADMIN-only)
   - `GET /api/v1/conta-referencial` - listar contas com paginação (ADMIN e CONTADOR podem ler)
   - `GET /api/v1/conta-referencial/{id}` - visualizar conta (ADMIN e CONTADOR podem ler)
   - `PUT /api/v1/conta-referencial/{id}` - editar conta (ADMIN-only)
   - `PATCH /api/v1/conta-referencial/{id}/status` - alternar status (ADMIN-only)
2. DTOs criados: `CreateContaReferencialRequest`, `UpdateContaReferencialRequest`, `ContaReferencialResponse`
3. `CreateContaReferencialRequest`:
   - `codigoRfb` (obrigatório, String)
   - `descricao` (obrigatório, String, max 1000 chars)
   - `anoValidade` (opcional, Integer)
4. `ContaReferencialResponse`:
   - `id`, `codigoRfb`, `descricao`, `anoValidade`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateContaReferencialUseCase`: valida unicidade (codigoRfb + anoValidade), salva (ADMIN-only via @PreAuthorize)
   - `ListContaReferencialUseCase`: retorna todas contas (CONTADOR pode ler para lookup)
   - `GetContaReferencialUseCase`: retorna conta por ID (CONTADOR pode ler)
   - `UpdateContaReferencialUseCase`: permite editar descricao e anoValidade (ADMIN-only, não permite editar codigoRfb)
   - `ToggleContaReferencialStatusUseCase`: alterna status (ADMIN-only)
6. Validações:
   - codigoRfb não pode ser vazio ou conter apenas espaços
   - descricao não pode ser vazia
   - anoValidade se fornecido deve ser >= 2000 e <= ano atual + 5
   - Combinação (codigoRfb + anoValidade) deve ser única
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=codigoRfb,asc`
   - Filtro por ano: `?anoValidade=2024`
   - Busca: `?search=texto` (busca em codigoRfb e descricao)
   - Filtro por status: `?include_inactive=true` (padrão: apenas ACTIVE)
8. Autorização:
   - Endpoints POST, PUT, PATCH: `@PreAuthorize("hasRole('ADMIN')")`
   - Endpoints GET: `@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")` - CONTADOR pode ler para fazer lookup ao criar plano de contas
9. DTOs adicionais: `ToggleStatusRequest`, `ToggleStatusResponse` (reutilizados)
10. Teste valida:
    - ADMIN consegue criar conta referencial
    - CONTADOR consegue listar e visualizar contas (read-only)
    - CONTADOR NÃO consegue criar/editar/inativar (403 Forbidden)
    - Código duplicado com mesmo anoValidade retorna 400
    - Edição não permite mudar codigoRfb
    - Toggle status funciona (ADMIN-only)
    - Listagem com filtros funciona

---

## Story 3.6: Entidade ContaParteB (Conta da Parte B e-Lalur/e-Lacs) e Repository

**Como** desenvolvedor,
**Eu quero** entidade ContaParteB relacionada a Company com campos para e-Lalur/e-Lacs,
**Para que** possamos persistir Contas da Parte B específicas de IRPJ/CSLL com vigência temporal e saldo inicial.

### Acceptance Criteria

1. Entidade JPA `ContaParteBEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (empresa dona do cadastro)
   - `@Column(nullable=false) String codigoConta` (código da conta Parte B, ex: "4.01.01")
   - `@Column(nullable=false, length=1000) String descricao` (descrição da conta)
   - `@Column(nullable=false) Integer anoBase` (ano base de criação/referência)
   - `@Column(nullable=false) LocalDate dataVigenciaInicio` (data início de vigência)
   - `@Column LocalDate dataVigenciaFim` (data fim de vigência - nullable, null = vigente)
   - `@Enumerated(STRING) TipoTributo tipoTributo` (IRPJ, CSLL, AMBOS)
   - `@Column(precision=19, scale=2) BigDecimal saldoInicial` (saldo inicial da conta)
   - `@Enumerated(STRING) TipoSaldo tipoSaldo` (DEVEDOR, CREDOR)
2. Enums criados:
   - `TipoTributo`: IRPJ, CSLL, AMBOS
   - `TipoSaldo`: DEVEDOR, CREDOR
3. Constraint de unicidade: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "codigo_conta", "ano_base"}))`
4. Interface `ContaParteBRepositoryPort` criada em `application/port/out/`:
   - `ContaParteB save(ContaParteB conta)`
   - `Optional<ContaParteB> findById(Long id)`
   - `List<ContaParteB> findByCompanyIdAndAnoBase(Long companyId, Integer anoBase)`
   - `Optional<ContaParteB> findByCompanyIdAndCodigoContaAndAnoBase(Long companyId, String codigoConta, Integer anoBase)`
   - `Page<ContaParteB> findByCompanyId(Long companyId, Pageable pageable)`
5. Interface `ContaParteBJpaRepository` criada estendendo `JpaRepository<ContaParteBEntity, Long>`:
   - `List<ContaParteBEntity> findByCompanyIdAndAnoBase(Long companyId, Integer anoBase)`
   - `Optional<ContaParteBEntity> findByCompanyIdAndCodigoContaAndAnoBase(Long companyId, String codigoConta, Integer anoBase)`
6. Classe `ContaParteBRepositoryAdapter` implementa `ContaParteBRepositoryPort`
7. Model `ContaParteB` (domain) criado em `domain/model/` como POJO puro
8. Mapper MapStruct `ContaParteBMapper` criado
9. Teste de integração valida:
   - Salvar conta Parte B com todos campos e recuperar por company + anoBase
   - Unique constraint funciona
   - Buscar conta por company + codigoConta + anoBase
   - Soft delete funciona
   - Listagem paginada por empresa
   - Campos tipoTributo e tipoSaldo são persistidos corretamente

---

## Story 3.7: CRUD de Contas da Parte B (Manual)

**Como** CONTADOR,
**Eu quero** criar, listar, visualizar, editar e inativar Contas da Parte B manualmente,
**Para que** eu possa cadastrar contas específicas de e-Lalur/e-Lacs para apuração de IRPJ/CSLL.

### Acceptance Criteria

1. Controller `ContaParteBController` criado com endpoints:
   - `POST /api/v1/conta-parte-b` - criar conta (CONTADOR com header X-Company-Id)
   - `GET /api/v1/conta-parte-b` - listar contas com paginação (CONTADOR com header)
   - `GET /api/v1/conta-parte-b/{id}` - visualizar conta (CONTADOR com header)
   - `PUT /api/v1/conta-parte-b/{id}` - editar conta (CONTADOR com header)
   - `PATCH /api/v1/conta-parte-b/{id}/status` - alternar status (CONTADOR com header)
2. DTOs criados: `CreateContaParteBRequest`, `UpdateContaParteBRequest`, `ContaParteBResponse`
3. `CreateContaParteBRequest`:
   - `codigoConta` (obrigatório, String)
   - `descricao` (obrigatório, String, max 1000 chars)
   - `anoBase` (obrigatório, Integer)
   - `dataVigenciaInicio` (obrigatório, LocalDate)
   - `dataVigenciaFim` (opcional, LocalDate)
   - `tipoTributo` (obrigatório, enum: IRPJ/CSLL/AMBOS)
   - `saldoInicial` (obrigatório, BigDecimal)
   - `tipoSaldo` (obrigatório, enum: DEVEDOR/CREDOR)
4. `ContaParteBResponse`:
   - `id`, `codigoConta`, `descricao`, `anoBase`, `dataVigenciaInicio`, `dataVigenciaFim`, `tipoTributo`, `saldoInicial`, `tipoSaldo`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateContaParteBUseCase`: valida company via `CompanyContext`, verifica unicidade, salva
   - `ListContaParteBUseCase`: retorna contas da empresa no contexto
   - `GetContaParteBUseCase`: retorna conta por ID
   - `UpdateContaParteBUseCase`: permite editar (exceto codigoConta e anoBase)
   - `ToggleContaParteBStatusUseCase`: alterna status
6. Validações:
   - codigoConta não pode ser vazio
   - descricao não pode ser vazia
   - anoBase deve ser >= 2000 e <= ano atual + 1
   - dataVigenciaInicio obrigatória
   - Se dataVigenciaFim fornecida, deve ser >= dataVigenciaInicio
   - saldoInicial >= 0
   - Combinação (company + codigoConta + anoBase) deve ser única
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=codigoConta,asc`
   - Filtro por ano: `?anoBase=2024`
   - Filtro por tipo tributo: `?tipoTributo=IRPJ`
   - Busca: `?search=texto` (busca em codigoConta e descricao)
   - Filtro por status: `?include_inactive=true`
8. DTOs adicionais: `ToggleStatusRequest`, `ToggleStatusResponse` (reutilizados)
9. Teste valida:
   - CONTADOR consegue criar conta Parte B com todos campos
   - Código duplicado para mesma empresa/ano retorna 400
   - dataVigenciaFim < dataVigenciaInicio retorna 400
   - saldoInicial negativo retorna 400
   - CONTADOR sem header X-Company-Id recebe 400
   - Edição não permite mudar codigoConta ou anoBase
   - Toggle status funciona
   - Listagem com filtros funciona

---

## Story 3.8: Entidade LancamentoParteB (Lançamento da Parte B) e Repository

**Como** desenvolvedor,
**Eu quero** entidade LancamentoParteB relacionada a Company, ChartOfAccount, ContaParteB e TaxParameter,
**Para que** possamos persistir lançamentos de ajustes fiscais (adições/exclusões) na Parte B com vinculação flexível a contas contábeis e/ou contas Parte B.

### Acceptance Criteria

1. Entidade JPA `LancamentoParteBEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company`
   - `@Column(nullable=false) Integer mesReferencia` (mês de referência 1-12)
   - `@Column(nullable=false) Integer anoReferencia` (ano de referência)
   - `@Enumerated(STRING) TipoApuracao tipoApuracao` (IRPJ, CSLL)
   - `@Enumerated(STRING) TipoRelacionamento tipoRelacionamento` (CONTA_CONTABIL, CONTA_PARTE_B, AMBOS)
   - `@ManyToOne @JoinColumn(name="conta_contabil_id") ChartOfAccountEntity contaContabil` (nullable)
   - `@ManyToOne @JoinColumn(name="conta_parte_b_id") ContaParteBEntity contaParteB` (nullable)
   - `@ManyToOne @JoinColumn(nullable=false) TaxParameterEntity parametroTributario` (FK obrigatória)
   - `@Enumerated(STRING) TipoAjuste tipoAjuste` (ADICAO, EXCLUSAO)
   - `@Column(nullable=false, length=2000) String descricao` (descrição do ajuste)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal valor` (valor sempre positivo)
2. Enums criados:
   - `TipoApuracao`: IRPJ, CSLL
   - `TipoRelacionamento`: CONTA_CONTABIL, CONTA_PARTE_B, AMBOS
   - `TipoAjuste`: ADICAO, EXCLUSAO
3. Check constraints (validação via JPA @PrePersist e @PreUpdate):
   - Se tipoRelacionamento = CONTA_CONTABIL: contaContabil NOT NULL e contaParteB NULL
   - Se tipoRelacionamento = CONTA_PARTE_B: contaParteB NOT NULL e contaContabil NULL
   - Se tipoRelacionamento = AMBOS: contaContabil NOT NULL e contaParteB NOT NULL
   - valor > 0
4. Interface `LancamentoParteBRepositoryPort` criada:
   - `LancamentoParteB save(LancamentoParteB lancamento)`
   - `Optional<LancamentoParteB> findById(Long id)`
   - `List<LancamentoParteB> findByCompanyIdAndAnoReferencia(Long companyId, Integer anoReferencia)`
   - `List<LancamentoParteB> findByCompanyIdAndAnoReferenciaAndTipoApuracao(Long companyId, Integer anoReferencia, TipoApuracao tipoApuracao)`
   - `Page<LancamentoParteB> findByCompanyId(Long companyId, Pageable pageable)`
5. Interface `LancamentoParteBJpaRepository` criada estendendo `JpaRepository<LancamentoParteBEntity, Long>`:
   - Métodos de busca customizados conforme port
6. Classe `LancamentoParteBRepositoryAdapter` implementa `LancamentoParteBRepositoryPort`
7. Model `LancamentoParteB` (domain) criado como POJO puro
8. Mapper MapStruct `LancamentoParteBMapper` criado
9. Validação customizada `@LancamentoParteBValidator` annotation criada:
   - Valida regras condicionais de FK baseadas em tipoRelacionamento
   - Valida que valor > 0
10. Teste de integração valida:
    - Salvar lançamento com tipoRelacionamento=CONTA_CONTABIL (contaContabil NOT NULL, contaParteB NULL)
    - Salvar lançamento com tipoRelacionamento=CONTA_PARTE_B (contaParteB NOT NULL, contaContabil NULL)
    - Salvar lançamento com tipoRelacionamento=AMBOS (ambos NOT NULL)
    - Tentar salvar com FKs incorretas conforme tipoRelacionamento lança validation exception
    - Valor negativo ou zero lança validation exception
    - Soft delete funciona
    - Listagem paginada e filtrada por anoReferencia e tipoApuracao

---

## Story 3.9: CRUD de Lançamentos da Parte B (Manual)

**Como** CONTADOR,
**Eu quero** criar, listar, visualizar, editar e inativar Lançamentos da Parte B manualmente,
**Para que** eu possa registrar ajustes fiscais (adições/exclusões) IRPJ/CSLL com fundamentação via parâmetros tributários.

### Acceptance Criteria

1. Controller `LancamentoParteBController` criado com endpoints:
   - `POST /api/v1/lancamento-parte-b` - criar lançamento (CONTADOR com header X-Company-Id)
   - `GET /api/v1/lancamento-parte-b` - listar lançamentos com paginação (CONTADOR com header)
   - `GET /api/v1/lancamento-parte-b/{id}` - visualizar lançamento (CONTADOR com header)
   - `PUT /api/v1/lancamento-parte-b/{id}` - editar lançamento (CONTADOR com header)
   - `PATCH /api/v1/lancamento-parte-b/{id}/status` - alternar status (CONTADOR com header)
2. DTOs criados: `CreateLancamentoParteBRequest`, `UpdateLancamentoParteBRequest`, `LancamentoParteBResponse`
3. `CreateLancamentoParteBRequest`:
   - `mesReferencia` (obrigatório, Integer 1-12)
   - `anoReferencia` (obrigatório, Integer)
   - `tipoApuracao` (obrigatório, enum: IRPJ/CSLL)
   - `tipoRelacionamento` (obrigatório, enum: CONTA_CONTABIL/CONTA_PARTE_B/AMBOS)
   - `contaContabilId` (Long, nullable - obrigatoriedade conforme tipoRelacionamento)
   - `contaParteBId` (Long, nullable - obrigatoriedade conforme tipoRelacionamento)
   - `parametroTributarioId` (obrigatório, Long)
   - `tipoAjuste` (obrigatório, enum: ADICAO/EXCLUSAO)
   - `descricao` (obrigatório, String max 2000 chars)
   - `valor` (obrigatório, BigDecimal > 0)
4. `LancamentoParteBResponse`:
   - `id`, `mesReferencia`, `anoReferencia`, `tipoApuracao`, `tipoRelacionamento`
   - `contaContabilId`, `contaContabilCodigo`, `contaContabilNome` (nullable)
   - `contaParteBId`, `contaParteBCodigo`, `contaParteBDescricao` (nullable)
   - `parametroTributarioId`, `parametroTributarioCodigo`, `parametroTributarioDescricao`
   - `tipoAjuste`, `descricao`, `valor`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateLancamentoParteBUseCase`:
     - Valida company via `CompanyContext`
     - Valida que parametroTributarioId existe e está ACTIVE
     - Valida FKs condicionais baseadas em tipoRelacionamento:
       - Se CONTA_CONTABIL: contaContabilId obrigatório, deve existir e pertencer à empresa
       - Se CONTA_PARTE_B: contaParteBId obrigatório, deve existir e pertencer à empresa
       - Se AMBOS: ambos obrigatórios, devem existir e pertencer à empresa
     - Valida valor > 0
     - Salva
   - `ListLancamentoParteBUseCase`: retorna lançamentos da empresa no contexto
   - `GetLancamentoParteBUseCase`: retorna lançamento por ID
   - `UpdateLancamentoParteBUseCase`: permite editar todos campos (mantém mesmas validações do create)
   - `ToggleLancamentoParteBStatusUseCase`: alterna status
6. Validações:
   - mesReferencia entre 1 e 12
   - anoReferencia >= 2000 e <= ano atual + 1
   - parametroTributarioId deve existir e estar ACTIVE
   - Validações condicionais de FKs conforme tipoRelacionamento
   - descricao não pode ser vazia
   - valor > 0
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=anoReferencia,desc&sort=mesReferencia,desc`
   - Filtro por ano: `?anoReferencia=2024`
   - Filtro por mês: `?mesReferencia=1`
   - Filtro por tipo apuração: `?tipoApuracao=IRPJ`
   - Filtro por tipo ajuste: `?tipoAjuste=ADICAO`
   - Filtro por status: `?include_inactive=true`
8. DTOs adicionais: `ToggleStatusRequest`, `ToggleStatusResponse` (reutilizados)
9. Teste valida:
   - CONTADOR consegue criar lançamento com tipoRelacionamento=CONTA_CONTABIL
   - CONTADOR consegue criar lançamento com tipoRelacionamento=CONTA_PARTE_B
   - CONTADOR consegue criar lançamento com tipoRelacionamento=AMBOS
   - Tentar criar com contaContabilId de outra empresa retorna 400
   - Tentar criar com contaParteBId de outra empresa retorna 400
   - Tentar criar com parametroTributarioId inexistente retorna 400
   - Tentar criar com parametroTributarioId INACTIVE retorna 400
   - mesReferencia fora do range 1-12 retorna 400
   - valor zero ou negativo retorna 400
   - Edição funciona com revalidações
   - Toggle status funciona
   - Listagem com filtros funciona

---

## Story 3.10: Entidade LancamentoContabil (Lançamento Contábil) e Repository

**Como** desenvolvedor,
**Eu quero** entidade LancamentoContabil relacionada a ChartOfAccount com partidas dobradas,
**Para que** possamos persistir lançamentos contábeis com método de débito/crédito e permitir CRUD completo além de importação CSV.

### Acceptance Criteria

1. Entidade JPA `LancamentoContabilEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company`
   - `@ManyToOne @JoinColumn(name="conta_debito_id", nullable=false) ChartOfAccountEntity contaDebito` (conta de débito)
   - `@ManyToOne @JoinColumn(name="conta_credito_id", nullable=false) ChartOfAccountEntity contaCredito` (conta de crédito)
   - `@Column(nullable=false) LocalDate data` (data do lançamento)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal valor` (valor sempre positivo)
   - `@Column(nullable=false, length=2000) String historico` (descrição/histórico)
   - `@Column(length=100) String numeroDocumento` (número do documento - opcional)
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal do lançamento)
2. Check constraints (validação via JPA @PrePersist e @PreUpdate):
   - valor > 0
   - contaDebito.id != contaCredito.id (débito e crédito devem ser contas diferentes)
3. Interface `TemporalEntity` implementada (para Período Contábil):
   - `LocalDate getData()` retorna o campo `data`
4. Interface `LancamentoContabilRepositoryPort` criada:
   - `LancamentoContabil save(LancamentoContabil lancamento)`
   - `Optional<LancamentoContabil> findById(Long id)`
   - `List<LancamentoContabil> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Page<LancamentoContabil> findByCompanyId(Long companyId, Pageable pageable)`
5. Interface `LancamentoContabilJpaRepository` criada estendendo `JpaRepository<LancamentoContabilEntity, Long>`:
   - `List<LancamentoContabilEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
6. Classe `LancamentoContabilRepositoryAdapter` implementa `LancamentoContabilRepositoryPort`
7. Model `LancamentoContabil` (domain) criado como POJO puro
8. Mapper MapStruct `LancamentoContabilMapper` criado
9. Validação customizada `@LancamentoContabilValidator` annotation criada:
   - Valida que contaDebitoId != contaCreditoId
   - Valida que valor > 0
   - Valida que data respeita Período Contábil da empresa
10. Teste de integração valida:
    - Salvar lançamento com partidas dobradas (débito/crédito diferentes)
    - Tentar salvar com mesma conta em débito e crédito lança validation exception
    - Valor zero ou negativo lança validation exception
    - Data anterior a Período Contábil lança validation exception
    - Soft delete funciona
    - Listagem paginada por empresa e fiscalYear

---

## Story 3.11: Importação de Lançamentos Contábeis via CSV/TXT

**Como** CONTADOR,
**Eu quero** importar lançamentos contábeis via arquivo CSV ou TXT com partidas dobradas,
**Para que** eu possa carregar rapidamente milhares de lançamentos de sistemas ERP externos.

### Acceptance Criteria

1. Endpoint `POST /api/v1/lancamento-contabil/import` criado (CONTADOR com header X-Company-Id):
   - Aceita `multipart/form-data` com arquivo CSV/TXT
   - Query param obrigatório: `?fiscalYear=2024`
   - Query param opcional: `?dryRun=true`
2. DTO `ImportLancamentoContabilRequest`:
   - `file` (MultipartFile, obrigatório)
   - `fiscalYear` (Integer, obrigatório)
   - `dryRun` (Boolean, default false)
3. DTO `ImportLancamentoContabilResponse`:
   - `success`, `message`, `totalLines`, `processedLines`, `skippedLines`, `errors`, `preview`
4. Formato do arquivo CSV/TXT esperado:
   - **Separador**: `;` ou `,` (detectado automaticamente)
   - **Encoding**: UTF-8
   - **Header obrigatório**: `contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento`
   - **Exemplo**:
     ```
     contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento
     1.1.01.001;3.1.01.001;2024-01-15;1000.00;Venda mercadoria;NF-123
     5.1.01.001;1.1.02.001;2024-01-20;500.00;Pagamento fornecedor;DOC-456
     ```
5. Use case `ImportLancamentoContabilUseCase` implementado:
   - Valida arquivo (max 50MB)
   - Parse linha por linha
   - Para cada linha:
     - Busca ChartOfAccount por contaDebitoCode + companyId + fiscalYear
     - Busca ChartOfAccount por contaCreditoCode + companyId + fiscalYear
     - Se alguma conta não existe: registra erro e pula linha
     - Valida formato de data (ISO 8601: YYYY-MM-DD)
     - Valida que contaDebito != contaCredito
     - Valida valor > 0
     - Valida que data >= company.periodoContabil
     - Cria LancamentoContabil e persiste
   - Se dryRun=true: retorna preview sem persistir
6. Validações por linha:
   - contaDebitoCode e contaCreditoCode devem existir no plano de contas da empresa/ano
   - contaDebitoCode != contaCreditoCode
   - data obrigatória, formato válido (YYYY-MM-DD)
   - data >= Período Contábil
   - valor obrigatório, > 0
   - historico obrigatório
   - numeroDocumento opcional
7. Tratamento de erros:
   - Conta não encontrada: `Account code '{code}' not found for company/year`
   - Mesma conta em débito e crédito: `Debit and credit accounts must be different`
   - Data anterior a Período Contábil: `Data {date} is before Período Contábil {date}`
   - Valor inválido: `Invalid value: must be > 0`
8. Response:
   - 200 OK com relatório completo
   - 400 Bad Request se arquivo vazio ou fiscalYear ausente
   - 413 Payload Too Large se arquivo > 50MB
9. Teste valida:
   - Importação bem-sucedida processa todas linhas
   - Conta inexistente registra erro e continua
   - Mesma conta em débito e crédito registra erro
   - Data anterior a Período Contábil registra erro
   - Dry run retorna preview
   - Arquivo com 10.000 linhas é processado em < 30s

---

## Story 3.12: Exportação de Lançamentos Contábeis para CSV

**Como** CONTADOR,
**Eu quero** exportar lançamentos contábeis para arquivo CSV,
**Para que** eu possa fazer backup, análises externas ou compartilhar com outros sistemas.

### Acceptance Criteria

1. Endpoint `GET /api/v1/lancamento-contabil/export` criado (CONTADOR com header X-Company-Id):
   - Query param obrigatório: `?fiscalYear=2024`
   - Query param opcional: `?dataInicio=2024-01-01` e `?dataFim=2024-12-31`
   - Response: arquivo CSV para download (Content-Type: text/csv; charset=UTF-8)
2. Use case `ExportLancamentoContabilUseCase` implementado:
   - Busca todos lançamentos da empresa no contexto + fiscalYear
   - Se dataInicio/Fim fornecidos: filtra por range de data
   - Gera arquivo CSV com formato:
     ```
     contaDebitoCode;contaDebitoName;contaCreditoCode;contaCreditoName;data;valor;historico;numeroDocumento
     1.1.01.001;Caixa;3.1.01.001;Receita de Vendas;2024-01-15;1000.00;Venda mercadoria;NF-123
     ```
   - Header Content-Disposition: `attachment; filename="lancamentos-contabeis-{companyId}-{fiscalYear}.csv"`
3. Formato do arquivo gerado:
   - **Separador**: `;`
   - **Encoding**: UTF-8
   - **Linha 1**: Header
   - **Linhas 2+**: Dados, ordenados por data ASC
   - **Números**: formato `1000.00` (ponto decimal, 2 casas)
   - **Datas**: formato ISO 8601 (YYYY-MM-DD)
4. Validações:
   - fiscalYear obrigatório
   - Se dataInicio fornecido sem dataFim: erro 400
   - Se dataFim < dataInicio: erro 400
5. Response:
   - 200 OK com arquivo CSV
   - 400 Bad Request se validações falharem
   - 404 Not Found se nenhum lançamento encontrado
6. Teste valida:
   - Exportação gera arquivo válido com header correto
   - Arquivo pode ser reimportado sem erros (round-trip)
   - Filtro por range de data funciona
   - Encoding UTF-8 preserva caracteres especiais
   - Arquivo com 10.000 linhas é gerado em < 10s

---

## Story 3.13: CRUD de Lançamentos Contábeis (Manual)

**Como** CONTADOR,
**Eu quero** criar, visualizar, editar e inativar lançamentos contábeis manualmente,
**Para que** eu possa fazer ajustes pontuais sem precisar reimportar arquivo completo.

### Acceptance Criteria

1. Controller `LancamentoContabilController` criado com endpoints:
   - `POST /api/v1/lancamento-contabil` - criar lançamento (CONTADOR com header)
   - `GET /api/v1/lancamento-contabil` - listar lançamentos com paginação (CONTADOR com header)
   - `GET /api/v1/lancamento-contabil/{id}` - visualizar lançamento (CONTADOR com header)
   - `PUT /api/v1/lancamento-contabil/{id}` - editar lançamento (CONTADOR com header)
   - `PATCH /api/v1/lancamento-contabil/{id}/status` - alternar status (CONTADOR com header)
2. DTOs criados: `CreateLancamentoContabilRequest`, `UpdateLancamentoContabilRequest`, `LancamentoContabilResponse`
3. `CreateLancamentoContabilRequest`:
   - `contaDebitoId` (obrigatório, Long)
   - `contaCreditoId` (obrigatório, Long)
   - `data` (obrigatório, LocalDate)
   - `valor` (obrigatório, BigDecimal > 0)
   - `historico` (obrigatório, String max 2000 chars)
   - `numeroDocumento` (opcional, String max 100 chars)
   - `fiscalYear` (obrigatório, Integer)
4. `LancamentoContabilResponse`:
   - `id`, `contaDebitoId`, `contaDebitoCodigo`, `contaDebitoNome`, `contaCreditoId`, `contaCreditoCodigo`, `contaCreditoNome`, `data`, `valor`, `historico`, `numeroDocumento`, `fiscalYear`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateLancamentoContabilUseCase`:
     - Valida que contaDebitoId e contaCreditoId existem e pertencem à empresa do contexto
     - Valida que contaDebitoId != contaCreditoId
     - Valida que ambas contas têm fiscalYear compatível
     - Valida Período Contábil (data >= company.periodoContabil)
     - Valida valor > 0
     - Salva
   - `ListLancamentoContabilUseCase`: retorna lançamentos da empresa no contexto
   - `GetLancamentoContabilUseCase`: retorna lançamento por ID
   - `UpdateLancamentoContabilUseCase`: valida Período Contábil antes de permitir edição
   - `ToggleLancamentoContabilStatusUseCase`: alterna status (também valida Período Contábil)
6. Validações:
   - contaDebitoId e contaCreditoId devem existir e pertencer à empresa do contexto
   - contaDebitoId != contaCreditoId
   - data >= company.periodoContabil
   - valor > 0
   - historico não pode ser vazio
   - fiscalYear deve ser >= 2000
7. Listagem suporta:
   - Paginação: `?page=0&size=100`
   - Filtro por conta débito: `?contaDebitoId={id}`
   - Filtro por conta crédito: `?contaCreditoId={id}`
   - Filtro por data: `?data=2024-01-15`
   - Filtro por range: `?dataInicio=2024-01-01&dataFim=2024-12-31`
   - Filtro por fiscalYear: `?fiscalYear=2024`
   - Ordenação: `?sort=data,desc`
8. Annotation `@EnforcePeriodoContabil` aplicada em UpdateLancamentoContabilUseCase e ToggleLancamentoContabilStatusUseCase
9. DTO adicional: `ToggleStatusRequest` e `ToggleStatusResponse` (reutilizados)
10. Teste valida:
    - CONTADOR consegue criar lançamento com partidas dobradas
    - Tentativa de criar com mesma conta em débito e crédito retorna 400
    - Tentativa de criar com data < Período Contábil retorna 400
    - Tentativa de criar com valor <= 0 retorna 400
    - Tentativa de editar lançamento antigo (data < Período Contábil) retorna 400
    - Tentativa de inativar lançamento antigo retorna 400
    - Toggle status: ACTIVE → INACTIVE funciona
    - Toggle status: INACTIVE → ACTIVE funciona
    - Listagem com filtros funciona corretamente
    - Validação de partidas dobradas (débito != crédito) funciona

---

## Story 3.14: Testes de Integração End-to-End do Epic 3

**Como** desenvolvedor,
**Eu quero** testes de integração que validem fluxos completos do Epic 3 reestruturado,
**Para que** tenhamos confiança de que todas funcionalidades de plano de contas, lançamentos contábeis, contas referenciais RFB, contas Parte B e lançamentos Parte B estão integradas.

### Acceptance Criteria

1. Teste de integração: **Fluxo completo - Contas Referenciais RFB**
   - ADMIN cria 20 Contas Referenciais RFB
   - Valida que todas 20 foram criadas
   - CONTADOR lista contas referenciais (read-only)
   - CONTADOR tenta criar conta referencial → deve retornar 403 Forbidden
   - ADMIN edita descrição de 1 conta
   - Valida que edição foi persistida

2. Teste de integração: **Fluxo completo - Plano de Contas com Vinculação RFB**
   - ADMIN cria 10 Contas Referenciais RFB
   - CONTADOR seleciona empresa
   - Importa plano de contas via CSV (100 contas) com campo contaReferencialCodigo
   - Valida que todas 100 contas foram criadas com FK para ContaReferencial correta
   - Tenta reimportar mesmo arquivo → contas duplicadas são ignoradas
   - Tenta importar conta com contaReferencialCodigo inexistente → erro registrado

3. Teste de integração: **Fluxo completo - Lançamentos Contábeis com Partidas Dobradas**
   - CONTADOR cria empresa com Período Contábil = 2024-01-01
   - Importa plano de contas (50 contas)
   - Importa lançamentos contábeis via CSV (500 lançamentos com partidas dobradas)
   - Valida que 500 lançamentos foram criados
   - Valida partidas dobradas: cada lançamento tem débito != crédito
   - Exporta lançamentos → arquivo gerado deve ter 500 linhas + header
   - Round-trip: reimporta arquivo exportado → deve funcionar sem erros

4. Teste de integração: **Fluxo completo - Contas da Parte B**
   - CONTADOR cria empresa
   - Cria manualmente 5 Contas da Parte B com tipoTributo variado (IRPJ, CSLL, AMBOS)
   - Lista todas contas → deve retornar 5 registros
   - Edita descrição de 1 conta
   - Valida que edição foi persistida
   - Inativa 1 conta
   - Lista sem include_inactive → deve retornar 4 registros
   - Lista com include_inactive=true → deve retornar 5 registros

5. Teste de integração: **Fluxo completo - Lançamentos da Parte B**
   - CONTADOR cria empresa
   - Importa plano de contas (50 contas)
   - Cria 3 Contas da Parte B
   - ADMIN cria 5 parâmetros tributários
   - CONTADOR cria lançamento Parte B com tipoRelacionamento=CONTA_CONTABIL
   - CONTADOR cria lançamento Parte B com tipoRelacionamento=CONTA_PARTE_B
   - CONTADOR cria lançamento Parte B com tipoRelacionamento=AMBOS
   - Valida que 3 lançamentos foram criados com FKs corretas
   - Lista lançamentos por tipo apuração (IRPJ) → retorna apenas lançamentos IRPJ

6. Teste de integração: **Validação de Período Contábil (Lançamentos Contábeis)**
   - Empresa com Período Contábil = 2024-06-01
   - Importa plano de contas
   - Importa lançamentos com datas mistas (2024-05, 2024-06, 2024-07)
   - Validar que lançamentos de 2024-05 foram rejeitados
   - Validar que lançamentos de 2024-06 e 2024-07 foram aceitos
   - Tentar editar lançamento de 2024-06 → deve falhar (400)
   - ADMIN atualiza Período Contábil para 2024-05-01
   - Tentar editar lançamento de 2024-06 novamente → deve funcionar

7. Teste de integração: **Validação de Partidas Dobradas**
   - CONTADOR cria empresa
   - Importa plano de contas (10 contas)
   - Tenta criar lançamento com contaDebitoId = contaCreditoId → deve retornar 400
   - Tenta importar CSV com lançamentos onde débito = crédito → erro registrado para essas linhas
   - Cria lançamento válido com débito != crédito → sucesso

8. Teste de integração: **Validação de FKs Condicionais em Lançamentos Parte B**
   - CONTADOR cria empresa
   - Importa plano de contas
   - Cria conta Parte B
   - ADMIN cria parâmetro tributário
   - Tenta criar lançamento com tipoRelacionamento=CONTA_CONTABIL mas contaContabilId NULL → 400
   - Tenta criar lançamento com tipoRelacionamento=CONTA_PARTE_B mas contaParteBId NULL → 400
   - Tenta criar lançamento com tipoRelacionamento=AMBOS mas apenas contaContabilId → 400
   - Cria lançamentos válidos conforme cada tipo de relacionamento → sucesso

9. Teste de integração: **Dry Run de Importações**
   - CONTADOR chama importação de plano de contas com dryRun=true
   - Valida que preview é retornado
   - Valida que nenhuma conta foi persistida no banco
   - Chama importação sem dryRun
   - Valida que contas foram persistidas
   - CONTADOR chama importação de lançamentos contábeis com dryRun=true
   - Valida preview e ausência de persistência

10. Teste de integração: **Contexto de Empresa**
    - ADMIN cria empresa1 e empresa2
    - ADMIN cria 10 Contas Referenciais RFB (globais - não vinculadas a empresa)
    - CONTADOR seleciona empresa1
    - Importa Plano de Contas e Lançamentos Contábeis para empresa1
    - Cria Contas da Parte B e Lançamentos da Parte B para empresa1
    - CONTADOR seleciona empresa2
    - Lista todos cadastros → deve retornar vazio (empresa2 não tem dados)
    - Importa e cria mesmos dados para empresa2
    - Lista cadastros → deve retornar apenas dados da empresa2
    - Contas Referenciais RFB são compartilhadas (ambas empresas veem as mesmas)

11. Teste de integração: **Validação de Relacionamentos Cross-Entity**
    - CONTADOR cria empresa
    - ADMIN cria Contas Referenciais RFB
    - CONTADOR importa plano de contas vinculado a Contas Referenciais
    - Tenta criar lançamento contábil com contaDebitoId de outra empresa → deve retornar 400
    - Tenta criar lançamento Parte B com contaContabilId de outra empresa → deve retornar 400
    - Tenta criar lançamento Parte B com parametroTributarioId inexistente → deve retornar 400
    - ADMIN inativa Conta Referencial RFB
    - Tenta criar nova conta no plano de contas com contaReferencialId inativa → deve retornar 400

12. Todos testes usam TestContainers com PostgreSQL real
13. Todos testes limpam dados após execução

---

## Resumo do Epic 3

Ao final deste épico, o sistema terá:

1. **Tabela Mestra de Contas Referenciais RFB** (ADMIN-only CRUD, leitura compartilhada):
   - Contas oficiais da Receita Federal
   - Versionamento por ano (anoValidade)
   - Uso global por todas empresas

2. **Plano de Contas Contábil** com compliance ECF:
   - Estrutura plana vinculada obrigatoriamente a Contas Referenciais RFB
   - Campos ECF-specific (classe, nivel, natureza, afetaResultado, dedutivel)
   - CRUD manual completo
   - Importação CSV com validação de FK para ContaReferencial

3. **Lançamentos Contábeis** com partidas dobradas:
   - Método débito/crédito puro (cada lançamento tem 1 débito e 1 crédito)
   - CRUD manual completo
   - Importação/Exportação CSV
   - Validação de Período Contábil
   - Validação de partidas dobradas (débito != crédito)

4. **Contas da Parte B** (e-Lalur/e-Lacs):
   - Contas específicas para apuração fiscal IRPJ/CSLL
   - Vigência temporal (dataVigenciaInicio/Fim)
   - Tipo de tributo (IRPJ, CSLL, AMBOS)
   - Saldo inicial configurável

5. **Lançamentos da Parte B** com ajustes fiscais:
   - Adições e exclusões para IRPJ/CSLL
   - Vinculação flexível a conta contábil, conta Parte B ou ambos
   - Fundamentação via parâmetros tributários (obrigatória)
   - Validação condicional de FKs baseada em tipo de relacionamento

6. **Validações e Controles**:
   - Período Contábil protege dados históricos
   - Soft delete universal
   - Row-level security (CONTADOR vê apenas sua empresa)
   - ADMIN-only para Contas Referenciais RFB
   - Auditoria completa (@CreatedBy, @UpdatedBy)

**Dependências de Épicos Anteriores:**
- Epic 1: Autenticação JWT, roles ADMIN/CONTADOR
- Epic 2: Company entity com Período Contábil, TaxParameter entity, CompanyContext (X-Company-Id header)

**Próximos Passos:**
- Epic 4: Movimentações Lalur/Lacs & Motor de Cálculo IRPJ/CSLL (usará Lançamentos da Parte B e FiscalMovement)
