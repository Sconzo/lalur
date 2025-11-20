# Epic 3: Plano de Contas & Dados Contábeis

**Objetivo do Epic:**

Implementar cadastro e importação de plano de contas (estrutura plana, não hierárquica) por empresa/ano, permitir importação e exportação de dados contábeis via arquivos CSV/TXT, garantir unicidade de contas por empresa/ano, e fornecer endpoints para gestão completa desses dados. Este épico estabelece a base de dados contábeis que alimentará posteriormente os cálculos de IRPJ/CSLL e preenchimento da ECF. Adicionalmente, implementa três tipos específicos de cadastros contábeis fiscais: Conta da Parte B, Código de Enquadramento LALUR, e Linhas para Cadastro de Lucro Presumido. Ao final, CONTADOR poderá importar plano de contas e lançamentos contábeis de sistemas ERP externos, e ADMIN terá visibilidade total sobre os dados cadastrados.

---

## Story 3.1: Entidade ChartOfAccount (Plano de Contas) e Repository

**Como** desenvolvedor,
**Eu quero** entidade ChartOfAccount com repository JPA implementando port,
**Para que** possamos persistir contas contábeis de cada empresa por ano fiscal com estrutura plana.

### Acceptance Criteria

1. Entidade JPA `ChartOfAccountEntity` criada em `infrastructure/adapter/out/persistence/entity/` estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (empresa dona da conta)
   - `@Column(nullable=false) String accountCode` (código da conta, ex: "1.1.01.001")
   - `@Column(nullable=false) String accountName` (nome da conta, ex: "Caixa")
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal, ex: 2024)
   - `@Enumerated(STRING) AccountType accountType` (ATIVO, PASSIVO, RECEITA, DESPESA, RESULTADO)
   - `@Column(precision=19, scale=2) BigDecimal openingBalance` (saldo inicial - opcional)
2. Enum `AccountType` criado:
   - ATIVO, PASSIVO, PATRIMONIO_LIQUIDO, RECEITA, DESPESA, CUSTO, RESULTADO
3. Constraint de unicidade: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "account_code", "fiscal_year"}))`
   - Garante que não existam contas duplicadas para mesma empresa + código + ano
4. Interface `ChartOfAccountRepositoryPort` criada em `application/port/out/`:
   - `ChartOfAccount save(ChartOfAccount account)`
   - `Optional<ChartOfAccount> findById(Long id)`
   - `List<ChartOfAccount> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<ChartOfAccount> findByCompanyIdAndAccountCodeAndFiscalYear(Long companyId, String accountCode, Integer fiscalYear)`
   - `void deleteById(Long id)`
   - `Page<ChartOfAccount> findByCompanyId(Long companyId, Pageable pageable)`
5. Interface `ChartOfAccountJpaRepository` criada estendendo `JpaRepository<ChartOfAccountEntity, Long>`:
   - `List<ChartOfAccountEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<ChartOfAccountEntity> findByCompanyIdAndAccountCodeAndFiscalYear(Long companyId, String accountCode, Integer fiscalYear)`
6. Classe `ChartOfAccountRepositoryAdapter` implementa `ChartOfAccountRepositoryPort`
7. Model `ChartOfAccount` (domain) criado em `domain/model/` como POJO puro
8. Mapper MapStruct `ChartOfAccountMapper` criado
9. Teste de integração (TestContainers) valida:
   - Salvar conta e recuperar por company + fiscalYear
   - Unique constraint funciona (duplicata lança exception)
   - Buscar conta por company + accountCode + fiscalYear
   - Soft delete funciona corretamente
   - Listagem paginada por empresa

---

## Story 3.2: CRUD de Plano de Contas (Manual)

**Como** CONTADOR,
**Eu quero** criar, listar, visualizar, editar e inativar contas contábeis manualmente,
**Para que** eu possa cadastrar o plano de contas de uma empresa quando não tenho arquivo para importar.

### Acceptance Criteria

1. Controller `ChartOfAccountController` criado com endpoints:
   - `POST /api/v1/chart-of-accounts` - criar conta (CONTADOR com header X-Company-Id)
   - `GET /api/v1/chart-of-accounts` - listar contas com paginação (CONTADOR com header)
   - `GET /api/v1/chart-of-accounts/{id}` - visualizar conta (CONTADOR com header)
   - `PUT /api/v1/chart-of-accounts/{id}` - editar conta (CONTADOR com header)
   - `PATCH /api/v1/chart-of-accounts/{id}/status` - alternar status da conta (ativar/inativar, CONTADOR com header)
2. DTOs criados: `CreateChartOfAccountRequest`, `UpdateChartOfAccountRequest`, `ChartOfAccountResponse`
3. `CreateChartOfAccountRequest`:
   - `accountCode` (obrigatório)
   - `accountName` (obrigatório)
   - `fiscalYear` (obrigatório, inteiro, ex: 2024)
   - `accountType` (obrigatório, enum)
   - `openingBalance` (opcional, BigDecimal)
4. `ChartOfAccountResponse`:
   - `id`, `accountCode`, `accountName`, `fiscalYear`, `accountType`, `openingBalance`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateChartOfAccountUseCase`: valida company via `CompanyContext`, verifica unicidade (company + code + year), salva
   - `ListChartOfAccountsUseCase`: retorna contas da empresa no contexto
   - `GetChartOfAccountUseCase`: retorna conta por ID (validando que pertence à empresa do contexto)
   - `UpdateChartOfAccountUseCase`: permite editar (exceto accountCode e fiscalYear)
   - `ToggleChartOfAccountStatusUseCase`: alterna status entre ACTIVE e INACTIVE
