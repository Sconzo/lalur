# Epic 4: Movimentações Lalur/Lacs & Motor de Cálculo

**Objetivo do Epic:**

Implementar o núcleo central do sistema: a gestão de movimentações fiscais (adições, exclusões, compensações) no Lalur e Lacs, e o motor de cálculo do IRPJ e CSLL. Este épico entrega a capacidade de registrar ajustes fiscais ao lucro contábil, executar cálculos tributários sob demanda (apenas quando usuário solicitar explicitamente) com memória de cálculo detalhada, aplicar prejuízos fiscais e bases negativas de CSLL, e validar pré-requisitos antes de calcular. Ao final deste épico, o sistema será capaz de calcular impostos (IRPJ e CSLL) de forma precisa e auditável, respeitando o regime tributário e alíquotas configurados na empresa, armazenando todo histórico de cálculo para consulta e auditoria, e indicando visualmente quando dados foram modificados após o último cálculo (sem invalidar automaticamente).

---

## Story 4.1: Entidade FiscalMovement (Unificada Lalur/Lacs)

Como desenvolvedor,
Eu quero uma entidade unificada FiscalMovement para registrar movimentações fiscais de Lalur e Lacs,
Para que possamos armazenar todos ajustes fiscais em uma única tabela com campo diferenciador de livro.

**Acceptance Criteria:**

1. Entidade JPA `FiscalMovementEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company`
   - `@Column(nullable=false) Integer fiscalYear`
   - `@Enumerated(STRING) @Column(nullable=false) MovementBook movementBook` (LALUR, LACS)
   - `@Enumerated(STRING) @Column(nullable=false) MovementType movementType` (ADDITION, EXCLUSION, COMPENSATION)
   - `@ManyToOne @JoinColumn(nullable=true) CodigoEnquadramentoLalurEntity codigoEnquadramento` (referência à tabela auxiliar)
   - `@Column(nullable=false, length=500) String description`
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal amount` (sempre positivo)
   - `@Column(length=2000) String notes` (opcional)
   - `@Column(nullable=false) LocalDateTime recordDate` (data do registro, default now)
2. Enum `MovementBook` criado: LALUR, LACS
3. Enum `MovementType` criado: ADDITION, EXCLUSION, COMPENSATION
4. Constraint check: `amount > 0` (sempre positivo, tipo determina se adiciona ou subtrai)
5. Índice composto criado: `company_id + fiscal_year + movement_book` para otimizar queries
6. Domain model `FiscalMovement` criado como POJO
7. `FiscalMovementRepositoryPort` (application/port/out) criado:
   - `FiscalMovement save(FiscalMovement movement)`
   - `Optional<FiscalMovement> findById(Long id)`
   - `List<FiscalMovement> findByCompanyAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `List<FiscalMovement> findByCompanyAndFiscalYearAndBook(Long companyId, Integer fiscalYear, MovementBook book)`
   - `List<FiscalMovement> findByCompanyAndFiscalYearAndBookAndType(Long companyId, Integer fiscalYear, MovementBook book, MovementType type)`
   - `void deleteById(Long id)` (soft delete via status)
8. Adapter `FiscalMovementRepositoryAdapter` implementa port usando JPA + MapStruct
9. Teste de integração valida salvamento e recuperação por empresa, ano fiscal e livro
10. Teste valida que movimentos de empresas diferentes são isolados corretamente
11. Teste valida que movimentos LALUR e LACS são armazenados na mesma tabela mas filtrados corretamente
12. Teste valida constraint `amount > 0` (tentativa com valor negativo falha)

---

## Story 4.2: CRUD de Movimentações Fiscais

Como CONTADOR,
Eu quero criar, listar, visualizar, editar e inativar movimentações fiscais (Lalur e Lacs),
Para que eu possa registrar ajustes fiscais (adições, exclusões, compensações) em um único endpoint unificado.

**Acceptance Criteria:**

1. Controller `FiscalMovementController` criado com endpoints:
   - `POST /api/v1/fiscal-movements` (autenticado, requer X-Company-Id)
   - `GET /api/v1/fiscal-movements` (autenticado, requer X-Company-Id, suporta filtros)
   - `GET /api/v1/fiscal-movements/{id}` (autenticado, requer X-Company-Id)
   - `PUT /api/v1/fiscal-movements/{id}` (autenticado, requer X-Company-Id)
   - `PATCH /api/v1/fiscal-movements/{id}/status` (autenticado, requer X-Company-Id)
