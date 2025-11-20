# Epic 5: Geração ECF & Exportação Final

**Objetivo do Epic:**

Implementar a geração completa do arquivo ECF (Escrituração Contábil Fiscal) pronto para transmissão ao SPED, incluindo três tipos distintos de arquivos: (1) **Arquivo M isolado** contendo apenas os registros Lalur/Lacs gerados pelo sistema em **uma única operação atômica**, (2) **ECF importado** pelo usuário contendo a Parte A de sistemas externos, e (3) **ECF completo** resultado da **substituição simples** da Parte M antiga pela nova gerada. Este épico entrega a capacidade de gerar a Parte M (registros M001, M300, M350, M400, M410, M990) a partir das movimentações fiscais em **um único botão/endpoint**, importar ECF existente via upload, fazer **substituição simples** preservando toda a Parte A (tudo antes de `|M001|`) e substituindo a Parte M antiga pela nova gerada, validar campos obrigatórios conforme layout oficial da Receita Federal, e permitir download dos três tipos de arquivos. Ao final deste épico, contadores poderão gerar arquivos ECF validados e prontos para transmissão SPED sem necessidade de ferramentas externas, usando abordagem simples e direta sem parsers complexos.

---

## Story 5.1: Entidade EcfFile (Três Tipos de Arquivo)

Como desenvolvedor,
Eu quero entidade EcfFile para armazenar metadados dos três tipos de arquivos ECF (Arquivo M, ECF Importado, ECF Completo),
Para que possamos rastrear histórico de geração, uploads, status de validação e permitir download posterior.

**Acceptance Criteria:**

1. Entidade JPA `EcfFileEntity` criada estendendo `BaseEntity`:
   - `@ManyToOne @JoinColumn(nullable=false) CompanyEntity company`
   - `@Column(nullable=false) Integer fiscalYear`
   - `@Enumerated(STRING) @Column(nullable=false) EcfFileType fileType` (M_FILE_ONLY, IMPORTED_ECF, COMPLETE_ECF)
   - `@Column(nullable=false, length=255) String fileName` (ex: "Arquivo_M_2024_12345678000100.txt", "ECF_Importado_2024.txt", "ECF_Completo_2024.txt")
   - `@Column(nullable=false) String filePath` (caminho de armazenamento no servidor/storage)
   - `@Column(nullable=false) Long fileSizeBytes`
   - `@Enumerated(STRING) @Column(nullable=false) EcfFileStatus status` (DRAFT, VALIDATED, ERROR, FINALIZED)
   - `@Column(columnDefinition="TEXT") String validationErrors` (JSON array de erros se status ERROR)
   - `@Column(nullable=false) LocalDateTime createdAt` (data de geração ou upload)
   - `@Column(nullable=false) String createdBy` (email do usuário que gerou ou fez upload)
   - `@ManyToOne @JoinColumn(nullable=true) EcfFileEntity sourceImportedEcf` (referência ao ECF importado usado no merge, apenas para fileType COMPLETE_ECF)
   - `@ManyToOne @JoinColumn(nullable=true) EcfFileEntity sourceMFile` (referência ao Arquivo M usado no merge, apenas para fileType COMPLETE_ECF)
2. Enum `EcfFileType` criado:
   - **M_FILE_ONLY**: Arquivo contendo apenas registros M (M001, M300, M350, M400, M410, M990) gerados pelo sistema
   - **IMPORTED_ECF**: Arquivo ECF completo importado pelo usuário (contém Parte A e possivelmente outras partes)
   - **COMPLETE_ECF**: Arquivo ECF completo resultado do merge (Parte A do importado + Parte M gerada)
3. Enum `EcfFileStatus` criado: DRAFT, VALIDATED, ERROR, FINALIZED
4. Domain model `EcfFile` criado
5. `EcfFileRepositoryPort` criado:
   - `EcfFile save(EcfFile ecfFile)`
   - `Optional<EcfFile> findById(Long id)`
   - `List<EcfFile> findByCompanyAndFiscalYear(Long companyId, Integer fiscalYear)`
   - `List<EcfFile> findByCompanyAndFiscalYearAndType(Long companyId, Integer fiscalYear, EcfFileType type)`
   - `Optional<EcfFile> findLatestByCompanyAndFiscalYearAndTypeAndStatus(Long companyId, Integer fiscalYear, EcfFileType type, EcfFileStatus status)`
