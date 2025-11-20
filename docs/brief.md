# Project Brief: Sistema de Preenchimento de Arquivos M do ECF

## Executive Summary

**Sistema de Preenchimento de Arquivos M do ECF** é uma aplicação web contábil que automatiza o processo de geração e integração de arquivos M (Parte B - Lucro Real) para a Escrituração Contábil Fiscal (ECF), permitindo que contadores e empresas gerenciem todo o fluxo desde a importação de dados contábeis até a exportação do arquivo ECF completo, incluindo cálculos automáticos de IRPJ e CSLL.

**Problema Principal:** Atualmente, o preenchimento manual dos arquivos M do ECF é um processo complexo, propenso a erros e extremamente demorado para profissionais contábeis, especialmente no regime de Lucro Real, resultando em retrabalho, multas fiscais e alto custo operacional.

**Mercado-Alvo:** Escritórios de contabilidade de pequeno e médio porte, contadores autônomos, e departamentos contábeis de empresas tributadas pelo Lucro Real no Brasil.

**Proposta de Valor:** Redução de até 80% do tempo gasto no preenchimento da ECF Parte B, eliminação de erros de cálculo tributário, e integração completa entre dados contábeis e obrigações fiscais através de uma plataforma web intuitiva e segura.

## Problem Statement

### Estado Atual e Pontos de Dor

A Escrituração Contábil Fiscal (ECF) é uma obrigação acessória federal que substituiu a DIPJ em 2014, exigindo que empresas tributadas pelo Lucro Real ou Presumido entreguem anualmente informações contábeis e fiscais à Receita Federal. A **Parte B (Livro M)** contém os registros de apuração do IRPJ e CSLL, demandando:

1. **Processo Manual Complexo**: Contadores precisam transferir dados do SPED Contábil para a ECF manualmente, aplicar adições, exclusões e compensações conforme legislação tributária vigente
2. **Cálculos Tributários Sofisticados**: Apuração de IRPJ e CSLL envolve múltiplas alíquotas, regimes especiais, incentivos fiscais e ajustes do Lalur/Lacs
3. **Alta Propensão a Erros**: Estimativas indicam que 30-40% das ECFs enviadas contêm inconsistências que geram malha fiscal ou necessidade de retificação
4. **Consumo Intensivo de Tempo**: Escritórios relatam gastar entre 8-16 horas por empresa cliente no preenchimento da Parte B, dependendo da complexidade
5. **Atualizações Regulatórias Frequentes**: A Receita Federal publica layouts e regras de validação atualizadas anualmente, exigindo adaptação constante

### Impacto do Problema

**Quantificado:**
- Escritório médio com 50 clientes no Lucro Real: **400-800 horas/ano** apenas em preenchimento de ECF
- Custo de retificações e multas por erros: **R$ 500 a R$ 5.000** por empresa
- Taxa de retrabalho estimada: **25-35%** das entregas requerem correção

**Qualitativo:**
- Estresse e pressão nos períodos de entrega (prazos anuais)
- Risco reputacional para escritórios contábeis
- Baixa escalabilidade dos serviços contábeis
- Desmotivação de profissionais por trabalho repetitivo

### Por Que Soluções Existentes Falham

**Sistemas contábeis tradicionais** (ERP contábil):
- Focam em escrituração contábil, não em obrigações fiscais específicas
- Exportam dados, mas não geram o arquivo M completo
- Interface complexa para navegação tributária

**Planilhas customizadas**:
- Não escalam, quebradiças, difíceis de manter
- Sem validação automática de regras da Receita
- Alto risco de erro humano em fórmulas

**PVA da Receita Federal**:
- Interface desktop ultrapassada
- Preenchimento item por item sem automação
- Não integra com sistemas contábeis

### Urgência e Importância

- **Prazo anual obrigatório**: Não entrega = multa automática (R$ 500/mês-calendário)
- **Malha fina corporativa crescente**: Receita Federal intensificou fiscalização digital
- **Transformação digital contábil**: Profissionais buscam ferramentas que agreguem valor estratégico vs. trabalho operacional
- **Competitividade do setor**: Escritórios que automatizam ganham margem e capacidade de crescimento

## Proposed Solution

### Conceito Central e Abordagem

O **Sistema de Preenchimento de Arquivos M do ECF** é uma plataforma web SaaS completa que permite o preenchimento integral da ECF (Partes A e B) dentro do próprio sistema, automatizando cálculos tributários e gerando o arquivo final pronto para a Receita Federal:

1. **Gestão Completa de Dados Contábeis**: Cadastro de plano de contas, parâmetros tributários e lançamentos contábeis dentro do sistema
2. **Preenchimento de Parte A (ECF)**: Interface para preenchimento dos dados da Escrituração Contábil Fiscal
3. **Motor de Cálculo Tributário**: Processa dados contábeis e parâmetros fiscais para calcular IRPJ e CSLL, gerando adições, exclusões e compensações do Lalur/Lacs
4. **Geração Automática de Parte B (Arquivo M)**: Cria todos os registros do Livro M conforme layout oficial da Receita Federal
5. **Exportação ECF Completa**: Gera arquivo ECF final (Partes A + B) validado e pronto para transmissão ao SPED

### Diferenciais-Chave vs. Soluções Existentes

| Aspecto | Solução Proposta | ERPs Contábeis | Planilhas | PVA Receita |
|---------|-----------------|----------------|-----------|-------------|
| **Automação completa** | ✅ End-to-end | ⚠️ Parcial | ❌ Manual | ❌ Manual |
| **Cálculo IRPJ/CSLL** | ✅ Automático | ⚠️ Limitado | ⚠️ Com erros | ❌ Manual |
| **Validação RFB** | ✅ Pré-integrada | ❌ Não | ❌ Não | ✅ Só no envio |
| **Interface moderna** | ✅ Web/Mobile | ⚠️ Desktop | ⚠️ Excel | ❌ Desktop antigo |
| **Multi-empresa** | ✅ Gestão centralizada | ⚠️ Varia | ❌ Não | ❌ Uma por vez |
| **Atualização layouts** | ✅ Automática | ⚠️ Depende vendor | ❌ Manual | ✅ Automática |
| **Integridade temporal** | ✅ Período contábil protegido | ❌ Não | ❌ Não | ❌ Não |

### Por Que Esta Solução Terá Sucesso

**1. Especialização vs. Generalização**
- Enquanto ERPs tentam fazer tudo (contabilidade, folha, fiscal), focamos exclusivamente no problema mais doloroso: ECF Parte B
- Expertise profunda em um nicho vs. funcionalidade superficial em amplitude

**2. Integração Não-Invasiva**
- Não substituímos o sistema contábil do cliente
- Nos conectamos aos arquivos que eles já produzem (ECF, plano de contas)
- Baixa fricção de adoção: complementa workflow existente

**3. Time-to-Value Imediato**
- Primeira empresa processada em < 30 minutos após cadastro
- ROI visível na primeira temporada de ECF
- Curva de aprendizado mínima (interface intuitiva)

**4. Inteligência Tributária Embutida**
- Regras de negócio codificadas por especialistas tributários
- Atualizações regulatórias automatizadas
- Reduz dependência de conhecimento especializado individual

**5. Governança de Dados com Período Contábil**
- Controle temporal protege dados históricos contra alterações acidentais
- Auditoria clara de quando dados foram fechados vs. em edição
- Conformidade com boas práticas contábeis (imutabilidade de períodos encerrados)

### Visão de Alto Nível do Produto

**Fluxo principal do usuário:**

```
[Login] → [Primeiro Acesso ou Redefinição ADMIN: Trocar Senha Obrigatória]
     ↓
     ├─ ADMIN: [Dashboard Geral] → [Acesso a Todas Funcionalidades sem Empresa]
     │            ↓
     │         [Gestão de Usuários] | [Gestão de Parâmetros Tributários Globais]
     │
     └─ CONTADOR: [Seletor de Empresa] → [Escolher Empresa para Trabalhar]
                      ↓
                   [Dashboard da Empresa] → [Verificar Período Contábil Ativo]
                      ↓
                   [Cadastrar/Importar Plano de Contas (plano, sem hierarquia)]
                      ↓
                   [Importar/Cadastrar Dados Contábeis (lançamentos ou balancete)]
                      ↓
                   [Preencher Parte A da ECF (J100, J150, etc.)]
                      ↓
                   [Cadastrar Movimentações Lalur/Lacs (adições, exclusões, compensações)]
                      ↓
                   [Executar Motor de Cálculo IRPJ/CSLL] → [Revisar Memória de Cálculo]
                      ↓
                   [Gerar Arquivo M (Parte B)]
                      ↓
                   [Exportar ECF Completo (Partes A + B)]
                      │
                      └─ [Opcional: Importar ECF Externo e Combinar com Parte B gerada]

     [Trocar de Empresa] → [Voltar ao Seletor de Empresa] (disponível a qualquer momento)
```