2. DTOs criados:
   - `CreateFiscalMovementRequest`: `fiscalYear` (obrigatório), `movementBook` (obrigatório: LALUR ou LACS), `movementType` (obrigatório: ADDITION, EXCLUSION, COMPENSATION), `codigoEnquadramentoId` (opcional), `description` (obrigatório, max 500), `amount` (obrigatório, > 0), `notes` (opcional, max 2000)
   - `UpdateFiscalMovementRequest`: mesmos campos de Create
   - `FiscalMovementResponse`: `id`, `companyId`, `fiscalYear`, `movementBook`, `movementType`, `codigoEnquadramento` (objeto com id, codigo, historico), `description`, `amount`, `notes`, `recordDate`, `status`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
   - `ToggleStatusRequest`: `status` (ACTIVE/INACTIVE)
   - `ToggleStatusResponse`: `success`, `message`, `newStatus`
3. Use cases implementados: `CreateFiscalMovementUseCase`, `ListFiscalMovementsUseCase`, `GetFiscalMovementUseCase`, `UpdateFiscalMovementUseCase`, `ToggleFiscalMovementStatusUseCase`
4. Endpoint de listagem suporta filtros via query params:
   - `?fiscalYear=2024` (obrigatório)
   - `?movementBook=LALUR` (opcional, filtra por livro: LALUR ou LACS)
   - `?movementType=ADDITION` (opcional, filtra por tipo)
   - `?include_inactive=true` (opcional, default false)
5. Listagem retorna apenas movimentos ACTIVE por padrão, ordenados por `recordDate DESC`
6. Validação: CONTADOR só pode acessar movimentos da empresa no header X-Company-Id
7. Validação: não permite criar/editar movimento se período contábil (fiscal year) estiver fechado (integração com CompanyParameter.fechamentoContabil)
8. Response 403 Forbidden se tentar acessar movimento de outra empresa
9. Response 400 Bad Request se tentar criar movimento com período fechado
10. Response 404 Not Found se movimento não existe ou pertence a outra empresa
11. Teste valida criação de movimento LALUR com todos campos obrigatórios
12. Teste valida criação de movimento LACS com todos campos obrigatórios
13. Teste valida que CONTADOR só visualiza movimentos de sua empresa
14. Teste valida toggle status ACTIVE ↔ INACTIVE funciona
15. Teste valida que período fechado bloqueia criação/edição
16. Teste valida filtro por movementBook retorna apenas movimentos do livro especificado (LALUR ou LACS)
17. Teste valida filtro por movementType retorna apenas movimentos do tipo especificado

---

## Story 4.3: Entidade TaxCalculationResult

Como desenvolvedor,
Eu quero entidade TaxCalculationResult para armazenar resultados de cálculos tributários (IRPJ e CSLL),
Para que possamos registrar histórico de cálculos realizados com memória de cálculo detalhada para auditoria.

**Acceptance Criteria:**

1. Entidade JPA `TaxCalculationResultEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company`
   - `@Column(nullable=false) Integer fiscalYear`
   - `@Enumerated(STRING) @Column(nullable=false) TaxType taxType` (IRPJ, CSLL)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal contabilProfit` (lucro/prejuízo contábil antes dos ajustes)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal totalAdditions` (soma de adições)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal totalExclusions` (soma de exclusões)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal adjustedTaxableBase` (base antes de compensações)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal totalCompensations` (soma de compensações aplicadas)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal finalTaxableBase` (base final tributável)
   - `@Column(nullable=false, precision=5, scale=2) BigDecimal taxRate` (alíquota aplicada, ex: 15.00 para 15%)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal calculatedTax` (imposto calculado)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal additionalTax` (adicional de IRPJ se aplicável, 10% sobre excesso de R$ 20k/mês)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal totalTax` (calculatedTax + additionalTax)
   - `@Column(nullable=false, precision=19, scale=2) BigDecimal remainingLossCarryforward` (prejuízo fiscal ou base negativa restante após compensação)
   - `@Column(nullable=false, columnDefinition="TEXT") String calculationMemory` (JSON detalhado passo-a-passo)
   - `@Column(nullable=false) LocalDateTime calculationDate` (data/hora do cálculo)
   - `@Column(nullable=false) String calculatedBy` (email do usuário que disparou o cálculo)
2. Enum `TaxType` criado: IRPJ, CSLL
3. Domain model `TaxCalculationResult` criado
4. `TaxCalculationResultRepositoryPort` criado:
   - `TaxCalculationResult save(TaxCalculationResult result)`
   - `Optional<TaxCalculationResult> findLatestByCompanyAndFiscalYearAndTaxType(Long companyId, Integer fiscalYear, TaxType taxType)`
   - `List<TaxCalculationResult> findAllByCompanyAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `List<TaxCalculationResult> findHistoryByCompanyAndFiscalYearAndTaxType(Long companyId, Integer fiscalYear, TaxType taxType)` (retorna todos cálculos ordenados por calculationDate DESC)