6. Adapter implementado com JPA + MapStruct
7. Teste de integração valida salvamento e recuperação
8. Teste valida que findLatest retorna arquivo mais recente (por createdAt DESC) do tipo e status especificados
9. Teste valida relacionamento source (COMPLETE_ECF referencia IMPORTED_ECF e M_FILE_ONLY usados no merge)

---

## Story 5.2: Serviço Interno de Geração de Registros da Parte M

Como desenvolvedor,
Eu quero serviço interno que gere todos os registros da Parte M (M001, M300, M350, M400, M410, M990),
Para que a funcionalidade de geração do Arquivo M possa montar o arquivo completo em uma única operação.

**Acceptance Criteria:**

1. Service `PartMGeneratorService` criado com métodos internos (não são endpoints separados):
   - `generateM001Record()`: gera abertura do bloco M
   - `generateM300Records(Long companyId, Integer fiscalYear)`: gera registros Lalur Parte A
   - `generateM350Records(Long companyId, Integer fiscalYear)`: gera registros Lalur Parte B (conta gráfica)
   - `generateM400Records(Long companyId, Integer fiscalYear)`: gera registros Lacs
   - `generateM410Records(Long companyId, Integer fiscalYear)`: gera registros Lacs conta gráfica
   - `generateM990Record(int recordCount)`: gera encerramento do bloco M
2. **Método M001 (Abertura)**:
   - Retorna string: `|M001|0|` (indicador de movimento: 0 = com movimento)
   - Se não houver movimentos fiscais (nem LALUR nem LACS), retorna `|M001|1|` (sem movimento)
3. **Método M300 (Lalur Parte A - Movimentações)**:
   - Busca todas `FiscalMovement` ACTIVE da empresa, ano fiscal e `movementBook = LALUR`
   - Para cada movimento, gera linha M300 conforme layout oficial: `|M300|{movementType}|{codigoEnquadramento}|{description}|{amount}|`
   - Mapeamento de `MovementType` para código oficial RFB (ADDITION → "A", EXCLUSION → "E", COMPENSATION → "C")
   - Formatação de valores: 2 casas decimais, sem separador de milhares (ex: 50000.00)
   - Validação: lança exceção se description vazio ou amount <= 0
4. **Método M350 (Lalur Parte B - Conta Gráfica)**:
   - Busca último `TaxCalculationResult` ACTIVE tipo IRPJ da empresa e ano fiscal
   - Busca `CompanyParameter` para obter prejuízo fiscal anterior (saldo inicial)
   - Gera registro M350 de abertura: `|M350|SALDO_INICIAL|{prejuizoFiscalAnterior}|`
   - Gera registros M350 de movimentações (adições de prejuízo, compensações aplicadas)
   - Gera registro M350 de encerramento: `|M350|SALDO_FINAL|{remainingLossCarryforward}|`
   - Validação: se não houver TaxCalculationResult, lança exceção "Cálculo IRPJ não encontrado"
5. **Método M400 (Lacs - Movimentações)**:
   - Busca todas `FiscalMovement` ACTIVE da empresa, ano fiscal e `movementBook = LACS`
   - Para cada movimento, gera linha M400 análoga a M300: `|M400|{movementType}|{codigoEnquadramento}|{description}|{amount}|`
6. **Método M410 (Lacs - Conta Gráfica)**:
   - Busca último `TaxCalculationResult` ACTIVE tipo CSLL para obter saldos de base negativa
   - Gera registro M410 com saldo inicial, movimentações, saldo final
   - Validação: se não houver TaxCalculationResult CSLL, lança exceção
7. **Método M990 (Encerramento)**:
   - Retorna string: `|M990|{recordCount}|` onde recordCount é total de registros do bloco M (incluindo M001 e M990)
8. Teste valida geração de M001 com indicador correto (0 se houver movimentos, 1 se não houver)
9. Teste valida geração correta de 3 movimentos LALUR → 3 linhas M300
10. Teste valida que movimentos INACTIVE são ignorados
11. Teste valida geração de M350 com saldo inicial 100k, compensações 30k, saldo final 70k
12. Teste valida geração de M400 apenas com movimentos LACS
13. Teste valida geração de M410 com saldos iniciais e finais
14. Teste valida M990 contém contagem correta de registros
15. Teste valida formatação de valores com 2 decimais
16. Teste valida que exceção é lançada se cálculo IRPJ ou CSLL ausente

**Nota Importante:** Esta story implementa métodos internos do serviço. A funcionalidade exposta ao usuário (endpoint) está na Story 5.3.

---

## Story 5.3: Geração do Arquivo M Completo - UM ÚNICO BOTÃO (Tipo M_FILE_ONLY)

