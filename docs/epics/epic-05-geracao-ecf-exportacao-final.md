# Epic 5: Geração ECF & Exportação Final

**Objetivo do Epic:**

Implementar a geração do **Arquivo Parcial** (bloco M do LALUR/ECF) a partir dos lançamentos da Parte B cadastrados no sistema, permitir importação do ECF completo gerado por sistema externo, e executar o **merge inteligente por chave** entre o ECF Importado e o Arquivo Parcial para produzir o ECF Completo pronto para transmissão SPED. O merge substitui linhas do ECF Importado onde houver chave correspondente no Parcial, preserva o restante do Importado intacto, e recalcula os totalizadores (M300 valor e M990 contagem). Ao final, contadores poderão gerar o Arquivo Parcial com um único botão, importar o ECF da Receita Federal, combinar os dois e baixar o ECF Completo validado.

---

## Formato Real dos Registros ECF Bloco M

O Arquivo Parcial gerado pelo sistema contém os registros do bloco M em **três grupos distintos**:

### Grupo 1 — Ajustes IRPJ (Parte A do LALUR)

```
|M030|{dataInicio}|{dataFim}|{codigoApuracao}|   ← cabeçalho de período (A01=Jan ... A12=Dez)
|M300|{codigoEnq}|{descricao}|{A/E}|{indicador}|{totalValor}|{historico}|  ← ajuste LALUR, pai dos filhos
  |M305|{codigoContaParteB}|{valor}|{D/C}|       ← filho: conta Parte B
  |M310|{codigoContabil}||{valor}|{D/C}|          ← filho: conta contábil (campo vazio entre conta e valor)
```

### Grupo 2 — Ajustes CSLL (Parte A do LACS)

```
|M030|{dataInicio}|{dataFim}|{codigoApuracao}|   ← mesmo cabeçalho de período
|M350|{codigoEnq}|{descricao}|{A/E}|{indicador}|{totalValor}|{historico}|  ← ajuste LACS, pai dos filhos
  |M355|{codigoContaParteB}|{valor}|{D/C}|       ← filho: conta Parte B (equivalente do M305)
  |M360|{codigoContabil}||{valor}|{D/C}|          ← filho: conta contábil (equivalente do M310)
```

### Grupo 3 — Contas da Parte B (LALUR e LACS)

```
|M400|{codigoConta}|{descricao}|{codigoTabela}|...|  ← natureza da conta (definição/cadastro)
  |M410|{codigoConta}|{periodo}|{tipoLanc}|{valor}|...|  ← lançamento na conta (movimento)
|M405|{codigoConta}|{saldoAnterior}|{totalAdic}|{totalExcl}|{saldoAtual}|...|  ← resumo/saldo final
```

> **Nota:** Os campos exatos de M400/M410/M405 devem ser confirmados contra o layout oficial SPED ECF (Manual de Orientação do Leiaute).

---

**Relação pai-filho:**
- M300 é pai de M305 e M310 (IRPJ)
- M350 é pai de M355 e M360 (CSLL)
- M400 é independente; M410 são seus filhos; M405 é o resumo por conta

**Agregação por chave:** Múltiplos lançamentos com o mesmo `codigoEnquadramento` dentro de um mesmo período M030 são **somados** em um único M300/M350. Exemplo:
```
Lançamento 1: |M300|6|...|123|   →  agregado em →  |M300|6|...|333|
Lançamento 2: |M300|6|...|210|
```

**Indicadores do M300/M350:**
- `1` = Parte B only → gera apenas M305/M355
- `2` = Conta Contábil only → gera apenas M310/M360
- `3` = Parte B e Conta Contábil → gera M305/M355 + M310/M360

**Indicador D/C:**
- ADICAO → `D` (Débito)
- EXCLUSAO → `C` (Crédito)

**Totalizador M300/M350:** `totalValor` = soma dos campos `valor` de todos os `LancamentoParteB` do grupo, independente do `tipoRelacionamento`. Cada lançamento contribui seu `valor` uma única vez (M305/M355 e M310/M360 são a decomposição contábil do mesmo lançamento, não valores adicionais).

**Formato de datas:** `DDMMYYYY` sem separadores (padrão SPED). Ex: `01012024` = 01/01/2024.

**Exemplo de estrutura completa (IRPJ — um período):**
```
|M030|01012024|31012024|A01|
|M300|6|Provisoes nao dedutiveis|A|3|186877,58|Provisoes no periodo|
|M305|2130306|44249,63|D|
|M305|21405|142627,95|D|
|M310|3210205||44249,63|D|
|M310|3210201||142627,95|D|
|M300|8|Despesas nao dedutiveis|A|2|30614,68|Despesas nao dedutiveis no periodo|
|M310|3211005||30614,68|D|
```

**Exemplo de estrutura completa (CSLL — mesmo período):**
```
|M030|01012024|31012024|A01|
|M350|6|Provisoes nao dedutiveis|A|3|186877,58|Provisoes no periodo|
|M355|2130306|44249,63|D|
|M355|21405|142627,95|D|
|M360|3210205||44249,63|D|
|M360|3210201||142627,95|D|
```

---

## Story 5.1: Entidade EcfFile (Três Tipos de Arquivo)

Como desenvolvedor,
Eu quero entidade EcfFile para armazenar os três tipos de arquivo ECF com seu conteúdo diretamente no banco de dados,
Para que possamos gerar, consultar e exportar os arquivos sem depender de filesystem externo.

**Acceptance Criteria:**

1. Entidade JPA `EcfFileEntity` criada estendendo `BaseEntity` com **4 colunas principais**:
   - `@Enumerated(STRING) @Column(nullable=false) EcfFileType fileType` (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF)
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company` (FK para empresa)
   - `@Column(nullable=false) Integer fiscalYear` (ano fiscal)
   - `@Column(nullable=false, columnDefinition="TEXT") String content` (conteúdo completo do arquivo ECF como string)
   - Campos auxiliares: `fileName` (String), `fileStatus` (EcfFileStatus), `validationErrors` (TEXT, JSON array), `generatedAt` (LocalDateTime), `generatedBy` (String)
   - `@ManyToOne @JoinColumn(nullable=true) EcfFileEntity sourceImportedEcf` (apenas COMPLETE_ECF)
   - `@ManyToOne @JoinColumn(nullable=true) EcfFileEntity sourceParcialFile` (apenas COMPLETE_ECF)
   - **`@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"file_type", "company_id", "fiscal_year"}))`**: constraint garante no máximo 1 registro por tipo+empresa+ano
2. Enum `EcfFileType`:
   - `ARQUIVO_PARCIAL`: bloco M gerado pelo sistema (M030/M300/M305/M310) a partir dos Lançamentos da Parte B
   - `IMPORTED_ECF`: ECF completo importado pelo usuário de sistema externo
   - `COMPLETE_ECF`: ECF completo resultado do merge (Importado com bloco M atualizado pelo Parcial)
3. Enum `EcfFileStatus`: DRAFT, VALIDATED, ERROR, FINALIZED
4. Domain model `EcfFile` criado com campo `content: String`
5. `EcfFileRepositoryPort` criado:
   - `EcfFile saveOrReplace(EcfFile ecfFile)`: upsert — se já existir registro com mesmo `(fileType, companyId, fiscalYear)`, atualiza o existente (content, status, fileName, generatedAt, generatedBy, sourceRefs); caso contrário insere novo
   - `Optional<EcfFile> findById(Long id)`
   - `List<EcfFile> findByCompanyAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `Optional<EcfFile> findByCompanyAndFiscalYearAndType(Long companyId, Integer fiscalYear, EcfFileType type)` (retorna `Optional` pois é unique)