5. Adapter implementado com JPA + MapStruct
6. Teste de integração valida salvamento e recuperação
7. Teste valida que `findLatestByCompanyAndFiscalYearAndTaxType` retorna o cálculo mais recente
8. Teste valida que histórico de cálculos é preservado (múltiplos cálculos para mesmo ano/empresa/tipo)

---

## Story 4.4: Motor de Cálculo IRPJ

Como CONTADOR,
Eu quero disparar cálculo de IRPJ sob demanda para um ano fiscal,
Para que o sistema calcule automaticamente o lucro real ajustado, aplique compensações de prejuízos fiscais, calcule IRPJ (15% + adicional de 10%), e armazene memória de cálculo detalhada.

**Acceptance Criteria:**

1. Use case `CalculateIrpjUseCase` (application/port/in) criado com método `calculate(Long companyId, Integer fiscalYear, String requestedBy)`
2. Service `IrpjCalculationService` implementa use case com lógica:
   - **Passo 1**: Busca `AccountingData` (genérico) da empresa e ano fiscal para obter lucro/prejuízo contábil (campo `profit`)
   - **Passo 2**: Busca todas `FiscalMovement` ACTIVE da empresa, ano fiscal e `movementBook = LALUR`
   - **Passo 3**: Calcula `totalAdditions` (soma de movements tipo ADDITION)
   - **Passo 4**: Calcula `totalExclusions` (soma de movements tipo EXCLUSION)
   - **Passo 5**: Calcula `adjustedTaxableBase = contabilProfit + totalAdditions - totalExclusions`
   - **Passo 6**: Busca `CompanyParameter` para obter prejuízos fiscais anteriores (`prejuizoFiscalAnterior`)
   - **Passo 7**: Calcula compensação permitida: mínimo entre (a) prejuízo disponível e (b) 30% da base ajustada (se positiva)
   - **Passo 8**: Aplica compensações manuais (movements tipo COMPENSATION) se houver
   - **Passo 9**: Calcula `finalTaxableBase = adjustedTaxableBase - totalCompensations`
   - **Passo 10**: Se `finalTaxableBase <= 0`, imposto é zero, atualiza `remainingLossCarryforward`, termina
   - **Passo 11**: Se `finalTaxableBase > 0`, aplica alíquota básica (busca de `CompanyParameter.aliquotaIrpj`, default 15%)
   - **Passo 12**: Calcula `calculatedTax = finalTaxableBase * (taxRate / 100)`
   - **Passo 13**: Calcula adicional: se lucro anual > R$ 240.000 (R$ 20k/mês * 12), adicional de 10% sobre excesso
   - **Passo 14**: `additionalTax = (finalTaxableBase > 240000) ? (finalTaxableBase - 240000) * 0.10 : 0`
   - **Passo 15**: `totalTax = calculatedTax + additionalTax`
   - **Passo 16**: Atualiza `remainingLossCarryforward = prejuizoAnterior - totalCompensations`
   - **Passo 17**: Monta JSON de memória de cálculo com todos passos, valores intermediários, movimentos aplicados
   - **Passo 18**: Salva `TaxCalculationResult` com todos campos populados