Como CONTADOR,
Eu quero clicar em UM ÚNICO BOTÃO para gerar o arquivo M completo contendo todos os registros (M001, M300, M350, M400, M410, M990) de uma só vez,
Para que eu possa exportar e revisar as movimentações fiscais geradas pelo sistema sem precisar gerar cada parte separadamente.

**Acceptance Criteria:**

1. **FUNCIONALIDADE ÚNICA**: Um único endpoint `POST /api/v1/ecf/generate-m-file` gera o arquivo M completo em uma única operação atômica
2. Use case `GenerateMFileUseCase` criado com método `generate(Long companyId, Integer fiscalYear, String requestedBy)`
3. Service `MFileAssemblerService` implementa lógica que chama os métodos internos do `PartMGeneratorService` (Story 5.2) e monta o arquivo completo:
   - **Passo 1**: Valida pré-requisitos (existem cálculos IRPJ/CSLL finalizados?)
   - **Passo 2**: Chama `partMGeneratorService.generateM001Record()` → adiciona ao arquivo
   - **Passo 3**: Chama `partMGeneratorService.generateM300Records(...)` → adiciona todos M300 ao arquivo
   - **Passo 4**: Chama `partMGeneratorService.generateM350Records(...)` → adiciona todos M350 ao arquivo
   - **Passo 5**: Chama `partMGeneratorService.generateM400Records(...)` → adiciona todos M400 ao arquivo
   - **Passo 6**: Chama `partMGeneratorService.generateM410Records(...)` → adiciona todos M410 ao arquivo
   - **Passo 7**: Conta total de registros e chama `partMGeneratorService.generateM990Record(recordCount)` → adiciona M990 ao arquivo
   - **Passo 8**: Monta arquivo .txt completo com todas linhas separadas por quebras de linha `\n`
   - **Passo 9**: Salva arquivo em storage (filesystem ou S3)
   - **Passo 10**: Cria `EcfFile` com tipo **M_FILE_ONLY**, status DRAFT
4. DTO `GenerateMFileRequest`: `fiscalYear` (obrigatório)
5. DTO `GenerateMFileResponse`: `success`, `message`, `ecfFileId`, `fileName`, `fileSizeBytes`, `fileType` (sempre M_FILE_ONLY), `recordCount` (total de registros gerados)
6. Endpoint `POST /api/v1/ecf/generate-m-file` (autenticado, requer X-Company-Id)
7. Validação: requer que cálculos IRPJ e CSLL estejam finalizados e ACTIVE
8. Validação: CONTADOR só pode gerar para sua empresa
9. Response 200 OK com metadados do arquivo gerado
10. Response 400 Bad Request se cálculos ausentes: "Cálculo IRPJ não encontrado para o ano fiscal 2024"
11. Response 403 Forbidden se empresa não pertence ao CONTADOR
12. Nome do arquivo gerado: `Arquivo_M_{fiscalYear}_{cnpj}.txt` (ex: `Arquivo_M_2024_12345678000100.txt`)
13. Teste valida geração completa em uma única chamada: M001 → M300 → M350 → M400 → M410 → M990
14. Teste valida contagem de registros em M990 está correta
15. Teste valida que arquivo salvo contém todas linhas separadas por `\n`
16. Teste valida que `EcfFile.fileType = M_FILE_ONLY`
17. Teste valida que operação é atômica (se falhar em qualquer passo, nenhum arquivo é salvo)

**IMPORTANTE:** Esta é a ÚNICA funcionalidade exposta ao usuário para gerar o arquivo M. Não existem botões ou endpoints separados para gerar M300, M350, M400 ou M410 individualmente. A geração é sempre completa e atômica.

---

## Story 5.4: Upload e Armazenamento de ECF Importado (Tipo IMPORTED_ECF)

Como CONTADOR,
Eu quero fazer upload de arquivo ECF existente gerado por sistema externo,
Para que o sistema armazene o ECF importado e permita posteriormente adicionar a Parte M gerada pelo sistema.

**Acceptance Criteria:**

1. Use case `UploadImportedEcfUseCase` criado com método `upload(Long companyId, Integer fiscalYear, InputStream fileInputStream, String originalFileName, String uploadedBy)`
2. Service `EcfUploadService` implementa:
   - **Passo 1**: Valida extensão do arquivo (deve ser `.txt`)
   - **Passo 2**: Valida tamanho do arquivo (máximo 10MB)
   - **Passo 3**: Valida formato básico SPED (linhas devem iniciar e terminar com `|`)
   - **Passo 4**: Valida que arquivo contém ao menos um registro (não está vazio)
   - **Passo 5**: Salva arquivo completo em storage (ex: `uploads/{companyId}/{fiscalYear}/ecf_importado.txt`)
   - **Passo 6**: Cria registro `EcfFile` com tipo **IMPORTED_ECF**, status DRAFT
