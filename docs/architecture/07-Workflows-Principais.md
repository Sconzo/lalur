Workflows-Principais

### 1. Login e Seleção de Empresa

```mermaid
sequenceDiagram
    participant User as Usuário (CONTADOR)
    participant UI as Cliente (Postman/Swagger)
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant UserRepo as UserRepository
    participant JwtProvider as JwtTokenProvider

    User->>UI: Credenciais (email, senha)
    UI->>AuthController: POST /auth/login
    AuthController->>AuthService: authenticate(email, password)
    AuthService->>UserRepo: findByEmail(email)
    UserRepo-->>AuthService: User entity

    alt Senha incorreta
        AuthService->>UserRepo: incrementFailedAttempts()
        AuthService-->>AuthController: InvalidCredentialsException
        AuthController-->>UI: 401 Unauthorized
    else Conta bloqueada
        AuthService-->>AuthController: AccountLockedException
        AuthController-->>UI: 401 Locked
    else Senha correta
        AuthService->>UserRepo: resetFailedAttempts()
        AuthService->>JwtProvider: generateAccessToken(user)
        JwtProvider-->>AuthService: accessToken (15min)
        AuthService->>JwtProvider: generateRefreshToken(user)
        JwtProvider-->>AuthService: refreshToken (7 dias)
        AuthService-->>AuthController: LoginResponse
        AuthController-->>UI: 200 OK + tokens
        UI-->>User: Tokens armazenados
    end

    Note over User,UI: CONTADOR seleciona empresa
    User->>UI: Seleciona Company (ID=42)
    Note over UI: Armazena X-Company-Id: 42 em todas requests subsequentes
```

---

### 2. Importação de Balancete CSV

```mermaid
sequenceDiagram
    participant User as Usuário (CONTADOR)
    participant UI as Cliente
    participant Controller as AccountingDataController
    participant Service as AccountingDataService
    participant CsvParser as CsvParser
    participant Repo as AccountingDataRepository
    participant DB as PostgreSQL

    User->>UI: Upload balancete.csv (10k linhas)
    UI->>Controller: POST /companies/42/accounting-data/import<br/>Header: X-Company-Id: 42
    Controller->>Service: importAccountingData(companyId, file)
    Service->>CsvParser: parse(file)
    CsvParser-->>Service: List<AccountingDataRow> (10k)

    loop Batch de 1000 linhas
        Service->>Repo: saveAll(batch) com UPSERT
        Note over Repo,DB: INSERT ... ON CONFLICT (company_id, chart_of_account_id, competencia)<br/>DO UPDATE SET debito=EXCLUDED.debito, credito=...
        Repo->>DB: Batch INSERT/UPDATE
        DB-->>Repo: Success
    end

    Service-->>Controller: ImportResponse {importedCount: 10000, upsertedCount: 500}
    Controller-->>UI: 200 OK
    UI-->>User: "10.000 linhas importadas (500 atualizadas)"
```

**Performance:**
- Batch size: 1000 linhas por transaction
- UPSERT via unique constraint (idempotente)
- Target: 10k linhas em < 30s

---

### 3. Cálculo IRPJ