**Módulos principais:**
- **Gestão de Usuários (ADMIN)**: Criação de usuários, definição de permissões (Admin/Contador), redefinição de senhas, auditoria de acessos
- **Autenticação Segura**: Login sem auto-cadastro, troca de senha obrigatória (primeiro acesso e redefinição ADMIN), controle de sessão
- **Gestão de Empresas**: Cadastro de clientes tributados no Lucro Real com **Período Contábil** e vinculação a parâmetros tributários
- **Plano de Contas (Parte A)**: Cadastro/importação de plano de contas contábil (estrutura plana), classificação tributária
- **Parâmetros Tributários Globais (ADMIN)**: Tabela global hierárquica de alíquotas, regimes, incentivos fiscais
- **Dados Contábeis e Auxiliares**:
  - Importação/exportação de dados contábeis via CSV/TXT
  - Cadastro de Códigos de Enquadramento LALUR
  - Cadastro de Linhas de Lucro Presumido (código + descrição + contas)
- **Preenchimento Parte A (ECF)**: Interface para registros J100 (Balanço), J150 (DRE), etc.
- **Movimentações**:
  - Lançamentos para Conta da Parte B
  - Lançamentos Contábeis Manuais
  - Adições, Exclusões e Compensações Lalur/Lacs
- **Motor de Cálculo (Sob Demanda)**: Cálculo via botões de IRPJ e CSLL com memória de cálculo detalhada
- **Geração de Arquivo M (Parte B)**: Exportação de registros M300, M350, M400 conforme layout RFB
- **Importação e Combinação ECF**: Importação de arquivo ECF existente (opcional) e merge com Parte B gerada
- **Dashboard & Relatórios**: Visibilidade do processo, status por empresa, seletor de empresa (CONTADOR)

### Regras de Negócio Críticas

#### 1. Período Contábil

Cada empresa possui um campo **Período Contábil** que define a data de corte temporal:

- **Comportamento**: Apenas dados com competência igual ou posterior ao Período Contábil podem ser cadastrados/editados
- **Proteção de Histórico**: Dados anteriores ao período ficam em modo somente leitura, garantindo integridade do histórico contábil e fiscal
- **Flexibilidade Controlada**: O Período Contábil pode ser alterado (ex: para reabrir período fechado em casos excepcionais), mas com auditoria de quem e quando modificou
- **Benefícios**:
  - Previne alterações acidentais em períodos já encerrados/entregues
  - Facilita gestão de múltiplos exercícios fiscais simultâneos
  - Alinha com práticas contábeis de "fechamento de período"
  - Simplifica lógica de cálculos (apenas períodos "abertos" precisam recalcular)

**Exemplo de uso:**
- Empresa X tem Período Contábil = 01/01/2024
- Contador pode cadastrar/editar lançamentos de jan/2024 em diante
- Lançamentos de 2023 ficam visíveis mas bloqueados para edição
- Se precisar corrigir dado de 2023, deve alterar Período Contábil (ação auditada)

#### 2. Soft Delete (Exclusão Lógica)

**Política Global**: Nenhum dado pode ser deletado fisicamente do banco de dados. Todos os registros possuem campo **status** (ativo/inativo).

**Aplicação**:
- **Usuários**: Usuários desabilitados (status inativo)
- **Empresas**: Marcadas como inativas quando "deletadas"
- **Plano de Contas (Parte A)**: Contas contábeis marcadas como inativas
- **Parâmetros Tributários**: Parâmetros marcados como inativos
- **Dados Contábeis**: Dados importados marcados como inativos
- **Códigos de Enquadramento LALUR**: Marcados como inativos
- **Linhas de Lucro Presumido**: Marcadas como inativas
- **Lançamentos para Conta da Parte B**: Marcados como inativos
- **Lançamentos Contábeis Manuais**: Marcados como inativos
- **Adições/Exclusões/Compensações Lalur/Lacs**: Marcadas como inativas

**Benefícios**:
- Rastreabilidade completa de histórico
- Integridade referencial preservada (evita cascata de erros)
- Possibilidade de auditoria retroativa
- Recuperação de dados "deletados" acidentalmente
- Conformidade com boas práticas contábeis (imutabilidade de histórico)

**Implicações de UX**:
- Filtros em todas listagens para mostrar apenas ativos ou incluir inativos
- Indicação visual clara de registros inativos (ex: texto acinzentado, badge "Inativo")
- Permissão para reativar registros inativos (quando apropriado)

#### 3. Contexto de Empresa (para role CONTADOR)

**Comportamento**:
- Após login, contador **deve selecionar uma empresa** para iniciar trabalho
- Sistema exibe seletor de empresas (dropdown ou modal)
- Todas operações subsequentes são executadas no contexto da empresa selecionada
- Contador pode **trocar de empresa** a qualquer momento via menu/dropdown
- Ao trocar, sistema carrega dados da nova empresa selecionada
- ADMIN pode navegar livremente sem estar vinculado a empresa específica

**Benefícios**:
- Isolamento de dados entre empresas (segurança)
- Clareza sobre qual empresa está sendo trabalhada (evita erros)
- Workflow natural para escritórios que gerenciam múltiplos clientes

## Target Users

### Primary User Segment: Contador de Escritório Contábil

**Perfil Demográfico/Profissional:**
- **Idade**: 28-50 anos
- **Formação**: Graduação em Ciências Contábeis, registro CRC ativo
- **Experiência**: 3-15 anos em escritório contábil
- **Localização**: Principalmente capitais e cidades médias do Brasil
- **Estrutura**: Trabalha em escritório com 2-20 profissionais contábeis
- **Carteira**: Atende entre 10-50 empresas tributadas no Lucro Real

**Comportamentos e Workflow Atual:**
- Utiliza ERP contábil tradicional (ex: Domínio, Alter, Questor, Sage) para escrituração
- Gera SPED Contábil mensalmente
- Concentra trabalho de ECF em período pré-prazo (maio-junho para ano-calendário anterior)
- Trabalha com planilhas Excel paralelas para cálculos tributários
- Preenche PVA da Receita manualmente item por item
- Revisa múltiplas vezes por medo de erro
- Frequentemente trabalha horas extras em período de entrega

**Necessidades e Pontos de Dor Específicos:**
1. **Redução de Tempo Operacional**: "Preciso processar 40 empresas em 2 meses, não tenho tempo para preencher tudo manualmente"
2. **Confiança nos Cálculos**: "Tenho medo de errar IRPJ/CSLL e meu cliente receber multa por causa disso"
3. **Facilidade de Revisão**: "Preciso mostrar para o sócio/supervisor antes de enviar, mas o PVA é confuso"
4. **Escalabilidade**: "Queremos crescer a carteira, mas não consigo atender mais clientes com esse processo"
5. **Atualização Regulatória**: "Todo ano muda alguma coisa e preciso reaprender"

**Objetivos que Busca Alcançar:**
- Entregar todas as ECFs dentro do prazo sem estresse
- Zerar retrabalho de retificações
- Conquistar confiança para atender empresas maiores/mais complexas
- Ter tempo para atividades consultivas (maior valor agregado)
- Crescer profissionalmente sem aumentar horas trabalhadas

**Citações Representativas:**
> "Eu sei fazer contabilidade, mas a parte fiscal é um mundo à parte. Preciso de uma ferramenta que me ajude a não errar."

> "Se eu conseguir reduzir pela metade o tempo que gasto com ECF, posso atender o dobro de clientes ou ter mais qualidade de vida."

---

### Secondary User Segment: Controller/Analista Contábil Corporativo

**Perfil Demográfico/Profissional:**
- **Idade**: 25-45 anos
- **Formação**: Graduação em Contabilidade/Administração, frequentemente pós-graduação
- **Experiência**: 2-10 anos em controladoria
- **Localização**: Principalmente em empresas de médio porte (faturamento R$ 50M-500M)
- **Estrutura**: Trabalha em departamento contábil/fiscal com 3-15 pessoas
- **Responsabilidade**: Uma ou poucas empresas (grupo econômico)

**Comportamentos e Workflow Atual:**
- Usa ERP corporativo (SAP, TOTVS Protheus, Senior) para contabilidade
- Gera relatórios gerenciais além das obrigações fiscais
- Terceiriza ou tem consultoria tributária interna especializada
- Precisa de rastreabilidade e auditoria detalhada de todos os processos
- Interage frequentemente com auditoria externa e fiscal

**Necessidades e Pontos de Dor Específicos:**
1. **Auditabilidade**: "Preciso documentar cada ajuste que fizemos no Lalur para mostrar para auditoria"
2. **Governança**: "Não posso ter risco de alguém alterar dados de período fechado sem rastreio"
3. **Integração com ERP**: "Nosso ERP exporta dados, mas não tem inteligência fiscal - precisamos de ponte"
4. **Complexidade Tributária**: "Temos incentivos fiscais, múltiplas atividades, situações especiais - planilha não dá conta"
5. **Pressão por Conformidade**: "Um erro na ECF gera auditoria da Receita, exposição reputacional e pode custar milhões"

**Objetivos que Busca Alcançar:**
- Garantir 100% de conformidade fiscal (zero risco)
- Automatizar processos repetitivos para focar em análises estratégicas
- Ter visibilidade gerencial de carga tributária e oportunidades de planejamento
- Reduzir dependência de consultorias externas caras
- Profissionalizar processos fiscais da empresa

**Citações Representativas:**
> "Nosso auditor sempre questiona a memória de cálculo do IRPJ. Preciso de um sistema que documente tudo automaticamente."

> "Não posso depender de uma planilha que só uma pessoa sabe mexer. Preciso de processo estruturado."

## Goals & Success Metrics

### Business Objectives

