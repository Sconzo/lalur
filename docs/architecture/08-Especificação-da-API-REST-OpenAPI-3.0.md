Especificação-da-API-REST-OpenAPI-3.0

```yaml
openapi: 3.0.0
info:
  title: Sistema LALUR V2 ECF - API
  version: 1.0.0
  description: |
    API REST para cálculo de IRPJ/CSLL e geração de arquivos ECF para empresas no regime de Lucro Real.

    **Autenticação:** JWT Bearer token (header `Authorization: Bearer <token>`)

    **Multi-tenancy:** Header `X-Company-Id` obrigatório para role CONTADOR

    **Convenções:**
    - JSON camelCase
    - Timestamps ISO-8601 (UTC)
    - Erros RFC 7807 (Problem Details)

  contact:
    name: Suporte Técnico
    email: suporte@lalurecf.com.br

servers:
  - url: http://localhost:8080/api/v1
    description: Desenvolvimento Local
  - url: https://api-staging.lalurecf.com.br/api/v1
    description: Staging (Futuro)
  - url: https://api.lalurecf.com.br/api/v1
    description: Produção (Futuro)

tags:
  - name: Authentication
    description: Login e gestão de tokens JWT
  - name: Users
    description: Gestão de usuários (ADMIN only)
  - name: Companies
    description: Gestão de empresas
  - name: Chart of Accounts
    description: Plano de contas
  - name: Accounting Data
    description: Dados contábeis (balancetes)
  - name: Tax Parameters
    description: Parâmetros fiscais globais (ADMIN only)
  - name: Fiscal Movements
    description: Movimentos LALUR/LACS
  - name: Calculations
    description: Cálculos IRPJ/CSLL
  - name: ECF
    description: Geração e validação de ECF

security:
  - BearerAuth: []

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |
        JWT token obtido via `/auth/login`.

        Access token expira em 15 minutos.
        Refresh token expira em 7 dias.

  parameters:
    CompanyIdHeader:
      name: X-Company-Id
      in: header
      required: true
      description: ID da empresa (obrigatório para CONTADOR)
      schema:
        type: integer
        format: int64
        example: 42

    PageParam:
      name: page
      in: query
      description: Número da página (0-indexed)
      schema:
        type: integer
        default: 0
        minimum: 0

    SizeParam:
      name: size
      in: query
      description: Tamanho da página
      schema:
        type: integer
        default: 50
        minimum: 1
        maximum: 100

    SortParam:
      name: sort
      in: query
      description: Ordenação (campo,direção)
      schema:
        type: string
        example: "createdAt,desc"

  schemas:
    # ========== Authentication Schemas ==========
    LoginRequest:
      type: object
      required: [email, password]
      properties:
        email:
          type: string
          format: email
          example: "contador@empresa.com.br"
        password:
          type: string
          format: password
          minLength: 8
          example: "SenhaSegura123"

    LoginResponse:
      type: object
      properties:
        accessToken:
          type: string
          example: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        refreshToken:
          type: string
          example: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        email:
          type: string
          example: "contador@empresa.com.br"
        firstName:
          type: string
          example: "João"
        lastName:
          type: string
          example: "Silva"
        role:
          type: string
          enum: [ADMIN, CONTADOR]
          example: "CONTADOR"
        mustChangePassword:
          type: boolean
          example: false

    ChangePasswordRequest:
      type: object
      required: [currentPassword, newPassword]
      properties:
        currentPassword:
          type: string
          format: password
        newPassword:
          type: string
          format: password
          minLength: 8

    ChangePasswordResponse:
      type: object
      properties:
        success:
          type: boolean
        message:
          type: string
          example: "Senha alterada com sucesso"

    # ========== User Schemas ==========
    CreateUserRequest:
      type: object
      required: [firstName, lastName, email, password, role]
      properties:
        firstName:
          type: string
          minLength: 2
          maxLength: 100
          example: "Maria"
        lastName:
          type: string
          minLength: 2
          maxLength: 100
          example: "Santos"
        email:
          type: string
          format: email
          example: "maria@empresa.com.br"
        password:
          type: string
          format: password
          minLength: 8
          example: "SenhaTemporaria123"
        role:
          type: string
          enum: [ADMIN, CONTADOR]
          example: "CONTADOR"

    UserResponse:
      type: object
      properties:
        id:
          type: integer
          format: int64
          example: 1
        firstName:
          type: string
          example: "Maria"
        lastName:
          type: string
          example: "Santos"
        email:
          type: string
          example: "maria@empresa.com.br"
        role:
          type: string
          enum: [ADMIN, CONTADOR]
        status:
          type: string
          enum: [ACTIVE, INACTIVE]
        mustChangePassword:
          type: boolean
        lastLoginAt:
          type: string
          format: date-time
          nullable: true
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    ToggleStatusRequest:
      type: object
      required: [status]
      properties:
        status:
          type: string
          enum: [ACTIVE, INACTIVE]

    ResetPasswordRequest:
      type: object
      required: [temporaryPassword]
      properties:
        temporaryPassword:
          type: string
          format: password
          minLength: 8

    # ========== Company Schemas ==========
    CreateCompanyRequest:
      type: object
      required: [cnpj, razaoSocial, regimeTributario, periodoContabil]
      properties:
        cnpj:
          type: string
          pattern: '^\d{14}$'
          example: "12345678000195"
        razaoSocial:
          type: string
          minLength: 3
          maxLength: 255
          example: "EMPRESA TESTE LTDA"
        nomeFantasia:
          type: string
          maxLength: 255
          example: "Empresa Teste"
        regimeTributario:
          type: string
          enum: [LUCRO_REAL, LUCRO_PRESUMIDO, SIMPLES_NACIONAL]
          example: "LUCRO_REAL"
        periodoContabil:
          type: string
          pattern: '^\d{4}-(0[1-9]|1[0-2])$'
          example: "2024-01"

    CompanyResponse:
      type: object
      properties:
        id:
          type: integer
          format: int64
        cnpj:
          type: string
        razaoSocial:
          type: string
        nomeFantasia:
          type: string
        regimeTributario:
          type: string
        periodoContabil:
          type: string
        status:
          type: string
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    CnpjSearchResponse:
      type: object
      properties:
        cnpj:
          type: string
        razaoSocial:
          type: string
        nomeFantasia:
          type: string
        situacaoCadastral:
          type: string
        municipio:
          type: string
        uf:
          type: string
        source:
          type: string
          enum: [BRASIL_API, RECEITA_WS]

    # ========== Calculation Schemas ==========
    CalculateIrpjRequest:
      type: object
      required: [fiscalYear]
      properties:
        fiscalYear:
          type: integer
          minimum: 2000
          maximum: 2100
          example: 2024

    TaxCalculationResponse:
      type: object
      properties:
        calculationId:
          type: integer
          format: int64
        calculationType:
          type: string
          enum: [IRPJ, CSLL]
        fiscalYear:
          type: integer
        baseCalculationAmount:
          type: number
          format: double
          example: 530000.00
        totalTaxDue:
          type: number
          format: double
          example: 108500.00
        calculationMemory:
          type: string
          description: JSON string com steps detalhados
        isOutdated:
          type: boolean
        calculatedBy:
          type: string
        calculatedAt:
          type: string
          format: date-time

    OutdatedCalculationsResponse:
      type: object
      properties:
        companyId:
          type: integer
          format: int64
        outdatedCalculations:
          type: array
          items:
            type: object
            properties:
              calculationType:
                type: string
              fiscalYear:
                type: integer
              isOutdated:
                type: boolean
              lastCalculatedAt:
                type: string
                format: date-time

    # ========== ECF Schemas ==========
    GenerateMFileRequest:
      type: object
      required: [fiscalYear]
      properties:
        fiscalYear:
          type: integer
          example: 2024

    GenerateMFileResponse:
      type: object
      properties:
        ecfFileId:
          type: integer
          format: int64
        fileName:
          type: string
          example: "M_2024_empresa42.txt"
        recordCount:
          type: integer
          example: 10
        fileSizeBytes:
          type: integer
          format: int64

    GenerateCompleteEcfRequest:
      type: object
      required: [fiscalYear]
      properties:
        fiscalYear:
          type: integer

    ValidateEcfResponse:
      type: object
      properties:
        ecfFileId:
          type: integer
          format: int64
        validationStatus:
          type: string
          enum: [VALID, INVALID]
        errors:
          type: array
          items:
            type: object
            properties:
              line:
                type: integer
              field:
                type: string
              message:
                type: string

    EcfFileResponse:
      type: object
      properties:
        id:
          type: integer
          format: int64
        companyId:
          type: integer
          format: int64
        fileType:
          type: string
          enum: [IMPORTED_ECF, GENERATED_M_FILE, COMPLETE_ECF]
        fileName:
          type: string
        fiscalYear:
          type: integer
        recordCount:
          type: integer
        validationStatus:
          type: string
        createdAt:
          type: string
          format: date-time

    # ========== Common Schemas ==========
    ErrorResponse:
      type: object
      properties:
        type:
          type: string
          example: "https://api.lalurecf.com.br/errors/validation-error"
        title:
          type: string
          example: "Validation Error"
        status:
          type: integer
          example: 400
        detail:
          type: string
          example: "CNPJ inválido"
        instance:
          type: string
          example: "/api/v1/companies"
        timestamp:
          type: string
          format: date-time
        correlationId:
          type: string
          format: uuid
        errors:
          type: array
          items:
            type: object
            properties:
              field:
                type: string
              message:
                type: string

    PageResponse:
      type: object
      properties:
        content:
          type: array
          items:
            type: object
        totalElements:
          type: integer
          format: int64
        totalPages:
          type: integer
        size:
          type: integer
        number:
          type: integer

paths:
  # ========== Authentication Endpoints ==========
  /auth/login:
    post:
      tags: [Authentication]
      summary: Login com email e senha
      description: Autentica usuário e retorna tokens JWT (access + refresh)
      operationId: login
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginRequest'
      responses:
        '200':
          description: Login bem-sucedido
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LoginResponse'
        '401':
          description: Credenciais inválidas ou conta bloqueada
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '400':
          description: Dados de entrada inválidos
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /auth/change-password:
    post:
      tags: [Authentication]
      summary: Trocar senha
      description: Troca senha do usuário autenticado (obrigatório em primeiro acesso)
      operationId: changePassword
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChangePasswordRequest'
      responses:
        '200':
          description: Senha alterada com sucesso
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ChangePasswordResponse'
        '400':
          description: Senha atual incorreta ou nova senha inválida
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  # ========== User Endpoints ==========
  /users:
    post:
      tags: [Users]
      summary: Criar usuário (ADMIN only)
      description: ADMIN cria novo usuário (ADMIN ou CONTADOR)
      operationId: createUser
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUserRequest'
      responses:
        '201':
          description: Usuário criado com sucesso
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '400':
          description: Email duplicado ou dados inválidos
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '403':
          description: Acesso negado (apenas ADMIN)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

    get:
      tags: [Users]
      summary: Listar usuários (ADMIN only)
      description: Lista usuários com paginação e busca
      operationId: listUsers
      security:
        - BearerAuth: []
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
        - $ref: '#/components/parameters/SortParam'
        - name: search
          in: query
          description: Buscar por nome (firstName ou lastName)
          schema:
            type: string
        - name: includeInactive
          in: query
          description: Incluir usuários inativos
          schema:
            type: boolean
            default: false
      responses:
        '200':
          description: Lista de usuários
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageResponse'
        '403':
          description: Acesso negado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /users/{id}:
    get:
      tags: [Users]
      summary: Visualizar usuário (ADMIN only)
      operationId: getUser
      security:
        - BearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Detalhes do usuário
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '404':
          description: Usuário não encontrado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

    put:
      tags: [Users]
      summary: Editar usuário (ADMIN only)
      operationId: updateUser
      security:
        - BearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUserRequest'
      responses:
        '200':
          description: Usuário atualizado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '404':
          description: Usuário não encontrado

  /users/{id}/status:
    patch:
      tags: [Users]
      summary: Alternar status do usuário (ADMIN only)
      description: Ativar ou inativar usuário (soft delete)
      operationId: toggleUserStatus
      security:
        - BearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ToggleStatusRequest'
      responses:
        '200':
          description: Status alterado com sucesso

  /users/{id}/reset-password:
    post:
      tags: [Users]
      summary: Resetar senha (ADMIN only)
      description: ADMIN define senha temporária e força troca no próximo login
      operationId: resetUserPassword
      security:
        - BearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ResetPasswordRequest'
      responses:
        '200':
          description: Senha resetada com sucesso
        '404':
          description: Usuário não encontrado

  # ========== Company Endpoints ==========
  /companies:
    post:
      tags: [Companies]
      summary: Criar empresa
      description: Cria empresa com validação CNPJ via BrasilAPI
      operationId: createCompany
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateCompanyRequest'
      responses:
        '201':
          description: Empresa criada
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CompanyResponse'
        '400':
          description: CNPJ inválido ou duplicado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

    get:
      tags: [Companies]
      summary: Listar empresas
      description: ADMIN lista todas, CONTADOR lista apenas suas empresas
      operationId: listCompanies
      security:
        - BearerAuth: []
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
      responses:
        '200':
          description: Lista de empresas
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageResponse'

  /companies/{id}:
    get:
      tags: [Companies]
      summary: Visualizar empresa
      operationId: getCompany
      security:
        - BearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Detalhes da empresa
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CompanyResponse'
        '404':
          description: Empresa não encontrada

  /companies/search-cnpj/{cnpj}:
    get:
      tags: [Companies]
      summary: Buscar CNPJ em APIs externas
      description: Busca dados de CNPJ via BrasilAPI (fallback ReceitaWS)
      operationId: searchCnpj
      security:
        - BearerAuth: []
      parameters:
        - name: cnpj
          in: path
          required: true
          schema:
            type: string
            pattern: '^\d{14}$'
      responses:
        '200':
          description: Dados do CNPJ
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CnpjSearchResponse'
        '404':
          description: CNPJ não encontrado
        '503':
          description: APIs externas indisponíveis

  # ========== Calculation Endpoints ==========
  /companies/{companyId}/calculations/irpj:
    post:
      tags: [Calculations]
      summary: Calcular IRPJ
      description: Calcula IRPJ (15% base + 10% adicional) para ano fiscal
      operationId: calculateIrpj
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CalculateIrpjRequest'
      responses:
        '200':
          description: Cálculo realizado com sucesso
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TaxCalculationResponse'
        '400':
          description: Pré-requisitos não atendidos (dados contábeis ausentes)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /companies/{companyId}/calculations/csll:
    post:
      tags: [Calculations]
      summary: Calcular CSLL
      description: Calcula CSLL (9%) para ano fiscal
      operationId: calculateCsll
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CalculateIrpjRequest'
      responses:
        '200':
          description: Cálculo realizado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TaxCalculationResponse'

  /companies/{companyId}/calculations/recalculate-all:
    post:
      tags: [Calculations]
      summary: Recalcular todos cálculos desatualizados
      description: Recalcula IRPJ e CSLL marcados como outdated
      operationId: recalculateAll
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      responses:
        '200':
          description: Recálculo concluído
          content:
            application/json:
              schema:
                type: object
                properties:
                  recalculatedCount:
                    type: integer

  /companies/{companyId}/calculations/outdated-status:
    get:
      tags: [Calculations]
      summary: Listar cálculos desatualizados
      description: Retorna quais cálculos precisam ser recalculados
      operationId: getOutdatedCalculations
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      responses:
        '200':
          description: Status de cálculos
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OutdatedCalculationsResponse'

  # ========== ECF Endpoints ==========
  /companies/{companyId}/ecf/upload-imported:
    post:
      tags: [ECF]
      summary: Upload ECF importado (Parte A)
      description: Faz upload do arquivo ECF gerado por sistema externo
      operationId: uploadImportedEcf
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
                fiscalYear:
                  type: integer
      responses:
        '200':
          description: Arquivo importado com sucesso
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EcfFileResponse'

  /companies/{companyId}/ecf/generate-m-file:
    post:
      tags: [ECF]
      summary: Gerar Arquivo M (Parte M)
      description: Gera arquivo M completo (M001 + M300 + M350 + M400 + M410 + M990) em operação atômica
      operationId: generateMFile
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GenerateMFileRequest'
      responses:
        '200':
          description: Arquivo M gerado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GenerateMFileResponse'
        '400':
          description: Cálculos IRPJ/CSLL não encontrados

  /companies/{companyId}/ecf/generate-complete:
    post:
      tags: [ECF]
      summary: Gerar ECF Completo
      description: Merge de ECF importado (Parte A) + Arquivo M (Parte M) - substituição simples em |M001|
      operationId: generateCompleteEcf
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GenerateCompleteEcfRequest'
      responses:
        '200':
          description: ECF completo gerado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EcfFileResponse'
        '400':
          description: ECF importado ou Arquivo M não encontrados

  /companies/{companyId}/ecf/{fileId}/validate:
    post:
      tags: [ECF]
      summary: Validar ECF
      description: Executa validação interna do ECF (~80% das regras do SPED PVA)
      operationId: validateEcf
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - name: fileId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      responses:
        '200':
          description: Validação concluída
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ValidateEcfResponse'

  /companies/{companyId}/ecf/{fileId}/download:
    get:
      tags: [ECF]
      summary: Download de arquivo ECF
      description: Baixa arquivo ECF gerado (qualquer tipo)
      operationId: downloadEcf
      security:
        - BearerAuth: []
      parameters:
        - name: companyId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - name: fileId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - $ref: '#/components/parameters/CompanyIdHeader'
      responses:
        '200':
          description: Arquivo ECF
          content:
            application/octet-stream:
              schema:
                type: string
                format: binary
        '404':
          description: Arquivo não encontrado
```

---

