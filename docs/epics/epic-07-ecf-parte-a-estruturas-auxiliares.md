# Epic 7: Preenchimento ECF Parte A & Estruturas Auxiliares

**Objetivo do Epic:**

Habilitar o preenchimento dos registros da Parte A da ECF (0000 - Abertura, J100 - Balanço Patrimonial, J150 - Demonstração do Resultado, J800 - Outras Informações) com carregamento automático de saldos dos dados contábeis importados. Este épico complementa o sistema permitindo preenchimento completo da ECF, mas não é crítico para o fluxo principal de cálculos fiscais (Epics 4 e 5). Ao final, CONTADOR poderá opcionalmente preencher a Parte A da ECF dentro do sistema caso deseje centralizar toda escrituração em um único lugar, mas o sistema funcionará completamente mesmo sem esta funcionalidade (focando apenas na Parte M - Livro ECF).

---

## Story 7.1: Entidade EcfPartARecord (Registro Genérico Parte A) e Repository

**Como** desenvolvedor,
**Eu quero** entidade genérica para armazenar registros da Parte A da ECF,
**Para que** possamos persistir diferentes tipos de registros (0000, J100, J150, J800) com flexibilidade e estrutura extensível.

### Acceptance Criteria

1. Entidade JPA `EcfPartARecordEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (empresa dona do registro)
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal, ex: 2024)
   - `@Enumerated(STRING) @Column(nullable=false) RecordType recordType` (0000, J100, J150, J800)
   - `@Column(nullable=false, columnDefinition="TEXT") String recordData` (JSON com dados do registro)
   - `@Column(nullable=false) LocalDateTime lastModifiedDate` (data da última modificação)
2. Enum `RecordType` criado:
   - R0000 (Abertura), J100 (Balanço Patrimonial), J150 (DRE), J800 (Outras Informações)
3. Constraint de unicidade: `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "fiscal_year", "record_type"}))`
   - Garante apenas um registro de cada tipo por empresa/ano
4. Interface `EcfPartARecordRepositoryPort` criada em `application/port/out/`:
   - `EcfPartARecord save(EcfPartARecord record)`
   - `Optional<EcfPartARecord> findById(Long id)`
   - `Optional<EcfPartARecord> findByCompanyIdAndFiscalYearAndRecordType(Long companyId, Integer fiscalYear, RecordType recordType)`
   - `List<EcfPartARecord> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `void deleteById(Long id)`
5. Interface `EcfPartARecordJpaRepository` criada estendendo `JpaRepository<EcfPartARecordEntity, Long>`:
   - `Optional<EcfPartARecordEntity> findByCompanyIdAndFiscalYearAndRecordType(Long companyId, Integer fiscalYear, RecordType recordType)`
   - `List<EcfPartARecordEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear)`
6. Classe `EcfPartARecordRepositoryAdapter` implementa `EcfPartARecordRepositoryPort`
7. Model `EcfPartARecord` (domain) criado em `domain/model/` como POJO puro:
   - Campo `recordData` armazenado como String JSON no banco, mas exposto como Map<String, Object> no domain
8. Mapper MapStruct `EcfPartARecordMapper` criado:
   - Converte JSON string ↔ Map<String, Object> usando Jackson
9. Teste de integração valida:
   - Salvar registro 0000 e recuperar por company + fiscalYear + recordType
   - Unique constraint funciona (duplicata lança exception)
   - JSON serialization/deserialization funciona corretamente
   - Soft delete funciona
   - Listagem de todos registros Parte A por empresa/ano

---

## Story 7.2: CRUD de Registro 0000 (Abertura)

**Como** CONTADOR,
**Eu quero** preencher o registro 0000 (Abertura da ECF),
**Para que** eu possa informar dados iniciais da escrituração conforme layout oficial da ECF.

### Acceptance Criteria

1. Controller `EcfPartAController` criado com endpoints:
   - `POST /api/v1/ecf-part-a/0000` - criar registro 0000 (CONTADOR com header X-Company-Id)
   - `GET /api/v1/ecf-part-a/0000` - visualizar registro 0000 (CONTADOR com header)
   - `PUT /api/v1/ecf-part-a/0000` - editar registro 0000 (CONTADOR com header)
   - `DELETE /api/v1/ecf-part-a/0000` - deletar registro 0000 (CONTADOR com header - soft delete)