3. Controller endpoint `POST /api/v1/ecf/upload-imported` (autenticado, requer X-Company-Id):
   - Aceita multipart/form-data com arquivo `.txt`
   - Query param `fiscalYear` obrigatório
4. DTO `UploadImportedEcfRequest`: multipart file
5. DTO `UploadImportedEcfResponse`: `success`, `message`, `ecfFileId`, `fileName`, `fileSizeBytes`, `fileType` (sempre IMPORTED_ECF), `lineCount`
6. Validação: arquivo deve ter tamanho < 10MB
7. Validação: apenas um arquivo ECF importado por empresa/ano fiscal (substituir se já existir)
8. Nome do arquivo armazenado: `ECF_Importado_{fiscalYear}_{cnpj}.txt`
9. Response 200 OK com metadados da importação
10. Response 400 Bad Request se arquivo inválido (formato SPED incorreto ou vazio)
11. Response 403 Forbidden se empresa não pertence ao CONTADOR
12. Teste valida upload bem-sucedido de arquivo ECF válido
13. Teste valida que tentativa de importar arquivo com formato inválido retorna 400
14. Teste valida que arquivo é salvo corretamente em storage
15. Teste valida que `EcfFile.fileType = IMPORTED_ECF`
16. Teste valida que upload substitui arquivo importado anterior se já existir

---

## Story 5.5: Geração de ECF Completo - Substituição Simples (Tipo COMPLETE_ECF)

Como CONTADOR,
Eu quero gerar arquivo ECF completo substituindo a Parte M antiga do ECF importado pela Parte M gerada pelo sistema,
Para que eu possa baixar arquivo ECF final pronto para transmissão SPED contendo toda a Parte A original + Parte M atualizada.

**Acceptance Criteria:**

1. Use case `GenerateCompleteEcfUseCase` criado com método `generate(Long companyId, Integer fiscalYear, String requestedBy)`
2. Service `EcfMergerService` implementa **substituição simples por busca de linha**:
   - **Passo 1**: Busca `EcfFile` tipo **IMPORTED_ECF** da empresa e ano fiscal (se não existir, retorna erro)
   - **Passo 2**: Busca ou gera `EcfFile` tipo **M_FILE_ONLY** da empresa e ano fiscal
   - **Passo 3**: Lê arquivo ECF importado de storage linha por linha
   - **Passo 4**: **Encontra início da Parte M**: busca primeira linha que inicia com `|M001|`
   - **Passo 5**: **Extrai Parte A**: todas as linhas **antes** de `|M001|` (ou arquivo completo se não houver M001)
   - **Passo 6**: Lê arquivo M gerado de storage (todas as linhas: M001, M300, M350, M400, M410, M990)
   - **Passo 7**: **Monta ECF Completo**: Parte A (extraída) + quebra de linha + Arquivo M (completo)
   - **Passo 8**: Valida estrutura final (tem ao menos uma linha antes de M001, e tem M001 e M990)
   - **Passo 9**: Salva arquivo final em storage
   - **Passo 10**: Cria `EcfFile` com tipo **COMPLETE_ECF**, status DRAFT, `sourceImportedEcf` e `sourceMFile` referenciando arquivos usados
3. DTO `GenerateCompleteEcfRequest`: `fiscalYear` (obrigatório)
4. DTO `GenerateCompleteEcfResponse`: `success`, `message`, `ecfFileId`, `fileName`, `fileSizeBytes`, `fileType` (sempre COMPLETE_ECF), `sourceImportedEcfId`, `sourceMFileId`, `lineCount`
5. Endpoint `POST /api/v1/ecf/generate-complete` (autenticado, requer X-Company-Id)
6. Validação: requer que ECF importado exista (tipo IMPORTED_ECF)
7. Validação: requer que cálculos IRPJ e CSLL estejam finalizados (para gerar Arquivo M)
8. Nome do arquivo gerado: `ECF_Completo_{fiscalYear}_{cnpj}.txt`
9. Response 200 OK com metadados do arquivo ECF completo
10. Response 400 Bad Request se ECF importado não foi feito upload: "ECF importado não encontrado. Faça upload de um arquivo ECF existente primeiro."
11. Response 400 Bad Request se cálculos ausentes
12. Response 403 Forbidden se empresa não pertence ao CONTADOR
13. Teste valida geração bem-sucedida: arquivo final contém Parte A (do importado) + Parte M (gerada)
14. Teste valida que Parte M antiga do importado é completamente removida e substituída pela nova
15. Teste valida caso ECF importado não tem Parte M (M001 não existe): Parte M é adicionada ao final
16. Teste valida caso ECF importado tem Parte M antiga: tudo a partir de M001 é substituído
17. Teste valida que tentativa sem ECF importado retorna 400
18. Teste valida contagem total de linhas no arquivo final
19. Teste valida que `EcfFile.fileType = COMPLETE_ECF`
20. Teste valida relacionamentos source (`sourceImportedEcf` e `sourceMFile` estão preenchidos)