6. Adapter implementado com JPA + MapStruct; `saveOrReplace` implementado via `findByCompanyAndFiscalYearAndType` + update de campos (evita duplicata e respeita o constraint)
7. Migration Flyway adiciona o unique constraint na tabela `ecf_files`
8. Teste valida salvamento e recuperação por tipo e empresa (incluindo campo `content`)
9. Teste valida comportamento de upsert: salvar duas vezes o mesmo `(fileType, companyId, fiscalYear)` resulta em **1 único registro** com o conteúdo mais recente
10. Teste valida que o `content` do ARQUIVO_PARCIAL é recuperado íntegro (sem truncamento)
11. Teste valida que tipos distintos do mesmo ano/empresa coexistem (ARQUIVO_PARCIAL e IMPORTED_ECF são registros separados)

---

## Story 5.2: Serviço de Geração dos Registros M (Lógica Interna)

Como desenvolvedor,
Eu quero serviço interno que gere todos os registros do bloco M (M030/M300/M305/M310 para IRPJ, M030/M350/M355/M360 para CSLL, e M400/M410/M405 para Contas da Parte B) a partir dos dados cadastrados no sistema,
Para que o Arquivo Parcial possa ser montado em uma única operação.

**Acceptance Criteria:**

1. Service `PartMGeneratorService` criado com método principal:
   - `String generateArquivoParcial(Long companyId, Integer fiscalYear)`: retorna o conteúdo completo do arquivo com os três grupos de registros

2. **Grupo 1 — Ajustes IRPJ: algoritmo M030/M300/M305/M310**

   **Fonte**: `LancamentoParteB` com status ACTIVE, `anoReferencia = fiscalYear`, `tipoApuracao = IRPJ`.

   **Passo 1 — Agrupar por período (mês):**
   - Para cada `mesReferencia` distinto com lançamentos IRPJ ativos, ordenado crescentemente (jan → dez):
     - `dataInicio = primeiro dia do mês` (ex: `01012024` para janeiro)
     - `dataFim = último dia do mês` (ex: `31012024` para janeiro, `29022024` para fevereiro em ano bissexto)
     - `codigoApuracao` = A01 para mês 1, ..., A12 para mês 12
     - Gera linha: `|M030|{dataInicio}|{dataFim}|{codigoApuracao}|`

   **Passo 2 — Dentro de cada período, agrupar por codigoEnquadramento:**
   - **`codigoEnquadramento`**: confirmar com Epic 3 se campo direto em `LancamentoParteB` ou derivado via `parametroTributarioId` → `ParametroTributario.codigoEnquadramento`
   - **Chave de agrupamento**: `codigoEnquadramento`
   - Lançamentos com mesma chave dentro do mesmo período são **agregados em um único M300**

   **Passo 3 — Para cada grupo, gera o registro pai M300:**
   ```
   |M300|{codigoEnquadramento}|{descricao}|{A/E}|{indicador}|{somaValores}|{historico}|
   ```
   - `A/E` = "A" se `tipoAjuste = ADICAO`, "E" se `EXCLUSAO`
   - `somaValores` = soma dos `valor` de todos os lançamentos do grupo
   - `indicador` = derivado do `tipoRelacionamento` do grupo: todos `CONTA_PARTE_B` → `1` / todos `CONTA_CONTABIL` → `2` / mix ou algum `AMBOS` → `3`
   - `descricao` = `ParametroTributario.descricao`
   - `historico` = `LancamentoParteB.descricao` do primeiro lançamento do grupo

   **Passo 4 — Para cada lançamento do grupo, gera os filhos M305/M310:**
   - Se `tipoRelacionamento ∈ {CONTA_PARTE_B, AMBOS}`: gera `|M305|{contaParteB.codigoConta}|{valor}|{D/C}|`
   - Se `tipoRelacionamento ∈ {CONTA_CONTABIL, AMBOS}`: gera `|M310|{planoDeContas.code}||{valor}|{D/C}|`
   - `D/C` = "D" se ADICAO, "C" se EXCLUSAO

3. **Grupo 2 — Ajustes CSLL: algoritmo M030/M350/M355/M360**

   **Idêntico ao Grupo 1**, mas para `tipoApuracao = CSLL` e usando:
   - M350 no lugar de M300
   - M355 no lugar de M305
   - M360 no lugar de M310

   Os blocos M030/M350/M355/M360 são gerados após os blocos M030/M300/M305/M310 no arquivo.

4. **Grupo 3 — Contas da Parte B: algoritmo M400/M410/M405**

   **Fonte**: `ContaParteB` com lançamentos ACTIVE no `fiscalYear` da empresa.

   **Passo 1 — Para cada `ContaParteB` com atividade no ano, gera M400 (natureza/definição da conta):**
   ```
   |M400|{codigoConta}|{descricao}|{codigoTabela}|...|
   ```
   - Campos exatos a confirmar contra layout oficial SPED ECF
   - `codigoConta` = `ContaParteB.codigoConta`
   - `descricao` = `ContaParteB.descricao`

   **Passo 2 — Para cada `LancamentoParteB` da conta, gera M410 (movimento/lançamento na conta):**
   ```
   |M410|{codigoConta}|{periodo}|{tipoLanc}|{valor}|...|
   ```
   - Campos exatos a confirmar contra layout oficial SPED ECF
   - Um M410 por `LancamentoParteB` (visão por conta, diferente do M305/M355 que é visão por ajuste)

   **Passo 3 — Para cada `ContaParteB`, gera M405 (resumo/saldo final da conta):**
   ```
   |M405|{codigoConta}|{saldoAnterior}|{totalAdic}|{totalExcl}|{saldoAtual}|...|
   ```
   - `saldoAnterior` = `ContaParteB.saldoInicial`
   - `totalAdic` = soma dos `valor` dos lançamentos com `tipoAjuste = ADICAO` da conta no ano
   - `totalExcl` = soma dos `valor` dos lançamentos com `tipoAjuste = EXCLUSAO` da conta no ano
   - `saldoAtual` = `saldoAnterior + totalAdic - totalExcl`
   - Campos exatos a confirmar contra layout oficial SPED ECF

