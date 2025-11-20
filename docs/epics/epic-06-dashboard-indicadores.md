# Epic 6: Dashboard & Indicadores de Progresso

**Objetivo do Epic:**

Implementar dashboard centralizado com visão geral de empresas, indicadores de completude (porcentagem de dados preenchidos), status de cálculos tributários, alertas de pendências e rastreamento de progresso por etapa do workflow ECF. Este épico entrega visibilidade clara e actionable para contadores gerenciando múltiplas empresas sob prazo, permitindo identificar rapidamente o que está completo, o que está pendente e quais ações são necessárias para cada empresa. Ao final deste épico, o sistema fornecerá um painel executivo com métricas de progresso, indicadores visuais de completude e alertas inteligentes que guiam o contador através do fluxo ECF de forma eficiente e sem sobrecarga cognitiva.

---

## Story 6.1: Modelo de Completude (Completion Tracking)

Como desenvolvedor,
Eu quero serviço que calcule completude de dados ECF para uma empresa,
Para que possamos exibir indicadores de progresso (% concluído) no dashboard.

**Acceptance Criteria:**

1. Service `CompletionTrackingService` criado
2. Método `calculateCompletion(Long companyId, Integer fiscalYear)` retorna `CompletionStatus`:
   - **Etapa 1 - Parâmetros Tributários:** verifica se existe `CompanyParameter` ACTIVE
   - **Etapa 2 - Plano de Contas:** verifica se existem `ChartOfAccount` cadastrados (mínimo 1)
   - **Etapa 3 - Dados Contábeis:** verifica se existe `AccountingData` para o ano fiscal
   - **Etapa 4 - Movimentações Lalur:** verifica se existem `LalurMovement` (opcional, não bloqueia)
   - **Etapa 5 - Movimentações Lacs:** verifica se existem `LacsMovement` (opcional, não bloqueia)
   - **Etapa 6 - Cálculo IRPJ:** verifica se existe `TaxCalculationResult` ACTIVE tipo IRPJ
   - **Etapa 7 - Cálculo CSLL:** verifica se existe `TaxCalculationResult` ACTIVE tipo CSLL
   - **Etapa 8 - Geração ECF:** verifica se existe `EcfFile` VALIDATED ou FINALIZED
3. DTO `CompletionStatus`:
   - `overallCompletionPercentage` (0-100): porcentagem geral de completude
   - `steps`: lista de `CompletionStep`
4. DTO `CompletionStep`:
   - `stepName` (string): "Parâmetros Tributários", "Plano de Contas", etc.
   - `stepOrder` (int): ordem da etapa (1-8)
   - `completed` (boolean): true se etapa concluída
   - `optional` (boolean): true se etapa é opcional (movimentações Lalur/Lacs)
   - `message` (string): descrição do status (ex: "3 movimentações cadastradas" ou "Pendente")
5. Cálculo de `overallCompletionPercentage`:
   - Etapas obrigatórias: 1, 2, 3, 6, 7, 8 (6 etapas)
   - Etapas opcionais: 4, 5 (não afetam % se não completadas)
   - % = (etapas obrigatórias completas / 6) * 100
   - Etapas opcionais completas adicionam bonus visual (não mudam %)
6. Teste valida cálculo com todas etapas completas → 100%
7. Teste valida cálculo com apenas parâmetros e plano de contas → 33% (2/6)
8. Teste valida que etapas opcionais não afetam % (movimentações ausentes ainda permite 100%)
9. Teste valida mensagens descritivas para cada etapa

---

## Story 6.2: Endpoint de Completude por Empresa

Como CONTADOR,
Eu quero consultar completude de dados ECF para uma empresa específica,
Para que eu possa visualizar quais etapas estão concluídas e quais estão pendentes.

**Acceptance Criteria:**

1. Use case `GetCompletionStatusUseCase` criado
2. Endpoint `GET /api/v1/companies/{companyId}/completion?fiscalYear=2024` (autenticado)
3. Response DTO `CompletionStatusResponse`:
   - `companyId`, `fiscalYear`
   - `overallCompletionPercentage`
   - `steps`: lista de `CompletionStepResponse`
4. `CompletionStepResponse`:
   - `stepName`, `stepOrder`, `completed`, `optional`, `message`