**Algoritmo de Substituição Simples:**
```
linhasEcfImportado = lerArquivo(ecfImportado)
linhasArquivoM = lerArquivo(arquivoM)

indexM001 = encontrarPrimeiraLinha(linhasEcfImportado, inicia com "|M001|")

se (indexM001 encontrado):
    parteA = linhasEcfImportado[0 até indexM001-1]  // Tudo antes de M001
senão:
    parteA = linhasEcfImportado  // Arquivo completo (não tinha Parte M)

ecfCompleto = parteA + linhasArquivoM
salvarArquivo(ecfCompleto)
```

**IMPORTANTE:** Não usa parser complexo. Apenas busca linha que inicia com `|M001|` e substitui tudo a partir dali.

---

## Story 5.6: Validação de Campos Obrigatórios (Arquivo M e ECF Completo)

Como desenvolvedor,
Eu quero validador que verifique campos obrigatórios de registros M conforme layout SPED,
Para que arquivos gerados não sejam rejeitados pelo validador PVA da RFB.

**Acceptance Criteria:**

1. Service `EcfValidatorService` criado
2. Método `validateMFile(String mFileContent)` retorna `ValidationResult`:
   - Valida que M001 existe e tem formato correto
   - Valida que cada M300 tem campos obrigatórios: movementType, description, amount > 0
   - Valida que M350 tem saldo inicial, saldo final
   - Valida que cada M400 tem campos obrigatórios análogos a M300
   - Valida que M410 tem saldo inicial, saldo final
   - Valida que M990 existe e contagem de registros está correta
3. Método `validateCompleteEcf(String completeEcfContent)` retorna `ValidationResult`:
   - Valida Parte A: registros 0000, J100, J150 existem (mínimo)
   - Valida Parte M: mesmas validações de `validateMFile`
   - Valida estrutura geral do arquivo ECF
4. DTO `ValidationResult`:
   - `boolean valid`
   - `List<String> errors` (lista de mensagens de erro)
   - `List<String> warnings` (lista de avisos não bloqueantes)
5. Use case `ValidateEcfFileUseCase` com método `validate(Long ecfFileId)`
6. Endpoint `POST /api/v1/ecf/{ecfFileId}/validate` (autenticado, requer X-Company-Id)
7. Response 200 OK com `ValidationResult`
8. Se `valid = false`, atualiza `EcfFile.status = ERROR` e `validationErrors = JSON(errors)`
9. Se `valid = true`, atualiza `EcfFile.status = VALIDATED`
10. Validação funciona para os 3 tipos de arquivo:
    - **M_FILE_ONLY**: valida apenas registros M
    - **IMPORTED_ECF**: valida Parte A + Parte M se houver
    - **COMPLETE_ECF**: valida Parte A + Parte M completa
11. Teste valida que arquivo M correto retorna `valid = true`
12. Teste valida que arquivo ECF completo correto retorna `valid = true`
13. Teste valida que arquivo com M300 sem description retorna erro: "M300: campo description é obrigatório"
14. Teste valida que arquivo com M990 com contagem errada retorna erro
15. Teste valida que status do EcfFile é atualizado corretamente (VALIDATED ou ERROR)

---

## Story 5.7: Download de Arquivos ECF (Três Tipos)

Como CONTADOR,
Eu quero fazer download dos arquivos ECF gerados ou importados,
Para que eu possa revisar, transmitir ao SPED ou compartilhar com cliente.

**Acceptance Criteria:**

1. Use case `DownloadEcfFileUseCase` criado com método `download(Long ecfFileId, Long companyId)`
2. Service implementa:
   - Busca `EcfFile` por ID e valida que pertence à companyId
   - Lê arquivo de storage
   - Retorna stream de bytes + metadados (fileName, contentType, fileSizeBytes)