3. DTO `CalculateTaxRequest`: `fiscalYear` (obrigatório)
4. DTO `CalculateTaxResponse`: `success`, `message`, `result` (objeto `TaxCalculationResult` completo)
5. Endpoint `POST /api/v1/tax-calculations/irpj` (autenticado, requer X-Company-Id)
6. Validação: requer que exista `AccountingData` para o ano fiscal (senão retorna 400 Bad Request: "Dados contábeis não encontrados para o ano fiscal")
7. Validação: requer que exista `CompanyParameter` ativo (senão usa defaults: alíquota 15%, prejuízo anterior zero)
8. Campo `calculationMemory` (JSON) contém estrutura:
   ```json
   {
     "step1_contabilProfit": 500000.00,
     "step2_fiscalMovementsCount": 5,
     "step2_fiscalMovementsBook": "LALUR",
     "step3_totalAdditions": 50000.00,
     "step4_totalExclusions": 20000.00,
     "step5_adjustedTaxableBase": 530000.00,
     "step6_previousLossCarryforward": 100000.00,
     "step7_maxCompensationAllowed": 159000.00,
     "step8_manualCompensations": 80000.00,
     "step9_finalTaxableBase": 450000.00,
     "step11_taxRate": 15.00,
     "step12_calculatedTax": 67500.00,
     "step13_additionalThreshold": 240000.00,
     "step14_additionalTax": 21000.00,
     "step15_totalTax": 88500.00,
     "step16_remainingLossCarryforward": 20000.00,
     "movementsApplied": [
       {"id": 1, "book": "LALUR", "type": "ADDITION", "amount": 30000.00, "description": "..."},
       {"id": 2, "book": "LALUR", "type": "EXCLUSION", "amount": 20000.00, "description": "..."}
     ]
   }
   ```
9. Response 200 OK com resultado completo se cálculo bem-sucedido
10. Response 400 Bad Request se dados contábeis ausentes
11. Response 403 Forbidden se empresa no header não pertence ao CONTADOR
12. Teste valida cálculo completo com lucro positivo, adições, exclusões, compensações, adicional de 10%
13. Teste valida cálculo com prejuízo contábil resultando em base final negativa (imposto zero)
14. Teste valida compensação automática limitada a 30% da base ajustada
15. Teste valida que adicional de 10% só é aplicado se base > R$ 240k
16. Teste valida que memória de cálculo JSON é gerada corretamente
17. Teste valida que apenas movimentos com `movementBook = LALUR` são considerados

---

## Story 4.5: Motor de Cálculo CSLL

Como CONTADOR,
Eu quero disparar cálculo de CSLL sob demanda para um ano fiscal,
Para que o sistema calcule automaticamente a base de cálculo ajustada, aplique compensações de bases negativas, calcule CSLL (alíquota configurável), e armazene memória de cálculo detalhada.

**Acceptance Criteria:**

1. Use case `CalculateCsllUseCase` criado com método `calculate(Long companyId, Integer fiscalYear, String requestedBy)`
2. Service `CsllCalculationService` implementa use case com lógica análoga ao IRPJ:
   - **Passo 1**: Busca `AccountingData` para obter lucro/prejuízo contábil
   - **Passo 2**: Busca todas `FiscalMovement` ACTIVE da empresa, ano fiscal e `movementBook = LACS`
   - **Passo 3**: Calcula `totalAdditions` (soma de movements tipo ADDITION)
   - **Passo 4**: Calcula `totalExclusions` (soma de movements tipo EXCLUSION)
   - **Passo 5**: Calcula `adjustedTaxableBase = contabilProfit + totalAdditions - totalExclusions`
   - **Passo 6**: Busca `CompanyParameter` para obter bases negativas anteriores (`baseNegativaCsllAnterior`)
   - **Passo 7**: Calcula compensação permitida: mínimo entre (a) base negativa disponível e (b) 30% da base ajustada (se positiva)
   - **Passo 8**: Aplica compensações manuais (movements tipo COMPENSATION)
   - **Passo 9**: Calcula `finalTaxableBase = adjustedTaxableBase - totalCompensations`
   - **Passo 10**: Se `finalTaxableBase <= 0`, imposto é zero, atualiza `remainingLossCarryforward`, termina
   - **Passo 11**: Se `finalTaxableBase > 0`, aplica alíquota (busca de `CompanyParameter.aliquotaCsll`, default 9%)
   - **Passo 12**: Calcula `calculatedTax = finalTaxableBase * (taxRate / 100)`
   - **Passo 13**: `additionalTax = 0` (CSLL não tem adicional)
   - **Passo 14**: `totalTax = calculatedTax`
   - **Passo 15**: Atualiza `remainingLossCarryforward = baseNegativaAnterior - totalCompensations`
   - **Passo 16**: Monta JSON de memória de cálculo
   - **Passo 17**: Salva `TaxCalculationResult` com `taxType = CSLL`