5. Validação: CONTADOR só pode acessar completude de sua empresa (X-Company-Id)
6. Validação: ADMIN pode acessar completude de qualquer empresa
7. Response 200 OK com `CompletionStatusResponse`
8. Response 403 Forbidden se CONTADOR tentar acessar empresa que não é sua
9. Response 404 Not Found se empresa não existe
10. Teste valida que CONTADOR acessa completude de sua empresa
11. Teste valida que CONTADOR recebe 403 ao tentar acessar outra empresa
12. Teste valida que ADMIN pode acessar qualquer empresa
13. Teste valida estrutura correta da resposta com 8 etapas

---

## Story 6.3: Dashboard - Lista de Empresas com Indicadores

Como CONTADOR,
Eu quero visualizar lista de empresas com indicadores de completude,
Para que eu possa identificar rapidamente qual empresa precisa de atenção.

**Acceptance Criteria:**

1. Use case `GetDashboardUseCase` criado
2. Endpoint `GET /api/v1/dashboard?fiscalYear=2024` (autenticado, requer X-Company-Id se CONTADOR)
3. DTO `DashboardResponse`:
   - `fiscalYear`
   - `companies`: lista de `CompanyDashboardItem`
4. DTO `CompanyDashboardItem`:
   - `companyId`, `cnpj`, `razaoSocial`
   - `completionPercentage` (0-100)
   - `hasCalculations` (boolean): true se cálculos IRPJ e CSLL estão finalizados
   - `hasEcfFile` (boolean): true se arquivo ECF foi gerado
   - `ecfFileStatus` (enum): DRAFT, VALIDATED, FINALIZED, null se não gerado
   - `lastUpdatedAt` (LocalDateTime): data da última modificação em qualquer entidade relacionada
   - `pendingStepsCount` (int): quantidade de etapas obrigatórias pendentes
5. Lógica de `lastUpdatedAt`:
   - Busca max(updatedAt) entre: CompanyParameter, ChartOfAccount, AccountingData, LalurMovement, LacsMovement, TaxCalculationResult, EcfFile
6. Ordenação padrão: `completionPercentage ASC, lastUpdatedAt DESC` (empresas menos completas primeiro, mais recentes primeiro se empate)
7. Suporta filtro: `?status=pending` (retorna apenas empresas com completionPercentage < 100)
8. Suporta filtro: `?status=completed` (retorna apenas empresas com completionPercentage = 100)
9. Validação: CONTADOR vê apenas empresas associadas ao seu contexto (header X-Company-Id)
10. Validação: ADMIN vê todas empresas do sistema
11. Response 200 OK com lista de empresas e indicadores
12. Teste valida que CONTADOR vê apenas sua empresa
13. Teste valida que ADMIN vê todas empresas
14. Teste valida cálculo correto de completionPercentage para cada empresa
15. Teste valida ordenação (menos completas primeiro)
16. Teste valida filtro por status funciona

---

## Story 6.4: Dashboard - Alertas e Pendências

Como CONTADOR,
Eu quero visualizar alertas de pendências para uma empresa,
Para que eu seja notificado sobre ações críticas necessárias antes da geração ECF.

**Acceptance Criteria:**

1. Service `AlertService` criado
2. Método `generateAlerts(Long companyId, Integer fiscalYear)` retorna lista de `Alert`:
   - **Alerta Crítico:** "Parâmetros tributários não configurados" (se não existe CompanyParameter)
   - **Alerta Crítico:** "Plano de contas vazio. Cadastre ao menos uma conta contábil" (se ChartOfAccount vazio)
   - **Alerta Crítico:** "Dados contábeis não encontrados para o ano fiscal {year}" (se AccountingData ausente)
   - **Alerta Crítico:** "Cálculo IRPJ não realizado" (se TaxCalculationResult IRPJ ausente)
   - **Alerta Crítico:** "Cálculo CSLL não realizado" (se TaxCalculationResult CSLL ausente)
   - **Alerta Aviso:** "Nenhuma movimentação Lalur cadastrada" (se LalurMovement vazio - não bloqueia)
   - **Alerta Aviso:** "Nenhuma movimentação Lacs cadastrada" (se LacsMovement vazio - não bloqueia)
   - **Alerta Aviso:** "Cálculo IRPJ desatualizado (dados modificados após último cálculo)" (se calculationDate < max(updatedAt) de movimentos)
   - **Alerta Aviso:** "Cálculo CSLL desatualizado" (análogo ao IRPJ)
   - **Alerta Info:** "Arquivo ECF gerado com sucesso mas aguardando validação" (se EcfFile status DRAFT)
   - **Alerta Info:** "Arquivo ECF validado e pronto para transmissão" (se EcfFile status VALIDATED)