6. Validações no `CreateChartOfAccountUseCase`:
   - Account code não pode ser vazio ou conter apenas espaços
   - Fiscal year deve ser >= 2000 e <= ano atual + 1
   - Combinação (company + accountCode + fiscalYear) deve ser única
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=accountCode,asc`
   - Filtro por ano: `?fiscalYear=2024`
   - Filtro por tipo: `?accountType=ATIVO`
   - Busca: `?search=Caixa` (busca em accountCode e accountName)
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
   - CONTADOR consegue criar conta no contexto da empresa selecionada
   - Código duplicado para mesma empresa/ano retorna 400 Bad Request
   - CONTADOR sem header X-Company-Id recebe 400
   - Listagem retorna apenas contas da empresa no contexto
   - Edição não permite mudar accountCode ou fiscalYear
   - Toggle status: ACTIVE → INACTIVE funciona
   - Toggle status: INACTIVE → ACTIVE funciona
   - Listagem com include_inactive=true retorna contas inativas

---

## Story 3.3: Importação de Plano de Contas via CSV/TXT

**Como** CONTADOR,
**Eu quero** importar plano de contas via arquivo CSV ou TXT,
**Para que** eu possa carregar rapidamente centenas de contas de sistemas ERP externos.

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
   - **Header obrigatório** (primeira linha): `accountCode;accountName;accountType;openingBalance`
   - **Exemplo**:
     ```
     accountCode;accountName;accountType;openingBalance
     1.1.01.001;Caixa;ATIVO;5000.00
     1.1.02.001;Bancos Conta Movimento;ATIVO;100000.50
     2.1.01.001;Fornecedores;PASSIVO;25000.00
     ```
5. Use case `ImportChartOfAccountUseCase` implementado:
   - Valida que arquivo não está vazio (max 10MB)
   - Valida encoding UTF-8
   - Parse linha por linha usando biblioteca CSV (Apache Commons CSV ou OpenCSV)
   - Valida cada linha: campos obrigatórios, formato de accountType, formato de openingBalance
   - Se dryRun=false: persiste contas (transação atômica - falha em uma linha não impede as outras)
   - Se dryRun=true: retorna preview sem persistir
   - Ignora linhas duplicadas (mesmo accountCode para mesma empresa/ano)
   - Retorna relatório detalhado
6. Validações por linha:
   - accountCode não vazio
   - accountName não vazio
   - accountType deve ser um dos valores válidos do enum (case insensitive)
   - openingBalance opcional, se presente deve ser número válido (formato: 1000.00 ou 1000,00)
7. Tratamento de erros:
   - Linha com campos faltando: registra erro e continua próxima linha
   - accountType inválido: registra erro e continua
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
   - Duplicatas são ignoradas com warning
   - Encoding UTF-8 é respeitado (caracteres acentuados)
   - Arquivo > 10MB retorna 413

---

## Story 3.4: Entidade ContaDaParteB e Repository

**Como** desenvolvedor,
**Eu quero** entidade ContaDaParteB relacionada a Company,
**Para que** possamos persistir Contas da Parte B do LALUR com seus códigos e descrições.

### Acceptance Criteria

1. Entidade JPA `ContaDaParteBEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (empresa dona do cadastro)
   - `@Column(nullable=false) String codigoDaContaB` (código da conta da Parte B - obrigatório)
   - `@Column(nullable=false, length=1000) String descricao` (descrição da conta - obrigatório)
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal, ex: 2024)
2. Constraint de unicidade: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "codigo_da_conta_b", "fiscal_year"}))`
   - Garante que não existam códigos duplicados para mesma empresa + código + ano
3. Interface `ContaDaParteBRepositoryPort` criada em `application/port/out/`:
   - `ContaDaParteB save(ContaDaParteB conta)`
   - `Optional<ContaDaParteB> findById(Long id)`
   - `List<ContaDaParteB> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<ContaDaParteB> findByCompanyIdAndCodigoAndFiscalYear(Long companyId, String codigo, Integer fiscalYear)`
   - `Page<ContaDaParteB> findByCompanyId(Long companyId, Pageable pageable)`
4. Interface `ContaDaParteBJpaRepository` criada estendendo `JpaRepository<ContaDaParteBEntity, Long>`:
   - `List<ContaDaParteBEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<ContaDaParteBEntity> findByCompanyIdAndCodigoDaContaBAndFiscalYear(Long companyId, String codigo, Integer fiscalYear)`
5. Classe `ContaDaParteBRepositoryAdapter` implementa `ContaDaParteBRepositoryPort`
6. Model `ContaDaParteB` (domain) criado em `domain/model/` como POJO puro
7. Mapper MapStruct `ContaDaParteBMapper` criado
8. Teste de integração valida:
   - Salvar conta da Parte B e recuperar por company + fiscalYear
   - Unique constraint funciona (duplicata lança exception)
   - Buscar conta por company + codigoDaContaB + fiscalYear
   - Soft delete funciona
   - Listagem paginada por empresa

---

## Story 3.5: CRUD de Conta da Parte B (Manual)

**Como** CONTADOR,
**Eu quero** criar, listar, visualizar, editar e inativar Contas da Parte B manualmente,
**Para que** eu possa cadastrar os códigos da Parte B do LALUR de uma empresa.

### Acceptance Criteria

1. Controller `ContaDaParteBController` criado com endpoints:
   - `POST /api/v1/conta-parte-b` - criar conta (CONTADOR com header X-Company-Id)
   - `GET /api/v1/conta-parte-b` - listar contas com paginação (CONTADOR com header)
   - `GET /api/v1/conta-parte-b/{id}` - visualizar conta (CONTADOR com header)
   - `PUT /api/v1/conta-parte-b/{id}` - editar conta (CONTADOR com header)
   - `PATCH /api/v1/conta-parte-b/{id}/status` - alternar status (ativar/inativar, CONTADOR com header)
2. DTOs criados: `CreateContaDaParteBRequest`, `UpdateContaDaParteBRequest`, `ContaDaParteBResponse`
3. `CreateContaDaParteBRequest`:
   - `codigoDaContaB` (obrigatório, String)
   - `descricao` (obrigatório, String, max 1000 chars)
   - `fiscalYear` (obrigatório, Integer)
4. `ContaDaParteBResponse`:
   - `id`, `codigoDaContaB`, `descricao`, `fiscalYear`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateContaDaParteBUseCase`: valida company via `CompanyContext`, verifica unicidade, salva
   - `ListContaDaParteBUseCase`: retorna contas da empresa no contexto
   - `GetContaDaParteBUseCase`: retorna conta por ID
   - `UpdateContaDaParteBUseCase`: permite editar (exceto codigoDaContaB e fiscalYear)
   - `ToggleContaDaParteBStatusUseCase`: alterna status entre ACTIVE e INACTIVE