- **Adoção no Primeiro Ano**: Conquistar 100 escritórios contábeis ativos (média de 30 empresas/escritório = 3.000 ECFs processadas)
- **Receita Recorrente**: Atingir R$ 250.000 MRR (Monthly Recurring Revenue) em 12 meses com modelo SaaS
- **Redução de Tempo Comprovada**: Demonstrar em média 70% de redução no tempo de preenchimento vs. processo manual (baseline: 8-16h → target: 2-5h por empresa)
- **Taxa de Retenção**: Manter 85%+ de retenção de clientes após primeira temporada de ECF (prova de valor)
- **NPS (Net Promoter Score)**: Alcançar NPS > 50 (indicador de recomendação e satisfação)

### User Success Metrics

- **Time-to-First-Value**: Usuário completa primeira ECF completa em < 2 horas após onboarding
- **Taxa de Conclusão**: 95%+ das empresas cadastradas completam processo até exportação final
- **Precisão de Cálculos**: 99.5%+ de conformidade com validador da Receita Federal (zero rejeições por erro de cálculo)
- **Adoção de Features**: 80%+ dos usuários utilizam importação automática (vs. entrada manual)
- **Redução de Suporte**: Após 3 meses, < 1 ticket de suporte por usuário/mês (indicador de usabilidade)

### Key Performance Indicators (KPIs)

- **Activation Rate**: % de usuários que processam pelo menos 1 empresa até a exportação final em 30 dias - **Target: 70%**
- **Companies per Account**: Número médio de empresas processadas por conta ativa - **Target: 25 empresas/conta**
- **Processing Time**: Tempo médio do upload de ECF até exportação final - **Target: < 3 horas**
- **Error Rate**: % de arquivos M gerados que falham na validação RFB - **Target: < 0.5%**
- **Feature Adoption - Motor de Cálculo**: % de usuários que usam cálculo automático vs. manual override - **Target: 90%**
- **Revenue per Account (ARPA)**: Receita média mensal por conta ativa - **Target: R$ 2.500/mês**
- **Customer Acquisition Cost (CAC)**: Custo de aquisição por cliente - **Target: < R$ 3.000**
- **CAC Payback Period**: Tempo para recuperar investimento em aquisição - **Target: < 4 meses**
- **Churn Rate**: Taxa de cancelamento mensal - **Target: < 5% ao mês**

## MVP Scope

### Core Features (Must Have)

**1. Autenticação e Gestão de Usuários (Centralizada por ADMIN)**
- **Funcionalidades**:
  - Sistema de login seguro (email/senha) sem possibilidade de auto-cadastro
  - Apenas usuários ADMIN podem criar novos usuários no sistema
  - Apenas **2 roles** no sistema: **ADMIN** e **CONTADOR**
  - ADMIN pode redefinir senhas de usuários (sem auto-recuperação pelo usuário)
  - **Troca de senha obrigatória** em dois cenários:
    - No primeiro acesso ao sistema
    - Sempre que ADMIN redefinir a senha do usuário
  - Controle de sessão e timeout automático
  - Log de auditoria de acessos
- **Diferenças entre Roles**:
  - **ADMIN**:
    - Cria e gerencia usuários
    - Cria e gerencia parâmetros tributários globais (tb_parametros_tributarios)
    - Pode usar o sistema **sem estar associado a uma empresa específica**
    - Acesso total a todas funcionalidades
  - **CONTADOR**:
    - Após login, **deve selecionar uma empresa** para trabalhar
    - Pode **trocar de empresa** durante o uso do sistema sem fazer logout
    - Não pode criar usuários nem parâmetros tributários globais
- **Rationale**: Modelo de segurança centralizado com separação clara de responsabilidades. ADMIN gerencia infraestrutura e configurações globais; CONTADOR foca no trabalho operacional por empresa.

**2. Gestão de Empresas com Período Contábil**
- **Funcionalidades**:
  - CRUD completo de empresas clientes
  - Campos obrigatórios: CNPJ, razão social, regime tributário (Lucro Real)
  - **Durante a criação da empresa**: seleção dos **parâmetros tributários aplicáveis** (vindos de tb_parametros_tributarios criados pelo ADMIN)
  - Campo **Período Contábil** (data de corte temporal)
  - Lógica de bloqueio: dados anteriores ao Período Contábil ficam somente leitura
  - Alteração do Período Contábil com log de auditoria (quem, quando, motivo)
  - Visualização de status da empresa (pendente, em processamento, concluída)
  - **Soft Delete**: Empresas não podem ser deletadas fisicamente - apenas marcadas como inativas via campo de status (ativo/inativo)
  - **Seleção de Empresa (Contador)**: Após login, contador seleciona empresa para trabalhar e pode trocar durante a sessão
- **Rationale**: Base organizacional do sistema multi-empresa. Período Contábil é regra de negócio crítica para integridade e governança de dados históricos. Soft delete garante rastreabilidade completa e evita perda acidental de dados.

**3. Plano de Contas**
- **Funcionalidades**:
  - Cadastro manual de contas contábeis (**estrutura plana, não hierárquica**)
  - Importação de plano de contas via CSV/TXT
  - Campos: código da conta, nome, classificação contábil, exercício (ano)
  - Classificação tributária: receita, custo, despesa, ativo, passivo, patrimônio líquido
  - **Regra de Unicidade**: Uma empresa possui N Contas Contábeis, sendo que:
    - Só pode existir **uma conta contábil por empresa por ano**
    - Constraint: código da conta + empresa + ano = único
    - O código da conta se mantém consistente ano após ano (ex: conta "1.1.01.001 - Caixa" tem mesmo código em 2023 e 2024)
  - Visualização em lista/tabela com filtros por ano
  - Edição de contas existentes
  - **Soft Delete**: Contas não podem ser deletadas fisicamente - apenas marcadas como inativas via campo de status (ativo/inativo)
  - Filtros para exibir apenas contas ativas ou incluir inativas
- **Rationale**: Fundação para toda a contabilidade tributária. Cada empresa tem plano de contas próprio por exercício fiscal. Estrutura plana simplifica cadastro e manutenção. Regra de unicidade previne duplicação acidental. Soft delete preserva histórico e integridade referencial.

**4. Parâmetros Tributários (Gestão Global por ADMIN)**
- **Funcionalidades**:
  - **Tabela Global (tb_parametros_tributarios)**:
    - Apenas ADMIN pode criar/editar parâmetros tributários
    - Estrutura **hierárquica/ramificada**: filho → pai → avô → bisavô, etc.
    - Permite herança e especialização de parâmetros
    - Exemplos de hierarquia:
      - "IRPJ Padrão" (pai) → "IRPJ com Adicional" (filho) → "IRPJ Adicional com Incentivo X" (neto)
  - **Conteúdo dos Parâmetros**:
    - Alíquotas de IRPJ (15% + adicional 10% sobre lucro > R$ 20k/mês)
    - Alíquota de CSLL (9% padrão para Lucro Real)
    - Regime de apuração: Anual ou Trimestral
    - Limites e faixas de tributação
    - Incentivos fiscais aplicáveis
  - **Aplicação às Empresas**:
    - Durante a criação de uma empresa, ADMIN ou CONTADOR seleciona quais parâmetros da tabela global se aplicam àquela empresa
    - Empresa fica vinculada aos parâmetros selecionados
    - Alteração de parâmetros globais reflete automaticamente em todas empresas vinculadas (ou requer opt-in, a definir)
  - **Soft Delete**: Parâmetros não podem ser deletados fisicamente - apenas marcados como inativos
- **Rationale**: Centralização de parâmetros tributários evita duplicação e inconsistência. ADMIN mantém uma base de conhecimento tributário que é aplicada a todas empresas. Estrutura hierárquica permite modelar complexidade tributária de forma escalável.

**5. Dados Contábeis e Estruturas Auxiliares**

**5.1. Dados Contábeis (Importação/Exportação)**
- **Funcionalidades**:
  - **Importação de Dados Contábeis via CSV/TXT** (única forma de criar dados contábeis - sem cadastro manual)
  - **Exportação de Dados Contábeis** para CSV/TXT
  - Relacionamento: Uma Conta Contábil possui N Dados Contábeis
  - Visualização de dados contábeis por conta e período
  - Filtros por período, conta
  - Respeito ao Período Contábil (dados de períodos anteriores ficam somente leitura)
  - Geração de balancete e razão contábil a partir dos dados importados
  - **Soft Delete**: Dados contábeis marcados como inativos
  - Filtros para exibir apenas dados ativos ou incluir inativos
- **Rationale**: Dados contábeis são importados de sistemas externos (ERP contábil). Sistema processa e organiza para uso tributário.

**5.2. Código de Enquadramento LALUR**
- **Funcionalidades**:
  - Campos obrigatórios:
    - **Código de Enquadramento** (tipo texto)
    - **Histórico** (descrição, tipo texto)
  - **CRUD Completo**:
    - Listagem de códigos de enquadramento
    - Criação de novo código
    - Edição de código existente
    - Visualização de detalhes
  - **Soft Delete**: Ativação/Inativação via campo status
  - Filtros para exibir apenas ativos ou incluir inativos
- **Rationale**: Códigos de enquadramento estruturam e classificam lançamentos do Lalur conforme exigências da Receita Federal.