3. DTO `CalculateTaxRequest` e `CalculateTaxResponse` reutilizados
4. Endpoint `POST /api/v1/tax-calculations/csll` (autenticado, requer X-Company-Id)
5. Validação: requer `AccountingData` para o ano fiscal
6. Validação: requer `CompanyParameter` ativo (senão usa defaults: alíquota 9%, base negativa anterior zero)
7. Campo `calculationMemory` segue estrutura similar ao IRPJ (sem step13/14 de adicional, com `fiscalMovementsBook = "LACS"`)
8. Response 200 OK com resultado completo
9. Response 400 Bad Request se dados contábeis ausentes
10. Response 403 Forbidden se empresa não pertence ao CONTADOR
11. Teste valida cálculo completo com lucro positivo, adições, exclusões, compensações
12. Teste valida cálculo com base negativa resultando em imposto zero
13. Teste valida compensação automática limitada a 30%
14. Teste valida que não há adicional (additionalTax sempre zero)
15. Teste valida memória de cálculo JSON
16. Teste valida que apenas movimentos com `movementBook = LACS` são considerados

---

## Story 4.6: Endpoint de Consulta de Resultados de Cálculo

Como CONTADOR,
Eu quero consultar resultados de cálculos tributários (IRPJ e CSLL) para um ano fiscal,
Para que eu possa visualizar o resultado mais recente e o histórico completo de cálculos realizados.

**Acceptance Criteria:**

1. Controller `TaxCalculationController` criado com endpoints:
   - `GET /api/v1/tax-calculations/irpj/latest?fiscalYear=2024` (autenticado, requer X-Company-Id)
   - `GET /api/v1/tax-calculations/csll/latest?fiscalYear=2024` (autenticado, requer X-Company-Id)
   - `GET /api/v1/tax-calculations/history?fiscalYear=2024` (autenticado, requer X-Company-Id)
2. DTO `TaxCalculationResultResponse`:
   - `id`, `companyId`, `fiscalYear`, `taxType`
   - `contabilProfit`, `totalAdditions`, `totalExclusions`, `adjustedTaxableBase`
   - `totalCompensations`, `finalTaxableBase`, `taxRate`
   - `calculatedTax`, `additionalTax`, `totalTax`
   - `remainingLossCarryforward`, `calculationMemory` (JSON string)
   - `calculationDate`, `calculatedBy`, `status`, `createdAt`
3. DTO `TaxCalculationHistoryResponse`:
   - `fiscalYear`, `companyId`
   - `irpjCalculations`: lista de `TaxCalculationResultResponse` ordenada por `calculationDate DESC`
   - `csllCalculations`: lista de `TaxCalculationResultResponse` ordenada por `calculationDate DESC`
4. Use cases implementados: `GetLatestIrpjCalculationUseCase`, `GetLatestCsllCalculationUseCase`, `GetCalculationHistoryUseCase`
5. Endpoint `/latest` retorna 404 Not Found se nunca foi feito cálculo para o ano fiscal
6. Endpoint `/latest` retorna apenas resultados ACTIVE (ignora inativos)
7. Endpoint `/history` retorna lista completa de cálculos (incluindo inativos se `?include_inactive=true`)
8. Validação: CONTADOR só acessa cálculos da empresa no header X-Company-Id
9. Response 403 Forbidden se tentar acessar cálculos de outra empresa
10. Response 200 OK com resultado mais recente ou histórico completo
11. Teste valida que `/latest` retorna último cálculo realizado
12. Teste valida que `/history` retorna todos cálculos ordenados por data decrescente
13. Teste valida que CONTADOR só acessa cálculos de sua empresa
14. Teste valida que se não houver cálculo, `/latest` retorna 404

