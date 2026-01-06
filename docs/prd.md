# Sistema de Preenchimento de Arquivos M do ECF - Product Requirements Document (PRD)

## Goals and Background Context

### Goals

**Objetivos do Projeto (Resultados Desejados):**

- Reduzir o tempo de preparação da ECF Parte B em 70% (de 8-16 horas para 2-5 horas por empresa)
- Alcançar 99,5%+ de precisão nos cálculos de IRPJ/CSLL com zero erros de validação
- Permitir que usuários contadores processem ECF completa (Parte A + Parte B) dentro do sistema sem ferramentas externas
- Suportar gestão multi-empresa com troca clara de contexto de empresa para role CONTADOR
- Garantir governança de dados através do Período Contábil (proteção temporal de períodos encerrados)
- Fornecer trilha de auditoria completa com soft delete em todas as entidades
- Entregar API backend com arquitetura hexagonal pronta para futura integração com frontend
- Alcançar primeiro deployment em produção com 10 beta users processando mínimo de 3 empresas cada
- Estabelecer fundação para expansão pós-MVP (Lucro Presumido, relatórios avançados, integrações com ERP)

### Background Context

O sistema tributário brasileiro exige que empresas no regime de Lucro Real submetam anualmente a ECF (Escrituração Contábil Fiscal), que inclui Parte A (registros contábeis gerais) e Parte B (Livro M - cálculos de IRPJ e CSLL). Atualmente, profissionais contábeis gastam 8-16 horas por empresa preenchendo manualmente a ECF usando o PVA da Receita Federal, aplicando ajustes fiscais complexos (adições, exclusões, compensações) via planilhas, e arriscando erros custosos que resultam em multas ou retrabalho. Sistemas ERP existentes focam em contabilidade geral mas carecem de motores de cálculo tributário especializados para geração da ECF Parte B.

Este PRD define um sistema de **API REST backend-only** que automatiza todo o fluxo ECF - desde gestão de plano de contas e importação de dados contábeis até configuração de parâmetros tributários, ajustes Lalur/Lacs, cálculo de IRPJ/CSLL e geração final do arquivo ECF. O sistema usa **Java 21 com Spring Boot 3.x** e **Arquitetura Hexagonal (Ports & Adapters)** para garantir manutenibilidade, testabilidade e escalabilidade futura. Diferenciais-chave incluem autenticação centralizada controlada por ADMIN, Período Contábil obrigatório para integridade de dados, soft delete universal, parâmetros tributários globais hierárquicos e triggers de cálculo sob demanda. O MVP visa escritórios contábeis gerenciando 10-50 empresas no Lucro Real, com timeline de 6-9 meses até lançamento em produção.

### Change Log

| Data | Versão | Descrição | Autor |
|------|---------|-------------|--------|
| 2025-10-17 | 1.0 | PRD inicial criado a partir do Project Brief | John (PM) |

## Requirements

### Requisitos Funcionais

**FR1:** O sistema deve permitir apenas ao usuário ADMIN criar novos usuários com roles ADMIN ou CONTADOR (sem auto-cadastro).

**FR2:** O sistema deve armazenar dados do usuário com campos obrigatórios: firstName (nome), lastName (sobrenome), email (usado para login) e role.

**FR3:** O sistema deve forçar troca de senha obrigatória no primeiro acesso e sempre que ADMIN redefinir a senha de um usuário.

**FR4:** Após login, usuário CONTADOR deve selecionar uma empresa para trabalhar e pode trocar de empresa durante a sessão sem logout.

**FR5:** Usuário ADMIN pode usar o sistema sem estar associado a uma empresa específica.

**FR6:** O sistema deve permitir cadastro de empresas com os seguintes campos obrigatórios: CNPJ, Razão Social, CNAE, Qualificação da Pessoa Jurídica, Natureza Jurídica, Período Contábil e Status.

**FR7:** O sistema deve permitir pesquisa de dados da empresa por CNPJ através de integração com site do governo para preenchimento automático de campos (Razão Social, CNAE, Qualificação PJ, Natureza Jurídica).

**FR8:** Durante criação de empresa, o sistema deve permitir seleção de parâmetros tributários aplicáveis da tabela global (tb_parametros_tributarios).

**FR9:** O sistema deve bloquear edição de dados com competência anterior ao Período Contábil da empresa (modo somente leitura).

**FR10:** O sistema deve registrar em log de auditoria qualquer alteração do Período Contábil (quem, quando, valor anterior/novo).

**FR11:** O sistema deve implementar soft delete universal - todas entidades possuem campo status (ACTIVE/INACTIVE) e nunca são deletadas fisicamente.

**FR12:** O sistema deve permitir cadastro manual de contas contábeis com estrutura plana (não hierárquica), incluindo campos: código, nome, tipo, classe contábil, nível hierárquico, natureza (devedora/credora), se afeta resultado, se é dedutível, e vínculo obrigatório com Conta Referencial RFB.

**FR13:** O sistema deve permitir importação de plano de contas via arquivos CSV/TXT com campos obrigatórios: accountCode, accountName, accountType, classe, nivel, natureza, resultado, dedutivel, contaReferencialCodigo (código RFB para lookup automático).

**FR14:** O sistema deve garantir que exista apenas uma conta contábil por empresa por ano (constraint: company_id + account_code + fiscal_year = único).

**FR15:** Apenas usuário ADMIN pode criar e editar parâmetros tributários na tabela global (tb_parametros_tributarios).

**FR16:** O sistema deve suportar hierarquia de parâmetros tributários com relacionamento pai/filho (self-referential).

**FR17:** O sistema deve permitir importação E cadastro manual de lançamentos contábeis via CRUD completo, com validação de partidas dobradas (conta débito, conta crédito, valor, data) e integração com Período Contábil.

**FR18:** O sistema deve permitir exportação de dados contábeis para arquivos CSV/TXT.

**FR19:** Uma conta contábil pode ser utilizada em N lançamentos contábeis tanto como conta débito quanto como conta crédito (relacionamento One-to-Many bidirecional).

**FR20:** O sistema deve manter tabela mestra de Contas Referenciais RFB com campos: código oficial, descrição, ano de validade (opcional), permitindo CRUD completo apenas por usuário ADMIN.

**FR21:** O sistema deve vincular obrigatoriamente cada conta do plano de contas contábil a uma Conta Referencial RFB válida para garantir compliance com estrutura oficial ECF.

**FR22:** O sistema deve permitir CRUD completo de Contas da Parte B (e-Lalur/e-Lacs) com campos: código conta, descrição, ano-base, data vigência (início/fim), tipo tributo (IRPJ/CSLL/Ambos), saldo inicial e tipo saldo (Devedor/Credor).

**FR23:** O sistema deve permitir CRUD completo de Lançamentos da Parte B com campos: mês/ano referência, tipo apuração (IRPJ/CSLL), tipo relacionamento (Conta Contábil/Conta Parte B/Ambos), código parâmetro tributário, tipo ajuste (Adição/Exclusão), descrição, valor, e vinculações opcionais a conta contábil e/ou conta Parte B conforme tipo relacionamento.

**FR24:** O sistema deve validar obrigatoriedade de FKs em Lançamentos da Parte B baseado em tipo relacionamento: se "Conta Contábil" então contaContabilId obrigatório; se "Conta Parte B" então contaParteBId obrigatório; se "Ambos" então ambos obrigatórios.

**FR25:** O sistema deve permitir preenchimento de registros da Parte A da ECF (0000, J100, J150, J800).