**5.3. Linhas para Cadastro de Lucro Presumido**
- **Funcionalidades**:
  - Campos obrigatórios:
    - **Código** (tipo numeral)
    - **Descrição** (tipo texto)
    - **Conteúdo** (lista de Contas Contábeis - seleção múltipla)
  - **CRUD Completo**:
    - Listagem de linhas de lucro presumido
    - Criação de nova linha
    - Edição de linha existente (incluindo modificação da lista de contas)
    - Visualização de detalhes
  - **Soft Delete**: Ativação/Inativação via campo status
  - Filtros para exibir apenas ativas ou incluir inativas
- **Rationale**: Prepara estrutura para suporte futuro a Lucro Presumido, permitindo configurar agrupamentos de contas contábeis para cálculos específicos desse regime.

**6. Preenchimento de Parte A (ECF)**
- **Funcionalidades**:
  - Interface para preenchimento dos principais registros da Parte A:
    - Registro 0000: Identificação da pessoa jurídica
    - Registro J100: Balanço Patrimonial
    - Registro J150: Demonstração do Resultado do Exercício (DRE)
    - Registro J800: Outras informações
  - Preenchimento assistido com campos obrigatórios destacados
  - Validação básica de campos (formato, obrigatoriedade)
  - Carregamento automático de saldos dos dados contábeis para J100/J150
  - Salvamento incremental (rascunho)
- **Rationale**: Parte A é pré-requisito para a ECF completa. Sistema completo precisa permitir preenchimento, não apenas importação.

**7. Movimentações (Lalur/Lacs e Lançamentos)**

**7.1. Lançamento para Conta da Parte B**
- **Funcionalidades**:
  - Cadastro de lançamentos específicos para contas do Livro M (Parte B)
  - Interface para registrar movimentações nas contas da Parte B
  - **CRUD Completo**:
    - Listagem de lançamentos da Parte B
    - Criação de novo lançamento
    - Edição de lançamento existente
    - Visualização de detalhes
  - **Soft Delete**: Ativação/Inativação via campo status
  - Filtros para exibir apenas lançamentos ativos ou incluir inativos
  - Respeito ao Período Contábil
- **Rationale**: Permite registrar movimentações diretamente nas contas estruturais do Lalur/Lacs (Parte B).

**7.2. Lançamento Contábil (Manual)**
- **Funcionalidades**:
  - Cadastro manual de lançamentos contábeis (complementar à importação)
  - Campos típicos: data, conta débito, conta crédito, valor, histórico
  - **CRUD Completo**:
    - Listagem de lançamentos contábeis manuais
    - Criação de novo lançamento
    - Edição de lançamento existente
    - Visualização de detalhes
  - Validação de partidas dobradas (débito = crédito)
  - **Soft Delete**: Ativação/Inativação via campo status
  - Filtros para exibir apenas lançamentos ativos ou incluir inativos
  - Respeito ao Período Contábil
- **Rationale**: Permite ajustes e complementos manuais aos dados contábeis importados, para correções ou lançamentos específicos.

**7.3. Adições, Exclusões e Compensações (Lalur/Lacs)**
- **Finalidade**: Transformar **Lucro Contábil** em **Lucro Fiscal** (base de cálculo de IRPJ/CSLL) através de ajustes exigidos pela legislação tributária
- **Funcionalidades**:
  - Interface para cadastro de ajustes ao lucro líquido:
    - **Adições**: Valores que aumentam a base de cálculo
      - Exemplos: despesas não dedutíveis (multas, brindes excedentes), provisões não aceitas, excesso de depreciação
    - **Exclusões**: Valores que diminuem a base de cálculo
      - Exemplos: receitas não tributáveis (dividendos), incentivos fiscais (PAT, ROTA), reversões de provisões
    - **Compensações**: Redução da base por prejuízos acumulados
      - Exemplos: prejuízos fiscais de exercícios anteriores (limite 30%), base negativa CSLL
  - Campos do ajuste:
    - Tipo (adição/exclusão/compensação)
    - Valor
    - Descrição/histórico
    - Natureza (permanente vs. temporário)
    - Vinculação a conta contábil específica (opcional)
    - Vinculação a código de enquadramento LALUR
    - Justificativa/fundamento legal
  - **Fórmula aplicada pelo motor de cálculo**:
    ```
    Lucro Real = Lucro Contábil + Adições - Exclusões - Compensações
    ```
  - **CRUD Completo**:
    - Listagem de adições/exclusões/compensações
    - Criação de novo ajuste
    - Edição de ajuste existente
    - Visualização de detalhes
  - **Soft Delete**: Ativação/Inativação via campo status
  - Filtros para exibir apenas ajustes ativos ou incluir inativos
- **Rationale**: Coração do Lalur/Lacs - ajustes obrigatórios que transformam lucro contábil em lucro fiscal conforme legislação. Sem esses ajustes, não é possível calcular IRPJ/CSLL corretamente. MVP permite cadastro manual (automação de sugestões fica para fase 2). Soft delete preserva memória de cálculo e auditoria.

**8. Motor de Cálculo de IRPJ e CSLL (Sob Demanda)**
- **Funcionalidades**:
  - **Cálculo sob demanda** (não automático - via botões/triggers):
    - **Botão "Calcular IRPJ"**: Executa cálculo de IRPJ
    - **Botão "Calcular CSLL"**: Executa cálculo de CSLL
    - **Botão "Recalcular Tudo"**: Reprocessa todos os cálculos
  - Cálculo a partir de:
    - Lucro Líquido (extraído da DRE ou dados contábeis)
    - Adições cadastradas (ativas)
    - Exclusões cadastradas (ativas)
    - Compensações aplicadas (ativas)
  - Fórmulas aplicadas:
    - Base de Cálculo = Lucro Líquido + Adições - Exclusões - Compensações
    - IRPJ: 15% + adicional 10% sobre base que exceder R$ 20k/mês
    - CSLL: 9% sobre a base
  - Geração de memória de cálculo detalhada (passo a passo)
  - Exibição de valores parciais e finais
  - Indicação visual se cálculo está desatualizado (dados foram alterados após último cálculo)
- **Rationale**: Cálculo sob demanda dá controle ao usuário sobre quando processar, evita reprocessamentos desnecessários e permite revisão antes de calcular. Diferencial competitivo - elimina erros de cálculo manual.

**9. Geração de Arquivo M (Parte B)**
- **Funcionalidades**:
  - Exportação de registros M conforme layout oficial da Receita Federal:
    - M300: Lalur - Parte A (Lucro Real)
    - M350: Lalur - Parte B (Controle de valores)
    - M400: Lacs - Apuração da base de cálculo da CSLL
  - Validação de campos obrigatórios conforme layout
  - Geração de arquivo .txt formatado
  - Download do arquivo M gerado
  - Preview do arquivo antes do download
- **Rationale**: Output essencial - Parte B em formato oficial RFB. Sem isso, dados ficam apenas no sistema, não podem ser entregues.

**10. Importação e Combinação de ECF**
- **Funcionalidades**:
  - Upload de arquivo ECF existente (opcional - caso usuário tenha Parte A em outro sistema)
  - Parsing e validação da estrutura do arquivo ECF
  - Identificação automática da Parte A no arquivo importado
  - Merge inteligente: substituição/combinação da Parte B do arquivo importado com a Parte B gerada no sistema
  - Geração de arquivo ECF completo (Partes A + B integradas)
  - Exportação de arquivo ECF final (.txt) pronto para transmissão SPED
- **Rationale**: Flexibilidade para usuários que preenchem Parte A em outro sistema. Também permite aproveitar ECF parcial existente e completar apenas a Parte B no sistema.

**11. Dashboard e Navegação**
- **Funcionalidades**:
  - Dashboard inicial com visão geral:
    - Lista de empresas cadastradas
    - Status de cada empresa (pendente, em andamento, concluída)
    - Indicadores de completude (% de dados preenchidos)
    - Alertas de prazos e pendências
  - Navegação clara entre módulos
  - Breadcrumbs (trilha de navegação)
  - Menu lateral com acesso rápido às funcionalidades
  - Lista de arquivos gerados com opção de download
- **Rationale**: UX básica - usuário precisa orientação clara sobre onde está e o que precisa fazer. Dashboard dá visibilidade de progresso.

### Out of Scope for MVP

- **Plano de Contas Hierárquico** - MVP usa estrutura plana (lista de contas sem relacionamento pai/filho)
- **Cadastro manual de Dados Contábeis** - Apenas importação via CSV/TXT (não digitação lançamento por lançamento)
- **Integração via API com ERPs** - MVP usa importação de arquivos CSV/TXT
- **Cálculo automático/sugestão de adições e exclusões** - MVP exige cadastro manual pelo contador
- **Suporte a Lucro Presumido/Arbitrado** - MVP foca apenas Lucro Real
- **Delete físico de registros** - Todos registros usam soft delete (status ativo/inativo)
- **Incentivos fiscais complexos** - PAT, SUDENE, SUDAM, Lei do Bem, etc.
- **Regimes especiais** - Exportação, ZPE, Zona Franca, Drawback
- **Controle de prejuízos fiscais acumulados** - Apenas compensação manual no MVP
- **Planejamento tributário** - Simulações, cenários "what-if"
- **Auditoria completa** - Log detalhado de todas alterações (apenas Período Contábil no MVP)
- **Relatórios gerenciais avançados** - Dashboards customizáveis, gráficos, comparativos
- **Multi-idioma** - Apenas português brasileiro
- **App mobile nativo** - Apenas web responsivo
- **Múltiplos layouts ECF** - Apenas layout mais recente (2024/2025)
- **Importação automática de SPED Contábil** - Apenas ECF e CSV no MVP
- **Validação integrada com PVA** - Validação manual pelo usuário no PVA da Receita
- **Workflow de aprovação** - Fluxo multi-etapa com aprovadores (para empresas grandes)
- **Backup automático** - Apenas backup de banco de dados no servidor
- **Notificações por email/SMS** - Sistema não envia notificações ativas no MVP
- **Integração com certificado digital** - Transmissão ao SPED feita fora do sistema