```mermaid
sequenceDiagram
    participant User as Usuário (CONTADOR)
    participant UI as Cliente
    participant Controller as CalculationController
    participant Service as TaxCalculationService
    participant AccRepo as AccountingDataRepository
    participant FiscalRepo as FiscalMovementRepository
    participant ParamRepo as TaxParameterRepository
    participant Calculator as IrpjCalculator (Domain)
    participant CalcRepo as CalculationResultRepository
    participant EventPub as EventPublisher

    User->>UI: Solicita cálculo IRPJ 2024
    UI->>Controller: POST /companies/42/calculations/irpj<br/>Body: {fiscalYear: 2024}
    Controller->>Service: calculateIrpj(companyId=42, fiscalYear=2024)

    Service->>AccRepo: hasDataForYear(42, 2024)
    AccRepo-->>Service: true

    Service->>FiscalRepo: findByCompanyAndYear(42, 2024)
    FiscalRepo-->>Service: List<FiscalMovement> (10 movimentos)

    Service->>AccRepo: calculateLucroLiquido(42, 2024)
    AccRepo-->>Service: BigDecimal (500.000,00)

    Service->>ParamRepo: findByCode("IRPJ_ALIQUOTA_BASE")
    ParamRepo-->>Service: 15%

    Service->>ParamRepo: findByCode("IRPJ_ALIQUOTA_ADICIONAL")
    ParamRepo-->>Service: 10%

    Service->>Calculator: calculate(lucroLiquido, movements, params)
    Note over Calculator: Domain logic pura:<br/>1. Aplicar adições LALUR<br/>2. Aplicar exclusões LALUR<br/>3. Calcular Lucro Real<br/>4. IRPJ 15% base<br/>5. IRPJ 10% adicional (> R$ 240k)<br/>6. Gerar memória de cálculo JSON
    Calculator-->>Service: TaxCalculationResult {totalTaxDue: 126.500,00}

    Service->>CalcRepo: save(result)
    CalcRepo-->>Service: Saved with ID=123

    Service->>EventPub: publish(CalculationCompletedEvent)

    Service-->>Controller: TaxCalculationResponse
    Controller-->>UI: 200 OK {totalTaxDue: 126500.00, calculationMemory: {...}}
    UI-->>User: "IRPJ 2024: R$ 126.500,00"
```

**Memória de Cálculo JSON:**
```json
{
  "calculationId": "calc-123-irpj-2024",
  "fiscalMovementIds": [101, 102, 105, 110, 115, 120, 125, 130, 135, 140],
  "steps": [
    {"step": 1, "description": "Lucro Líquido Contábil", "value": 500000.00},
    {"step": 2, "description": "Adições LALUR (10 movimentos)", "value": 50000.00},
    {"step": 3, "description": "Exclusões LALUR", "value": -20000.00},
    {"step": 4, "description": "Lucro Real (Base de Cálculo)", "value": 530000.00},
    {"step": 5, "description": "IRPJ Base 15%", "value": 79500.00},
    {"step": 6, "description": "IRPJ Adicional 10% (sobre R$ 290k)", "value": 29000.00},
    {"step": 7, "description": "IRPJ Total Devido", "value": 108500.00}
  ]
}
```

---

### 4. Geração de ECF Completo

```mermaid
sequenceDiagram
    participant User as Usuário (CONTADOR)
    participant UI as Cliente
    participant Controller as EcfController
    participant Service as EcfService
    participant EcfRepo as EcfFileRepository
    participant CalcRepo as CalculationResultRepository
    participant FileStorage as FileStorageAdapter
    participant FS as File System

    Note over User: Pré-requisitos:<br/>1. ECF importado (Parte A)<br/>2. Cálculos IRPJ/CSLL

    User->>UI: Upload ECF importado
    UI->>Controller: POST /companies/42/ecf/upload-imported
    Controller->>Service: uploadImportedEcf(file)
    Service->>FileStorage: save(file)
    FileStorage->>FS: Write file
    FS-->>FileStorage: filePath
    Service->>EcfRepo: save(EcfFile {type: IMPORTED_ECF})
    EcfRepo-->>Service: EcfFile ID=1
    Service-->>UI: 200 OK {ecfFileId: 1}

    User->>UI: Gerar Arquivo M
    UI->>Controller: POST /companies/42/ecf/generate-m-file
    Controller->>Service: generateMFile(companyId, fiscalYear)
    Service->>CalcRepo: findIrpjResult(42, 2024)
    CalcRepo-->>Service: IRPJ result
    Service->>CalcRepo: findCsllResult(42, 2024)
    CalcRepo-->>Service: CSLL result

    Note over Service: Gera registros M:<br/>|M001| Abertura<br/>|M300| LALUR Parte A<br/>|M350| LALUR Parte B<br/>|M400| LACS Parte A<br/>|M410| LACS Parte B<br/>|M990| Encerramento

    Service->>FileStorage: save(mFileContent)
    FileStorage->>FS: Write M file
    Service->>EcfRepo: save(EcfFile {type: GENERATED_M_FILE})
    EcfRepo-->>Service: EcfFile ID=2
    Service-->>UI: 200 OK {ecfFileId: 2, recordCount: 10}

    User->>UI: Gerar ECF Completo
    UI->>Controller: POST /companies/42/ecf/generate-complete
    Controller->>Service: generateCompleteEcf(companyId, fiscalYear)
    Service->>EcfRepo: findImportedEcf(42, 2024)
    EcfRepo-->>Service: EcfFile ID=1
    Service->>EcfRepo: findMFile(42, 2024)
    EcfRepo-->>Service: EcfFile ID=2

    Service->>FileStorage: read(importedEcfPath)
    FileStorage->>FS: Read file
    FS-->>FileStorage: importedContent
    Service->>FileStorage: read(mFilePath)
    FileStorage->>FS: Read file
    FS-->>FileStorage: mFileContent

    Note over Service: Merge Strategy:<br/>1. Encontrar linha |M001| no ECF importado<br/>2. Pegar Parte A (tudo antes de |M001|)<br/>3. Concatenar Parte A + Arquivo M completo

    Service->>FileStorage: save(completeEcfContent)
    FileStorage->>FS: Write complete ECF
    Service->>EcfRepo: save(EcfFile {type: COMPLETE_ECF, sourceImportedId: 1, sourceMId: 2})
    EcfRepo-->>Service: EcfFile ID=3
    Service-->>UI: 200 OK {ecfFileId: 3, recordCount: 1500}

    User->>UI: Download ECF
    UI->>Controller: GET /companies/42/ecf/3/download
    Controller->>Service: downloadEcf(3)
    Service->>EcfRepo: findById(3)
    EcfRepo-->>Service: EcfFile
    Service->>FileStorage: read(filePath)
    FileStorage->>FS: Read file
    FS-->>FileStorage: fileContent
    Service-->>Controller: File bytes
    Controller-->>UI: 200 OK (application/octet-stream)
    UI-->>User: ecf_2024_empresa42.txt baixado
```