5. **Ordem das seções no arquivo gerado:**
   ```
   [blocos M030/M300/M305/M310 — IRPJ — um por mês]
   [blocos M030/M350/M355/M360 — CSLL — um por mês]
   [registros M400/M410/M405 — Contas da Parte B]
   ```

6. **Formatação de valores:**
   - Decimais com vírgula: `1234,56` (padrão SPED brasileiro)
   - Sem separador de milhares
   - 2 casas decimais

7. **Validações:**
   - Lança exceção se não existirem `LancamentoParteB` ACTIVE para o `fiscalYear` especificado
   - Ignora lançamentos com status INACTIVE

8. Teste valida geração correta de M030 para um mês com lançamentos IRPJ
9. Teste valida que ADICAO → A no M300 e D no M305/M310; EXCLUSAO → E no M300 e C no M305/M310
10. Teste valida que `tipoRelacionamento = AMBOS` gera tanto M305 quanto M310 para o mesmo lançamento
11. Teste valida que `tipoRelacionamento = CONTA_CONTABIL` não gera M305
12. Teste valida que `tipoRelacionamento = CONTA_PARTE_B` não gera M310
13. Teste valida que lançamentos IRPJ vão para M300/M305/M310 e CSLL para M350/M355/M360
14. Teste valida formatação de valor com vírgula decimal
15. Teste valida que `somaValores` no M300 = soma dos `valor` dos lançamentos do grupo
16. Teste valida que lançamentos INACTIVE são ignorados
17. Teste valida separação por mês: lançamentos de meses diferentes geram M030 separados
18. Teste valida que M405.saldoAtual = saldoInicial + totalAdic - totalExcl da ContaParteB
19. Teste valida que ContaParteB sem lançamentos no ano não gera M400/M410/M405

---

## Story 5.3: Geração do Arquivo Parcial (Um Único Botão)

Como CONTADOR,
Eu quero clicar em um único botão para gerar o Arquivo Parcial contendo todos os registros M dos Lançamentos da Parte B do ano fiscal,
Para que eu possa revisar o arquivo antes de fazer o merge com o ECF Importado.

**Acceptance Criteria:**

1. Use case `GenerateArquivoParcialUseCase` criado
2. Endpoint: `POST /api/v1/ecf/generate-parcial` (autenticado, requer X-Company-Id)
3. Service `ArquivoParcialAssemblerService` implementa:
   - **Passo 1**: Valida que existem `LancamentoParteB` ACTIVE para o `fiscalYear` e companyId
   - **Passo 2**: Chama `partMGeneratorService.generateArquivoParcial(companyId, fiscalYear)` → retorna `String` com o conteúdo completo
   - **Passo 3**: Cria ou atualiza `EcfFile` com `fileType = ARQUIVO_PARCIAL`, `fileStatus = DRAFT`, **`content = string gerada`**, `fileName = "Parcial_M_{fiscalYear}_{cnpj}.txt"`
   - **Passo 4**: Persiste via `saveOrReplace` — se já existir ARQUIVO_PARCIAL para esta empresa e ano, **substitui o conteúdo no mesmo registro** (upsert)
   - **Passo 5**: Se existir COMPLETE_ECF para esta empresa e ano com status VALIDATED ou FINALIZED, **rebaixa para DRAFT** e limpa `validationErrors` (o Parcial mudou, o ECF Completo está desatualizado)
4. DTO `GenerateArquivoParcialRequest`: `fiscalYear` (obrigatório)
5. DTO `GenerateArquivoParcialResponse`: `success`, `message`, `ecfFileId`, `fileName`, `periodoCount` (número de M030 gerados), `lancamentosCount` (total de lançamentos processados)
6. Nome do arquivo: `Parcial_M_{fiscalYear}_{cnpj}.txt`
7. Response 200 OK com metadados
8. Response 400 Bad Request se não existirem lançamentos: "Nenhum Lançamento da Parte B encontrado para o ano fiscal {fiscalYear}"
9. Response 403 Forbidden se empresa não pertence ao CONTADOR
10. Teste valida geração bem-sucedida com múltiplos lançamentos e períodos
11. Teste valida que `EcfFile.fileType = ARQUIVO_PARCIAL`
12. Teste valida que arquivo salvo contém registros M030/M300/M305/M310 corretos
13. Teste valida que tentativa sem lançamentos retorna 400

---

## Story 5.4: Upload e Armazenamento do ECF Importado

Como CONTADOR,
Eu quero fazer upload do ECF completo gerado por sistema externo (Receita Federal / outro sistema),
Para que o sistema armazene o ECF Importado e permita posteriormente fazer o merge com o Arquivo Parcial.

**Acceptance Criteria:**

1. Use case `UploadImportedEcfUseCase` criado
2. Service `EcfUploadService` implementa:
   - **Passo 1**: Valida extensão do arquivo (deve ser `.txt`)
   - **Passo 2**: Valida tamanho (máximo 50MB)
   - **Passo 3**: Valida formato SPED básico (linhas devem iniciar e terminar com `|`)
   - **Passo 4**: Valida que o arquivo contém bloco M (`|M001|` presente)
   - **Passo 5**: Lê conteúdo do arquivo como String usando encoding **ISO-8859-1 (LATIN-1)** — padrão SPED ECF
   - **Passo 6**: Cria ou atualiza `EcfFile` com `fileType = IMPORTED_ECF`, `fileStatus = DRAFT`, **`content = string do arquivo`**, `fileName = "ECF_Importado_{fiscalYear}_{cnpj}.txt"`
   - **Passo 7**: Persiste via `saveOrReplace` — se já existir IMPORTED_ECF para esta empresa e ano, **substitui o conteúdo no mesmo registro** (upsert)
   - **Passo 8**: Se existir COMPLETE_ECF para esta empresa e ano com status VALIDATED ou FINALIZED, **rebaixa para DRAFT** e limpa `validationErrors` (o ECF Importado mudou, o ECF Completo está desatualizado)
3. Endpoint: `POST /api/v1/ecf/upload-importado` (autenticado, multipart/form-data, requer X-Company-Id)
   - Query param: `fiscalYear` (obrigatório)