3. DTO `Alert`:
   - `severity` (enum): CRITICAL, WARNING, INFO
   - `message` (string): mensagem do alerta
   - `actionable` (boolean): true se requer ação do usuário
   - `suggestedAction` (string): ação sugerida (ex: "Clique aqui para cadastrar parâmetros tributários")
4. Use case `GetAlertsUseCase` criado
5. Endpoint `GET /api/v1/companies/{companyId}/alerts?fiscalYear=2024` (autenticado)
6. Response DTO `AlertsResponse`:
   - `companyId`, `fiscalYear`
   - `criticalAlertsCount` (int)
   - `warningAlertsCount` (int)
   - `infoAlertsCount` (int)
   - `alerts`: lista de `AlertResponse`
7. Validação: CONTADOR só acessa alertas de sua empresa
8. Response 200 OK com lista de alertas
9. Response 403 Forbidden se CONTADOR tentar acessar outra empresa
10. Teste valida geração de alerta crítico se parâmetros ausentes
11. Teste valida geração de alerta warning se movimentações ausentes
12. Teste valida geração de alerta info se ECF foi gerado
13. Teste valida que alertas críticos aparecem primeiro na lista
14. Teste valida contagem correta de alertas por severidade

---

## Story 6.5: Dashboard - Status de Cálculos

Como CONTADOR,
Eu quero visualizar status resumido dos cálculos tributários (IRPJ e CSLL),
Para que eu possa verificar rapidamente se cálculos estão atualizados e valores calculados.

**Acceptance Criteria:**

1. Service `CalculationStatusService` criado
2. Método `getCalculationStatus(Long companyId, Integer fiscalYear)` retorna `CalculationStatusSummary`:
   - Busca último `TaxCalculationResult` ACTIVE tipo IRPJ
   - Busca último `TaxCalculationResult` ACTIVE tipo CSLL
   - Verifica se cálculos estão desatualizados (calculationDate < max(updatedAt) de movimentos ou dados contábeis)
3. DTO `CalculationStatusSummary`:
   - `fiscalYear`, `companyId`
   - `irpjStatus`: objeto `TaxCalculationSummary`
   - `csllStatus`: objeto `TaxCalculationSummary`
4. DTO `TaxCalculationSummary`:
   - `calculated` (boolean): true se cálculo existe
   - `calculationDate` (LocalDateTime): data do último cálculo
   - `totalTax` (BigDecimal): imposto total calculado
   - `isOutdated` (boolean): true se dados foram modificados após cálculo
   - `status` (enum): NOT_CALCULATED, UP_TO_DATE, OUTDATED
5. Use case `GetCalculationStatusUseCase` criado
6. Endpoint `GET /api/v1/companies/{companyId}/calculation-status?fiscalYear=2024` (autenticado)
7. Response 200 OK com `CalculationStatusSummary`
8. Response 403 Forbidden se CONTADOR tentar acessar outra empresa
9. Response 404 Not Found se empresa não existe
10. Teste valida status NOT_CALCULATED se cálculo ausente
11. Teste valida status UP_TO_DATE se cálculo atualizado
12. Teste valida status OUTDATED se dados modificados após cálculo
13. Teste valida valores totalTax corretos

---

## Story 6.6: Dashboard - Indicador de Prazo (Deadline Tracking)

Como CONTADOR,
Eu quero visualizar indicador de proximidade do prazo de entrega ECF,
Para que eu seja alertado se prazo está próximo e empresa ainda não está completa.

**Acceptance Criteria:**

1. Service `DeadlineService` criado
2. Método `calculateDeadlineStatus(Integer fiscalYear)` retorna `DeadlineStatus`:
   - Prazo oficial ECF: **último dia útil de julho do ano seguinte ao ano fiscal**
   - Ex: ano fiscal 2024 → prazo 31/07/2025
   - Calcula dias úteis restantes até prazo
   - Classifica urgência: SAFE (> 30 dias), WARNING (15-30 dias), CRITICAL (< 15 dias), OVERDUE (já passou)