**FR26:** O sistema deve carregar automaticamente saldos dos lançamentos contábeis para registros J100 (Balanço) e J150 (DRE).

**FR27:** O sistema deve permitir CRUD completo de Adições, Exclusões e Compensações Lalur/Lacs com campos: tipo, valor, descrição, natureza (permanente/temporário), vinculação a conta contábil e justificativa legal.

**FR28:** O sistema deve calcular IRPJ sob demanda via trigger/botão "Calcular IRPJ" aplicando fórmula: Base = Lucro Líquido + Adições - Exclusões - Compensações; IRPJ = 15% + adicional 10% sobre base que exceder R$ 20k/mês.

**FR29:** O sistema deve calcular CSLL sob demanda via trigger/botão "Calcular CSLL" aplicando alíquota de 9% sobre a base de cálculo.

**FR30:** O sistema deve fornecer botão "Recalcular Tudo" que reprocessa todos os cálculos de IRPJ e CSLL.

**FR31:** O sistema deve exibir indicação visual quando dados foram alterados após último cálculo (cálculo desatualizado).

**FR32:** O sistema deve gerar memória de cálculo detalhada mostrando passo a passo dos cálculos de IRPJ/CSLL.

**FR33:** O sistema deve gerar a Parte M completa (arquivo M inteiro) conforme layout oficial RFB incluindo registros: M300 (Lalur Parte A), M350 (Lalur Parte B), M400 (Lacs) e demais registros M pertinentes.

**FR34:** O sistema deve validar campos obrigatórios dos registros M conforme layout oficial antes da exportação.

**FR35:** O sistema deve gerar arquivo .txt formatado da Parte M para download.

**FR36:** O sistema deve permitir upload e parsing de arquivo ECF existente (opcional).

**FR37:** O sistema deve identificar automaticamente Parte A no arquivo ECF importado.

**FR38:** O sistema deve realizar merge do arquivo ECF importado com a Parte M completa gerada no sistema.

**FR39:** O sistema deve exportar arquivo ECF completo (Parte A + Parte M completa integrada) em formato .txt pronto para transmissão SPED.

**FR40:** O sistema deve exibir dashboard com lista de empresas cadastradas e status (pendente, em andamento, concluída).

**FR41:** O sistema deve exibir indicadores de completude (% de dados preenchidos) por empresa no dashboard.

**FR42:** O sistema deve gerar alertas de prazos e pendências no dashboard.

**FR43:** O sistema deve permitir filtros em todas listagens para exibir apenas registros ativos ou incluir inativos.

### Requisitos Não-Funcionais

**NFR1:** O sistema deve ser desenvolvido como API REST backend-only em Java 21 com Spring Boot 3.x.

**NFR2:** O sistema deve seguir Arquitetura Hexagonal (Ports & Adapters) com separação clara entre domain, application e infrastructure.

**NFR3:** O sistema deve usar Maven como ferramenta de build e gerenciamento de dependências.

**NFR4:** O sistema deve usar PostgreSQL 15+ como banco de dados primário.

**NFR5:** O sistema deve garantir ACID compliance para todas transações de dados contábeis/fiscais.

**NFR6:** O sistema deve usar JWT para autenticação com access token (15min) e refresh token (7 dias).

**NFR7:** O sistema deve usar BCrypt (strength: 12) para hash de senhas.

**NFR8:** O sistema deve implementar role-based access control (RBAC) com roles ADMIN e CONTADOR.

**NFR9:** O sistema deve implementar row-level security para CONTADOR acessar apenas dados da empresa selecionada.

**NFR10:** O sistema deve usar HTTPS obrigatório em produção.

**NFR11:** O sistema deve configurar CORS para domínios específicos do frontend externo.

**NFR12:** O sistema deve implementar rate limiting de 100 req/min por IP (configurável).

**NFR13:** O sistema deve usar Spring Data JPA com JPQL/Criteria API para prevenção de SQL Injection.

**NFR14:** O sistema deve validar todos inputs com Bean Validation (@Valid) nos controllers.

**NFR15:** O sistema deve criptografar dados pessoais em repouso com AES-256.

**NFR16:** O sistema deve registrar logs de acesso e auditoria para compliance com LGPD.

**NFR17:** O sistema deve executar operações de cálculo IRPJ/CSLL em < 5 segundos para empresa típica.

**NFR18:** O sistema deve processar importação de CSV com até 10.000 linhas em < 30 segundos.

**NFR19:** O sistema deve gerar arquivo ECF completo em < 10 segundos.

**NFR20:** Endpoints de leitura devem ter tempo de resposta médio < 500ms.

**NFR21:** Endpoints de processamento pesado devem ter tempo de resposta < 3s.

**NFR22:** O sistema deve suportar 100 requisições concorrentes (meta ano 1).

**NFR23:** O sistema deve usar Docker para containerização da aplicação e banco de dados.

**NFR24:** O sistema deve usar docker-compose para orquestração local (app + PostgreSQL).

**NFR25:** O sistema deve usar Flyway ou Liquibase para versionamento de schema do banco de dados.

**NFR26:** O sistema deve implementar Spring Boot Actuator para health checks.

**NFR27:** O sistema deve usar SLF4J + Logback para logging estruturado.

**NFR28:** O sistema deve ter cobertura de testes > 80% para regras de cálculo tributário.

**NFR29:** O sistema deve usar JUnit 5 + Mockito para testes unitários.

**NFR30:** O sistema deve usar TestContainers para testes de integração com PostgreSQL real.

**NFR31:** O sistema deve documentar API REST com Swagger/OpenAPI 3.0.

**NFR32:** O sistema deve implementar backup automatizado diário do PostgreSQL.

**NFR33:** O sistema deve ter RPO (Recovery Point Objective) de 24 horas.

**NFR34:** O sistema deve ter RTO (Recovery Time Objective) de 4 horas.

**NFR35:** O sistema deve usar Lombok para redução de boilerplate code.

**NFR36:** O sistema deve usar MapStruct para mapeamento entre DTOs e Entities.

**NFR37:** O sistema deve implementar auditoria automática com @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy em todas entidades.

**NFR38:** A integração com site do governo para consulta CNPJ deve ter timeout de 10 segundos e fallback para cadastro manual em caso de falha.

## User Interface Design Goals

### Overall UX Vision

**Visão de UX (para futuro frontend):**

A interface futura deve priorizar **eficiência e clareza** para contadores processando múltiplas empresas sob pressão de prazo. O workflow deve ser **linear e guiado**, reduzindo carga cognitiva ao apresentar apenas informações relevantes ao contexto atual (empresa selecionada, período contábil ativo). Dashboards devem fornecer **visibilidade imediata de progresso** (% completude, status de cálculos, pendências) para que o contador saiba exatamente o que falta fazer. A experiência deve ser **tolerante a erros** com validações claras e mensagens de erro acionáveis, especialmente em importações de CSV e validações de ECF.

**Premissas de UX:**
- Usuários são profissionais contábeis (não público geral) - podem absorver terminologia técnica fiscal
- Uso sazonal intensivo (pico em junho-julho) exige performance e estabilidade sob carga
- Multi-empresa implica necessidade de seletor de contexto sempre visível
- Cálculos sob demanda (botões) exigem feedback visual claro (loading states, resultados, erros)

### Key Interaction Paradigms

**Paradigmas de Interação (para orientar API design):**