4. DTO `UploadImportedEcfResponse`: `success`, `message`, `ecfFileId`, `fileName`, `fileSizeBytes`, `lineCount`
5. Se já existir um IMPORTED_ECF para a empresa e ano fiscal: **upsert** (atualiza o registro existente, não cria novo)
6. Nome armazenado: `ECF_Importado_{fiscalYear}_{cnpj}.txt`
7. Response 200 OK com metadados
8. Response 400 Bad Request se arquivo inválido ou sem bloco M
9. Response 403 Forbidden se empresa não pertence ao CONTADOR
10. Teste valida upload bem-sucedido de ECF válido
11. Teste valida que arquivo sem `|M001|` retorna 400
12. Teste valida que `EcfFile.fileType = IMPORTED_ECF`
13. Teste valida substituição de upload anterior

---

## Story 5.5: Geração do ECF Completo (Merge por Chave)

Como CONTADOR,
Eu quero gerar o ECF Completo fazendo o merge do ECF Importado com o Arquivo Parcial,
Para que o ECF final contenha todos os dados do sistema externo com o bloco M atualizado com os Lançamentos da Parte B.

**Acceptance Criteria:**

1. Use case `GenerateCompleteEcfUseCase` criado
2. Service `EcfMergerService` implementa o **algoritmo de merge por chave**:

   **Passo 1 — Carregar conteúdos:**
   - Busca `EcfFile` tipo `IMPORTED_ECF` da empresa e ano fiscal → **erro 400 se não existir** ("ECF Importado não encontrado. Faça upload antes de gerar o ECF Completo.")
   - Busca `EcfFile` tipo `ARQUIVO_PARCIAL` da empresa e ano fiscal → **erro 400 se não existir** ("Arquivo Parcial não encontrado. Gere o Arquivo Parcial antes de gerar o ECF Completo.")
   - Usa `ecfFile.content` de cada um (string do banco, sem filesystem)

   **Passo 2 — Parsear o Arquivo Parcial em mapa indexado:**
   - Percorre o Parcial e indexa cada M300/M400 junto com seus filhos:
     - Chave: `{codigoApuracao}|{tipoRegistro}|{codigoEnquadramento}` (ex: `"A01|M300|6"`, `"A01|M400|6"`)
     - Valor: lista de strings — linha do M300/M400 + todas as linhas filhas M305/M310/M405/M410 imediatamente abaixo
   - Também indexa os M030 presentes no Parcial: `Set<String> periodosParcial` (ex: {"A01", "A03"})

   **Passo 3 — Construir bloco M do resultado linha a linha (granularidade M300):**
   - Percorre o ECF Importado linha a linha:
     - Linhas **fora do bloco M** (antes de `|M001|` e após `|M990|`) → copiadas sem alteração
     - `|M001|` e `|M010|` → copiados do Importado sem alteração
     - `|M030|...|{codigoApuracao}|` → copiada do Importado; atualiza variável "período atual"
     - **`|M300|{codigoEnq}|...`** ou **`|M400|{codigoEnq}|...`** (período atual):
       - Constrói chave `"{período atual}|M300|{codigoEnq}"`
       - Se chave **existe no Parcial** → **substitui**: emite o M300/M400 do Parcial + todos seus filhos; pula as linhas filhas do Importado (M305/M310/M405/M410) até o próximo M300/M400 ou M030
       - Se chave **não existe no Parcial** → **preserva**: copia M300/M400 do Importado e todos seus filhos filhos sem alteração
     - `|M990|` → recalculado no Passo 5; placeholder no loop

   **Passo 4 — Adicionar do Parcial o que não existia no Importado:**
   - Para cada M030 do Parcial (`periodosParcial`):
     - Se o M030 **não existia no Importado**: adiciona o M030 inteiro (M030 + todos M300/M305/M310 filhos) ao final do bloco M antes do M990
   - Para cada M300/M400 do Parcial:
     - Se o M030 **existia no Importado** mas o `codigoEnquadramento` **não existia** naquele período: adiciona M300/M400 e seus filhos dentro do bloco do período correspondente no resultado

   **Passo 5 — Recalcular totalizadores:**
   - Para cada M300/M400 no resultado: `totalValor` = soma dos campos `valor` de todos os `LancamentoParteB` que originaram aquele M300/M400 (para linhas substituídas pelo Parcial); para linhas preservadas do Importado mantém o totalValor original
   - `M990` = contagem total de **todas** as linhas do bloco M no resultado (incluindo `M001` e o próprio `M990`)

   **Passo 6 — Salvar:**
   - Cria ou atualiza `EcfFile` com `fileType = COMPLETE_ECF`, `fileStatus = DRAFT`, **`content = string resultante do merge`**, `fileName = "ECF_Completo_{fiscalYear}_{cnpj}.txt"`, `sourceImportedEcf` e `sourceParcialFile` preenchidos
   - Persiste via `saveOrReplace` — se já existir COMPLETE_ECF para esta empresa e ano, **substitui o conteúdo no mesmo registro** (upsert)

3. Endpoint: `POST /api/v1/ecf/generate-completo` (autenticado, requer X-Company-Id)
4. DTO `GenerateCompleteEcfRequest`: `fiscalYear` (obrigatório)
5. DTO `GenerateCompleteEcfResponse`: `success`, `message`, `ecfFileId`, `fileName`, `fileSizeBytes`, `sourceImportedEcfId`, `sourceParcialFileId`, `totalLinhas`
6. Response 400 se ECF Importado não existe: "ECF Importado não encontrado. Faça upload do arquivo ECF antes de gerar o ECF Completo."
7. Response 400 se Arquivo Parcial não existe: "Arquivo Parcial não encontrado. Gere o Arquivo Parcial antes de gerar o ECF Completo."
8. Response 403 Forbidden se empresa não pertence ao CONTADOR
9. Teste valida merge: M300 com código presente no Parcial é substituído
10. Teste valida merge: M300 com código ausente no Parcial é preservado do Importado
11. Teste valida merge: M030 do Parcial (A01-A12) não presente no Importado é adicionado ao final
12. Teste valida que M010 do Importado é sempre preservado
13. Teste valida que conteúdo fora do bloco M (antes de M001 e depois de M990) é preservado intacto
14. Teste valida recálculo: totalValor do M300 = soma dos M305 filhos
15. Teste valida recálculo: M990 contém contagem total correta de linhas do bloco M
16. Teste valida que `EcfFile.fileType = COMPLETE_ECF`
17. Teste valida relacionamentos `sourceImportedEcf` e `sourceParcialFile` preenchidos

---

## Story 5.6: Validação de Campos Obrigatórios (Arquivo Parcial e ECF Completo)

Como desenvolvedor,
Eu quero validador que verifique campos obrigatórios dos registros M conforme layout SPED,
Para que arquivos gerados não sejam rejeitados pelo validador PVA da RFB.