6. Validações:
   - codigoDaContaB não pode ser vazio ou conter apenas espaços
   - descricao não pode ser vazia
   - fiscalYear deve ser >= 2000 e <= ano atual + 1
   - Combinação (company + codigoDaContaB + fiscalYear) deve ser única
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=codigoDaContaB,asc`
   - Filtro por ano: `?fiscalYear=2024`
   - Busca: `?search=texto` (busca em codigoDaContaB e descricao)
   - Filtro por status: `?include_inactive=true` (padrão: apenas ACTIVE)
8. DTOs adicionais: `ToggleStatusRequest`, `ToggleStatusResponse` (reutilizados dos stories anteriores)
9. Teste valida:
   - CONTADOR consegue criar conta da Parte B
   - Código duplicado para mesma empresa/ano retorna 400
   - CONTADOR sem header X-Company-Id recebe 400
   - Edição não permite mudar codigoDaContaB ou fiscalYear
   - Toggle status funciona corretamente
   - Listagem com filtros funciona

---

## Story 3.6: Entidade CodigoEnquadramentoLalur e Repository

**Como** desenvolvedor,
**Eu quero** entidade CodigoEnquadramentoLalur relacionada a Company,
**Para que** possamos persistir Códigos de Enquadramento do LALUR com seus códigos e históricos.

### Acceptance Criteria

1. Entidade JPA `CodigoEnquadramentoLalurEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (empresa dona do cadastro)
   - `@Column(nullable=false) String codigoDeEnquadramento` (código de enquadramento - obrigatório)
   - `@Column(nullable=false, length=2000) String historico` (histórico/descrição - obrigatório)
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal, ex: 2024)
2. Constraint de unicidade: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "codigo_de_enquadramento", "fiscal_year"}))`
3. Interface `CodigoEnquadramentoLalurRepositoryPort` criada em `application/port/out/`:
   - `CodigoEnquadramentoLalur save(CodigoEnquadramentoLalur codigo)`
   - `Optional<CodigoEnquadramentoLalur> findById(Long id)`
   - `List<CodigoEnquadramentoLalur> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<CodigoEnquadramentoLalur> findByCompanyIdAndCodigoAndFiscalYear(Long companyId, String codigo, Integer fiscalYear)`
   - `Page<CodigoEnquadramentoLalur> findByCompanyId(Long companyId, Pageable pageable)`
4. Interface `CodigoEnquadramentoLalurJpaRepository` criada estendendo `JpaRepository<CodigoEnquadramentoLalurEntity, Long>`:
   - `List<CodigoEnquadramentoLalurEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<CodigoEnquadramentoLalurEntity> findByCompanyIdAndCodigoDeEnquadramentoAndFiscalYear(Long companyId, String codigo, Integer fiscalYear)`
5. Classe `CodigoEnquadramentoLalurRepositoryAdapter` implementa `CodigoEnquadramentoLalurRepositoryPort`
6. Model `CodigoEnquadramentoLalur` (domain) criado em `domain/model/` como POJO puro
7. Mapper MapStruct `CodigoEnquadramentoLalurMapper` criado
8. Teste de integração valida:
   - Salvar código e recuperar por company + fiscalYear
   - Unique constraint funciona
   - Buscar código por company + codigoDeEnquadramento + fiscalYear
   - Soft delete funciona
   - Listagem paginada por empresa

---

## Story 3.7: CRUD de Código de Enquadramento LALUR (Manual)

**Como** CONTADOR,
**Eu quero** criar, listar, visualizar, editar e inativar Códigos de Enquadramento LALUR manualmente,
**Para que** eu possa cadastrar os códigos de enquadramento fiscal de uma empresa.

### Acceptance Criteria

1. Controller `CodigoEnquadramentoLalurController` criado com endpoints:
   - `POST /api/v1/codigo-enquadramento-lalur` - criar código (CONTADOR com header X-Company-Id)
   - `GET /api/v1/codigo-enquadramento-lalur` - listar códigos com paginação (CONTADOR com header)
   - `GET /api/v1/codigo-enquadramento-lalur/{id}` - visualizar código (CONTADOR com header)
   - `PUT /api/v1/codigo-enquadramento-lalur/{id}` - editar código (CONTADOR com header)
   - `PATCH /api/v1/codigo-enquadramento-lalur/{id}/status` - alternar status (ativar/inativar, CONTADOR com header)
2. DTOs criados: `CreateCodigoEnquadramentoLalurRequest`, `UpdateCodigoEnquadramentoLalurRequest`, `CodigoEnquadramentoLalurResponse`
3. `CreateCodigoEnquadramentoLalurRequest`:
   - `codigoDeEnquadramento` (obrigatório, String)
   - `historico` (obrigatório, String, max 2000 chars)
   - `fiscalYear` (obrigatório, Integer)
4. `CodigoEnquadramentoLalurResponse`:
   - `id`, `codigoDeEnquadramento`, `historico`, `fiscalYear`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateCodigoEnquadramentoLalurUseCase`: valida company via `CompanyContext`, verifica unicidade, salva
   - `ListCodigoEnquadramentoLalurUseCase`: retorna códigos da empresa no contexto
   - `GetCodigoEnquadramentoLalurUseCase`: retorna código por ID
   - `UpdateCodigoEnquadramentoLalurUseCase`: permite editar (exceto codigoDeEnquadramento e fiscalYear)
   - `ToggleCodigoEnquadramentoLalurStatusUseCase`: alterna status entre ACTIVE e INACTIVE