### MVP Success Criteria

**O MVP será considerado bem-sucedido quando:**

1. **Completude Funcional**: Um contador consegue, sem suporte técnico:
   - Cadastrar uma empresa do zero
   - Criar plano de contas (manual ou importação)
   - Parametrizar tributação
   - Importar dados contábeis
   - Preencher Parte A da ECF
   - Cadastrar adições/exclusões Lalur/Lacs
   - Executar cálculo de IRPJ/CSLL
   - Gerar arquivo M (Parte B)
   - Exportar ECF completa pronta para transmissão
   - Tempo total: < 4 horas para empresa padrão

2. **Precisão Técnica**:
   - 100% dos arquivos ECF gerados passam nas validações estruturais do PVA da Receita
   - Zero erros de cálculo matemático (IRPJ/CSLL conferidos manualmente)
   - Arquivo M gerado conforme layout oficial (nenhum campo obrigatório faltando)

3. **Validação de Mercado**:
   - 10 beta users (escritórios contábeis) processam pelo menos 3 empresas cada
   - Reportam economia de tempo > 60% vs. método atual (planilha + PVA manual)
   - NPS > 40 entre beta users

4. **Qualidade Técnica**:
   - Cobertura de testes > 80% nas regras de cálculo tributário
   - Zero bugs críticos (que impedem conclusão do fluxo)
   - Performance: operações de cálculo < 3 segundos

5. **Segurança e Integridade**:
   - Autenticação funcional sem brechas
   - Período Contábil efetivamente bloqueia edições retroativas
   - Dados de diferentes empresas isolados (multi-tenancy)

## Post-MVP Vision

### Phase 2 Features (6-12 meses após MVP)

**1. Automação Inteligente de Adições/Exclusões**
- Motor de sugestões baseado em regras tributárias
- Análise automática do plano de contas para identificar ajustes comuns
- Machine learning para aprender padrões de ajustes por empresa
- Sugestões com explicação e fundamento legal
- Rationale: Reduz ainda mais tempo do contador e risco de esquecer ajustes obrigatórios

**2. Suporte a Lucro Presumido**
- Ativação das "Linhas de Lucro Presumido" (já estruturadas no MVP)
- Cálculo automático de base presumida por atividade
- Tabelas de presunção atualizadas automaticamente
- Comparativo Lucro Real vs. Presumido (planejamento tributário)
- Rationale: Expande mercado endereçável para empresas de menor porte

**3. Plano de Contas Hierárquico**
- Suporte a estrutura pai/filho de contas
- Visualização em árvore expansível
- Totalização automática de contas sintéticas
- Importação de estruturas hierárquicas de ERPs
- Rationale: Melhora UX para empresas com planos de contas complexos

**4. Integração via API com ERPs**
- APIs para sincronização bidirecional com ERPs populares (TOTVS, SAP, Omie)
- Importação automática periódica de dados contábeis
- Exportação de ajustes tributários de volta para o ERP
- Webhooks para notificações de mudanças
- Rationale: Elimina importação manual de CSV, reduz erros de digitação

**5. Relatórios Gerenciais e Dashboards Customizáveis**
- Análise de carga tributária efetiva
- Comparativo histórico de IRPJ/CSLL
- Gráficos de evolução de adições/exclusões
- Relatório de economia fiscal (incentivos utilizados)
- Exportação para Excel/PDF
- Rationale: Transforma dados em insights para planejamento tributário

**6. Incentivos Fiscais Complexos**
- Suporte detalhado para PAT, SUDENE, SUDAM, Lei do Bem
- Cálculo automático de limites e restrições
- Controle de documentação comprobatória
- Alertas de vencimento de benefícios
- Rationale: Atende empresas de médio/grande porte com tributação complexa

**7. Workflow de Aprovação Multi-Níveis**
- Fluxo: Contador prepara → Supervisor revisa → Sócio aprova
- Comentários e histórico de alterações por versão
- Notificações por email de pendências
- Assinatura digital de aprovação
- Rationale: Atende demanda de escritórios grandes e empresas corporativas

**8. Auditoria Completa e Rastreabilidade**
- Log detalhado de todas alterações (quem, quando, o que, valor anterior/novo)
- Filtros avançados de auditoria
- Relatório de trilha de auditoria para fiscalização
- Exportação de logs para compliance
- Rationale: Requisito para empresas auditadas e compliance rigoroso

### Long-term Vision (12-24 meses)

**Plataforma Completa de Compliance Fiscal**
- Extensão para outras obrigações acessórias (DCTF, EFD-Contribuições, DIRF)
- Calendário fiscal integrado com alertas automáticos
- Inteligência artificial para revisão de inconsistências
- Marketplace de consultores especializados
- Módulo de planejamento tributário simulado

**Diferenciais Estratégicos:**
- **De ferramenta operacional → Plataforma estratégica**: Não apenas preenche ECF, mas oferece insights para redução de carga tributária
- **De desktop → Cloud-first com mobile**: App nativo para revisão e aprovação em trânsito
- **De single-purpose → Ecossistema fiscal**: Hub central para todas obrigações federais

### Expansion Opportunities

**1. Segmentos Adjacentes**
- Pequenas empresas do Simples Nacional (ECF não obrigatória, mas desejável)
- Empresas do Lucro Arbitrado (nicho pequeno, baixa concorrência)
- Organizações sem fins lucrativos (IRPJ/CSLL com particularidades)

**2. Integrações Estratégicas**
- Parcerias com vendedores de ERP (integração nativa)
- Integração com certificação digital (assinatura e transmissão direto no sistema)
- API pública para desenvolvedores externos

**3. Modelo de Receita Expandido**
- SaaS padrão (mensalidade por empresa)
- Add-ons premium (automação IA, consultoria, relatórios avançados)
- White-label para grandes escritórios (customização de marca)
- Marketplace de serviços (consultores certificados, treinamentos)

**4. Internacionalização (Longo Prazo)**
- Adaptação para países com sistemas similares de Lalur (América Latina)
- Parcerias com networks contábeis internacionais

## Technical Considerations

### Platform Requirements

**Escopo do Projeto:**
- **Backend API Only**: O projeto consiste **apenas no desenvolvimento do backend** (API REST)
- **Frontend**: Não será desenvolvido neste projeto (será consumido por cliente frontend externo)
- **Entregáveis**: API REST documentada, endpoints funcionais, lógica de negócio completa

**Performance Requirements:**
- Operações de cálculo (IRPJ/CSLL): < 5 segundos para empresa típica
- Importação de CSV (até 10.000 linhas): < 30 segundos
- Geração de arquivo ECF completo: < 10 segundos
- Suporte simultâneo: 100 requisições concorrentes (meta ano 1)
- Tempo de resposta médio da API: < 500ms (endpoints de leitura)
- Tempo de resposta aceitável: < 3s (endpoints de processamento pesado)

### Technology Stack

**Backend (Core):**
- **Java 17+** (LTS - Long Term Support)
- **Spring Boot 3.x**
  - Spring Web (REST API)
  - Spring Data JPA (ORM)
  - Spring Security (Autenticação/Autorização)
  - Spring Validation (Validação de dados)
- **Maven** (gerenciamento de dependências e build)
- **API RESTful** (padrão REST com JSON)
- **Swagger/OpenAPI 3.0** para documentação automática da API
- **Arquitetura Hexagonal (Ports & Adapters)**
- Rationale: Java/Spring é stack madura, robusta, com vasta comunidade e ferramentas enterprise-ready. Ideal para aplicações que exigem confiabilidade e performance em cálculos complexos. Arquitetura hexagonal facilita testes e desacoplamento.

**Bibliotecas e Frameworks Adicionais:**
- **Lombok**: Redução de boilerplate (getters, setters, construtores)
- **MapStruct**: Mapeamento entre DTOs e Entities
- **Hibernate Validator**: Validações de bean
- **Apache Commons CSV** ou **OpenCSV**: Parsing de arquivos CSV
- **Jackson**: Serialização/deserialização JSON
- **JUnit 5 + Mockito**: Testes unitários
- **TestContainers**: Testes de integração com banco de dados real
- **Flyway** ou **Liquibase**: Versionamento de schema do banco de dados
- **SLF4J + Logback**: Logging estruturado

**Database:**
- **PostgreSQL 15+**
  - Suporte robusto a JSON/JSONB para dados semi-estruturados
  - ACID compliance essencial para dados contábeis/fiscais
  - Excelente performance em consultas complexas
  - Suporte nativo a arrays e tipos customizados
  - Soft delete via status ENUM
- **Redis**: Não será usado no MVP
- Rationale: PostgreSQL é robusto, maduro, open-source, com excelente integração com Spring Data JPA e Hibernate. Suficiente para MVP sem necessidade de cache adicional.