**Acceptance Criteria:**

1. Service `EcfValidatorService` criado com **3 métodos**, um por tipo de arquivo:

2. Método `validateArquivoParcial(String content)` retorna `ValidationResult` (para `ARQUIVO_PARCIAL`):
   - Valida que existe ao menos um `|M030|`
   - Valida que cada M300/M400 tem: codigoEnquadramento não vazio, totalValor numérico, indicadorRelacionamento válido (`1`, `2` ou `3`)
   - Valida que cada M305/M405 tem: codigoContaParteB não vazio, valor numérico, D/C válido (`D` ou `C`)
   - Valida que cada M310/M410 tem: codigoContabil não vazio, valor numérico, D/C válido (`D` ou `C`)
   - Valida consistência do indicador: se `indicador=1`, não deve existir M310 filhos; se `indicador=2`, não deve existir M305 filhos
   - Avisa (warning) se `totalValor` do M300 divergir da soma dos `valor` dos lançamentos do grupo

3. Método `validateImportedEcf(String content)` retorna `ValidationResult` (para `IMPORTED_ECF`):
   - Valida que `|M001|` existe
   - Valida que `|M990|` existe
   - Valida que todas as linhas iniciam e terminam com `|` (formato SPED básico)
   - Não valida conteúdo interno do bloco M em detalhes — o arquivo é de sistema externo

4. Método `validateCompleteEcf(String content)` retorna `ValidationResult` (para `COMPLETE_ECF`):
   - Valida estrutura do bloco M (mesmas validações do `validateArquivoParcial`)
   - Valida que `|M001|` existe
   - Valida que `|M990|` existe e que a contagem de linhas do bloco M está correta
   - Valida que todo o conteúdo (bloco M e demais blocos) tem linhas iniciando e terminando com `|`

5. DTO `ValidationResult`: `boolean valid`, `List<String> errors`, `List<String> warnings`
6. Use case `ValidateEcfFileUseCase` com método `validate(Long ecfFileId, Long companyId)` — delega ao método correto conforme `EcfFile.fileType`
7. Endpoint: `POST /api/v1/ecf/{ecfFileId}/validate` (autenticado, requer X-Company-Id)
8. Response 200 OK com `ValidationResult`
9. Se `valid = false`: atualiza `EcfFile.fileStatus = ERROR` e `validationErrors = JSON(errors)`
10. Se `valid = true`: atualiza `EcfFile.fileStatus = VALIDATED`
11. Funciona para os 3 tipos de arquivo (chama método correspondente ao tipo)
12. Teste valida que Arquivo Parcial bem formado retorna `valid = true`
13. Teste valida que M300 com `indicador=1` e linha M310 filha gera erro de inconsistência
14. Teste valida que M990 com contagem errada gera erro no COMPLETE_ECF
15. Teste valida que IMPORTED_ECF sem `|M001|` gera erro
16. Teste valida que status do EcfFile é atualizado corretamente

---

## Story 5.7: Download de Arquivos ECF

Como CONTADOR,
Eu quero fazer download dos três tipos de arquivo ECF,
Para que eu possa revisar, transmitir ao SPED ou compartilhar.

**Acceptance Criteria:**

1. Use case `DownloadEcfFileUseCase` criado
2. Service: busca `EcfFile` por ID, valida que pertence à empresa, retorna `ecfFile.content` como stream de bytes (UTF-8)
3. Endpoint: `GET /api/v1/ecf/{ecfFileId}/download` (autenticado, requer X-Company-Id)
4. Response headers: `Content-Type: text/plain; charset=UTF-8`, `Content-Disposition: attachment; filename="{fileName}"`, `Content-Length: {fileSizeBytes}`
5. Response 200 OK com arquivo `.txt` no body
6. Response 404 se arquivo não existe
7. Response 403 se arquivo não pertence à empresa do CONTADOR
8. Funciona para os 3 tipos de arquivo
9. Teste valida download bem-sucedido de ARQUIVO_PARCIAL
10. Teste valida download bem-sucedido de IMPORTED_ECF
11. Teste valida download bem-sucedido de COMPLETE_ECF
12. Teste valida 403 ao tentar baixar arquivo de outra empresa

---

## Story 5.8: Listagem de Arquivos ECF

Como CONTADOR,
Eu quero visualizar lista de arquivos ECF gerados e importados para um ano fiscal,
Para que eu possa acompanhar o histórico e status de cada arquivo.

**Acceptance Criteria:**

1. Use case `ListEcfFilesUseCase` criado
2. Endpoint: `GET /api/v1/ecf?fiscalYear=2024` (autenticado, requer X-Company-Id)
3. DTO `EcfFileListResponse`:
   - `arquivoParcial`: `EcfFileSummary` ou null (único por empresa+ano)
   - `ecfImportado`: `EcfFileSummary` ou null (único por empresa+ano)
   - `ecfCompleto`: `EcfFileSummary` ou null (único por empresa+ano)
   - Nota: a constraint `UNIQUE(fileType, company, fiscalYear)` garante no máximo 1 de cada tipo por ano
4. DTO `EcfFileSummary`: `id`, `fiscalYear`, `fileType`, `fileName`, `fileSizeBytes`, `fileStatus`, `generatedAt`, `generatedBy`, `validationErrors` (se ERROR), `sourceImportedEcfId`, `sourceParcialFileId` (se COMPLETE_ECF)
5. Suporte a filtro: `?fileType=ARQUIVO_PARCIAL`
6. CONTADOR só visualiza arquivos de sua empresa
7. Response 200 OK
8. Teste valida que retorna os 3 campos separados por tipo (null se não gerado ainda)
9. Teste valida que CONTADOR não vê arquivos de outra empresa

---

## Story 5.9: Finalização de Arquivo ECF

Como CONTADOR,
Eu quero marcar o ECF Completo como finalizado,
Para que eu indique que o arquivo foi transmitido ao SPED e bloqueie modificações posteriores.

**Acceptance Criteria:**

1. Use case `FinalizeEcfFileUseCase` criado
2. Endpoint: `PATCH /api/v1/ecf/{ecfFileId}/finalize` (autenticado, requer X-Company-Id)
3. Service valida que o arquivo é do tipo `COMPLETE_ECF` — **somente ECF Completo pode ser finalizado** (ARQUIVO_PARCIAL e IMPORTED_ECF são intermediários, não são transmitidos ao SPED)
4. Service valida que arquivo está `VALIDATED` (não pode finalizar se DRAFT ou ERROR)
5. Atualiza `EcfFile.fileStatus = FINALIZED`
6. Registra auditoria (`updatedBy`, `updatedAt`)
7. DTO `FinalizeEcfFileResponse`: `success`, `message`, `newStatus`, `fileType`
8. Response 200 OK com confirmação
9. Response 400 se arquivo não é `COMPLETE_ECF`: "Apenas o ECF Completo pode ser finalizado"
10. Response 400 se arquivo não está VALIDATED: "Apenas arquivos validados podem ser finalizados"
11. Response 404 se arquivo não existe
12. Response 403 se arquivo não pertence ao CONTADOR
13. Teste valida finalização de COMPLETE_ECF com status VALIDATED
14. Teste valida que tentativa de finalizar ARQUIVO_PARCIAL retorna 400
15. Teste valida que tentativa de finalizar DRAFT retorna 400