---

## Story 4.7: Validação de Pré-requisitos para Cálculo

Como CONTADOR,
Eu quero que o sistema valide pré-requisitos antes de permitir cálculo tributário,
Para que eu seja avisado se faltam dados contábeis, parâmetros tributários ou movimentações obrigatórias.

**Acceptance Criteria:**

1. Use case `ValidateCalculationPrerequisitesUseCase` criado com método `validate(Long companyId, Integer fiscalYear)`
2. Service implementa validações:
   - **Validação 1**: Existe `AccountingData` ACTIVE para a empresa e ano fiscal?
   - **Validação 2**: Existe `CompanyParameter` ACTIVE para a empresa?
   - **Validação 3**: Existem `ChartOfAccount` cadastrados para a empresa? (mínimo 1 conta)
   - **Validação 4**: Período contábil não está fechado? (`CompanyParameter.fechamentoContabil != fiscalYear`)
3. DTO `ValidationResultResponse`:
   - `valid` (boolean)
   - `errors`: lista de mensagens de erro/avisos
   - `warnings`: lista de avisos não bloqueantes (ex: "Nenhuma movimentação fiscal cadastrada")
4. Endpoint `GET /api/v1/tax-calculations/validate?fiscalYear=2024` (autenticado, requer X-Company-Id)
5. Response 200 OK com `ValidationResultResponse`:
   - Se `valid = true`, cálculo pode ser executado
   - Se `valid = false`, `errors` contém lista de problemas bloqueantes
6. Exemplos de mensagens de erro:
   - "Dados contábeis não encontrados para o ano fiscal 2024"
   - "Parâmetros tributários não configurados para a empresa"
   - "Plano de contas vazio. Cadastre ao menos uma conta contábil"
   - "Período contábil 2024 já está fechado. Não é possível calcular impostos"
7. Exemplos de avisos (warnings):
   - "Nenhuma movimentação fiscal LALUR cadastrada para o ano fiscal 2024"
   - "Nenhuma movimentação fiscal LACS cadastrada para o ano fiscal 2024"
8. Validação: CONTADOR só valida para sua empresa
9. Response 403 Forbidden se empresa não pertence ao CONTADOR
10. Teste valida que falta de AccountingData torna validação inválida
11. Teste valida que falta de CompanyParameter torna validação inválida
12. Teste valida que plano de contas vazio torna validação inválida
13. Teste valida que período fechado torna validação inválida
14. Teste valida que ausência de movimentações gera apenas warning (não bloqueia)
15. Teste valida que com todos pré-requisitos, `valid = true`

---

## Story 4.8: Detecção de Desatualização de Cálculos (Sem Invalidação Automática)

Como CONTADOR,
Eu quero visualizar indicador se dados foram modificados após o último cálculo,
Para que eu saiba quando preciso recalcular impostos, mas sem que o sistema invalide automaticamente os resultados anteriores.

**Acceptance Criteria:**

1. Service `CalculationStatusService` criado
2. Método `isCalculationOutdated(Long companyId, Integer fiscalYear, TaxType taxType)` retorna boolean:
   - Busca último `TaxCalculationResult` ACTIVE do tipo especificado
   - Se não existe cálculo, retorna false (não há cálculo para estar desatualizado)
   - Se existe cálculo, compara `calculationDate` com:
     - `max(updatedAt)` de todas `FiscalMovement` ACTIVE do livro correspondente (LALUR para IRPJ, LACS para CSLL)
     - `updatedAt` de `AccountingData` da empresa e ano fiscal
   - Se qualquer dado foi modificado APÓS `calculationDate`, retorna true (desatualizado)
   - Caso contrário, retorna false (atualizado)
3. Método `getOutdatedStatus(Long companyId, Integer fiscalYear)` retorna `CalculationOutdatedStatus`:
   - `irpjOutdated` (boolean)
   - `csllOutdated` (boolean)
   - `lastIrpjCalculationDate` (LocalDateTime, null se nunca calculado)
   - `lastCsllCalculationDate` (LocalDateTime, null se nunca calculado)
   - `lastDataModificationDate` (LocalDateTime): max entre AccountingData.updatedAt e FiscalMovement.updatedAt (ambos livros)