6. Validações:
   - codigoDeEnquadramento não pode ser vazio ou conter apenas espaços
   - historico não pode ser vazio
   - fiscalYear deve ser >= 2000 e <= ano atual + 1
   - Combinação (company + codigoDeEnquadramento + fiscalYear) deve ser única
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=codigoDeEnquadramento,asc`
   - Filtro por ano: `?fiscalYear=2024`
   - Busca: `?search=texto` (busca em codigoDeEnquadramento e historico)
   - Filtro por status: `?include_inactive=true` (padrão: apenas ACTIVE)
8. DTOs adicionais: `ToggleStatusRequest`, `ToggleStatusResponse` (reutilizados)
9. Teste valida:
   - CONTADOR consegue criar código de enquadramento
   - Código duplicado para mesma empresa/ano retorna 400
   - CONTADOR sem header X-Company-Id recebe 400
   - Edição não permite mudar codigoDeEnquadramento ou fiscalYear
   - Toggle status funciona corretamente
   - Listagem com filtros funciona

---

## Story 3.8: Entidade LinhaLucroPresumido e Repository

**Como** desenvolvedor,
**Eu quero** entidade LinhaLucroPresumido relacionada a Company e ChartOfAccount,
**Para que** possamos persistir Linhas para Cadastro de Lucro Presumido com seus códigos, descrições e contas contábeis associadas.

### Acceptance Criteria

1. Entidade JPA `LinhaLucroPresumidoEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (empresa dona do cadastro)
   - `@Column(nullable=false) Integer codigo` (código numérico da linha - obrigatório)
   - `@Column(nullable=false, length=1000) String descricao` (descrição da linha - obrigatório)
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal, ex: 2024)
2. Entidade de associação `LinhaLucroPresumidoContaEntity` criada (ManyToMany explícito):
   - `@Id @GeneratedValue(strategy = IDENTITY) Long id`
   - `@ManyToOne @JoinColumn(nullable=false) LinhaLucroPresumidoEntity linhaLucroPresumido`
   - `@ManyToOne @JoinColumn(nullable=false) ChartOfAccountEntity chartOfAccount`
   - `@Column(nullable=false) Integer orderIndex` (ordem das contas na lista - para preservar ordem de inserção)
3. Relacionamento em `LinhaLucroPresumidoEntity`:
   - `@OneToMany(mappedBy="linhaLucroPresumido", cascade=ALL, orphanRemoval=true) List<LinhaLucroPresumidoContaEntity> contas`