---

## Story 5.10: Testes End-to-End do Fluxo de Geração ECF

Como desenvolvedor,
Eu quero testes E2E cobrindo o fluxo completo de geração do Arquivo Parcial, upload do ECF Importado e merge para ECF Completo,
Para garantir que o pipeline funciona corretamente de ponta a ponta.

**Acceptance Criteria:**

1. **Teste E2E `GenerateArquivoParcialFlowTest`:**
   - Setup: cria empresa, ContasParteB, LancamentosParteB (IRPJ e CSLL, meses Jan-Dez, tipoRelacionamento variados)
   - Executa: gera Arquivo Parcial (`POST /ecf/generate-parcial`)
   - Valida: `EcfFile.fileType = ARQUIVO_PARCIAL`, `fileStatus = DRAFT`
   - Valida: arquivo contém M030 para cada mês com lançamentos
   - Valida: lançamentos IRPJ geraram M300/M305/M310, CSLL geraram M400/M405/M410
   - Valida: lançamento com `tipoRelacionamento = AMBOS` gerou tanto M305 quanto M310
   - Valida: totalValor no M300 = soma dos M305 filhos
   - Executa: valida arquivo (`POST /ecf/{id}/validate`)
   - Valida: `fileStatus = VALIDATED`
   - Executa: download (`GET /ecf/{id}/download`)
   - Valida: arquivo baixado é idêntico ao gerado

2. **Teste E2E `UploadImportedEcfFlowTest`:**
   - Setup: prepara arquivo ECF mock com bloco M (M001, M010, M030|A01, M300|6, M305, M310, M990)
   - Executa: upload (`POST /ecf/upload-importado?fiscalYear=2024`)
   - Valida: `EcfFile.fileType = IMPORTED_ECF`, `fileStatus = DRAFT`
   - Executa: download
   - Valida: arquivo baixado idêntico ao uploadado

3. **Teste E2E `MergeByKeyFlowTest` (validação principal do algoritmo de merge):**
   - Setup: LancamentosParteB com código "6" (mesmo que ECF_IMPORTADO tem) e código "99" (não existe no Importado)
   - Setup: ECF Importado com M300|6 (valor antigo) e M300|8 (sem equivalente no Parcial)
   - Executa: gera Arquivo Parcial
   - Executa: upload ECF Importado
   - Executa: gera ECF Completo (`POST /ecf/generate-completo`)
   - Valida: **M300|6 no resultado usa valor do Parcial** (replace bem-sucedido)
   - Valida: **M300|8 no resultado usa valor do Importado** (preservação de código sem match)
   - Valida: **M300|99 do Parcial foi adicionado** (código novo não existia no Importado)
   - Valida: **M010 do Importado preservado** (nunca substituído)
   - Valida: **Conteúdo antes de M001 e após M990 idêntico ao Importado** (resto do ECF intacto)
   - Valida: **totalValor do M300|6 = soma dos M305 filhos** (totalizador recalculado)
   - Valida: **M990 count correto** (contagem total de linhas do bloco M)
   - Valida: M030|A01..A12 do Parcial adicionados ao resultado
   - Executa: valida ECF Completo
   - Valida: `fileStatus = VALIDATED`
   - Executa: finaliza (`PATCH /ecf/{id}/finalize`)
   - Valida: `fileStatus = FINALIZED`

4. **Teste E2E `M305M310RoutingTest`:**
   - Setup: lançamentos com os 4 tipos de `tipoRelacionamento`
   - Valida: `CONTA_PARTE_B` → apenas M305, nenhum M310
   - Valida: `CONTA_CONTABIL` → apenas M310, nenhum M305
   - Valida: `AMBOS` → M305 + M310 para o mesmo lançamento
   - Valida: indicadorRelacionamento no M300 correto para cada caso

5. **Teste E2E `MultiTenantIsolationTest`:**
   - Setup: duas empresas com lançamentos e arquivos ECF
   - Valida: CONTADOR da empresa A não acessa arquivos da empresa B (403)
   - Valida: listagem retorna apenas arquivos da empresa própria

6. Todos os testes usam TestContainers PostgreSQL
7. Todos os testes criam contexto completo (usuário CONTADOR, empresa, header X-Company-Id)
8. Cobertura de código do Epic 5 deve ser >= 80%

---

## Resumo do Epic

Ao final deste épico, o sistema terá:

- Entidade `EcfFile` com 3 tipos: ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF
- Geração do **Arquivo Parcial** (bloco M) a partir de `LancamentoParteB` e `ContaParteB` (Epic 3), sem dependência de motor de cálculo fiscal
- Registros gerados:
  - `M030`/`M300`/`M305`/`M310` — ajustes IRPJ (Parte A LALUR)
  - `M030`/`M350`/`M355`/`M360` — ajustes CSLL (Parte A LACS)
  - `M400`/`M410`/`M405` — contas da Parte B (natureza, lançamentos e saldo)
  - `M990` — totalizador de linhas
- Upload do ECF Importado com validação de formato SPED
- **Merge inteligente por chave**: substitui M300/M350 onde código coincide, preserva onde não coincide, adiciona blocos do Parcial, recalcula totalizadores
- Validação de campos obrigatórios e totalizadores
- Download dos 3 tipos de arquivo
- Listagem agrupada por tipo
- Finalização para bloqueio pós-transmissão SPED
- Testes E2E cobrindo merge por chave, roteamento M305/M310/M355/M360, e isolamento multi-tenant

**Entidades utilizadas (todas do Epic 3, sem dependência do Epic 4):**
- `LancamentoParteB`: fonte dos ajustes fiscais (M300/M350 e sub-registros; M410)
- `ContaParteB`: definição das contas da Parte B (M305/M355; M400; M405)
- `PlanoDeContas` (ChartOfAccount): referência para contas contábeis (M310/M360)