3. DTO `DeadlineStatus`:
   - `fiscalYear`
   - `deadlineDate` (LocalDate): data do prazo oficial
   - `daysRemaining` (int): dias úteis restantes (negativo se atrasado)
   - `urgency` (enum): SAFE, WARNING, CRITICAL, OVERDUE
   - `message` (string): mensagem contextual (ex: "Faltam 45 dias úteis até o prazo")
4. Integração com dashboard: cada `CompanyDashboardItem` inclui campo `deadlineUrgency`
5. Use case `GetDeadlineStatusUseCase` criado
6. Endpoint `GET /api/v1/deadline?fiscalYear=2024` (público ou autenticado)
7. Response 200 OK com `DeadlineStatus`
8. Teste valida cálculo correto de dias úteis restantes
9. Teste valida classificação de urgência (SAFE, WARNING, CRITICAL, OVERDUE)
10. Teste valida mensagem contextual apropriada para cada urgência

---

## Story 6.7: Dashboard - Resumo Executivo Multi-Empresa (ADMIN)

Como ADMIN,
Eu quero visualizar resumo executivo de todas empresas do sistema,
Para que eu possa monitorar progresso geral e identificar gargalos operacionais.

**Acceptance Criteria:**

1. Use case `GetExecutiveSummaryUseCase` criado (apenas ADMIN)
2. Endpoint `GET /api/v1/dashboard/executive-summary?fiscalYear=2024` (autenticado, ADMIN only)
3. DTO `ExecutiveSummaryResponse`:
   - `fiscalYear`
   - `totalCompanies` (int): total de empresas cadastradas
   - `completedCompanies` (int): empresas com 100% completude
   - `pendingCompanies` (int): empresas com < 100% completude
   - `averageCompletionPercentage` (double): média de completude de todas empresas
   - `companiesWithEcfGenerated` (int): empresas que geraram arquivo ECF
   - `companiesWithEcfFinalized` (int): empresas com ECF finalizado
   - `topPendingSteps`: lista de etapas mais pendentes (ex: "Cálculo IRPJ" aparece em 20 empresas)
   - `deadlineUrgency`: objeto `DeadlineStatus` para o ano fiscal
4. DTO `TopPendingStep`:
   - `stepName` (string)
   - `pendingCount` (int): quantidade de empresas com essa etapa pendente
5. Validação: apenas ADMIN pode acessar
6. Response 200 OK com resumo executivo
7. Response 403 Forbidden se usuário não é ADMIN
8. Teste valida cálculo correto de totalCompanies, completedCompanies, pendingCompanies
9. Teste valida cálculo de averageCompletionPercentage
10. Teste valida identificação de topPendingSteps (etapas mais comuns pendentes)
11. Teste valida que CONTADOR recebe 403

---

## Story 6.8: Dashboard - Filtros e Busca

Como CONTADOR,
Eu quero filtrar e buscar empresas no dashboard,
Para que eu possa encontrar rapidamente empresas específicas ou grupos de empresas.

**Acceptance Criteria:**

1. Endpoint `GET /api/v1/dashboard` suporta query params adicionais:
   - `?search={razaoSocial ou CNPJ}` - busca textual por razão social ou CNPJ
   - `?completionMin={0-100}` - filtra empresas com completude >= valor
   - `?completionMax={0-100}` - filtra empresas com completude <= valor
   - `?hasEcfFile=true` - filtra apenas empresas que geraram ECF
   - `?ecfFileStatus=VALIDATED` - filtra por status do arquivo ECF
   - `?sort=completionPercentage,asc` ou `sort=lastUpdatedAt,desc`
2. Busca textual é case-insensitive e usa LIKE
3. Filtros são cumulativos (AND)
4. Paginação: `?page=0&size=20`
5. Response inclui metadados de paginação: `totalElements`, `totalPages`, `currentPage`
6. Validação: CONTADOR filtra apenas dentro de suas empresas
7. Validação: ADMIN filtra dentro de todas empresas
8. Teste valida busca por razão social encontra empresa correta
9. Teste valida busca por CNPJ encontra empresa correta
10. Teste valida filtro por completionMin funciona
11. Teste valida filtro por hasEcfFile funciona
12. Teste valida ordenação por completionPercentage ASC e DESC
13. Teste valida paginação retorna metadados corretos