**Containerização e Deploy:**
- **Docker** para aplicação e banco de dados:
  - `Dockerfile` para build da aplicação Spring Boot
  - `docker-compose.yml` para orquestração local (app + PostgreSQL)
  - Volumes Docker para persistência de dados do PostgreSQL
  - Network bridge para comunicação app ↔ DB
- **Ambientes**:
  - **Dev**: Docker Compose local
  - **Staging/Produção**: Docker em cloud provider (a definir)
- **CI/CD**: GitHub Actions ou GitLab CI (pipelines Maven + Docker)
- **Monitoring**: Não será implementado no MVP
  - Spring Boot Actuator disponível para health checks básicos
  - Logs via Logback (console output)
- **Backup**: Backup automatizado diário do PostgreSQL (via cron ou cloud provider)
- Rationale: Docker garante consistência entre ambientes (dev, staging, prod). Simplifica deploy e elimina "funciona na minha máquina".

### Architecture Considerations

**Arquitetura Hexagonal (Ports & Adapters)**

A aplicação seguirá a **Arquitetura Hexagonal** para garantir desacoplamento, testabilidade e manutenibilidade:

```
┌─────────────────────────────────────────────────────────────┐
│                    ADAPTERS (Driving)                       │
│                   Entrada / Inbound                         │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  REST API    │  │   Scheduled  │  │  Event       │     │
│  │  Controller  │  │   Jobs       │  │  Listeners   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │             │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                      PORTS (Inbound)                        │
│                  Interfaces de Entrada                      │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  UserService  │  CompanyService  │  CalculationService │ │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    DOMAIN (Core)                            │
│                  Lógica de Negócio                          │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Domain      │  │  Use Cases   │  │  Business    │     │
│  │  Entities    │  │  (Services)  │  │  Rules       │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  • User, Company, ChartOfAccount, LalurAdjustment          │
│  • Regras: Período Contábil, Soft Delete, Cálculos        │
│  • Value Objects: CNPJ, CPF, Money                         │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    PORTS (Outbound)                         │
│                 Interfaces de Saída                         │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ UserRepository │ CompanyRepository │ EcfParserPort    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                   ADAPTERS (Driven)                         │
│                   Saída / Outbound                          │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  JPA         │  │  File System │  │  External    │     │
│  │  Repository  │  │  (CSV, ECF)  │  │  APIs        │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

**Project Structure (Arquitetura Hexagonal):**
```
src/main/java/com/lalur/ecf/
│
├── domain/                      # CORE - Lógica de Negócio (isolada)
│   ├── model/                   # Entidades de domínio (POJOs puros)
│   │   ├── User.java
│   │   ├── Company.java
│   │   ├── ChartOfAccount.java
│   │   ├── LalurAdjustment.java
│   │   └── valueobject/         # Value Objects
│   │       ├── Cnpj.java
│   │       ├── Money.java
│   │       └── FiscalPeriod.java
│   ├── service/                 # Use Cases / Application Services
│   │   ├── UserService.java
│   │   ├── CompanyService.java
│   │   ├── CalculationService.java
│   │   └── EcfService.java
│   ├── exception/               # Domain exceptions
│   │   ├── BusinessException.java
│   │   ├── EntityNotFoundException.java
│   │   └── InvalidPeriodException.java
│   └── enums/                   # Enums de domínio
│       ├── Status.java
│       ├── UserRole.java
│       └── AdjustmentType.java
│
├── application/                 # PORTS (Interfaces)
│   ├── port/
│   │   ├── in/                  # Inbound Ports (use cases)
│   │   │   ├── UserUseCase.java
│   │   │   ├── CompanyUseCase.java
│   │   │   └── CalculationUseCase.java
│   │   └── out/                 # Outbound Ports (repositories, parsers)
│   │       ├── UserRepositoryPort.java
│   │       ├── CompanyRepositoryPort.java
│   │       ├── EcfParserPort.java
│   │       └── CsvExporterPort.java
│   └── dto/                     # DTOs (Request/Response)
│       ├── request/
│       │   ├── CreateUserRequest.java
│       │   └── CreateCompanyRequest.java
│       └── response/
│           ├── UserResponse.java
│           └── CompanyResponse.java
│
├── infrastructure/              # ADAPTERS (Implementações)
│   ├── adapter/
│   │   ├── in/                  # Inbound Adapters (Controllers)
│   │   │   ├── rest/
│   │   │   │   ├── UserController.java
│   │   │   │   ├── CompanyController.java
│   │   │   │   └── CalculationController.java
│   │   │   └── config/          # Configs REST
│   │   │       └── SwaggerConfig.java
│   │   └── out/                 # Outbound Adapters
│   │       ├── persistence/     # JPA Implementation
│   │       │   ├── entity/      # JPA Entities (mapeamento DB)
│   │       │   │   ├── UserEntity.java
│   │       │   │   └── CompanyEntity.java
│   │       │   ├── repository/  # JPA Repositories
│   │       │   │   ├── UserJpaRepository.java
│   │       │   │   └── CompanyJpaRepository.java
│   │       │   └── adapter/     # Implementação dos Ports
│   │       │       ├── UserRepositoryAdapter.java
│   │       │       └── CompanyRepositoryAdapter.java
│   │       ├── parser/          # File Parsers
│   │       │   ├── EcfParserAdapter.java
│   │       │   └── CsvExporterAdapter.java
│   │       └── calculator/      # Calculadoras
│   │           ├── IrpjCalculator.java
│   │           └── CsllCalculator.java
│   ├── config/                  # Configurações Spring
│   │   ├── SecurityConfig.java
│   │   ├── JpaConfig.java
│   │   └── BeanConfiguration.java
│   └── security/                # JWT, Filters
│       ├── JwtTokenProvider.java
│       └── JwtAuthenticationFilter.java
│
├── shared/                      # Utilitários compartilhados
│   ├── mapper/                  # MapStruct mappers
│   │   ├── UserMapper.java
│   │   └── CompanyMapper.java
│   └── util/
│       ├── CnpjValidator.java
│       └── DateUtils.java
│
└── EcfApplication.java          # Main class

src/main/resources/
├── application.yml              # Config principal
├── application-dev.yml          # Config dev
├── application-prod.yml         # Config produção
└── db/migration/                # Flyway migrations
    ├── V1__create_users_table.sql
    ├── V2__create_companies_table.sql
    └── V3__create_chart_of_accounts_table.sql

docker/
├── Dockerfile                   # Build da aplicação
└── docker-compose.yml           # App + PostgreSQL
```

**Benefícios da Arquitetura Hexagonal:**
1. **Testabilidade**: Domain isolado pode ser testado sem dependências externas
2. **Independência de Framework**: Lógica de negócio não depende do Spring
3. **Flexibilidade**: Fácil trocar adapters (ex: JPA → MongoDB) sem afetar domínio
4. **Manutenibilidade**: Separação clara de responsabilidades
5. **Evolução**: Facilita adição de novos adapters (ex: GraphQL, gRPC)

**Service Architecture:**
- **MVP**: Monolito modular (single deployable JAR)
- **Módulos de Negócio** (bounded contexts hexagonais):
  - `user` - Gestão de usuários e autenticação
  - `company` - Gestão de empresas e período contábil
  - `chartofaccounts` - Plano de contas
  - `taxparameter` - Parâmetros tributários
  - `accountingdata` - Dados contábeis
  - `lalur` - Movimentações Lalur/Lacs
  - `calculation` - Motor de cálculo IRPJ/CSLL
  - `ecf` - Geração e importação ECF
- **Fase 2**: Arquitetura hexagonal facilita migração para microserviços se necessário
- Rationale: Hexagonal permite crescimento orgânico - começar monolito, evoluir para microserviços apenas se necessário.

**Integration Requirements:**
- **Parser de arquivos ECF** (layout RFB - .txt estruturado)
  - Classe `EcfParser` em `util.parser`
  - Leitura de arquivo linha por linha
  - Validação de estrutura conforme layout oficial
- **Parser/Generator de CSV** (importação/exportação)
  - Apache Commons CSV ou OpenCSV
  - Classes `CsvImporter` e `CsvExporter`
- **Validador de CNPJ**
  - Classe utilitária `CnpjValidator` com algoritmo de validação
  - Annotation customizada `@ValidCnpj` para validação automática
- **Motor de Cálculos Tributários**
  - Package `util.calculator` isolado e testável
  - Classes: `IrpjCalculator`, `CsllCalculator`, `LalurProcessor`
  - 100% testado com casos de borda
- Rationale: Componentes críticos isolados facilitam testes e manutenção

**Security/Compliance:**
- **Autenticação**:
  - JWT (JSON Web Token) com Spring Security
  - Access token (15min) + Refresh token (7 dias)
  - BCryptPasswordEncoder (strength: 12) para senhas
  - ADMIN redefine senhas → força troca no primeiro login
- **Autorização**:
  - Role-based access control (RBAC): ADMIN, CONTADOR
  - Method-level security com `@PreAuthorize`
  - CONTADOR só acessa dados da empresa selecionada (row-level security)
- **HTTPS obrigatório** em produção
- **CORS** configurado para domínios específicos (frontend externo)
- **Rate Limiting**:
  - Bucket4j ou Spring Cloud Gateway
  - Limite: 100 req/min por IP (configurável)
- **SQL Injection Prevention**:
  - Spring Data JPA com JPQL/Criteria API (prepared statements automáticos)
  - Validação de inputs com Bean Validation
- **Input Validation**:
  - `@Valid` em controllers
  - Hibernate Validator para regras complexas
  - Sanitização de strings para prevenir injection
- **LGPD Compliance**:
  - Dados pessoais criptografados em repouso (AES-256)
  - Logs de acesso e auditoria
  - Política de retenção de dados (definir período)
  - Termos de uso e política de privacidade
- **Backup e Disaster Recovery**:
  - Backup diário automatizado
  - Retenção: 30 dias rolling + snapshot anual
  - RPO (Recovery Point Objective): 24 horas
  - RTO (Recovery Time Objective): 4 horas
- Rationale: Dados contábeis e fiscais são sensíveis, exigem proteção rigorosa

### Data Model Highlights (JPA Entities)

**Principais Entidades (annotations JPA):**
```java
@Entity @Table(name = "users")
- User (ADMIN, CONTADOR) - @Enumerated(EnumType.STRING) role