3. Endpoint `GET /api/v1/ecf/{ecfFileId}/download` (autenticado, requer X-Company-Id)
4. Response com headers:
   - `Content-Type: text/plain; charset=UTF-8`
   - `Content-Disposition: attachment; filename="{fileName}"`
   - `Content-Length: {fileSizeBytes}`
5. Response 200 OK com arquivo .txt no body
6. Response 404 Not Found se arquivo não existe
7. Response 403 Forbidden se arquivo não pertence à empresa do CONTADOR
8. Validação: CONTADOR só pode baixar arquivos de sua empresa
9. Download funciona para os 3 tipos de arquivo:
   - **M_FILE_ONLY**: download do Arquivo M isolado
   - **IMPORTED_ECF**: download do ECF importado original
   - **COMPLETE_ECF**: download do ECF completo (Parte A + Parte M)
10. Teste valida download bem-sucedido de arquivo M_FILE_ONLY
11. Teste valida download bem-sucedido de arquivo IMPORTED_ECF
12. Teste valida download bem-sucedido de arquivo COMPLETE_ECF
13. Teste valida que tentativa de baixar arquivo de outra empresa retorna 403
14. Teste valida headers Content-Type e Content-Disposition corretos

---

## Story 5.8: Listagem de Arquivos ECF (Por Tipo)

Como CONTADOR,
Eu quero visualizar lista de arquivos ECF gerados/importados para uma empresa e ano fiscal,
Para que eu possa acompanhar histórico de gerações, uploads e downloads.

**Acceptance Criteria:**

1. Use case `ListEcfFilesUseCase` criado
2. Endpoint `GET /api/v1/ecf?fiscalYear=2024` (autenticado, requer X-Company-Id)
3. DTO `EcfFileListResponse`:
   - `id`, `fiscalYear`, `fileType`, `fileName`, `fileSizeBytes`
   - `status`, `createdAt`, `createdBy`
   - `validationErrors` (se status ERROR)
   - `sourceImportedEcfId`, `sourceMFileId` (se fileType COMPLETE_ECF)
4. Listagem agrupa arquivos por tipo:
   - `mFiles`: lista de arquivos tipo M_FILE_ONLY
   - `importedEcfs`: lista de arquivos tipo IMPORTED_ECF
   - `completeEcfs`: lista de arquivos tipo COMPLETE_ECF
5. Cada lista ordenada por `createdAt DESC` (mais recentes primeiro)
6. Suporta filtro por tipo: `?fileType=M_FILE_ONLY`
7. Suporta filtro por status: `?status=VALIDATED`
8. Response 200 OK com listas agrupadas por tipo
9. Validação: CONTADOR só visualiza arquivos de sua empresa
10. Response 403 Forbidden se tentar acessar arquivos de outra empresa
11. Teste valida listagem retorna arquivos separados por tipo
12. Teste valida que cada tipo está ordenado por data decrescente
13. Teste valida filtro por fileType funciona
14. Teste valida filtro por status funciona
15. Teste valida que CONTADOR só vê arquivos de sua empresa

---

## Story 5.9: Finalização de Arquivo ECF

Como CONTADOR,
Eu quero marcar arquivo ECF como finalizado,
Para que eu possa bloquear modificações posteriores e indicar que arquivo foi transmitido ao SPED.

**Acceptance Criteria:**

1. Use case `FinalizeEcfFileUseCase` criado
2. Endpoint `PATCH /api/v1/ecf/{ecfFileId}/finalize` (autenticado, requer X-Company-Id)
3. Service valida que arquivo está VALIDATED (não pode finalizar se status ERROR ou DRAFT)
4. Atualiza `EcfFile.status = FINALIZED`
5. Registra auditoria (updatedBy, updatedAt)
6. Finalização funciona para os 3 tipos de arquivo
7. DTO `FinalizeEcfFileResponse`: `success`, `message`, `newStatus`, `fileType`
8. Response 200 OK com confirmação
9. Response 400 Bad Request se arquivo não está VALIDATED: "Apenas arquivos validados podem ser finalizados"
10. Response 404 Not Found se arquivo não existe
11. Response 403 Forbidden se arquivo não pertence à empresa do CONTADOR
12. Teste valida finalização bem-sucedida de arquivo VALIDATED
13. Teste valida que tentativa de finalizar arquivo DRAFT retorna 400
14. Teste valida auditoria (updatedBy, updatedAt) está correta
15. Teste valida finalização para os 3 tipos de arquivo