4. Use case `GetCalculationOutdatedStatusUseCase` criado
5. Endpoint `GET /api/v1/tax-calculations/outdated-status?fiscalYear=2024` (autenticado, requer X-Company-Id)
6. DTO `CalculationOutdatedStatusResponse`:
   - `fiscalYear`, `companyId`
   - `irpjOutdated`, `csllOutdated`
   - `lastIrpjCalculationDate`, `lastCsllCalculationDate`, `lastDataModificationDate`
   - `message`: mensagem descritiva (ex: "Cálculo IRPJ está desatualizado. Dados foram modificados em 18/10/2024 às 14:30, após o último cálculo realizado em 15/10/2024 às 10:00.")
7. Response 200 OK com status de desatualização
8. Response 403 Forbidden se empresa não pertence ao CONTADOR
9. **IMPORTANTE**: Sistema **NÃO** invalida automaticamente cálculos existentes quando dados são modificados
10. **IMPORTANTE**: Cálculos antigos permanecem ACTIVE e acessíveis via `/latest` mesmo se dados foram modificados posteriormente
11. Teste valida que após criar movimento fiscal, `isCalculationOutdated` retorna true
12. Teste valida que após atualizar AccountingData, `isCalculationOutdated` retorna true
13. Teste valida que se não houver modificações após cálculo, `isCalculationOutdated` retorna false
14. Teste valida que cálculos antigos permanecem ACTIVE mesmo com dados desatualizados
15. Teste valida que endpoint `/outdated-status` retorna indicadores corretos

---

## Story 4.9: Testes End-to-End do Fluxo de Cálculo

Como desenvolvedor,
Eu quero testes E2E cobrindo fluxo completo de cálculo tributário,
Para garantir que o motor de cálculo funciona corretamente de ponta a ponta.

**Acceptance Criteria:**

1. Teste E2E `IrpjCalculationFlowTest`:
   - Setup: cria empresa, parâmetros tributários, plano de contas, dados contábeis (lucro 500k)
   - Setup: cria 3 movimentos fiscais LALUR (2 adições de 50k, 1 exclusão de 20k)
   - Executa: valida pré-requisitos (GET /validate) → `valid = true`
   - Executa: dispara cálculo IRPJ (POST /irpj)
   - Valida: resultado retorna `totalTax > 0`
   - Valida: `finalTaxableBase = 500000 + 100000 - 20000 = 580000`
   - Valida: `calculatedTax = 580000 * 0.15 = 87000`
   - Valida: `additionalTax = (580000 - 240000) * 0.10 = 34000`
   - Valida: `totalTax = 121000`
   - Executa: consulta resultado (GET /irpj/latest)
   - Valida: resultado retornado é o mesmo do cálculo
   - Executa: cria novo movimento fiscal LALUR (adição de 30k)
   - Executa: consulta status desatualização (GET /outdated-status)
   - Valida: `irpjOutdated = true` (cálculo ficou desatualizado)
   - Valida: cálculo anterior ainda está ACTIVE (GET /irpj/latest retorna o cálculo antigo, não 404)
   - Executa: recalcula IRPJ manualmente (usuário clica no botão)
   - Valida: novo resultado reflete novo movimento
   - Valida: histórico contém ambos cálculos (GET /history)
2. Teste E2E `CsllCalculationFlowTest`:
   - Setup: cria empresa, parâmetros (alíquota CSLL 9%), plano de contas, dados contábeis (lucro 400k)
   - Setup: cria 2 movimentos fiscais LACS (1 adição de 20k, 1 exclusão de 10k)
   - Executa: dispara cálculo CSLL
   - Valida: `finalTaxableBase = 400000 + 20000 - 10000 = 410000`
   - Valida: `calculatedTax = 410000 * 0.09 = 36900`
   - Valida: `additionalTax = 0`
   - Valida: `totalTax = 36900`
   - Executa: consulta histórico (GET /history)
   - Valida: histórico contém 1 cálculo CSLL