**Campos-chave do `LancamentoParteB` usados na geração:**
- `tipoApuracao` (IRPJ → M300/M305/M310; CSLL → M350/M355/M360)
- `tipoAjuste` (ADICAO → A e D; EXCLUSAO → E e C)
- `tipoRelacionamento` (define se gera M305/M355 e/ou M310/M360)
- `codigoEnquadramento` → campo do M300/M350 (referência ao código RFB)
- `valor` → valor do sub-registro M305/M355/M310/M360 e do M410
- `mesReferencia` + `anoReferencia` → período do M030
- `contaParteBId` → código para M305
- `contaContabilId` → código para M310

**Dependências de Epics Anteriores:**
- Epic 1: Autenticação JWT, usuários CONTADOR
- Epic 2: Entidades Company e CompanyParameter
- Epic 3: LancamentoParteB, ContaParteB, PlanoDeContas

**Próximos Passos:**
- Este é o último épico funcional do MVP
- Epic 6 e Epic 7 foram removidos do escopo

---

## Apêndice A: Mapeamento Campo a Campo — Como Cada Registro M É Gerado

Este apêndice serve como referência definitiva para o desenvolvedor implementar a Story 5.2. Para cada tipo de registro, mostra o layout SPED e a origem exata de cada campo.

---

## GRUPO 1 — Ajustes IRPJ (Parte A do LALUR)

---

### M030 — Cabeçalho de Período

**Layout:**
```
|M030|{dataInicio}|{dataFim}|{codigoApuracao}|
```

| Campo | Tipo | Origem |
|---|---|---|
| `dataInicio` | DDMMYYYY | Primeiro dia do mês: `01` + `mesReferencia` com 2 dígitos + `anoReferencia` (ex: janeiro/2024 → `01012024`) |
| `dataFim` | DDMMYYYY | Último dia do mês (considerar ano bissexto em fevereiro) |
| `codigoApuracao` | String | `"A"` + `mesReferencia` com 2 dígitos (mês 1 → `A01`, mês 12 → `A12`) |

**Gerado**: 1 linha por `mesReferencia` distinto com lançamentos ACTIVE no `fiscalYear`. Aparece tanto na seção IRPJ quanto na seção CSLL.

---

### M300 — Ajuste LALUR, IRPJ (pai de M305/M310)

**Layout:**
```
|M300|{codigoEnquadramento}|{descricao}|{tipoAjuste}|{indicador}|{totalValor}|{historico}|
```

| Campo | Tipo | Origem |
|---|---|---|
| `codigoEnquadramento` | String | Confirmar Epic 3: campo direto em `LancamentoParteB.codigoEnquadramento` ou via `ParametroTributario.codigoEnquadramento` |
| `descricao` | String | `ParametroTributario.descricao` — descrição oficial do código RFB |
| `tipoAjuste` | `A` ou `E` | `LancamentoParteB.tipoAjuste`: `ADICAO` → `"A"` / `EXCLUSAO` → `"E"` |
| `indicador` | `1`, `2` ou `3` | Derivado do `tipoRelacionamento` do grupo: todos `CONTA_PARTE_B` → `1` / todos `CONTA_CONTABIL` → `2` / mix ou algum `AMBOS` → `3` |
| `totalValor` | Decimal BR | Soma de `LancamentoParteB.valor` de todos os lançamentos do grupo (vírgula decimal, 2 casas, sem milhar) |
| `historico` | String | `LancamentoParteB.descricao` do primeiro lançamento do grupo |

**Gerado**: 1 linha por grupo `{codigoEnquadramento}` dentro de cada período M030 IRPJ. Lançamentos com mesma chave são somados.

---

### M305 — Conta da Parte B, IRPJ (filho de M300)

**Layout:**
```
|M305|{codigoContaParteB}|{valor}|{DC}|
```

| Campo | Tipo | Origem |
|---|---|---|
| `codigoContaParteB` | String | `ContaParteB.codigoConta` — via `LancamentoParteB.contaParteBId` |
| `valor` | Decimal BR | `LancamentoParteB.valor` |
| `DC` | `D` ou `C` | `LancamentoParteB.tipoAjuste`: `ADICAO` → `"D"` / `EXCLUSAO` → `"C"` |

**Condição**: somente quando `LancamentoParteB.tipoRelacionamento ∈ {CONTA_PARTE_B, AMBOS}` E `tipoApuracao = IRPJ`.

---

### M310 — Conta Contábil, IRPJ (filho de M300)

**Layout:**
```
|M310|{codigoContabil}||{valor}|{DC}|
```
> Pipes consecutivos `||` = campo vazio obrigatório no layout SPED.

| Campo | Tipo | Origem |
|---|---|---|
| `codigoContabil` | String | `PlanoDeContas.code` — via `LancamentoParteB.contaContabilId` |
| *(vazio)* | — | Sempre vazio |
| `valor` | Decimal BR | `LancamentoParteB.valor` |
| `DC` | `D` ou `C` | `LancamentoParteB.tipoAjuste`: `ADICAO` → `"D"` / `EXCLUSAO` → `"C"` |

**Condição**: somente quando `LancamentoParteB.tipoRelacionamento ∈ {CONTA_CONTABIL, AMBOS}` E `tipoApuracao = IRPJ`.

---

## GRUPO 2 — Ajustes CSLL (Parte A do LACS)

Os registros M350/M355/M360 são **estruturalmente idênticos** a M300/M305/M310, mas para lançamentos com `tipoApuracao = CSLL`.

---

### M350 — Ajuste LACS, CSLL (pai de M355/M360)

**Layout:**
```
|M350|{codigoEnquadramento}|{descricao}|{tipoAjuste}|{indicador}|{totalValor}|{historico}|
```

Mesmos campos e mesmas origens que M300. **Condição**: `tipoApuracao = CSLL`.

---

### M355 — Conta da Parte B, CSLL (filho de M350)

**Layout:**
```
|M355|{codigoContaParteB}|{valor}|{DC}|
```

Mesmos campos e mesmas origens que M305. **Condição**: `tipoRelacionamento ∈ {CONTA_PARTE_B, AMBOS}` E `tipoApuracao = CSLL`.

---

### M360 — Conta Contábil, CSLL (filho de M350)

**Layout:**
```
|M360|{codigoContabil}||{valor}|{DC}|
```

Mesmos campos e mesmas origens que M310. **Condição**: `tipoRelacionamento ∈ {CONTA_CONTABIL, AMBOS}` E `tipoApuracao = CSLL`.

---

## GRUPO 3 — Contas da Parte B (LALUR e LACS)

Os registros M400/M410/M405 gerenciam as **contas da Parte B** — são uma visão por conta, complementar à visão por ajuste (M300/M350).

---

### M400 — Natureza da Conta da Parte B (definição/cadastro)

**Função**: define/identifica cada conta da Parte B utilizada no ano. Equivale a cadastrar a "gaveta" antes de movimentá-la.