4. Constraint de unicidade em `LinhaLucroPresumidoEntity`: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "codigo", "fiscal_year"}))`
5. Interface `LinhaLucroPresumidoRepositoryPort` criada em `application/port/out/`:
   - `LinhaLucroPresumido save(LinhaLucroPresumido linha)`
   - `Optional<LinhaLucroPresumido> findById(Long id)`
   - `List<LinhaLucroPresumido> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<LinhaLucroPresumido> findByCompanyIdAndCodigoAndFiscalYear(Long companyId, Integer codigo, Integer fiscalYear)`
   - `Page<LinhaLucroPresumido> findByCompanyId(Long companyId, Pageable pageable)`
6. Interface `LinhaLucroPresumidoJpaRepository` criada estendendo `JpaRepository<LinhaLucroPresumidoEntity, Long>`:
   - `List<LinhaLucroPresumidoEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<LinhaLucroPresumidoEntity> findByCompanyIdAndCodigoAndFiscalYear(Long companyId, Integer codigo, Integer fiscalYear)`
7. Classe `LinhaLucroPresumidoRepositoryAdapter` implementa `LinhaLucroPresumidoRepositoryPort`
8. Model `LinhaLucroPresumido` (domain) criado em `domain/model/` como POJO puro:
   - Contém campo `List<Long> chartOfAccountIds` (IDs das contas na ordem)
9. Mapper MapStruct `LinhaLucroPresumidoMapper` criado:
   - Mapeia `List<LinhaLucroPresumidoContaEntity>` para `List<Long>` (IDs ordenados)
   - Mapeia `List<Long>` para `List<LinhaLucroPresumidoContaEntity>` (ordem preservada via orderIndex)
10. Teste de integração valida:
    - Salvar linha com 3 contas associadas
    - Recuperar linha e validar que ordem das contas é preservada
    - Unique constraint funciona
    - Atualizar linha (adicionar/remover contas) funciona
    - Soft delete funciona
    - Orphan removal funciona (remover conta da lista remove associação)

---

## Story 3.9: CRUD de Linha para Cadastro de Lucro Presumido (Manual)

**Como** CONTADOR,
**Eu quero** criar, listar, visualizar, editar e inativar Linhas para Cadastro de Lucro Presumido manualmente,
**Para que** eu possa cadastrar as linhas de apuração de lucro presumido com suas respectivas contas contábeis.

### Acceptance Criteria

1. Controller `LinhaLucroPresumidoController` criado com endpoints:
   - `POST /api/v1/linha-lucro-presumido` - criar linha (CONTADOR com header X-Company-Id)
   - `GET /api/v1/linha-lucro-presumido` - listar linhas com paginação (CONTADOR com header)
   - `GET /api/v1/linha-lucro-presumido/{id}` - visualizar linha (CONTADOR com header)
   - `PUT /api/v1/linha-lucro-presumido/{id}` - editar linha (CONTADOR com header)
   - `PATCH /api/v1/linha-lucro-presumido/{id}/status` - alternar status (ativar/inativar, CONTADOR com header)
2. DTOs criados: `CreateLinhaLucroPresumidoRequest`, `UpdateLinhaLucroPresumidoRequest`, `LinhaLucroPresumidoResponse`
3. `CreateLinhaLucroPresumidoRequest`:
   - `codigo` (obrigatório, Integer)
   - `descricao` (obrigatório, String, max 1000 chars)
   - `fiscalYear` (obrigatório, Integer)
   - `chartOfAccountIds` (obrigatório, List<Long>, não pode ser vazia)
4. `LinhaLucroPresumidoResponse`:
   - `id`, `codigo`, `descricao`, `fiscalYear`, `status`, `createdAt`, `updatedAt`
   - `contas` (lista de objetos: `{chartOfAccountId, accountCode, accountName}` - na ordem inserida)
5. Use cases implementados:
   - `CreateLinhaLucroPresumidoUseCase`:
     - Valida company via `CompanyContext`
     - Verifica unicidade (company + codigo + fiscalYear)
     - Valida que todas contas em `chartOfAccountIds` existem e pertencem à empresa + fiscalYear
     - Salva linha preservando ordem das contas via `orderIndex`
   - `ListLinhaLucroPresumidoUseCase`: retorna linhas da empresa no contexto
   - `GetLinhaLucroPresumidoUseCase`: retorna linha por ID com contas ordenadas
   - `UpdateLinhaLucroPresumidoUseCase`:
     - Permite editar descricao e chartOfAccountIds
     - Não permite editar codigo ou fiscalYear
     - Remove contas antigas e adiciona novas (orphan removal automático)
   - `ToggleLinhaLucroPresumidoStatusUseCase`: alterna status entre ACTIVE e INACTIVE
6. Validações:
   - codigo deve ser >= 1
   - descricao não pode ser vazia
   - fiscalYear deve ser >= 2000 e <= ano atual + 1
   - chartOfAccountIds não pode ser vazia (mínimo 1 conta)
   - Todas contas devem existir, pertencer à empresa, e ter fiscalYear compatível
   - Combinação (company + codigo + fiscalYear) deve ser única
7. Listagem suporta:
   - Paginação: `?page=0&size=100&sort=codigo,asc`
   - Filtro por ano: `?fiscalYear=2024`
   - Busca: `?search=texto` (busca em descricao)
   - Filtro por status: `?include_inactive=true` (padrão: apenas ACTIVE)
8. DTOs adicionais: `ToggleStatusRequest`, `ToggleStatusResponse` (reutilizados)
9. Teste valida:
   - CONTADOR consegue criar linha com lista de contas
   - Ordem das contas é preservada
   - Código duplicado para mesma empresa/ano retorna 400
   - Conta inexistente em chartOfAccountIds retorna 400
   - Conta de outra empresa retorna 400
   - Edição permite adicionar/remover contas
   - Edição não permite mudar codigo ou fiscalYear
   - Toggle status funciona corretamente
   - Listagem com filtros funciona
   - Contas órfãs são removidas ao editar linha

---

## Story 3.10: Entidade AccountingData (Dados Contábeis Genéricos) e Repository

**Como** desenvolvedor,
**Eu quero** entidade AccountingData relacionada a ChartOfAccount,
**Para que** possamos persistir lançamentos contábeis genéricos (saldos, movimentações) por conta/competência vindos de sistemas ERP.

### Acceptance Criteria

1. Entidade JPA `AccountingDataEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) ChartOfAccountEntity chartOfAccount` (conta contábil)
   - `@Column(nullable=false) LocalDate competencia` (data de competência, ex: 2024-01-31)
   - `@Column(precision=19, scale=2) BigDecimal debitAmount` (valor débito - opcional)
   - `@Column(precision=19, scale=2) BigDecimal creditAmount` (valor crédito - opcional)
   - `@Column(precision=19, scale=2) BigDecimal balance` (saldo final da conta naquela competência - calculado)
   - `@Column(length=500) String description` (descrição/histórico - opcional)
2. Interface `TemporalEntity` implementada (para Período Contábil):
   - `LocalDate getCompetencia()` retorna o campo `competencia`
3. Interface `AccountingDataRepositoryPort` criada:
   - `AccountingData save(AccountingData data)`
   - `Optional<AccountingData> findById(Long id)`
   - `List<AccountingData> findByChartOfAccountIdAndCompetencia(Long chartOfAccountId, LocalDate competencia)`
   - `List<AccountingData> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `void deleteById(Long id)`
   - `Page<AccountingData> findByChartOfAccountId(Long chartOfAccountId, Pageable pageable)`