3. Teste E2E `CompensationFlowTest`:
   - Setup: cria empresa com prejuízo fiscal anterior de 150k nos parâmetros
   - Setup: dados contábeis com lucro 500k
   - Executa: cálculo IRPJ
   - Valida: compensação automática aplicada = min(150k, 500k * 0.30) = 150k
   - Valida: `finalTaxableBase = 500000 - 150000 = 350000`
   - Valida: `remainingLossCarryforward = 0`
4. Teste E2E `InvalidDataBlocksCalculationTest`:
   - Setup: cria empresa SEM dados contábeis
   - Executa: valida pré-requisitos
   - Valida: `valid = false`, erro "Dados contábeis não encontrados"
   - Executa: tenta calcular IRPJ
   - Valida: retorna 400 Bad Request
5. Teste E2E `UnifiedEntityIsolationTest`:
   - Setup: cria empresa com 5 movimentos fiscais (3 LALUR, 2 LACS)
   - Executa: cálculo IRPJ
   - Valida: apenas 3 movimentos LALUR são considerados no cálculo
   - Executa: cálculo CSLL
   - Valida: apenas 2 movimentos LACS são considerados no cálculo
6. Teste E2E `NoAutomaticInvalidationTest`:
   - Setup: cria empresa, dados contábeis, movimentos fiscais LALUR
   - Executa: cálculo IRPJ (resultado A salvo)
   - Executa: cria novo movimento fiscal LALUR
   - Valida: resultado A permanece ACTIVE
   - Valida: GET /irpj/latest retorna resultado A (não 404)
   - Valida: GET /outdated-status retorna `irpjOutdated = true`
   - Executa: usuário clica em recalcular (POST /irpj)
   - Valida: novo resultado B é salvo
   - Valida: ambos resultados A e B aparecem no histórico (GET /history)
7. Todos testes usam TestContainers PostgreSQL
8. Todos testes criam contexto completo (usuário CONTADOR, empresa, X-Company-Id header)
9. Cobertura de código do Epic 4 deve ser >= 80%

---

## Resumo do Epic

Ao final deste épico, o sistema terá:

- **Entidade unificada `FiscalMovement`** com campo diferenciador `movementBook` (LALUR ou LACS) substituindo LalurMovement e LacsMovement
- CRUD completo de movimentações fiscais com filtro por livro (LALUR/LACS)
- Entidade `TaxCalculationResult` para histórico de cálculos
- Motor de cálculo IRPJ com adicional de 10%, compensações automáticas (30%), memória de cálculo JSON
- Motor de cálculo CSLL com compensações automáticas (30%), memória de cálculo JSON
- Endpoints de consulta de resultados (latest, history)
- Validação de pré-requisitos antes de calcular
- **Detecção de desatualização de cálculos SEM invalidação automática** - cálculos antigos permanecem acessíveis
- Endpoint `/outdated-status` para verificar se dados foram modificados após último cálculo
- **Recálculo APENAS sob demanda** quando usuário clicar explicitamente no botão
- Testes E2E cobrindo fluxos completos de cálculo e isolamento entre LALUR/LACS
- Isolamento multi-tenant via X-Company-Id
- Proteção contra modificações em períodos fechados
- Auditoria completa (createdBy, updatedBy, calculatedBy)

**Mudanças em relação à versão anterior:**
- ✅ Unificou LalurMovement e LacsMovement em uma única entidade `FiscalMovement` com campo `movementBook`
- ✅ Removeu Story 4.10 de invalidação automática
- ✅ Adicionou Story 4.8 de detecção de desatualização sem invalidação
- ✅ Endpoints de cálculo IRPJ/CSLL agora usam filtro `movementBook` ao buscar movimentos
- ✅ Sistema não invalida cálculos automaticamente - apenas indica visualmente quando estão desatualizados
- ✅ Recálculo ocorre APENAS quando usuário clicar no botão

**Dependências de Epics Anteriores:**
- Epic 1: Autenticação JWT, usuários CONTADOR
- Epic 2: Entidades Company e CompanyParameter
- Epic 3: ChartOfAccount, CodigoEnquadramentoLalur, AccountingData

**Próximos Passos (Epic 5):**
- Geração do arquivo ECF final (formato SPED)
- Exportação XML
- Validações de completude
- Assinatura digital (opcional)