1. **Seleção de Contexto de Empresa (CONTADOR):**
   - API deve sempre validar empresa_id no header/token
   - Endpoints retornam apenas dados da empresa selecionada (row-level security)

2. **Workflow Sequencial com Checkpoints:**
   - API deve fornecer endpoint de "status de completude" retornando quais etapas estão completas
   - Cada etapa (plano de contas, dados contábeis, parâmetros, Parte A, movimentações, cálculos, geração) deve ter endpoint de validação

3. **Importação com Preview:**
   - Endpoints de importação CSV devem ter modo "dry-run" retornando preview sem persistir
   - Retorno deve incluir validações, warnings e contagem de registros

4. **Cálculo On-Demand com Feedback Detalhado:**
   - Endpoints de cálculo (/calculate-irpj, /calculate-csll, /recalculate-all) devem retornar:
     - Status (success/error)
     - Memória de cálculo (step-by-step breakdown)
     - Valores calculados
     - Timestamp do cálculo
   - Endpoint separado para verificar se cálculo está desatualizado

5. **Soft Delete Transparente:**
   - Todos endpoints de listagem devem aceitar query param `?include_inactive=true`
   - Endpoints de "delete" na verdade fazem PATCH atualizando status para INACTIVE

### Core Screens and Views

**Telas Conceituais (para orientar estrutura de endpoints da API):**

1. **Login Screen** → Endpoint: `POST /api/auth/login`
2. **Forced Password Change** → Endpoint: `POST /api/auth/change-password`
3. **Company Selector (CONTADOR)** → Endpoint: `GET /api/companies` + `POST /api/auth/select-company`
4. **Dashboard (visão geral empresa selecionada)** → Endpoint: `GET /api/companies/{id}/dashboard`
5. **Company Management** → Endpoints: CRUD `/api/companies`, `GET /api/companies/search-cnpj/{cnpj}`
6. **Chart of Accounts** → Endpoints: CRUD `/api/companies/{id}/chart-of-accounts`, `POST /api/companies/{id}/chart-of-accounts/import`
7. **Tax Parameters** → Endpoints: CRUD `/api/tax-parameters` (ADMIN only)
8. **Accounting Data Import/Export** → Endpoints: `POST /api/companies/{id}/accounting-data/import`, `GET /api/companies/{id}/accounting-data/export`
9. **ECF Part A Filling** → Endpoints: CRUD `/api/companies/{id}/ecf-part-a`
10. **Lalur/Lacs Adjustments** → Endpoints: CRUD `/api/companies/{id}/lalur-adjustments`
11. **Calculation Trigger** → Endpoints: `POST /api/companies/{id}/calculate-irpj`, `POST /api/companies/{id}/calculate-csll`, `POST /api/companies/{id}/recalculate-all`
12. **Calculation Results View** → Endpoint: `GET /api/companies/{id}/calculation-results`
13. **ECF Generation & Download** → Endpoint: `POST /api/companies/{id}/generate-ecf`, `GET /api/companies/{id}/ecf-file`

### Accessibility

**Acessibilidade:** Não aplicável para backend API. Futura interface web deve visar conformidade **WCAG 2.1 AA** mínima (padrão para sistemas corporativos no Brasil).

### Branding

**Branding:** Backend API não possui elementos visuais. Respostas JSON devem seguir padrão consistente (snake_case ou camelCase - **sugestão: camelCase** por ser padrão Java/Spring).

### Target Device and Platforms

**Plataformas-Alvo (para futuro frontend):**
- **Primary:** Web Responsive (desktop-first, otimizado para telas >= 1366x768)
- **Secondary:** Tablet landscape (iPad, Android tablets)
- **Out of Scope:** Mobile phones (telas pequenas inadequadas para workflow contábil complexo)

**Implicações para API:**
- Paginação obrigatória em listagens (evitar payloads grandes)
- Compressão GZIP habilitada
- Respostas otimizadas (DTOs não devem retornar dados desnecessários)

## Technical Assumptions

### Repository Structure

**Estrutura de Repositório: Monorepo**

O projeto será desenvolvido em um **único repositório** contendo:
- Backend API (Java/Spring Boot)
- Scripts de banco de dados (schema inicial via JPA/Hibernate DDL)
- Docker configuration (Dockerfile, docker-compose.yml)
- Documentação (README, API docs)

**Rationale:**
- MVP com um único serviço backend não justifica complexidade de polyrepo
- Facilita versionamento sincronizado entre código e schema
- Simplifica CI/CD (single pipeline)
- Estrutura hexagonal já fornece separação lógica de módulos

**Estrutura do Monorepo:**
```
lalur-ecf-system/
├── src/main/java/com/lalur/ecf/          # Backend hexagonal
│   ├── domain/                            # Camada de domínio
│   ├── application/                       # Ports (interfaces)
│   └── infrastructure/                    # Adapters (implementações)
├── src/main/resources/
│   └── application.yml                    # Configurações
├── src/test/                              # Testes (unit + integration)
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── docs/
│   ├── prd.md
│   ├── brief.md
│   └── api/                               # Swagger exports
├── pom.xml                                # Maven
└── README.md
```

### Service Architecture

**Arquitetura de Serviços: Monolito Modular com Arquitetura Hexagonal (Ports & Adapters)**

O sistema será desenvolvido como **um único deployable JAR** (monolito) internamente organizado com **Arquitetura Hexagonal** para garantir:
- **Testabilidade:** Domain isolado pode ser testado sem dependências externas
- **Manutenibilidade:** Separação clara de responsabilidades (domain vs. infrastructure)
- **Flexibilidade:** Fácil substituição de adapters (ex: trocar JPA por outro ORM) sem afetar domínio
- **Evolução:** Preparação para eventual migração a microserviços se necessário

**Camadas Hexagonais:**

1. **Domain (Core):**
   - Entidades de domínio (POJOs puros): `User`, `Company`, `ChartOfAccount`, `LalurAdjustment`, etc.
   - Value Objects: `CNPJ`, `Money`, `FiscalPeriod`
   - Regras de negócio: Período Contábil, cálculos IRPJ/CSLL, validações
   - **Sem dependências** de frameworks (Spring, JPA, etc.)

2. **Application (Ports - Interfaces):**
   - **Inbound Ports:** Interfaces de casos de uso (`UserUseCase`, `CompanyUseCase`, `CalculationUseCase`)
   - **Outbound Ports:** Interfaces de repositórios e serviços externos (`UserRepositoryPort`, `EcfParserPort`, `CnpjSearchPort`)
   - **DTOs:** Request/Response objects para comunicação com adapters

3. **Infrastructure (Adapters - Implementações):**
   - **Inbound Adapters:** Controllers REST (Spring Web)
   - **Outbound Adapters:**
     - Persistence (JPA repositories implementando ports)
     - File parsers (CSV, ECF)
     - External APIs (consulta CNPJ gov.br)
     - Calculators (IRPJ, CSLL)
   - **Configurações:** Spring Security, JPA, Swagger

**Bounded Contexts (módulos lógicos):**
- `user` - Gestão de usuários e autenticação
- `company` - Gestão de empresas e período contábil
- `chartofaccounts` - Plano de contas
- `taxparameter` - Parâmetros tributários
- `accountingdata` - Dados contábeis
- `lalur` - Movimentações Lalur/Lacs
- `calculation` - Motor de cálculo IRPJ/CSLL
- `ecf` - Geração e importação ECF