2. DTOs criados: `CreateRecord0000Request`, `UpdateRecord0000Request`, `Record0000Response`
3. `CreateRecord0000Request` (campos conforme layout ECF):
   - `fiscalYear` (obrigatório, Integer)
   - `lecd` (String, número do recibo da ECD do período)
   - `nomeEmpresarial` (obrigatório, String)
   - `cnpj` (obrigatório, String - formatado)
   - `uf` (obrigatório, String, 2 chars)
   - `municipio` (obrigatório, String)
   - `dataInicio` (obrigatório, LocalDate - início do período)
   - `dataFim` (obrigatório, LocalDate - fim do período)
   - `retificadora` (Boolean, default false)
   - `numeroRecibo` (String, opcional - se retificadora=true)
4. `Record0000Response`:
   - `id`, `fiscalYear`, `lecd`, `nomeEmpresarial`, `cnpj`, `uf`, `municipio`, `dataInicio`, `dataFim`, `retificadora`, `numeroRecibo`, `status`, `lastModifiedDate`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateRecord0000UseCase`: valida company via `CompanyContext`, valida campos obrigatórios, converte DTO para JSON, salva
   - `GetRecord0000UseCase`: retorna registro 0000 da empresa no contexto + fiscalYear
   - `UpdateRecord0000UseCase`: permite editar todos campos (validando formato CNPJ, UF, datas)
   - `DeleteRecord0000UseCase`: soft delete (marca status como INACTIVE)
6. Validações:
   - CNPJ deve ter formato válido (14 dígitos)
   - UF deve ser sigla válida de estado brasileiro
   - dataInicio < dataFim
   - Se retificadora=true, numeroRecibo é obrigatório
   - fiscalYear deve ser >= 2000 e <= ano atual + 1
7. Contexto de empresa obrigatório:
   - CONTADOR deve enviar header `X-Company-Id`
   - Endpoints retornam apenas registros da empresa selecionada
   - ADMIN pode usar header ou passar `?companyId={id}` como query param
8. Teste valida:
   - CONTADOR consegue criar registro 0000
   - Tentativa de criar duplicata (mesma empresa/ano/tipo) retorna 400
   - CONTADOR sem header X-Company-Id recebe 400
   - Validação de CNPJ inválido retorna 400
   - Validação de dataInicio >= dataFim retorna 400
   - Edição funciona corretamente
   - Soft delete marca status como INACTIVE

---

## Story 7.3: CRUD de Registro J100 (Balanço Patrimonial) com Carga Automática

**Como** CONTADOR,
**Eu quero** preencher o registro J100 (Balanço Patrimonial) com carga automática de saldos dos dados contábeis,
**Para que** o balanço seja preenchido automaticamente a partir do plano de contas e dados contábeis importados.

### Acceptance Criteria

1. Endpoints adicionados ao `EcfPartAController`:
   - `POST /api/v1/ecf-part-a/j100` - criar registro J100 (CONTADOR com header)
   - `GET /api/v1/ecf-part-a/j100` - visualizar registro J100 (CONTADOR com header)
   - `PUT /api/v1/ecf-part-a/j100` - editar registro J100 (CONTADOR com header)
   - `POST /api/v1/ecf-part-a/j100/load-from-accounting-data` - carregar saldos automaticamente (CONTADOR com header)
   - `DELETE /api/v1/ecf-part-a/j100` - deletar registro J100 (CONTADOR com header)
2. DTOs criados: `CreateRecordJ100Request`, `UpdateRecordJ100Request`, `RecordJ100Response`, `LoadJ100FromAccountingDataRequest`
3. `CreateRecordJ100Request`:
   - `fiscalYear` (obrigatório, Integer)
   - `balanceData` (obrigatório, Map<String, BigDecimal> - código conta → saldo)
4. `LoadJ100FromAccountingDataRequest`:
   - `fiscalYear` (obrigatório, Integer)
   - `referenceDate` (obrigatório, LocalDate - data de referência para buscar saldos, ex: 2024-12-31)
5. `RecordJ100Response`:
   - `id`, `fiscalYear`, `balanceData` (Map<String, BigDecimal>), `totalAtivo`, `totalPassivo`, `totalPatrimonioLiquido`, `status`, `lastModifiedDate`, `createdAt`, `updatedAt`
6. Use cases implementados:
   - `CreateRecordJ100UseCase`: valida company, cria registro vazio ou com dados manuais
   - `GetRecordJ100UseCase`: retorna registro J100 da empresa + fiscalYear
   - `UpdateRecordJ100UseCase`: permite editar balanceData manualmente
   - `LoadJ100FromAccountingDataUseCase`:
     - Busca todas contas do plano de contas da empresa + fiscalYear
     - Para cada conta, busca saldo mais recente em AccountingData até referenceDate
     - Agrupa saldos por accountType (ATIVO, PASSIVO, PATRIMONIO_LIQUIDO)
     - Calcula totais e valida equação patrimonial: Ativo = Passivo + Patrimônio Líquido
     - Cria/atualiza registro J100 com balanceData populado
   - `DeleteRecordJ100UseCase`: soft delete
7. Validações:
   - balanceData não pode ser vazio
   - referenceDate deve estar dentro do fiscalYear
   - Equação patrimonial deve bater: Total Ativo = Total Passivo + Total Patrimônio Líquido
   - Se equação não bater: retorna warning mas permite salvar (diferença registrada em campo `patrimonialDifference`)
8. Response da carga automática inclui:
   - `success` (boolean)
   - `message` (string)
   - `recordJ100` (RecordJ100Response preenchido)
   - `patrimonialDifference` (BigDecimal - diferença se equação não bateu)
   - `warnings` (lista de warnings, ex: "Conta 1.1.01.001 sem saldo no período")
9. Teste valida:
   - CONTADOR consegue criar registro J100 manualmente
   - Carga automática preenche balanceData corretamente a partir de AccountingData
   - Validação de equação patrimonial funciona
   - Warning é retornado se equação não bater
   - Edição manual de balanceData funciona
   - Soft delete funciona

---

## Story 7.4: CRUD de Registro J150 (Demonstração do Resultado) com Carga Automática

**Como** CONTADOR,
**Eu quero** preencher o registro J150 (DRE) com carga automática de saldos dos dados contábeis,
**Para que** a DRE seja preenchida automaticamente a partir do plano de contas e dados contábeis importados.

### Acceptance Criteria

1. Endpoints adicionados ao `EcfPartAController`:
   - `POST /api/v1/ecf-part-a/j150` - criar registro J150 (CONTADOR com header)
   - `GET /api/v1/ecf-part-a/j150` - visualizar registro J150 (CONTADOR com header)
   - `PUT /api/v1/ecf-part-a/j150` - editar registro J150 (CONTADOR com header)
   - `POST /api/v1/ecf-part-a/j150/load-from-accounting-data` - carregar saldos automaticamente (CONTADOR com header)
   - `DELETE /api/v1/ecf-part-a/j150` - deletar registro J150 (CONTADOR com header)
2. DTOs criados: `CreateRecordJ150Request`, `UpdateRecordJ150Request`, `RecordJ150Response`, `LoadJ150FromAccountingDataRequest`
3. `CreateRecordJ150Request`:
   - `fiscalYear` (obrigatório, Integer)
   - `dreData` (obrigatório, Map<String, BigDecimal> - código conta → valor)
4. `LoadJ150FromAccountingDataRequest`:
   - `fiscalYear` (obrigatório, Integer)
   - `startDate` (obrigatório, LocalDate - início do período, ex: 2024-01-01)
   - `endDate` (obrigatório, LocalDate - fim do período, ex: 2024-12-31)
5. `RecordJ150Response`:
   - `id`, `fiscalYear`, `dreData` (Map<String, BigDecimal>), `totalReceitas`, `totalDespesas`, `totalCustos`, `lucroLiquido`, `status`, `lastModifiedDate`, `createdAt`, `updatedAt`
6. Use cases implementados:
   - `CreateRecordJ150UseCase`: valida company, cria registro vazio ou com dados manuais
   - `GetRecordJ150UseCase`: retorna registro J150 da empresa + fiscalYear
   - `UpdateRecordJ150UseCase`: permite editar dreData manualmente
   - `LoadJ150FromAccountingDataUseCase`:
     - Busca todas contas de resultado do plano de contas (accountType: RECEITA, DESPESA, CUSTO)
     - Para cada conta, soma movimentações (debitAmount - creditAmount) em AccountingData entre startDate e endDate
     - Agrupa por accountType (RECEITA, DESPESA, CUSTO, RESULTADO)
     - Calcula Lucro Líquido = Receitas - Despesas - Custos
     - Cria/atualiza registro J150 com dreData populado
   - `DeleteRecordJ150UseCase`: soft delete
7. Validações:
   - dreData não pode ser vazio
   - startDate < endDate
   - Período (startDate → endDate) deve estar dentro do fiscalYear
8. Response da carga automática inclui:
   - `success` (boolean)
   - `message` (string)
   - `recordJ150` (RecordJ150Response preenchido)
   - `lucroLiquido` (BigDecimal calculado)
   - `warnings` (lista de warnings, ex: "Nenhuma conta de receita encontrada")
9. Teste valida:
   - CONTADOR consegue criar registro J150 manualmente
   - Carga automática preenche dreData corretamente
   - Cálculo de Lucro Líquido funciona
   - Validação de período funciona
   - Edição manual de dreData funciona
   - Soft delete funciona

---

## Story 7.5: CRUD de Registro J800 (Outras Informações)

**Como** CONTADOR,
**Eu quero** preencher o registro J800 (Outras Informações),
**Para que** eu possa informar dados complementares da ECF conforme layout oficial.

### Acceptance Criteria

1. Endpoints adicionados ao `EcfPartAController`:
   - `POST /api/v1/ecf-part-a/j800` - criar registro J800 (CONTADOR com header)
   - `GET /api/v1/ecf-part-a/j800` - visualizar registro J800 (CONTADOR com header)
   - `PUT /api/v1/ecf-part-a/j800` - editar registro J800 (CONTADOR com header)
   - `DELETE /api/v1/ecf-part-a/j800` - deletar registro J800 (CONTADOR com header)
2. DTOs criados: `CreateRecordJ800Request`, `UpdateRecordJ800Request`, `RecordJ800Response`
3. `CreateRecordJ800Request`:
   - `fiscalYear` (obrigatório, Integer)
   - `otherInfo` (obrigatório, Map<String, String> - campo → valor livre)
   - Campos comuns (podem ser adicionados conforme necessário):
     - `numProcessoAdministrativo` (String, opcional)
     - `numProcessoJudicial` (String, opcional)
     - `observacoes` (String, opcional, max 5000 chars)
4. `RecordJ800Response`:
   - `id`, `fiscalYear`, `otherInfo` (Map<String, String>), `status`, `lastModifiedDate`, `createdAt`, `updatedAt`
5. Use cases implementados:
   - `CreateRecordJ800UseCase`: valida company, cria registro
   - `GetRecordJ800UseCase`: retorna registro J800 da empresa + fiscalYear
   - `UpdateRecordJ800UseCase`: permite editar otherInfo
   - `DeleteRecordJ800UseCase`: soft delete
6. Validações:
   - fiscalYear obrigatório
   - otherInfo pode ser vazio (registro opcional)
   - Observações não podem exceder 5000 caracteres
7. Teste valida:
   - CONTADOR consegue criar registro J800
   - Edição funciona
   - Soft delete funciona
   - otherInfo serializado/desserializado corretamente

---

## Story 7.6: Endpoint de Listagem de Registros Parte A

**Como** CONTADOR,
**Eu quero** listar todos os registros da Parte A de uma empresa,
**Para que** eu possa visualizar rapidamente quais registros já foram preenchidos e quais estão pendentes.

### Acceptance Criteria

1. Endpoint adicionado ao `EcfPartAController`:
   - `GET /api/v1/ecf-part-a` - listar registros Parte A (CONTADOR com header X-Company-Id)
   - Query params:
     - `fiscalYear` (obrigatório, Integer)
     - `include_inactive` (opcional, Boolean, default false)
2. DTO `PartARecordsSummaryResponse`:
   - `fiscalYear` (Integer)
   - `companyId` (Long)
   - `records` (lista de objetos):
     - `recordType` (String: "0000", "J100", "J150", "J800")
     - `status` (String: "ACTIVE", "INACTIVE", "NOT_CREATED")
     - `lastModifiedDate` (LocalDateTime, nullable se NOT_CREATED)
     - `completenessPercentage` (Integer - % de campos obrigatórios preenchidos)
   - `overallCompleteness` (Integer - % geral de completude da Parte A)
3. Use case `ListPartARecordsUseCase` implementado:
   - Busca todos registros Parte A da empresa + fiscalYear
   - Para cada tipo de registro (0000, J100, J150, J800):
     - Se existe: retorna status (ACTIVE/INACTIVE), lastModifiedDate, completenessPercentage
     - Se não existe: retorna status NOT_CREATED, completenessPercentage=0
   - Calcula overallCompleteness: média dos completenessPercentage dos 4 registros
4. Cálculo de `completenessPercentage`:
   - **0000:** 100% se todos campos obrigatórios preenchidos (nomeEmpresarial, cnpj, uf, municipio, dataInicio, dataFim)
   - **J100:** 100% se balanceData não vazio e equação patrimonial bate
   - **J150:** 100% se dreData não vazio
   - **J800:** 100% sempre (registro opcional)
5. Response exemplo:
   ```json
   {
     "fiscalYear": 2024,
     "companyId": 123,
     "records": [
       {
         "recordType": "0000",
         "status": "ACTIVE",
         "lastModifiedDate": "2025-10-17T10:30:00",
         "completenessPercentage": 100
       },
       {
         "recordType": "J100",
         "status": "ACTIVE",
         "lastModifiedDate": "2025-10-17T11:00:00",
         "completenessPercentage": 80
       },
       {
         "recordType": "J150",
         "status": "NOT_CREATED",
         "lastModifiedDate": null,
         "completenessPercentage": 0
       },
       {
         "recordType": "J800",
         "status": "NOT_CREATED",
         "lastModifiedDate": null,
         "completenessPercentage": 100
       }
     ],
     "overallCompleteness": 70
   }
   ```
6. Teste valida:
   - Listagem retorna todos 4 tipos de registro
   - Registros não criados aparecem com status NOT_CREATED
   - completenessPercentage calculado corretamente
   - overallCompleteness é a média dos 4 registros
   - Filtro include_inactive funciona

---

## Story 7.7: Validação de Completude da Parte A

**Como** CONTADOR,
**Eu quero** validar se a Parte A está completa antes de prosseguir para cálculos,
**Para que** o sistema me avise de pendências e evite erros na geração da ECF.

### Acceptance Criteria

1. Endpoint adicionado ao `EcfPartAController`:
   - `POST /api/v1/ecf-part-a/validate` - validar completude da Parte A (CONTADOR com header)
   - Query param obrigatório: `fiscalYear` (Integer)
2. DTO `ValidatePartAResponse`:
   - `isComplete` (Boolean - true se todos registros obrigatórios preenchidos corretamente)
   - `overallCompleteness` (Integer - % de completude)
   - `validationErrors` (lista de erros críticos que impedem prosseguir):
     - `recordType` (String: "0000", "J100", "J150", "J800")
     - `field` (String - campo com problema)
     - `message` (String - descrição do erro)
   - `validationWarnings` (lista de warnings não críticos):
     - `recordType`, `field`, `message`
3. Use case `ValidatePartACompletenessUseCase` implementado:
   - Valida registro 0000:
     - Existe? (erro se não)
     - Campos obrigatórios preenchidos? (erro se não)
     - CNPJ válido? (erro se não)
     - Datas válidas (dataInicio < dataFim)? (erro se não)
   - Valida registro J100:
     - Existe? (erro se não)
     - balanceData não vazio? (erro se não)
     - Equação patrimonial bate? (warning se não bater, mas não impede)
   - Valida registro J150:
     - Existe? (erro se não)
     - dreData não vazio? (erro se não)
   - Valida registro J800:
     - Existe? (warning se não, mas não impede - registro opcional)
   - Retorna `isComplete=true` apenas se não há erros críticos
4. Response exemplo (com erros):
   ```json
   {
     "isComplete": false,
     "overallCompleteness": 60,
     "validationErrors": [
       {
         "recordType": "0000",
         "field": "cnpj",
         "message": "CNPJ inválido: 12.345.678/0001-00"
       },
       {
         "recordType": "J150",
         "field": "dreData",
         "message": "Registro J150 não criado"
       }
     ],
     "validationWarnings": [
       {
         "recordType": "J100",
         "field": "patrimonialDifference",
         "message": "Equação patrimonial não bate: diferença de R$ 1.000,00"
       },
       {
         "recordType": "J800",
         "field": null,
         "message": "Registro J800 (Outras Informações) não criado"
       }
     ]
   }
   ```
5. Teste valida:
   - Validação retorna isComplete=true se tudo OK
   - Validação retorna isComplete=false se há erros críticos
   - Erros críticos impedem prosseguir
   - Warnings não impedem mas alertam
   - overallCompleteness calculado corretamente

---

## Story 7.8: Testes de Integração End-to-End do Epic 7

**Como** desenvolvedor,
**Eu quero** testes de integração que validem fluxos completos do Epic 7,
**Para que** tenhamos confiança de que preenchimento da Parte A e estruturas auxiliares estão funcionando corretamente.

### Acceptance Criteria

1. Teste de integração: **Fluxo completo - Preenchimento Parte A**
   - CONTADOR cria empresa
   - Importa plano de contas (100 contas)
   - Importa dados contábeis (500 lançamentos)
   - Cria registro 0000
   - Cria registro J100 com carga automática
   - Valida que J100 foi preenchido com saldos corretos
   - Cria registro J150 com carga automática
   - Valida que J150 foi preenchido com valores corretos e Lucro Líquido calculado
   - Cria registro J800 (opcional)
   - Lista registros Parte A → deve retornar 4 registros com completeness 100%
   - Valida completude → deve retornar isComplete=true

2. Teste de integração: **Carga automática J100 - Equação Patrimonial**
   - CONTADOR importa plano de contas e dados contábeis
   - Total Ativo = R$ 100.000,00
   - Total Passivo = R$ 60.000,00
   - Total Patrimônio Líquido = R$ 40.000,00
   - Cria J100 com carga automática
   - Valida que equação bate: 100.000 = 60.000 + 40.000
   - Edita manualmente balanceData de uma conta para causar desbalanceamento
   - Valida que warning é retornado com patrimonialDifference

3. Teste de integração: **Carga automática J150 - Cálculo DRE**
   - CONTADOR importa plano de contas e dados contábeis
   - Contas de RECEITA: R$ 500.000,00
   - Contas de DESPESA: R$ 200.000,00
   - Contas de CUSTO: R$ 150.000,00
   - Cria J150 com carga automática
   - Valida que Lucro Líquido = 500.000 - 200.000 - 150.000 = R$ 150.000,00

4. Teste de integração: **Validação de Completude - Cenários de Erro**
   - CONTADOR cria empresa
   - Cria registro 0000 com CNPJ inválido
   - Valida completude → deve retornar isComplete=false com erro em 0000.cnpj
   - Corrige CNPJ
   - Não cria J100
   - Valida completude → deve retornar isComplete=false com erro "J100 não criado"
   - Cria J100 e J150
   - Valida completude → deve retornar isComplete=true (J800 é opcional)

5. Teste de integração: **Listagem de Registros Parte A**
   - CONTADOR cria empresa
   - Lista registros Parte A → deve retornar 4 registros com status NOT_CREATED
   - Cria 0000 e J100
   - Lista registros Parte A → 0000 e J100 ACTIVE, J150 e J800 NOT_CREATED
   - overallCompleteness deve ser ~50%

6. Teste de integração: **Soft Delete de Registros**
   - CONTADOR cria todos registros Parte A
   - Deleta registro J100
   - Lista sem include_inactive → J100 não aparece
   - Lista com include_inactive=true → J100 aparece com status INACTIVE
   - Valida completude → deve retornar erro "J100 não ativo"

7. Teste de integração: **Edição de Registros**
   - CONTADOR cria registro 0000
   - Edita campo nomeEmpresarial
   - Valida que lastModifiedDate foi atualizado
   - Busca registro 0000 → campo editado está correto

8. Teste de integração: **Contexto de Empresa**
   - ADMIN cria empresa1 e empresa2
   - CONTADOR seleciona empresa1
   - Cria registros Parte A para empresa1
   - CONTADOR seleciona empresa2
   - Lista registros Parte A → deve retornar vazio (empresa2 não tem registros)
   - Cria registros para empresa2
   - CONTADOR seleciona empresa1 novamente
   - Lista registros → deve retornar apenas registros da empresa1

9. Todos testes usam TestContainers com PostgreSQL real
10. Todos testes limpam dados após execução

---