---

## Story 6.9: Dashboard - Exportação de Relatório CSV

Como CONTADOR,
Eu quero exportar relatório CSV do dashboard com status de todas empresas,
Para que eu possa compartilhar progresso com cliente ou gerar relatórios externos.

**Acceptance Criteria:**

1. Use case `ExportDashboardReportUseCase` criado
2. Endpoint `GET /api/v1/dashboard/export?fiscalYear=2024&format=csv` (autenticado)
3. Service gera arquivo CSV com colunas:
   - CNPJ, Razão Social, Completude (%), Cálculo IRPJ (Sim/Não), Cálculo CSLL (Sim/Não), ECF Gerado (Sim/Não), Status ECF, Última Atualização, Pendências Críticas
4. Formatação CSV: cabeçalho na primeira linha, valores separados por vírgula, strings com aspas duplas
5. Response com headers:
   - `Content-Type: text/csv; charset=UTF-8`
   - `Content-Disposition: attachment; filename="dashboard_ECF_{fiscalYear}_{timestamp}.csv"`
6. Response 200 OK com arquivo CSV no body
7. Validação: CONTADOR exporta apenas suas empresas
8. Validação: ADMIN exporta todas empresas
9. Response 403 Forbidden se CONTADOR sem empresas associadas
10. Teste valida geração de CSV com 3 empresas
11. Teste valida que CONTADOR exporta apenas suas empresas
12. Teste valida formato CSV correto (cabeçalho + linhas)
13. Teste valida Content-Type e Content-Disposition headers

---

## Story 6.10: Dashboard - Histórico de Atividades (Audit Log)

Como CONTADOR,
Eu quero visualizar histórico de atividades recentes em uma empresa,
Para que eu possa rastrear quem fez o quê e quando.

**Acceptance Criteria:**

1. Service `ActivityLogService` criado
2. Método `getRecentActivities(Long companyId, int limit)` retorna lista de `ActivityLogEntry`:
   - Agrega atividades de: criação/edição de movimentos Lalur/Lacs, execução de cálculos, geração de ECF, importação de Parte A
   - Busca campos `createdAt`, `createdBy`, `updatedAt`, `updatedBy` de todas entidades relacionadas
   - Ordena por timestamp DESC (mais recentes primeiro)
   - Limita a `limit` registros (default 20)
3. DTO `ActivityLogEntry`:
   - `timestamp` (LocalDateTime)
   - `activityType` (enum): CREATED, UPDATED, CALCULATED, GENERATED
   - `entityType` (string): "LalurMovement", "TaxCalculationResult", "EcfFile"
   - `entityId` (Long)
   - `performedBy` (string): email do usuário
   - `description` (string): descrição legível (ex: "Criou movimentação Lalur: Adição de R$ 50.000,00")
4. Use case `GetActivityLogUseCase` criado
5. Endpoint `GET /api/v1/companies/{companyId}/activity-log?limit=20` (autenticado)
6. Response 200 OK com lista de `ActivityLogEntry`
7. Response 403 Forbidden se CONTADOR tentar acessar outra empresa
8. Validação: CONTADOR só acessa atividades de sua empresa
9. Teste valida que atividades aparecem ordenadas por timestamp DESC
10. Teste valida descrição legível para cada tipo de atividade
11. Teste valida limite de registros funciona
12. Teste valida que CONTADOR só vê atividades de sua empresa

---

## Story 6.11: Testes End-to-End do Dashboard

Como desenvolvedor,
Eu quero testes E2E cobrindo fluxo completo do dashboard,
Para garantir que indicadores, alertas e filtros funcionam corretamente.

**Acceptance Criteria:**

1. Teste E2E `DashboardCompletenessFlowTest`:
   - Setup: cria 3 empresas com diferentes níveis de completude (0%, 50%, 100%)
   - Executa: consulta dashboard (GET /dashboard)
   - Valida: retorna 3 empresas ordenadas por completionPercentage ASC
   - Valida: empresa 0% tem completionPercentage = 0
   - Valida: empresa 50% tem 3 etapas completas (parâmetros, plano de contas, dados contábeis)
   - Valida: empresa 100% tem todas etapas completas
   - Executa: consulta completude de empresa 50% (GET /companies/{id}/completion)
   - Valida: retorna 8 etapas, 3 completas, 3 pendentes, 2 opcionais