4. Interface `AccountingDataJpaRepository` criada estendendo `JpaRepository<AccountingDataEntity, Long>`:
   - `@Query` customizadas para buscar por company (via join com ChartOfAccount)
5. Classe `AccountingDataRepositoryAdapter` implementa `AccountingDataRepositoryPort`
6. Model `AccountingData` (domain) criado como POJO puro
7. Mapper MapStruct `AccountingDataMapper` criado
8. Teste de integração valida:
   - Salvar dado contábil e recuperar por chartOfAccountId
   - Relacionamento ManyToOne com ChartOfAccount funciona
   - Campo competencia é persistido corretamente
   - Soft delete funciona
   - Query por company via join

---

## Story 3.11: Importação de Dados Contábeis Genéricos via CSV/TXT

**Como** CONTADOR,
**Eu quero** importar dados contábeis genéricos via arquivo CSV ou TXT,
**Para que** eu possa carregar saldos e movimentações de sistemas ERP externos.

### Acceptance Criteria

1. Endpoint `POST /api/v1/accounting-data/import` criado (CONTADOR com header X-Company-Id):
   - Aceita `multipart/form-data` com arquivo CSV/TXT
   - Query param obrigatório: `?fiscalYear=2024`
   - Query param opcional: `?dryRun=true`
2. DTO `ImportAccountingDataRequest`:
   - `file` (MultipartFile, obrigatório)
   - `fiscalYear` (Integer, obrigatório)
   - `dryRun` (Boolean, default false)
3. DTO `ImportAccountingDataResponse`:
   - `success`, `message`, `totalLines`, `processedLines`, `skippedLines`, `errors`, `preview`
4. Formato do arquivo CSV/TXT esperado:
   - **Separador**: `;` ou `,` (detectado automaticamente)
   - **Header obrigatório**: `accountCode;competencia;debitAmount;creditAmount;balance;description`
   - **Exemplo**:
     ```
     accountCode;competencia;debitAmount;creditAmount;balance;description
     1.1.01.001;2024-01-31;1000.00;500.00;5500.00;Lançamento Janeiro
     1.1.02.001;2024-01-31;50000.00;25000.00;125000.50;Movimentação Bancária
     ```
5. Use case `ImportAccountingDataUseCase` implementado:
   - Valida arquivo (max 50MB - maior que plano de contas pois pode ter muito mais linhas)
   - Parse linha por linha
   - Para cada linha:
     - Busca ChartOfAccount por accountCode + companyId + fiscalYear
     - Se conta não existe: registra erro e pula linha
     - Valida formato de competencia (ISO 8601: YYYY-MM-DD)
     - Valida valores numéricos (debitAmount, creditAmount, balance)
     - Cria AccountingData e persiste
   - Validação de Período Contábil:
     - Verifica se competencia >= company.periodoContabil
     - Se competencia < periodoContabil: registra erro e pula linha
   - Se dryRun=true: retorna preview sem persistir
6. Validações por linha:
   - accountCode deve existir no plano de contas da empresa/ano
   - competencia obrigatória, formato válido
   - debitAmount, creditAmount, balance opcionais, mas se presentes devem ser números válidos
   - competencia não pode ser anterior ao Período Contábil
7. Tratamento de erros:
   - Conta não encontrada: `Account code '{code}' not found for company/year`
   - Competência anterior a Período Contábil: `Competência {date} is before Período Contábil {date}`
   - Formato de data inválido: `Invalid date format: {value}`
   - Valor numérico inválido: `Invalid numeric value: {field}`
8. Response:
   - 200 OK com relatório completo
   - 400 Bad Request se arquivo vazio ou fiscalYear ausente
   - 413 Payload Too Large se arquivo > 50MB
9. Teste valida:
   - Importação bem-sucedida processa todas linhas
   - Conta inexistente registra erro e continua
   - Competência anterior a Período Contábil registra erro
   - Dry run retorna preview
   - Arquivo com 10.000 linhas é processado em < 30s