**Rationale:**
- Monolito reduz complexidade operacional do MVP (deploy, monitoring, debugging)
- Hexagonal permite crescimento orgânico: começar simples, evoluir se necessário
- Bounded contexts claros facilitam futura extração de microserviços (se justificável por escala)

### Testing Requirements

**Requisitos de Testes: Pirâmide Completa (Unit + Integration + API)**

**Estratégia de Testes:**

1. **Unit Tests (JUnit 5 + Mockito) - 70% coverage mínimo:**
   - **Domain puro:** Regras de negócio, cálculos, validações (sem mocks de infra)
   - **Services:** Use cases com repositories mockados
   - **Calculators:** `IrpjCalculator`, `CsllCalculator` (100% coverage - crítico fiscal)
   - **Validators:** CNPJ, partidas dobradas, validações de períodos
   - **Parsers:** CSV, ECF (casos normais + edge cases)

2. **Integration Tests (Spring Boot Test + TestContainers):**
   - **Repositories:** Queries customizadas com PostgreSQL real (TestContainers)
   - **Services completos:** Fluxos end-to-end de casos de uso
   - **Adapters:** Integração entre camadas hexagonais
   - **Schema validation:** Validar que entities JPA criam schema corretamente

3. **API Tests (MockMvc ou RestAssured):**
   - **Endpoints críticos:** Login, cálculos, geração ECF
   - **Fluxo completo:** Criar empresa → importar dados → calcular → gerar ECF
   - **Validações de segurança:** RBAC, row-level security
   - **Error handling:** Validações, erros de negócio, erros técnicos
   - Mínimo 15 cenários de API (happy path + error cases)

4. **Load Testing (Gatling ou JMeter) - Pré-Produção:**
   - Simular 100 requisições concorrentes
   - Validar performance targets (< 5s cálculos, < 30s importação)
   - Identificar bottlenecks antes do lançamento

**Ferramentas:**
- **JUnit 5:** Framework de testes
- **Mockito:** Mocks e stubs
- **TestContainers:** PostgreSQL real em containers para testes de integração
- **MockMvc:** Testes de controllers sem subir servidor
- **JaCoCo:** Code coverage reports (integrado ao Maven)

**Métrica de Qualidade:**
- **>80% line coverage** em módulos de cálculo tributário
- **>70% line coverage** geral
- **>60% branch coverage**
- **Zero bugs críticos** (que impedem fluxo principal)

**Convenções de Teste:**
- Métodos de teste seguem padrão: `should{ExpectedBehavior}_when{Condition}`
- Estrutura Given-When-Then clara
- Testes de integração separados em package `**/integration/**`
- TestContainers com PostgreSQL 15 (mesma versão de produção)

**Rationale:**
- Cálculos fiscais não podem ter erros - 80% coverage mínimo é crítico
- TestContainers garante testes próximos de produção (vs. H2 in-memory)
- Load testing pré-produção evita surpresas no lançamento (pico de uso em junho-julho)

### Additional Technical Assumptions and Requests

**Stack Técnico Confirmado (do Project Brief):**

- **Linguagem:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
  - Spring Web (REST API)
  - Spring Data JPA (ORM + repositories)
  - Spring Security (autenticação/autorização)
  - Spring Validation (validações de beans)
- **Build Tool:** Maven
- **Banco de Dados:** PostgreSQL 15+
- **Schema Management:** JPA/Hibernate DDL (spring.jpa.hibernate.ddl-auto=update em dev, validate em prod)
- **Containerização:** Docker + Docker Compose
- **CI/CD:** GitHub Actions
- **Documentação API:** Swagger/OpenAPI 3.0 (Springdoc-openapi)
- **Logging:** SLF4J + Logback
- **Bibliotecas Auxiliares:**
  - Lombok (redução de boilerplate)
  - MapStruct (mapeamento DTO ↔ Entity)
  - Apache Commons CSV ou OpenCSV (parsing CSV)
  - Jackson (JSON serialization/deserialization)

**Decisões Técnicas Adicionais:**

1. **JSON Naming Convention:** **camelCase** (padrão Spring Boot Jackson)
   - Request/Response DTOs em camelCase
   - Facilita consumo por frontend JavaScript/TypeScript futuro

2. **Company Context Handling:** **Header `X-Company-Id`**
   - CONTADOR envia header em toda requisição após seleção
   - Backend valida via `@RequestHeader` + Spring Security filter
   - ADMIN não precisa enviar (pode acessar qualquer empresa via path param)

3. **Paginação Padrão:**
   - **Listagens gerais:** 50 itens/página (ex: plano de contas, dados contábeis)
   - **Dashboard:** 20 itens/página (empresas)
   - Query params: `?page=0&size=50&sort=createdAt,desc`
   - Response inclui metadados: `totalElements`, `totalPages`, `currentPage`

4. **Error Response Format (RFC 7807 - Problem Details):**
   ```json
   {
     "type": "https://api.lalur.com/errors/validation-error",
     "title": "Validation Error",
     "status": 400,
     "detail": "CNPJ inválido: 12.345.678/0001-00",
     "instance": "/api/companies",
     "timestamp": "2025-10-17T10:30:00Z",
     "errors": [
       {
         "field": "cnpj",
         "message": "CNPJ deve ter formato válido"
       }
     ]
   }
   ```

5. **Versionamento de API:** **URL Path versioning** (`/api/v1/companies`)
   - MVP inicia com `/api/v1`
   - Permite breaking changes futuras sem quebrar clientes existentes

6. **CNPJ Validation Service:** **BrasilAPI ou ReceitaWS** (API pública gratuita)
   - Timeout 10s, retry 1x, fallback para modo manual
   - Cache de consultas bem-sucedidas (1 dia) para reduzir chamadas
   - Endpoint: `https://brasilapi.com.br/api/cnpj/v1/{cnpj}` ou similar

7. **Arquivo ECF Parser:** **Custom parser** (não biblioteca third-party)
   - Layout ECF é específico e muda anualmente
   - Parser custom dá controle total sobre validações e atualizações
   - Estrutura: `EcfParser` interface → `EcfParserImpl` adapter

8. **Soft Delete Query Strategy:** **Spring Data JPA @Where annotation**
   ```java
   @Entity
   @Where(clause = "status = 'ACTIVE'")
   public class Company extends BaseEntity {
       // ...
   }
   ```
   - Queries padrão retornam apenas ACTIVE
   - Query nativa para incluir INACTIVE quando necessário

9. **Auditoria:** **Spring Data JPA Auditing**
   - `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`
   - `AuditorAware<String>` implementado via Spring Security (email do token JWT)

10. **Background Jobs:** **Não necessário no MVP**
    - Todas operações (importação, cálculo, geração) são síncronas
    - Se futuramente necessário: Spring `@Async` ou biblioteca dedicada (Quartz, JobRunr)

11. **CI/CD Pipeline (GitHub Actions):**
    - **Trigger:** Push em `main` e `develop`, Pull Requests
    - **Jobs:**
      - Build: Maven clean package
      - Test: Maven test + JaCoCo coverage report
      - Docker: Build image e push para registry (se merge em main)
    - **Artifacts:** JAR, Docker image, coverage reports
    - **Deploy:** Manual trigger ou automático para ambiente de staging

12. **Database Schema Management:**
    - **Desenvolvimento:** `spring.jpa.hibernate.ddl-auto=update` (auto-create/update schema)
    - **Produção:** `spring.jpa.hibernate.ddl-auto=validate` (apenas valida, não modifica)
    - Schema inicial criado via JPA entities com annotations (`@Entity`, `@Table`, `@Column`)
    - Sem migrations (Flyway/Liquibase) - controle via JPA