@Entity @Table(name = "companies")
- Company (CNPJ, razão social, período contábil, status)

@Entity @Table(name = "tax_parameters")
- TaxParameter (hierárquico via parent_id, global)

@Entity @Table(name = "chart_of_accounts")
- ChartOfAccount (código, nome, empresa, exercício, status)
  @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "account_code", "fiscal_year"}))

@Entity @Table(name = "accounting_data")
- AccountingData (relacionado a ChartOfAccount, valores)

@Entity @Table(name = "lalur_codes")
- LalurCode (código de enquadramento, histórico)

@Entity @Table(name = "presumido_lines")
- PresumidoLine (código, descrição, @ManyToMany com ChartOfAccount)

@Entity @Table(name = "part_b_launches")
- PartBLaunch (lançamentos Parte B)

@Entity @Table(name = "manual_entries")
- ManualEntry (lançamentos contábeis manuais)

@Entity @Table(name = "lalur_adjustments")
- LalurAdjustment (tipo: ADDITION/EXCLUSION/COMPENSATION, valor, justificativa)

@Entity @Table(name = "ecf_part_a", "ecf_part_b")
- EcfPartA, EcfPartB (dados da ECF)
```

**Relacionamentos JPA:**
```java
Company → ChartOfAccount: @OneToMany(mappedBy = "company")
ChartOfAccount → AccountingData: @OneToMany(mappedBy = "chartOfAccount")
Company → TaxParameter: @ManyToMany via join table company_tax_parameters
TaxParameter → TaxParameter: @ManyToOne(self-referential) parent
```

**Constraints e Annotations:**
```java
// Unique constraint
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "account_code", "fiscal_year"}))

// Soft delete
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Status status; // ACTIVE, INACTIVE

// Auditoria (via @EntityListeners)
@CreatedDate
private LocalDateTime createdAt;
@LastModifiedDate
private LocalDateTime updatedAt;
@CreatedBy
private String createdBy;
@LastModifiedBy
private String updatedBy;
```

**Base Entity (classe abstrata para reuso):**
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
```

### Testing Strategy (Java/Spring Boot)

**Níveis de Teste:**

1. **Unit Tests (JUnit 5 + Mockito)** - 70% coverage mínimo:
   - **Cálculos tributários**: `IrpjCalculatorTest`, `CsllCalculatorTest`
   - **Parsers**: `EcfParserTest`, `CsvImporterTest`
   - **Validações**: `CnpjValidatorTest`
   - **Services**: Mock de repositories, testa lógica isolada
   - Exemplo:
   ```java
   @ExtendWith(MockitoExtension.class)
   class IrpjCalculatorTest {
       @Test
       void shouldCalculateIrpjCorrectly() {
           // Given, When, Then
       }
   }
   ```

2. **Integration Tests (Spring Boot Test + TestContainers)**:
   - **Repositories**: Testa queries customizadas com banco real
   - **Controllers**: `@WebMvcTest` para testar endpoints
   - **Services completos**: `@SpringBootTest` com banco embutido
   - TestContainers para PostgreSQL real em testes
   - Exemplo:
   ```java
   @SpringBootTest
   @Testcontainers
   class CompanyServiceIntegrationTest {
       @Container
       static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15");
   }
   ```

3. **API Tests (REST Assured ou MockMvc)**:
   - Fluxos completos via API REST
   - Login → Criação empresa → Importação → Cálculo → Exportação
   - Validação de responses HTTP, JSON schemas
   - Pelo menos 10 cenários críticos

4. **Load Testing (Gatling ou JMeter)** - Opcional MVP, obrigatório produção:
   - Simular 100 requisições concorrentes
   - Identificar bottlenecks (queries lentas, endpoints pesados)
   - Medir tempo de resposta percentis (p50, p95, p99)

**Coverage Tools:**
- **JaCoCo**: Code coverage report (integrado ao Maven/Gradle)
- **SonarQube**: Análise de qualidade de código e cobertura
- Meta: >70% line coverage, >60% branch coverage

**Docker Configuration:**

`Dockerfile`:
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

`docker-compose.yml`:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ecf-postgres
    environment:
      POSTGRES_DB: ecf_db
      POSTGRES_USER: ecf_user
      POSTGRES_PASSWORD: ecf_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - ecf-network

  app:
    build: .
    container_name: ecf-api
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ecf_db
      SPRING_DATASOURCE_USERNAME: ecf_user
      SPRING_DATASOURCE_PASSWORD: ecf_password
      SPRING_PROFILES_ACTIVE: dev
    ports:
      - "8080:8080"
    networks:
      - ecf-network

volumes:
  postgres_data:

networks:
  ecf-network:
    driver: bridge
```

**CI/CD Pipeline (GitHub Actions exemplo):**
```yaml
name: CI/CD
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: mvn clean test
      - name: Run integration tests
        run: mvn verify
      - name: Generate coverage report
        run: mvn jacoco:report
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build with Maven
        run: mvn clean package -DskipTests
      - name: Build Docker image
        run: docker build -t ecf-api:${{ github.sha }} .
      - name: Push to registry (if main branch)
        if: github.ref == 'refs/heads/main'
        run: |
          # docker login & push

  deploy-staging:
    needs: build
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to staging
        run: |
          # Deploy via docker-compose ou cloud provider

  deploy-production:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy to production
        run: |
          # Deploy to production (manual approval required)