---

## Story 5.10: Testes End-to-End do Fluxo de Geração ECF

Como desenvolvedor,
Eu quero testes E2E cobrindo fluxo completo de geração ECF com os três tipos de arquivo,
Para garantir que todo pipeline (geração M, upload ECF, merge, validação, download) funciona corretamente.

**Acceptance Criteria:**

1. Teste E2E `GenerateMFileFlowTest` (valida Story 5.3 - Geração do Arquivo M completo em uma única operação):
   - Setup: cria empresa, parâmetros, dados contábeis, movimentos fiscais (LALUR e LACS), cálculos IRPJ/CSLL
   - Executa: gera Arquivo M (POST /ecf/generate-m-file) → UM ÚNICO BOTÃO gera tudo
   - Valida: arquivo gerado existe em storage
   - Valida: `EcfFile` criado com tipo M_FILE_ONLY, status DRAFT
   - Valida: arquivo contém registros M001, M300, M350, M400, M410, M990 (todos gerados de uma só vez)
   - Valida: não foi necessário chamar endpoints separados para M300, M350, M400 (não existem)
   - Executa: valida arquivo (POST /ecf/{id}/validate)
   - Valida: status atualizado para VALIDATED
   - Executa: download (GET /ecf/{id}/download)
   - Valida: arquivo baixado é idêntico ao gerado
   - Valida: apenas registros M estão presentes (sem Parte A)
2. Teste E2E `UploadImportedEcfFlowTest`:
   - Setup: prepara arquivo ECF mock com Parte A (0000, J100, J150, J800) + Parte M antiga
   - Executa: upload (POST /ecf/upload-imported)
   - Valida: `EcfFile` criado com tipo IMPORTED_ECF, status DRAFT
   - Valida: arquivo salvo em storage
   - Valida: `lineCount` correto
   - Executa: download
   - Valida: arquivo baixado é idêntico ao uploadado
3. Teste E2E `GenerateCompleteEcfFlowTest` (valida Story 5.5 - Substituição Simples):
   - Setup: mesmo setup de GenerateMFileFlowTest + upload ECF importado
   - Executa: gera Arquivo M (POST /ecf/generate-m-file)
   - Executa: upload ECF importado com Parte A + Parte M antiga
   - Executa: gera ECF completo (POST /ecf/generate-complete) → usa algoritmo de substituição simples
   - Valida: arquivo gerado contém Parte A completa (do importado, tudo antes de |M001|)
   - Valida: arquivo gerado contém Parte M nova (gerada pelo sistema)
   - Valida: Parte M antiga foi completamente removida (tudo a partir de |M001| do importado foi descartado)
   - Valida: `EcfFile` criado com tipo COMPLETE_ECF, status DRAFT
   - Valida: `sourceImportedEcf` e `sourceMFile` estão preenchidos corretamente
   - Valida: não houve parsing complexo, apenas busca de linha |M001|
   - Executa: valida arquivo
   - Valida: status VALIDATED
   - Executa: finaliza (PATCH /ecf/{id}/finalize)
   - Valida: status FINALIZED
4. Teste E2E `ValidationErrorFlowTest`:
   - Setup: cria movimentos fiscais com description vazio (simula erro)
   - Executa: gera Arquivo M
   - Executa: valida
   - Valida: status ERROR, validationErrors contém mensagem de erro
   - Valida: tentativa de finalizar retorna 400 Bad Request
5. Teste E2E `ListAndDownloadMultipleFilesTest`:
   - Setup: gera 2 Arquivos M, faz upload de 1 ECF importado, gera 1 ECF completo
   - Executa: lista arquivos (GET /ecf?fiscalYear=2024)
   - Valida: retorna 2 M_FILE_ONLY, 1 IMPORTED_ECF, 1 COMPLETE_ECF agrupados por tipo
   - Valida: cada grupo ordenado por createdAt DESC
   - Executa: baixa cada arquivo
   - Valida: todos downloads bem-sucedidos
6. Teste E2E `SimpleReplacementTest` (valida algoritmo de substituição simples):
   - Setup: upload ECF importado com Parte A + Parte M antiga desatualizada
   - Setup: cria novos movimentos fiscais e recalcula IRPJ/CSLL
   - Executa: gera Arquivo M novo
   - Executa: gera ECF completo (substituição simples por busca de |M001|)
   - Valida: algoritmo encontrou corretamente a linha |M001| no ECF importado
   - Valida: Parte A foi extraída corretamente (todas linhas antes de |M001|)
   - Valida: ECF completo contém Parte M nova (registros refletem novos movimentos)
   - Valida: Parte M antiga foi completamente removida (não mesclada, substituída)
   - Valida: quantidade de linhas corretas (linhas Parte A + linhas Parte M nova)