---

## Story 3.12: Exportação de Dados Contábeis Genéricos para CSV

**Como** CONTADOR,
**Eu quero** exportar dados contábeis genéricos para arquivo CSV,
**Para que** eu possa fazer backup, análises externas ou compartilhar com outros sistemas.

### Acceptance Criteria

1. Endpoint `GET /api/v1/accounting-data/export` criado (CONTADOR com header X-Company-Id):
   - Query param obrigatório: `?fiscalYear=2024`
   - Query param opcional: `?competenciaInicio=2024-01-01` e `?competenciaFim=2024-12-31`
   - Response: arquivo CSV para download (Content-Type: text/csv; charset=UTF-8)
2. Use case `ExportAccountingDataUseCase` implementado:
   - Busca todos dados contábeis da empresa no contexto + fiscalYear
   - Se competenciaInicio/Fim fornecidos: filtra por range de competência
   - Gera arquivo CSV com formato:
     ```
     accountCode;accountName;competencia;debitAmount;creditAmount;balance;description
     1.1.01.001;Caixa;2024-01-31;1000.00;500.00;5500.00;Lançamento Janeiro
     ```
   - Header Content-Disposition: `attachment; filename="accounting-data-{companyId}-{fiscalYear}.csv"`
3. Formato do arquivo gerado:
   - **Separador**: `;` (ponto e vírgula)
   - **Encoding**: UTF-8
   - **Linha 1**: Header
   - **Linhas 2+**: Dados, ordenados por accountCode ASC, competencia ASC
   - **Números**: formato `1000.00` (ponto decimal, 2 casas)
   - **Datas**: formato ISO 8601 (YYYY-MM-DD)
4. Validações:
   - fiscalYear obrigatório
   - Se competenciaInicio fornecido sem competenciaFim: erro 400
   - Se competenciaFim < competenciaInicio: erro 400
5. Response:
   - 200 OK com arquivo CSV
   - 400 Bad Request se validações falharem
   - 404 Not Found se nenhum dado encontrado
6. Teste valida:
   - Exportação gera arquivo válido com header correto
   - Arquivo pode ser reimportado sem erros (round-trip)
   - Filtro por range de competência funciona
   - Encoding UTF-8 preserva caracteres especiais
   - Arquivo com 10.000 linhas é gerado em < 10s

---

## Story 3.13: CRUD de Dados Contábeis Genéricos (Manual)

**Como** CONTADOR,
**Eu quero** criar, visualizar, editar e inativar dados contábeis genéricos manualmente,
**Para que** eu possa fazer ajustes pontuais sem precisar reimportar arquivo completo.

### Acceptance Criteria

1. Controller `AccountingDataController` criado com endpoints:
   - `POST /api/v1/accounting-data` - criar dado contábil (CONTADOR com header)
   - `GET /api/v1/accounting-data` - listar dados com paginação (CONTADOR com header)
   - `GET /api/v1/accounting-data/{id}` - visualizar dado (CONTADOR com header)
   - `PUT /api/v1/accounting-data/{id}` - editar dado (CONTADOR com header)
   - `PATCH /api/v1/accounting-data/{id}/status` - alternar status do dado (ativar/inativar, CONTADOR com header)
2. DTOs criados: `CreateAccountingDataRequest`, `UpdateAccountingDataRequest`, `AccountingDataResponse`
3. `CreateAccountingDataRequest`:
   - `chartOfAccountId` (obrigatório)
   - `competencia` (obrigatório, LocalDate)
   - `debitAmount` (opcional, BigDecimal)
   - `creditAmount` (opcional, BigDecimal)
   - `balance` (opcional, BigDecimal)
   - `description` (opcional, String max 500 chars)