```

## Constraints & Assumptions

### Constraints

**Budget:**
- Investimento inicial estimado: R$ 150.000 - R$ 300.000 (6-9 meses desenvolvimento)
- Breakdown estimado:
  - Equipe dev (2-3 devs fullstack): R$ 100k-200k
  - Infra/Cloud (MVP): R$ 2k-5k/mês
  - Design/UX: R$ 20k-30k
  - Jurídico/compliance: R$ 10k-15k
  - Marketing inicial: R$ 20k-30k

**Timeline:**
- MVP completo: 6-9 meses a partir do kickoff
- Breakdown:
  - Mês 1-2: Setup + Auth + Gestão Empresas/Usuários + Plano de Contas
  - Mês 3-4: Parâmetros Tributários + Dados Contábeis + Parte A ECF
  - Mês 5-6: Movimentações + Motor de Cálculo + Arquivo M
  - Mês 7-8: Importação/Combinação ECF + Dashboard + Testes
  - Mês 9: Beta users + Ajustes + Go-live
- Risco: Cronograma apertado, buffer de 20% recomendado

**Resources:**
- Equipe mínima:
  - 1 Tech Lead/Fullstack Senior
  - 1-2 Devs Fullstack Pleno/Júnior
  - 1 Designer UX/UI (part-time ou consultoria)
  - 1 Especialista Tributário (consultoria, validação de regras)
- Equipe desejável:
  - + 1 QA/Tester
  - + 1 DevOps (ou dev com expertise)

**Technical:**
- Dependência de layout ECF da Receita Federal (fora de controle)
  - Mudanças anuais podem exigir adaptações emergenciais
- Complexidade da legislação tributária
  - Regras mudam frequentemente (MPs, leis, instruções normativas)
  - Requer acompanhamento constante
- Performance de parsing de arquivos grandes
  - Empresas grandes podem ter ECFs com >100MB
  - Otimização crítica para UX

### Key Assumptions

**Mercado e Produto:**
1. **Disposição a pagar**: Escritórios estão dispostos a pagar R$ 80-150/empresa/ano ou R$ 2.000-3.000/mês por plano ilimitado
2. **Adoção gradual**: Primeiros 100 clientes virão de network pessoal, indicações e marketing direto (não viralização orgânica)
3. **Sazonalidade controlável**: Apesar da ECF ser anual (entrega em julho), haverá uso ao longo do ano para planejamento e simulações
4. **Competição limitada**: Poucos concorrentes focados especificamente em ECF Parte B (maioria são ERPs generalistas)
5. **Ciclo de vendas**: 1-2 meses para fechar escritórios pequenos, 3-6 meses para médios

**Técnico:**
1. **Layout ECF estável**: Mudanças anuais da RFB não quebrarão funcionalidades core (apenas ajustes incrementais)
2. **Performance adequada**: Arquitetura proposta suportará 100-200 usuários concorrentes sem refatoração

 significativa
3. **Dados limpos**: Arquivos CSV importados terão qualidade razoável (não 100% sujos)
4. **Empresas padrão**: 60-70% das empresas-alvo não têm regimes especiais complexos (MVP é suficiente)

**Negócio:**
1. **Retenção alta**: Uma vez que escritório adota, tende a manter (switching cost alto, sazonalidade anual)
2. **Upsell natural**: Clientes que começam com poucas empresas tendem a aumentar uso
3. **Boca-a-boca forte**: Contadores recomendam ferramentas para pares (network effect)
4. **Suporte gerenciável**: Com boa UX, < 10% dos usuários precisarão suporte ativo
5. **Compliance interno**: Não há requisitos de certificação específica para software fiscal (diferente de NF-e)

**Equipe e Execução:**
1. **Expertise tributária acessível**: Conseguiremos contratar/consultar especialista tributário para validar regras
2. **Recrutamento viável**: Mercado tem devs fullstack TypeScript/React disponíveis na faixa salarial
3. **Foco mantido**: Equipe conseguirá manter foco no MVP sem scope creep por 6-9 meses
4. **Beta users disponíveis**: Conseguiremos 10-15 escritórios dispostos a testar MVP gratuitamente

## Risks & Open Questions

### Key Risks

**1. Risco Regulatório (Probabilidade: Alta, Impacto: Alto)**
- **Descrição**: Receita Federal altera significativamente layout ECF ou regras de cálculo
- **Mitigação**:
  - Monitoramento ativo de publicações da RFB
  - Arquitetura modular permite ajustes isolados
  - Buffer de 1-2 meses antes do prazo de entrega para adaptações
  - Parcerias com consultorias tributárias para early warning

**2. Complexidade Subestimada do Motor de Cálculo (Probabilidade: Média, Impacto: Alto)**
- **Descrição**: Regras tributárias são mais complexas que o previsto, atrasando desenvolvimento
- **Mitigação**:
  - Envolver especialista tributário desde o início
  - Começar com cenários simples (empresas padrão), adicionar complexidade iterativamente
  - MVP aceita override manual se cálculo automático falhar
  - Testes exaustivos com casos reais de escritórios beta

**3. Adoção Mais Lenta Que Esperado (Probabilidade: Média, Impacto: Alto)**
- **Descrição**: Mercado demora mais para adotar, CAC mais alto ou ciclo de vendas mais longo
- **Mitigação**:
  - Freemium tier para primeiros usuários (primeiras 3 empresas grátis)
  - Programa de afiliados/referral (desconto para quem indicar)
  - Parcerias com fornecedores de ERP (canal de distribuição)
  - Conteúdo educativo (blog, webinars) para gerar leads

**4. Qualidade de Dados Importados (Probabilidade: Alta, Impacto: Médio)**
- **Descrição**: CSVs de ERPs têm formatos inconsistentes, exigindo limpeza manual excessiva
- **Mitigação**:
  - Templates de CSV bem documentados
  - Validação robusta com mensagens de erro claras
  - Feature de "pré-visualização" antes de importar definitivamente
  - Suporte a múltiplos formatos de CSV (detecção automática de encoding, delimitador)

**5. Concorrência de ERPs Incumbentes (Probabilidade: Média, Impacto: Médio)**
- **Descrição**: ERPs tradicionais adicionam funcionalidade similar
- **Mitigação**:
  - Foco em UX superior e especialização (vs. generalistas)
  - Speed to market - ser primeiro com solução completa
  - Pricing agressivo para capturar market share inicial
  - Integração com ERPs (parceria, não competição direta)

**6. Dependência de Chave (Bus Factor) (Probabilidade: Média, Impacto: Alto)**
- **Descrição**: Perda de membro crítico da equipe (ex: tech lead ou especialista tributário)
- **Mitigação**:
  - Documentação técnica rigorosa
  - Code review obrigatório (conhecimento distribuído)
  - Pair programming em features críticas
  - Contratos de consultoria com SLA para especialista tributário

### Open Questions

**Produto:**
1. **Pricing final**: Modelo por empresa ou plano flat mensal? Qual valor maximiza receita vs. adoção?
2. **Trial period**: Oferecer trial gratuito de 30 dias? Ou apenas demo assistida?
3. **Suporte a múltiplos anos fiscais**: Sistema deve permitir processar ECF de anos anteriores (retificadoras)?
4. **Validação com PVA**: Devemos integrar com validador oficial da Receita ou confiar em validação própria?
5. **Importação SPED Contábil**: Adicionar no MVP ou deixar para fase 2?

**Técnico:**
1. **ORM escolhido**: Prisma, TypeORM ou Sequelize? (Impacta performance e DX)
2. **Monorepo tool**: Turborepo, Nx ou Lerna? (Impacta CI/CD)
3. **Autenticação**: Implementar próprio ou usar Auth0/Supabase Auth? (Build vs. buy)
4. **Hospedagem específica**: AWS (mais features), Google Cloud (mais simples) ou Azure (integração MS)?
5. **Background jobs**: Como processar importações grandes? (Bull/BullMQ, worker threads, serviço separado?)

**Negócio:**
1. **Canal de vendas primário**: Inside sales, self-service, ou parcerias?
2. **Segmento inicial**: Focar em escritórios pequenos (<20 clientes) ou médios (20-100)?
3. **Região geográfica**: Começar em SP/RJ ou distribuição nacional desde o início?
4. **Modelo de contrato**: Anual (pré-pago) ou mensal (recorrente)? Qual gera melhor cash flow?
5. **Política de reembolso**: Oferecer money-back guarantee? Por quanto tempo?

**Compliance e Legal:**
1. **Responsabilidade por erros**: Qual o disclaimer legal se cálculo estiver errado?
2. **SLA de uptime**: Qual compromisso de disponibilidade (99%? 99.9%)?
3. **Retenção de dados**: Por quanto tempo manter dados de empresas inativas?
4. **Propriedade intelectual**: Dados inseridos pertencem ao cliente? Podemos usar para treinamento de IA (anonimizados)?
5. **Certificações**: Precisamos de alguma certificação/auditoria de segurança para vender para médias empresas?

### Areas Needing Further Research

**1. Análise Competitiva Profunda**
- Identificar todos concorrentes diretos e indiretos
- Feature comparison detalhado
- Pricing comparison
- Entrevistas com usuários de concorrentes (switch barriers)

**2. Validação de Mercado (TAM/SAM/SOM)**
- Quantos escritórios contábeis no Brasil têm clientes no Lucro Real?
- Quantas empresas são tributadas no Lucro Real (base total)?
- Qual % do mercado é endereçável no MVP (sem regimes especiais)?
- Willingness to pay research (survey ou entrevistas)

**3. Estudo de Viabilidade Técnica de Parser ECF**
- Análise de layout ECF completo (centenas de registros)
- POC de parser para validar complexidade
- Performance testing com arquivos reais grandes
- Edge cases e exceções no formato

**4. Benchmarking de Performance**
- Testar ERPs concorrentes com arquivos grandes
- Estabelecer baseline de performance esperada
- Identificar bottlenecks antecipadamente

**5. Requisitos de LGPD Específicos para Dados Fiscais**
- Consultar advogado especialista em LGPD
- Definir termos de uso e política de privacidade
- Identificar se há requisitos especiais para dados de PJ (vs. PF)

**6. Estratégia de Go-to-Market**
- Pesquisa de canais de distribuição viáveis
- Custo de aquisição por canal
- Partnerships com ERPs, consultorias, contadores influenciadores
- Conteúdo e SEO strategy

## Next Steps

### Immediate Actions (Semana 1-2)

1. **Validar Premissas de Mercado**:
   - Entrevistar 10-15 contadores de escritórios pequenos/médios
   - Validar pain points, willingness to pay, features must-have
   - Identificar early adopters dispostos a ser beta users

2. **POC Técnico - Parser ECF**:
   - Obter amir layout ECF oficial da RFB
   - Desenvolver POC de parser básico
   - Testar com 3-5 arquivos ECF reais (diversos tamanhos)
   - Documentar complexidade e riscos

3. **Definir Stack Técnico Final**:
   - Decisão sobre ORM, monorepo tool, cloud provider
   - Setup de repositório e CI/CD básico
   - Definir padrões de código e estrutura

4. **Formar Equipe Core**:
   - Contratar ou alocar tech lead
   - Identificar especialista tributário (consultoria ou full-time)
   - Definir designers UX (interno ou agência)

5. **Refinar Roadmap e Cronograma**:
   - Quebrar MVP em sprints de 2 semanas
   - Identificar dependências críticas
   - Estabelecer milestones e checkpoints

### PM Handoff

Este Project Brief fornece o contexto completo para o **Sistema de Preenchimento de Arquivos M do ECF**.

**Próximos passos para o PM:**
1. Revisar brief completo, questionar premissas, sugerir ajustes
2. Iniciar criação do PRD (Product Requirements Document) detalhado
3. Trabalhar seção por seção do PRD com foco em:
   - User stories detalhadas para cada feature
   - Wireframes e fluxos de tela
   - Critérios de aceitação específicos
   - Priorização de features dentro do MVP
4. Solicitar clarificações sobre regras de negócio tributárias
5. Coordenar com tech lead sobre viabilidade técnica de cada feature

**Questões para discussão inicial com PM:**
- Há alguma feature do MVP que parece sub-especificada?
- Há dependências técnicas ou de negócio não mapeadas?
- O cronograma de 6-9 meses parece realista dada a complexidade?
- Quais riscos você considera mais críticos?

---

**🎉 Project Brief Completo!**