**Premissas de Deploy:**
- **Ambiente Local:** Docker Compose (app + PostgreSQL)
- **Staging/Produção:** Deploy environment independente de cloud provider específico (pode ser AWS, GCP, Azure, Heroku, ou VPS tradicional)
- **Secrets Management:** Variáveis de ambiente (não hardcoded)

**Rationale das Decisões:**
- **Sem migrations:** Simplicidade para MVP - JPA gerencia schema automaticamente
- **GitHub Actions:** Integração nativa com GitHub, configuração simples via YAML
- **BrasilAPI/ReceitaWS:** APIs públicas gratuitas mantidas pela comunidade
- **camelCase:** Alinha com padrão Java e frontend JavaScript moderno
- **Header X-Company-Id:** Mais flexível que path param (ADMIN não precisa enviar)
- **Custom ECF parser:** Layout muda anualmente, controle total é essencial
- **Versionamento URL:** Mais explícito e fácil de testar que header versioning

## Epic List

### Lista de Épicos Proposta

**Epic 1: Fundação & Autenticação Centralizada**
Estabelecer infraestrutura base do projeto (Spring Boot, PostgreSQL, Docker), implementar autenticação JWT com roles ADMIN/CONTADOR e gestão centralizada de usuários com troca de senha obrigatória.

**Epic 2: Gestão de Empresas, Seleção de Contexto & Parâmetros Tributários**
Criar CRUD de empresas com integração CNPJ gov.br, implementar seleção de empresa para CONTADOR (header X-Company-Id), Período Contábil e gestão de parâmetros tributários hierárquicos globais (apenas ADMIN).

**Epic 3: Plano de Contas & Dados Contábeis**
Implementar cadastro e importação de plano de contas contábeis (estrutura plana vinculada a Contas Referenciais RFB), importação/exportação e CRUD completo de lançamentos contábeis via CSV e interface, validações de unicidade e partidas dobradas, gestão de tabela mestra de Contas Referenciais RFB, cadastro de Contas da Parte B (e-Lalur/e-Lacs), e CRUD completo de Lançamentos da Parte B com ajustes fiscais.

**Epic 4: Movimentações Lalur/Lacs & Motor de Cálculo**
Implementar CRUD de movimentações fiscais (adições, exclusões, compensações) para Lalur e Lacs, motor de cálculo IRPJ/CSLL sob demanda com compensações automáticas (limite 30%) e adicional de 10% no IRPJ, memória de cálculo detalhada em JSON, validação de pré-requisitos antes de calcular e invalidação automática de resultados quando dados são modificados.

**Epic 5: Geração ECF & Exportação Final**
Desenvolver gerador completo da Parte M (registros M001, M300, M350, M400, M410, M990), parser ECF customizado para importação de Parte A existente, merge automático de Parte A com Parte M gerada, validação de campos obrigatórios conforme layout SPED, e exportação de arquivo ECF final .txt pronto para transmissão à Receita Federal.

**Epic 6: Dashboard & Indicadores de Progresso**
Criar dashboard centralizado com visão geral de empresas, indicadores de completude em 8 etapas (% dados preenchidos), sistema de alertas inteligentes (críticos, warnings, info), status de cálculos tributários com detecção de desatualização, indicador de prazo de entrega ECF, resumo executivo para ADMIN, filtros avançados e exportação de relatório CSV.

**Epic 7: Preenchimento ECF Parte A & Estruturas Auxiliares**
Habilitar preenchimento opcional de registros da Parte A (0000, J100, J150, J800) com armazenamento genérico em JSON e carga automática de saldos contábeis a partir dos dados importados.

---

## Épicos Detalhados

Os épicos estão detalhados em arquivos separados na pasta `docs/epics/`:

- [Epic 1: Fundação & Autenticação Centralizada](./epics/epic-01-fundacao-autenticacao.md)
- [Epic 2: Gestão de Empresas, Seleção de Contexto & Parâmetros Tributários](./epics/epic-02-gestao-empresas-parametros.md)
- [Epic 3: Plano de Contas & Dados Contábeis](./epics/epic-03-plano-contas-dados-contabeis.md)
- [Epic 4: Movimentações Lalur/Lacs & Motor de Cálculo](./epics/epic-04-movimentacoes-lalur-lacs-motor-calculo.md)
- [Epic 5: Geração ECF & Exportação Final](./epics/epic-05-geracao-ecf-exportacao-final.md)
- [Epic 6: Dashboard & Indicadores de Progresso](./epics/epic-06-dashboard-indicadores.md)
- [Epic 7: Preenchimento ECF Parte A & Estruturas Auxiliares](./epics/epic-07-ecf-parte-a-estruturas-auxiliares.md)

---

## Checklist Results Report

### PRD Completeness Validation

Este PRD foi desenvolvido seguindo rigorosamente os critérios de qualidade estabelecidos. Abaixo está a validação ponto a ponto de cada requisito:

#### ✅ Goals and Background Context
- **Goals:** Documentados com 8 objetivos mensuráveis (redução 70% tempo, 99.5%+ precisão cálculos, multi-empresa, período contábil, auditoria, hexagonal architecture, 10 beta users, roadmap pós-MVP)
- **Background Context:** Contexto completo fornecido incluindo problema (8-16h manuais, erros custosos), solução (API REST backend-only), stack técnico (Java 21, Spring Boot 3.x, Hexagonal), diferenciais-chave (autenticação centralizada, período contábil, soft delete, parâmetros hierárquicos, cálculo sob demanda) e público-alvo (escritórios contábeis, 10-50 empresas Lucro Real, timeline 6-9 meses)

#### ✅ Requirements
- **Requisitos Funcionais:** 42 requisitos funcionais numerados (FR1-FR42) cobrindo autenticação, gestão de empresas, plano de contas, dados contábeis, parâmetros tributários, movimentações Lalur/Lacs, cálculos IRPJ/CSLL, geração ECF, dashboard e soft delete
- **Requisitos Não-Funcionais:** 38 requisitos não-funcionais numerados (NFR1-NFR38) cobrindo stack tecnológico, segurança (JWT, BCrypt, HTTPS, RBAC, row-level security), performance (< 5s cálculos, < 30s CSV import, < 500ms reads), escalabilidade (100 req concurrent), testes (>80% coverage cálculos, >70% geral), observabilidade (Actuator, logging), backup/recovery (RPO 24h, RTO 4h), compliance (LGPD, auditoria) e ferramentas (Docker, TestContainers, Swagger)
- **Priorização Clara:** Requisitos organizados logicamente por domínio funcional (autenticação → empresas → dados → cálculos → geração → dashboard). Épicos sequenciados refletindo dependências técnicas (Epic 1 fundação → Epic 2 empresas → Epic 3 dados → Epic 4 cálculos → Epic 5 geração → Epic 6 dashboard → Epic 7 opcional Parte A)