**Layout:**
```
|M400|{codigoConta}|{descricao}|{codigoTabela}|...|
```
> ⚠️ Campos exatos a confirmar contra o layout oficial SPED ECF (Manual de Orientação do Leiaute da RFB).

| Campo | Tipo | Origem (estimada) |
|---|---|---|
| `codigoConta` | String | `ContaParteB.codigoConta` |
| `descricao` | String | `ContaParteB.descricao` |
| `codigoTabela` | String | Referência à tabela dinâmica da RFB — confirmar campo em `ContaParteB` |

**Gerado**: 1 linha por `ContaParteB` com lançamentos ACTIVE no `fiscalYear`.

---

### M410 — Lançamento na Conta da Parte B (movimento)

**Função**: registra cada movimentação que aumenta ou diminui o saldo de uma conta da Parte B. O M410 é a "ficha de movimentação" da conta.

**Layout:**
```
|M410|{codigoConta}|{periodo}|{tipoLanc}|{valor}|...|
```
> ⚠️ Campos exatos a confirmar contra o layout oficial SPED ECF.

| Campo | Tipo | Origem (estimada) |
|---|---|---|
| `codigoConta` | String | `ContaParteB.codigoConta` — via `LancamentoParteB.contaParteBId` |
| `periodo` | DDMMYYYY ou similar | Derivado de `LancamentoParteB.mesReferencia` + `anoReferencia` |
| `tipoLanc` | String | Derivado de `LancamentoParteB.tipoAjuste` |
| `valor` | Decimal BR | `LancamentoParteB.valor` |

**Gerado**: 1 linha por `LancamentoParteB` com `contaParteBId` preenchido (i.e., `tipoRelacionamento ∈ {CONTA_PARTE_B, AMBOS}`).

---

### M405 — Resumo/Saldo Final da Conta da Parte B

**Função**: apresenta o saldo final de cada conta da Parte B após todos os lançamentos do período. É a "conferência" do que sobrou na gaveta.

**Layout:**
```
|M405|{codigoConta}|{saldoAnterior}|{totalAdic}|{totalExcl}|{saldoAtual}|...|
```
> ⚠️ Campos exatos a confirmar contra o layout oficial SPED ECF.

| Campo | Tipo | Origem |
|---|---|---|
| `codigoConta` | String | `ContaParteB.codigoConta` |
| `saldoAnterior` | Decimal BR | `ContaParteB.saldoInicial` |
| `totalAdic` | Decimal BR | Soma de `LancamentoParteB.valor` onde `tipoAjuste = ADICAO` para esta conta no `fiscalYear` |
| `totalExcl` | Decimal BR | Soma de `LancamentoParteB.valor` onde `tipoAjuste = EXCLUSAO` para esta conta no `fiscalYear` |
| `saldoAtual` | Decimal BR | `saldoAnterior + totalAdic - totalExcl` |

**Gerado**: 1 linha por `ContaParteB` com atividade no `fiscalYear`. Aparece após todos os M410 da conta.

---

## Tabela Resumo de Condições

### Grupo 1 (IRPJ) e Grupo 2 (CSLL) — Registros por Lançamento

| Registro | tipoApuracao | tipoRelacionamento | Gera |
|---|---|---|---|
| M300 | IRPJ | qualquer | Sempre (1 por grupo/período) |
| M305 | IRPJ | CONTA_PARTE_B | sim |
| M305 | IRPJ | CONTA_CONTABIL | **não** |
| M305 | IRPJ | AMBOS | sim |
| M310 | IRPJ | CONTA_PARTE_B | **não** |
| M310 | IRPJ | CONTA_CONTABIL | sim |
| M310 | IRPJ | AMBOS | sim |
| M350 | CSLL | qualquer | Sempre (1 por grupo/período) |
| M355 | CSLL | CONTA_PARTE_B | sim |
| M355 | CSLL | CONTA_CONTABIL | **não** |
| M355 | CSLL | AMBOS | sim |
| M360 | CSLL | CONTA_PARTE_B | **não** |
| M360 | CSLL | CONTA_CONTABIL | sim |
| M360 | CSLL | AMBOS | sim |

### Grupo 3 — Registros por Conta da Parte B

| Registro | Condição | Gera |
|---|---|---|
| M400 | ContaParteB com lançamentos no ano | 1 por conta |
| M410 | LancamentoParteB com contaParteBId preenchido | 1 por lançamento |
| M405 | ContaParteB com lançamentos no ano | 1 por conta (saldo final) |

---

## Exemplo Completo com Mapeamento

### Dados de entrada

```
ContaParteB { codigoConta="2130306", descricao="Provisões", saldoInicial=0 }
PlanoDeContas { code="3210205" }

LancamentoParteB A {
  tipoApuracao = IRPJ, tipoAjuste = ADICAO, tipoRelacionamento = AMBOS
  codigoEnquadramento = "6", valor = 44249,63, mesReferencia = 1, anoReferencia = 2024
  contaParteBId → "2130306", contaContabilId → "3210205"
  descricao = "Provisões não dedutíveis"
}
LancamentoParteB B {
  tipoApuracao = IRPJ, tipoAjuste = ADICAO, tipoRelacionamento = AMBOS
  codigoEnquadramento = "6", valor = 142627,95, mesReferencia = 1, anoReferencia = 2024
  contaParteBId → "21405", contaContabilId → "3210201"
  descricao = "Provisões não dedutíveis"
}
```

### Arquivo Parcial gerado

```
← GRUPO 1: IRPJ →
|M030|01012024|31012024|A01|
|M300|6|Provisoes nao dedutiveis|A|3|186877,58|Provisoes nao dedutiveis|
|M305|2130306|44249,63|D|
|M305|21405|142627,95|D|
|M310|3210205||44249,63|D|
|M310|3210201||142627,95|D|

← GRUPO 2: CSLL — exemplo com mesmo código "6" para CSLL →
|M030|01012024|31012024|A01|
|M350|6|Provisoes nao dedutiveis|A|3|186877,58|Provisoes nao dedutiveis|
|M355|2130306|44249,63|D|
|M355|21405|142627,95|D|
|M360|3210205||44249,63|D|
|M360|3210201||142627,95|D|

← GRUPO 3: Contas da Parte B →
|M400|2130306|Provisões|...|
|M410|2130306|...|44249,63|...|
|M405|2130306|0,00|186877,58|0,00|186877,58|

|M990|{totalLinhas}|
```

> `totalValor` do M300 = 44249,63 + 142627,95 = **186877,58** (soma dos dois lançamentos)
> `M405.saldoAtual` = 0 (saldoInicial) + 186877,58 (totalAdic) - 0 (totalExcl) = **186877,58**