---

### 5. Invalidação Automática de Cálculos

```mermaid
sequenceDiagram
    participant User as Usuário (CONTADOR)
    participant UI as Cliente
    participant Controller as FiscalMovementController
    participant Service as FiscalMovementService
    participant Repo as FiscalMovementRepository
    participant EventPub as EventPublisher
    participant Listener as CalculationInvalidationListener
    participant CalcRepo as CalculationResultRepository

    User->>UI: Editar movimento fiscal (ID=101)
    UI->>Controller: PUT /companies/42/fiscal-movements/101
    Controller->>Service: update(movementId, newAmount)

    Note over Service: @Transactional
    Service->>Repo: findById(101)
    Repo-->>Service: FiscalMovement {type: LALUR, year: 2024}
    Service->>Repo: update(movement)
    Repo-->>Service: Updated

    Service->>EventPub: publish(CalculationInvalidationEvent<br/>{companyId: 42, fiscalYear: 2024, type: IRPJ})

    Note over EventPub,Listener: @EventListener (síncrono, mesma transaction)
    EventPub->>Listener: onFiscalMovementChanged(event)
    Listener->>CalcRepo: markAsOutdated(companyId=42, year=2024, type=IRPJ)
    Note over CalcRepo: UPDATE tax_calculation_results<br/>SET is_outdated = true<br/>WHERE company_id = 42<br/>AND fiscal_year = 2024<br/>AND calculation_type = 'IRPJ'
    CalcRepo-->>Listener: Updated 1 row
    Listener-->>EventPub: Event handled

    Service-->>Controller: FiscalMovementResponse
    Controller-->>UI: 200 OK
    UI-->>User: "Movimento atualizado. Cálculo IRPJ 2024 precisa ser recalculado."

    Note over User: Próxima ação
    User->>UI: Consultar status de cálculos
    UI->>Controller: GET /companies/42/calculations/outdated-status
    Controller->>Service: getOutdatedCalculations(42)
    Service->>CalcRepo: findOutdated(42)
    CalcRepo-->>Service: List [{type: IRPJ, year: 2024, isOutdated: true}]
    Service-->>UI: 200 OK {outdatedCalculations: [...]}
    UI-->>User: "⚠️ Cálculo IRPJ 2024 desatualizado - recalcular"
```

---