#### ✅ User Interface Design Goals
- **Overall UX Vision:** Visão clara definida para futuro frontend: eficiência e clareza para contadores sob prazo, workflow linear e guiado, visibilidade imediata de progresso (% completude, status cálculos, pendências), tolerante a erros com validações claras
- **Key Interaction Paradigms:** 5 paradigmas documentados orientando design da API: (1) Seleção de contexto de empresa via header X-Company-Id, (2) Workflow sequencial com checkpoints de validação, (3) Importação com preview/dry-run, (4) Cálculo on-demand com feedback detalhado e memória de cálculo, (5) Soft delete transparente com filtro include_inactive
- **Core Screens and Views:** 13 telas conceituais mapeadas para endpoints específicos da API (login, change password, company selector, dashboard, company management, chart of accounts, tax parameters, accounting data import/export, ECF Part A filling, Lalur/Lacs adjustments, calculation trigger, calculation results, ECF generation & download)
- **Accessibility:** Declarado não aplicável para backend API, com recomendação WCAG 2.1 AA para futuro frontend
- **Branding:** Convenção camelCase definida para JSON responses (padrão Spring Boot Jackson)
- **Target Device and Platforms:** Plataformas-alvo documentadas (primary: web desktop-first >= 1366x768, secondary: tablet landscape, out of scope: mobile phones) com implicações para API (paginação obrigatória, GZIP, DTOs otimizados)