4. `AccountingDataResponse`:
   - `id`, `chartOfAccountId`, `accountCode`, `accountName`, `competencia`, `debitAmount`, `creditAmount`, `balance`, `description`, `status`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateAccountingDataUseCase`: valida que chartOfAccount pertence à empresa do contexto, valida Período Contábil, salva
   - `ListAccountingDataUseCase`: retorna dados da empresa no contexto
   - `GetAccountingDataUseCase`: retorna dado por ID
   - `UpdateAccountingDataUseCase`: valida Período Contábil antes de permitir edição
   - `ToggleAccountingDataStatusUseCase`: alterna status entre ACTIVE e INACTIVE (também valida Período Contábil)
6. Validações:
   - chartOfAccountId deve existir e pertencer à empresa do contexto
   - competencia >= company.periodoContabil
   - debitAmount, creditAmount, balance >= 0 (se fornecidos)
7. Listagem suporta:
   - Paginação: `?page=0&size=100`
   - Filtro por conta: `?chartOfAccountId={id}`
   - Filtro por competência: `?competencia=2024-01-31`
   - Filtro por range: `?competenciaInicio=2024-01-01&competenciaFim=2024-12-31`
   - Ordenação: `?sort=competencia,desc`
8. Annotation `@EnforcePeriodoContabil` aplicada em UpdateAccountingDataUseCase e ToggleAccountingDataStatusUseCase
9. DTO adicional para toggle status: `ToggleStatusRequest` e `ToggleStatusResponse` (reutilizados)
10. Teste valida:
    - CONTADOR consegue criar dado contábil
    - Tentativa de criar com competência < Período Contábil retorna 400
    - Tentativa de editar dado antigo (competência < Período Contábil) retorna 400
    - Tentativa de inativar dado antigo (competência < Período Contábil) retorna 400
    - Toggle status: ACTIVE → INACTIVE funciona
    - Toggle status: INACTIVE → ACTIVE funciona
    - Listagem com filtros funciona corretamente

---

## Story 3.14: Testes de Integração End-to-End do Epic 3

**Como** desenvolvedor,
**Eu quero** testes de integração que validem fluxos completos do Epic 3,
**Para que** tenhamos confiança de que todas funcionalidades de plano de contas e dados contábeis fiscais estão integradas.

### Acceptance Criteria

1. Teste de integração: **Fluxo completo - Importação Plano de Contas**
   - CONTADOR seleciona empresa
   - Importa plano de contas via CSV (100 contas)
   - Valida que todas 100 contas foram criadas
   - Tenta reimportar mesmo arquivo → contas duplicadas são ignoradas

2. Teste de integração: **Fluxo completo - Conta da Parte B**
   - CONTADOR cria empresa
   - Cria manualmente 5 Contas da Parte B
   - Lista todas contas → deve retornar 5 registros
   - Edita descrição de 1 conta
   - Valida que edição foi persistida
   - Inativa 1 conta
   - Lista sem include_inactive → deve retornar 4 registros
   - Lista com include_inactive=true → deve retornar 5 registros

3. Teste de integração: **Fluxo completo - Código de Enquadramento LALUR**
   - CONTADOR cria empresa
   - Cria manualmente 3 Códigos de Enquadramento
   - Valida que 3 registros foram criados
   - Edita histórico de 1 código
   - Valida que edição foi persistida
   - Toggle status de 1 código para INACTIVE
   - Lista sem include_inactive → deve retornar 2 registros

4. Teste de integração: **Fluxo completo - Linha de Lucro Presumido**
   - CONTADOR cria empresa
   - Importa plano de contas (100 contas)
   - Cria manualmente 3 linhas de lucro presumido, cada uma com contas associadas
   - Valida que 3 linhas foram criadas
   - Valida que ordem das contas em cada linha está correta
   - Edita linha 1: adiciona 2 novas contas
   - Valida que linha 1 agora tem contas adicionais
   - Lista linhas → deve retornar 3 registros com contas associadas

5. Teste de integração: **Fluxo completo - Dados Contábeis Genéricos**
   - CONTADOR cria empresa com Período Contábil = 2024-01-01
   - Importa plano de contas (50 contas)
   - Importa dados contábeis genéricos (500 lançamentos, competência 2024-01 a 2024-12)
   - Valida que 500 lançamentos foram criados
   - Exporta dados contábeis → arquivo gerado deve ter 500 linhas + header
   - Round-trip: reimporta arquivo exportado → deve funcionar sem erros

6. Teste de integração: **Validação de Período Contábil (Dados Genéricos)**
   - Empresa com Período Contábil = 2024-06-01
   - Importa plano de contas
   - Importa dados contábeis com competências mistas (2024-05, 2024-06, 2024-07)
   - Validar que lançamentos de 2024-05 foram rejeitados
   - Validar que lançamentos de 2024-06 e 2024-07 foram aceitos
   - Tentar editar lançamento de 2024-06 → deve falhar (400)
   - ADMIN atualiza Período Contábil para 2024-05-01
   - Tentar editar lançamento de 2024-06 novamente → deve funcionar

7. Teste de integração: **Dry Run de Importações**
   - CONTADOR chama importação de plano de contas com dryRun=true
   - Valida que preview é retornado
   - Valida que nenhuma conta foi persistida no banco
   - Chama importação sem dryRun
   - Valida que contas foram persistidas
   - CONTADOR chama importação de dados contábeis genéricos com dryRun=true
   - Valida preview e ausência de persistência

8. Teste de integração: **Erros de Importação**
   - Importa plano de contas com accountType inválido
   - Valida que relatório de erros é retornado
   - Valida que linhas válidas foram processadas
   - Importa dados contábeis genéricos com accountCode inexistente
   - Valida que erro específico é registrado para cada linha problemática

9. Teste de integração: **Contexto de Empresa**
   - ADMIN cria empresa1 e empresa2
   - CONTADOR seleciona empresa1
   - Importa Plano de Contas e Dados Genéricos para empresa1
   - Cria Conta da Parte B, Código de Enquadramento e Linha de Lucro Presumido para empresa1
   - CONTADOR seleciona empresa2
   - Lista todos cadastros → deve retornar vazio (empresa2 não tem dados)
   - Importa e cria mesmos dados para empresa2
   - Lista cadastros → deve retornar apenas dados da empresa2

10. Teste de integração: **Validação de Relacionamento Linha-Conta**
    - CONTADOR cria empresa
    - Importa plano de contas
    - Tenta criar linha de lucro presumido com chartOfAccountId de outra empresa → deve retornar 400
    - Tenta criar linha com chartOfAccountId inexistente → deve retornar 400
    - Cria linha válida com 5 contas
    - Inativa 1 conta do plano de contas
    - Busca linha → linha ainda contém referência à conta inativa (não há cascade de inativação)

11. Todos testes usam TestContainers com PostgreSQL real
12. Todos testes limpam dados após execução

---