2. Teste E2E `AlertsFlowTest`:
   - Setup: cria empresa sem parâmetros tributários e sem dados contábeis
   - Executa: consulta alertas (GET /companies/{id}/alerts)
   - Valida: retorna 2 alertas críticos: "Parâmetros não configurados", "Dados contábeis ausentes"
   - Setup: cadastra parâmetros e dados contábeis
   - Executa: consulta alertas novamente
   - Valida: alertas críticos foram resolvidos, agora tem apenas warnings (movimentações ausentes)
3. Teste E2E `CalculationStatusFlowTest`:
   - Setup: cria empresa com dados completos, executa cálculos IRPJ e CSLL
   - Executa: consulta status de cálculo (GET /companies/{id}/calculation-status)
   - Valida: irpjStatus.status = UP_TO_DATE, csllStatus.status = UP_TO_DATE
   - Setup: cria nova movimentação Lalur (modifica dados após cálculo)
   - Executa: consulta status novamente
   - Valida: irpjStatus.status = OUTDATED (cálculo desatualizado)
4. Teste E2E `ExecutiveSummaryFlowTest` (ADMIN):
   - Setup: cria 5 empresas (2 completas, 3 pendentes)
   - Executa: consulta resumo executivo (GET /dashboard/executive-summary) como ADMIN
   - Valida: totalCompanies = 5, completedCompanies = 2, pendingCompanies = 3
   - Valida: averageCompletionPercentage está correto
   - Valida: topPendingSteps identifica corretamente etapas mais pendentes
5. Teste E2E `DashboardFiltersFlowTest`:
   - Setup: cria 10 empresas com razões sociais e CNPJs diversos
   - Executa: busca por razão social parcial (GET /dashboard?search=Empresa ABC)
   - Valida: retorna apenas empresas que contêm "Empresa ABC" no nome
   - Executa: filtra por completionMin=80 (GET /dashboard?completionMin=80)
   - Valida: retorna apenas empresas com >= 80% completude
   - Executa: exporta CSV (GET /dashboard/export?format=csv)
   - Valida: CSV contém 10 linhas + cabeçalho
6. Todos testes usam TestContainers PostgreSQL
7. Todos testes criam contexto completo (usuário CONTADOR ou ADMIN, empresas, X-Company-Id header)
8. Cobertura de código do Epic 6 deve ser >= 75%

---

## Resumo do Epic

Ao final deste épico, o sistema terá:

- Serviço de cálculo de completude com 8 etapas rastreadas
- Endpoint de completude por empresa com detalhamento de etapas
- Dashboard com lista de empresas e indicadores visuais (%, status cálculos, ECF gerado)
- Sistema de alertas inteligentes (críticos, warnings, info)
- Status de cálculos tributários com detecção de desatualização
- Indicador de prazo de entrega ECF com classificação de urgência
- Resumo executivo multi-empresa para ADMIN
- Filtros avançados e busca textual no dashboard
- Exportação de relatório CSV
- Histórico de atividades (audit log) por empresa
- Testes E2E cobrindo fluxos completos de dashboard, alertas e filtros
- Isolamento multi-tenant via X-Company-Id
- Visibilidade completa de progresso para contadores gerenciando múltiplas empresas

**Dependências de Epics Anteriores:**
- Epic 1: Autenticação JWT, usuários ADMIN/CONTADOR
- Epic 2: Entidades Company e CompanyParameter
- Epic 3: ChartOfAccount, AccountingData
- Epic 4: Movimentações Lalur/Lacs, TaxCalculationResult
- Epic 5: EcfFile, geração ECF

**Épico Final (Epic 7):**
- Preenchimento opcional ECF Parte A (já implementado)
- Sistema completo pronto para produção

**Observações de UX:**
- Dashboard prioriza empresas menos completas (ordenação ASC por completionPercentage)
- Alertas críticos impedem geração ECF, warnings são informativos
- Cálculos desatualizados são claramente sinalizados
- Prazo de entrega sempre visível com indicador de urgência
- Histórico de atividades fornece auditabilidade e rastreamento