#### ✅ Technical Assumptions
- **Repository Structure:** Estrutura de monorepo detalhada com diretórios específicos (src/main/java hexagonal, src/test, docker, docs/epics, pom.xml, README.md) e rationale (MVP não justifica polyrepo, versionamento sincronizado, CI/CD simplificado, separação lógica via hexagonal)
- **Service Architecture:** Arquitetura hexagonal (Ports & Adapters) completamente especificada com 3 camadas (Domain core sem frameworks → Application ports/DTOs → Infrastructure adapters), 8 bounded contexts (user, company, chartofaccounts, taxparameter, accountingdata, lalur, calculation, ecf) e rationale (testabilidade, manutenibilidade, flexibilidade, preparação para evolução)
- **Testing Requirements:** Estratégia de testes pirâmide completa: (1) Unit tests JUnit 5 + Mockito 70% coverage mínimo, (2) Integration tests Spring Boot Test + TestContainers com PostgreSQL real, (3) API tests MockMvc/RestAssured 15+ cenários, (4) Load testing Gatling/JMeter pré-produção 100 req concorrentes. Métricas: >80% coverage cálculos tributários, >70% geral, >60% branch coverage, zero bugs críticos. Convenções: padrão should/when, Given-When-Then, package integration/**, TestContainers PostgreSQL 15
- **Additional Technical Assumptions and Requests:** 12 decisões técnicas detalhadas incluindo JSON camelCase, company context header X-Company-Id, paginação padrão (50 itens gerais, 20 dashboard), error response RFC 7807, versionamento URL /api/v1, CNPJ validation BrasilAPI/ReceitaWS com timeout 10s e cache 1 dia, custom ECF parser, soft delete @Where annotation, auditoria Spring Data JPA, sem background jobs no MVP, CI/CD GitHub Actions (build + test + docker), schema management JPA/Hibernate DDL (update dev, validate prod sem Flyway/Liquibase), deploy Docker Compose local e cloud-agnostic staging/prod

#### ✅ Epic List
- **Lista de Épicos:** 7 épicos claramente descritos com escopo preciso:
  - **Epic 1:** Fundação & Autenticação (infraestrutura, Spring Boot, PostgreSQL, Docker, JWT, roles ADMIN/CONTADOR, gestão usuários, troca senha obrigatória) - **11 stories**
  - **Epic 2:** Gestão Empresas & Parâmetros (CRUD empresas, integração CNPJ gov.br, seleção contexto X-Company-Id, Período Contábil com auditoria, parâmetros tributários hierárquicos ADMIN-only) - **8 stories**
  - **Epic 3:** Plano Contas & Dados Contábeis (ChartOfAccount estrutura plana vinculada a ContaReferencial RFB, importação/exportação CSV plano de contas, LancamentoContabil com CRUD completo e validação partidas dobradas, importação/exportação CSV lançamentos, estruturas auxiliares fiscais: ContaReferencial RFB mestra, ContaParteB e-Lalur/e-Lacs, LancamentoParteB com ajustes fiscais) - **14 stories**
  - **Epic 4:** Movimentações Lalur/Lacs & Motor Cálculo (CRUD movimentações adições/exclusões/compensações, motor cálculo IRPJ com adicional 10% e compensações 30%, motor cálculo CSLL, memória cálculo JSON, validação pré-requisitos, invalidação automática) - **11 stories**
  - **Epic 5:** Geração ECF & Exportação (gerador Parte M completo M001/M300/M350/M400/M410/M990, parser customizado Parte A, merge Parte A + Parte M, validação campos obrigatórios SPED, exportação .txt, histórico arquivos, finalização) - **14 stories**
  - **Epic 6:** Dashboard & Indicadores (dashboard empresas, completude 8 etapas, alertas inteligentes críticos/warnings/info, status cálculos com detecção desatualização, indicador prazo entrega ECF, resumo executivo ADMIN, filtros avançados, exportação CSV, audit log) - **11 stories**
  - **Epic 7:** Preenchimento Parte A Opcional (registros 0000/J100/J150/J800, armazenamento JSON genérico, carga automática saldos contábeis) - **8 stories**
- **Total:** 77 stories distribuídas em 7 épicos
- **Sequenciamento:** Épicos ordenados respeitando dependências técnicas (fundação → dados → cálculos → geração → visualização → opcional)

#### ✅ Épicos Detalhados
- **Arquivos Separados:** 7 arquivos markdown criados na pasta docs/epics/ (epic-01 a epic-07)
- **Estrutura por Epic:** Cada épico contém:
  - Objetivo claro do épico (o que será entregue e por que é importante)
  - Stories detalhadas (As a... I want... So that...)
  - Acceptance Criteria completos para cada story (média de 10-15 critérios por story)
  - Resumo final do épico listando entregas
  - Dependências de épicos anteriores
  - Próximos passos ou observações relevantes
- **Qualidade dos Acceptance Criteria:** Critérios específicos, testáveis e mensuráveis incluindo:
  - Estruturas de entidades JPA com anotações (@Column nullable, unique, precision/scale)
  - Enums com valores definidos
  - Domain models (POJOs)
  - Repository ports com métodos específicos
  - DTOs de request/response com campos detalhados
  - Endpoints REST com métodos HTTP, paths e query params
  - Use cases e services
  - Códigos HTTP de response (200 OK, 400 Bad Request, 403 Forbidden, 404 Not Found)
  - Validações de negócio (CONTADOR row-level security, período contábil bloqueio, soft delete)
  - Testes de integração com cenários específicos (happy path + error cases)
  - Cobertura de testes esperada (>= 70-80% por épico)
- **Coesão:** Todas stories dentro de cada épico são coesas e contribuem para o objetivo declarado do épico

#### ✅ Coerência Geral
- **Alinhamento Requirements ↔ Epics:** Todos 42 requisitos funcionais e 38 não-funcionais mapeiam para stories específicas nos épicos. Exemplos:
  - FR1 (ADMIN cria usuários) → Epic 1 Story 1.8 CRUD Usuários
  - FR6 (campos obrigatórios empresas) → Epic 2 Story 2.1 Entidade Company
  - FR12-FR13 (plano contas manual + CSV) → Epic 3 Stories 3.1-3.3
  - FR27-FR28 (cálculo IRPJ/CSLL) → Epic 4 Stories 4.6-4.7
  - FR32-FR38 (geração ECF Parte M + merge Parte A) → Epic 5 Stories 5.2-5.9
  - FR39-FR41 (dashboard + indicadores + alertas) → Epic 6 Stories 6.3-6.4
  - NFR2 (arquitetura hexagonal) → Implementado em todos épicos via estrutura domain/application/infrastructure
  - NFR6-NFR8 (JWT + BCrypt + RBAC) → Epic 1 Stories 1.5-1.6
  - NFR28-NFR30 (testes >80% + JUnit 5 + TestContainers) → Stories finais de cada épico (1.11, 2.8, 3.14, 4.11, 5.14, 6.11, 7.8)
- **Consistência Técnica:** Stack tecnológico (Java 21, Spring Boot 3.x, PostgreSQL 15+, Docker, Hexagonal Architecture) aplicado consistentemente em todos épicos. Padrões arquiteturais (soft delete, auditoria @CreatedBy/@UpdatedBy, X-Company-Id header, toggle status endpoints, paginação, validation) seguidos uniformemente.
- **Sem Contradições:** Nenhuma contradição identificada entre requisitos, decisões técnicas e implementação nos épicos. Exemplo: FR11 declara soft delete universal → todos épicos implementam status ACTIVE/INACTIVE, nenhuma entity usa delete físico.

#### ✅ Clareza e Usabilidade
- **Linguagem Clara:** Documento escrito em português brasileiro técnico apropriado para equipe de desenvolvimento (termos fiscais brasileiros: LALUR, LACS, IRPJ, CSLL, ECF, SPED, Lucro Real explicados em contexto)
- **Organização Lógica:** Estrutura hierárquica clara (PRD overview → Requirements → UX Goals → Technical Assumptions → Epic List → Epic Details) com navegação fácil via índice de links para arquivos de épicos
- **Exemplos Concretos:** Múltiplos exemplos fornecidos:
  - JSON error response format (RFC 7807)
  - Estrutura de diretórios do monorepo
  - Annotations JPA (@Entity, @Column, @Where)
  - Endpoints REST (/api/v1/companies, /api/v1/lalur-movements)
  - Memória de cálculo JSON (Epic 4 Story 4.6)
  - Layout registros SPED (Epic 5 M300, M350, M400)
- **Actionable:** Cada story contém acceptance criteria suficientemente detalhados para desenvolvedores iniciarem implementação sem ambiguidade (estruturas de dados, regras de validação, códigos HTTP, cenários de teste especificados)

### Checklist Final

| Critério | Status | Evidência |
|----------|--------|-----------|
| **Goals documentados** | ✅ Completo | 8 objetivos mensuráveis + contexto detalhado do problema e solução |
| **Background context detalhado** | ✅ Completo | Problema atual (8-16h manuais), solução proposta (API REST Java 21), stack técnico, diferenciais, público-alvo, timeline |
| **Requirements completos e priorizados** | ✅ Completo | 42 FR + 38 NFR numerados, organizados por domínio, épicos sequenciados por dependência |
| **UX goals definidos** | ✅ Completo | Visão UX clara, 5 paradigmas de interação, 13 telas mapeadas para endpoints, accessibility/branding/platforms especificados |
| **Technical assumptions documentados** | ✅ Completo | Monorepo estruturado, hexagonal architecture 3 camadas + 8 bounded contexts, testing pirâmide completa, 12 decisões técnicas detalhadas |
| **Epic list com escopo claro** | ✅ Completo | 7 épicos com descrições precisas, 77 stories totais, sequenciamento respeitando dependências |
| **Épicos detalhados em arquivos separados** | ✅ Completo | 7 arquivos markdown (epic-01 a epic-07) na pasta docs/epics/ |
| **Stories com acceptance criteria testáveis** | ✅ Completo | Média 10-15 AC por story, incluindo entidades JPA, DTOs, endpoints, validações, testes |
| **Coerência entre requirements e epics** | ✅ Completo | Todos 42 FR + 38 NFR mapeados para stories, sem contradições identificadas |
| **Clareza e usabilidade** | ✅ Completo | Português técnico apropriado, estrutura hierárquica navegável, exemplos concretos, actionable |

**Status Geral:** ✅ **PRD COMPLETO E VALIDADO**

---

## Next Steps

### Immediate Next Steps (Post-PRD Approval)

1. **Aprovação de Stakeholders (Semana 1)**
   - Revisão do PRD completo com Product Owner, Tech Lead e Arquiteto de Software
   - Validação de requisitos funcionais com contador especialista (SME - Subject Matter Expert em ECF)
   - Aprovação formal do escopo MVP (Epic 1-6 obrigatórios, Epic 7 opcional pós-launch)
   - Sign-off de timeline 6-9 meses até produção

2. **Setup de Projeto (Semana 1-2)**
   - Criar repositório Git (GitHub/GitLab) com estrutura de monorepo definida
   - Configurar projeto Maven com Spring Boot 3.x, Java 21 e dependências iniciais (Spring Web, Spring Data JPA, Spring Security, PostgreSQL driver, Lombok, MapStruct, Springdoc OpenAPI)
   - Configurar Docker + docker-compose.yml (app service + PostgreSQL 15 service)
   - Configurar CI/CD pipeline inicial no GitHub Actions (build + test jobs)
   - Configurar ferramenta de gestão de projeto (Jira, Linear, GitHub Projects) com backlog de 77 stories dos 7 épicos
   - Definir Definition of Done (DoD) para stories: código revisado, testes passando (>70% coverage), documentação Swagger atualizada, merge em branch develop

3. **Epic 1: Fundação & Autenticação (Semana 2-4, ~3 semanas)**
   - **Objetivo:** Entregar infraestrutura base operacional + autenticação JWT funcional
   - **Stories prioritárias:** 1.1 (setup projeto), 1.2 (Docker), 1.3 (JPA auditoria), 1.4 (User entity), 1.5 (JWT), 1.6 (login), 1.7 (troca senha), 1.8 (CRUD usuários ADMIN)
   - **Entrega:** Sistema autenticado rodando em Docker, endpoint /actuator/health UP, Swagger UI acessível, testes passando
   - **Milestone:** M1 - Foundation Ready

4. **Epic 2: Gestão Empresas & Parâmetros (Semana 5-7, ~3 semanas)**
   - **Objetivo:** CRUD empresas funcional + seleção de contexto X-Company-Id + Período Contábil
   - **Stories prioritárias:** 2.1 (Company entity), 2.2 (CRUD empresas), 2.3 (CNPJ integration), 2.4 (Período Contábil), 2.5 (CompanyParameter entity), 2.6 (CRUD parâmetros ADMIN)
   - **Entrega:** CONTADOR pode criar empresa, selecionar contexto via header, ADMIN gerencia parâmetros tributários
   - **Milestone:** M2 - Company Management Ready

5. **Epic 3: Plano Contas & Dados Contábeis (Semana 8-11, ~4 semanas)**
   - **Objetivo:** Importação CSV de plano de contas e lançamentos contábeis funcionando com CRUD completo
   - **Stories prioritárias:** 3.1-3.3 (ChartOfAccount + CSV import vinculado a ContaReferencial), 3.4-3.5 (ContaReferencial RFB mestra), 3.6-3.7 (ContaParteB e-Lalur/e-Lacs), 3.8-3.9 (LancamentoParteB ajustes fiscais), 3.10-3.13 (LancamentoContabil + CSV import/export + CRUD manual)
   - **Entrega:** CONTADOR importa CSV de plano de contas vinculado a contas referenciais RFB, importa/exporta lançamentos contábeis com validação de partidas dobradas, cadastra contas da Parte B e lançamentos da Parte B com ajustes fiscais (adições/exclusões)
   - **Milestone:** M3 - Data Foundation Ready

6. **Epic 4: Movimentações Lalur/Lacs & Motor Cálculo (Semana 12-16, ~5 semanas) - CRÍTICO**
   - **Objetivo:** Motor de cálculo IRPJ/CSLL funcionando com memória de cálculo detalhada
   - **Stories prioritárias:** 4.1-4.2 (Lalur CRUD), 4.3-4.4 (Lacs CRUD), 4.5 (TaxCalculationResult entity), 4.6 (motor IRPJ), 4.7 (motor CSLL), 4.9 (validação pré-requisitos), 4.10 (invalidação automática)
   - **Entrega:** CONTADOR cadastra movimentações, dispara cálculo IRPJ/CSLL, recebe memória de cálculo JSON, sistema invalida cálculos automaticamente ao modificar dados
   - **Teste crítico:** Validar cálculo IRPJ com adicional 10%, compensações 30%, memória de cálculo precisa (objetivo: 99.5%+ precisão)
   - **Milestone:** M4 - Calculation Engine Ready (MARCO CRÍTICO DO MVP)

7. **Epic 5: Geração ECF & Exportação (Semana 17-21, ~5 semanas)**
   - **Objetivo:** Geração arquivo ECF completo validado pronto para transmissão SPED
   - **Stories prioritárias:** 5.1 (EcfFile entity), 5.2-5.6 (gerador Parte M M001/M300/M350/M400/M410/M990), 5.7-5.9 (parser Parte A + merge), 5.10 (validação campos obrigatórios), 5.11 (download)
   - **Entrega:** CONTADOR gera Parte M, importa Parte A existente, faz merge, valida, baixa arquivo .txt ECF completo
   - **Teste crítico:** Validar arquivo ECF gerado passa no validador oficial PVA da Receita Federal (zero erros de layout)
   - **Milestone:** M5 - ECF Generation Ready (MARCO DE FUNCIONALIDADE COMPLETA)

8. **Epic 6: Dashboard & Indicadores (Semana 22-24, ~3 semanas)**
   - **Objetivo:** Dashboard com visibilidade de progresso para múltiplas empresas
   - **Stories prioritárias:** 6.1-6.3 (completude + dashboard lista empresas), 6.4 (alertas), 6.5 (status cálculos), 6.8 (filtros + busca)
   - **Entrega:** CONTADOR visualiza lista de empresas com % completude, alertas críticos/warnings, status de cálculos desatualizados, filtra por nome/CNPJ/status
   - **Milestone:** M6 - Dashboard Ready

9. **Testes de Integração e QA (Semana 25-26, ~2 semanas)**
   - Executar testes E2E completos em todos épicos (Stories X.11 de cada épico)
   - Validar cobertura de testes >= 80% em cálculos tributários, >= 70% geral
   - Executar load testing com 100 requisições concorrentes, validar performance (cálculo <5s, importação <30s, reads <500ms)
   - Testar workflow completo end-to-end: ADMIN cria CONTADOR → CONTADOR cria empresa → importa plano de contas → importa dados contábeis → cadastra movimentações → calcula IRPJ/CSLL → gera ECF → valida → baixa arquivo
   - Bug fixing e refinamentos

10. **Beta Launch Preparation (Semana 27-28, ~2 semanas)**
    - Deploy em ambiente de staging (cloud ou VPS)
    - Configurar backup automatizado PostgreSQL (daily)
    - Configurar monitoring e logging (Spring Boot Actuator + logs estruturados)
    - Criar documentação de API completa no Swagger UI
    - Criar guia de onboarding para beta users (como criar empresas, importar dados, calcular, gerar ECF)
    - Recrutar 10 beta users (contadores parceiros) processando mínimo 3 empresas cada
    - Executar treinamento com beta users

11. **Beta Launch e Iteração (Semana 29-36, ~2 meses)**
    - Lançar para 10 beta users
    - Coletar feedback contínuo via canal dedicado (Slack, WhatsApp, Google Forms)
    - Monitorar métricas: tempo médio de geração ECF por empresa, taxa de erro de validação ECF, taxa de adoção de features (dashboard, alertas, cálculos)
    - Bug fixing prioritário (SLA: bugs críticos <24h, bugs não-críticos <1 semana)
    - Iterar baseado em feedback: ajustes de UX (mensagens de erro, validações), otimizações de performance se necessário
    - Validar objetivo: redução 70% tempo de preparação ECF (de 8-16h para 2-5h) com beta users reais

12. **Production Launch (Semana 37+)**
    - Correções finais baseadas em feedback beta
    - Deploy em produção
    - Anúncio público e marketing para escritórios contábeis
    - Suporte contínuo e manutenção

### Post-MVP Roadmap (Semanas 37+, Futuro)

**Epic 7: Preenchimento Parte A Opcional**
- Implementar após production launch se houver demanda
- Não bloqueia fluxo principal (Parte A pode ser importada de sistemas externos)

**Expansões Futuras Identificadas:**

1. **Regime Lucro Presumido (Q1-Q2 ano seguinte)**
   - Novo épico para suportar empresas no regime de Lucro Presumido
   - Cálculos simplificados (base presumida sem Lalur/Lacs)
   - Amplia público-alvo significativamente

2. **Relatórios Avançados e Analytics (Q2 ano seguinte)**
   - Relatórios gerenciais: comparação IRPJ/CSLL entre períodos
   - Gráficos de evolução de bases tributáveis
   - Exportação de relatórios PDF

3. **Integrações com ERPs (Q3 ano seguinte)**
   - Integração com ERPs populares (Totvs, SAP, Conta Azul) via API para importação automática de dados contábeis
   - Reduz ainda mais tempo de preparação ECF (elimina importação CSV manual)

4. **Assinatura Digital e Transmissão Direta SPED (Q4 ano seguinte)**
   - Integração com certificados digitais A1/A3
   - Transmissão direta ao SPED via WebService Receita Federal
   - Elimina necessidade de PVA completamente

5. **Frontend Web Completo (Ano 2)**
   - Desenvolvimento de interface web React/Vue.js consumindo API REST
   - Implementar todas telas conceituais definidas no PRD (login, dashboard, CRUD empresas, importações, cálculos, geração ECF)
   - Mobile responsivo (opcional)

### Success Criteria (Revisitado)

Ao final do MVP (Semana 36), considerar lançamento bem-sucedido se:

✅ 10 beta users ativos processando mínimo 3 empresas cada (30 empresas totais)
✅ Redução comprovada de 70% no tempo de preparação ECF (de 8-16h para 2-5h) medida com beta users
✅ Taxa de precisão de cálculos IRPJ/CSLL >= 99.5% (zero erros de validação SPED em arquivos gerados)
✅ Arquivos ECF gerados validam com sucesso no PVA da Receita Federal (zero erros de layout)
✅ Cobertura de testes >= 80% em módulos de cálculo, >= 70% geral
✅ Performance atende targets: cálculos <5s, importação CSV <30s, reads <500ms
✅ Uptime >= 99% em ambiente de produção durante período beta
✅ Net Promoter Score (NPS) >= 50 com beta users

**Se critérios atingidos:** Expandir para production launch público, investir em expansões pós-MVP (Lucro Presumido, integrações ERP, frontend web)

**Se critérios não atingidos:** Iterar com foco nos gaps identificados antes de production launch

---

**Status Final do PRD:** ✅ **COMPLETO E PRONTO PARA APROVAÇÃO DE STAKEHOLDERS**