7. Teste E2E `AppendWhenNoM001Test` (valida caso onde ECF importado não tem Parte M):
   - Setup: upload ECF importado contendo APENAS Parte A (sem |M001|)
   - Executa: gera Arquivo M
   - Executa: gera ECF completo
   - Valida: algoritmo não encontrou |M001|
   - Valida: Parte M foi adicionada ao final do arquivo (append)
   - Valida: ECF completo = ECF importado completo + Arquivo M
7. Todos testes usam TestContainers PostgreSQL
8. Todos testes criam contexto completo (usuário CONTADOR, empresa, X-Company-Id header)
9. Cobertura de código do Epic 5 deve ser >= 80%

---

## Resumo do Epic

Ao final deste épico, o sistema terá:

- Entidade `EcfFile` com suporte para **três tipos de arquivo**: M_FILE_ONLY, IMPORTED_ECF, COMPLETE_ECF
- **Serviço interno de geração de registros M** (Story 5.2): métodos para gerar M001, M300, M350, M400, M410, M990 (não são endpoints separados)
- **UM ÚNICO BOTÃO para gerar Arquivo M completo** (Story 5.3): endpoint único que gera todos os registros M de uma só vez em operação atômica
- **Arquivo M isolado (M_FILE_ONLY)**: contém apenas registros M gerados pelo sistema
- **Upload de ECF importado (IMPORTED_ECF)**: permite importar ECF completo de sistemas externos com validação básica de formato
- **Substituição simples de Parte M (COMPLETE_ECF)** (Story 5.5): busca linha |M001| no ECF importado e substitui tudo a partir dali pela Parte M gerada
- Validador de campos obrigatórios conforme layout SPED para os três tipos
- Endpoints de download para os três tipos de arquivo
- Listagem agrupada por tipo de arquivo
- Finalização de arquivos para bloqueio de modificações
- Rastreabilidade completa: ECF completo referencia arquivos source usados na substituição
- Testes E2E cobrindo fluxos completos de geração, upload, substituição simples, validação e download
- Isolamento multi-tenant via X-Company-Id
- Auditoria completa (createdBy, updatedBy)

**IMPORTANTE - Simplicidade:**
- **Geração M:** Não existem endpoints ou botões separados para gerar cada parte do arquivo M. Existe apenas UM ÚNICO ENDPOINT (`POST /api/v1/ecf/generate-m-file`) que gera o arquivo M completo de uma só vez.
- **Merge:** Não usa parser complexo. Usa algoritmo simples de substituição por busca de linha `|M001|`. Tudo antes de |M001| é preservado (Parte A), tudo a partir de |M001| é substituído pela Parte M nova gerada.

**Três Tipos de Arquivo ECF:**

1. **M_FILE_ONLY (Arquivo M isolado)**
   - Contém: Apenas registros M (M001, M300, M350, M400, M410, M990)
   - Gerado por: Sistema a partir de movimentações fiscais e cálculos (UM ÚNICO BOTÃO)
   - Uso: Revisão isolada da Parte M antes de merge, ou envio separado se necessário

2. **IMPORTED_ECF (ECF Importado)**
   - Contém: ECF completo importado (Parte A + Parte M antiga opcional)
   - Origem: Upload pelo usuário de arquivo gerado por sistema externo
   - Uso: Fornece Parte A para substituição, preserva backup do ECF original

3. **COMPLETE_ECF (ECF Completo)**
   - Contém: Parte A (do IMPORTED_ECF, tudo antes de |M001|) + Parte M (gerada pelo sistema, nova)
   - Gerado por: **Substituição simples** de IMPORTED_ECF + M_FILE_ONLY (busca |M001| e substitui)
   - Uso: Arquivo final pronto para transmissão SPED

**Dependências de Epics Anteriores:**
- Epic 1: Autenticação JWT, usuários CONTADOR
- Epic 2: Entidades Company e CompanyParameter
- Epic 3: CodigoEnquadramentoLalur
- Epic 4: Movimentações fiscais (FiscalMovement), TaxCalculationResult

**Próximos Passos (Epic 6):**
- Dashboard com visão geral de empresas
- Indicadores de completude (% dados preenchidos)
- Alertas de pendências e prazos
- Visualização de status de cálculos e arquivos ECF
