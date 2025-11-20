# Epic 1: Fundação & Autenticação Centralizada

**Objetivo do Epic:**

Estabelecer a infraestrutura técnica fundamental do projeto e implementar o sistema de autenticação e autorização que sustentará todas as funcionalidades subsequentes. Este épico entrega um backend operacional com Spring Boot 3.x, PostgreSQL, Docker, arquitetura hexagonal configurada, autenticação JWT funcional com dois roles (ADMIN e CONTADOR), e gestão centralizada de usuários onde apenas ADMIN pode criar/gerenciar contas. Ao final deste épico, teremos um sistema autenticado, testado, documentado (Swagger) e deployável via Docker, pronto para receber as funcionalidades de negócio.

---

## Story 1.1: Configuração Inicial do Projeto Spring Boot

Como desenvolvedor,
Eu quero um projeto Spring Boot 3.x configurado com Maven, estrutura hexagonal e dependências básicas,
Para que possamos iniciar o desenvolvimento com fundação técnica sólida e organizada.

**Acceptance Criteria:**

1. Projeto Maven criado com Java 21 e Spring Boot 3.x (versão estável mais recente)
2. Estrutura de pastas hexagonal implementada: `domain/`, `application/port/`, `infrastructure/adapter/`
3. Dependências configuradas no `pom.xml`: Spring Web, Spring Data JPA, Spring Security, Spring Validation, Lombok, MapStruct, PostgreSQL driver, Springdoc OpenAPI, JUnit 5, Mockito, TestContainers
4. Arquivo `application.yml` criado com profiles (dev, prod) e configurações básicas (porta 8080, context-path `/api/v1`)
5. Classe `EcfApplication.java` (main class) executável com `@SpringBootApplication`
6. Arquivo `.gitignore` configurado para Java/Maven/IntelliJ/Eclipse
7. README.md básico criado com instruções de setup e execução
8. Projeto compila com sucesso: `mvn clean package`
9. Aplicação inicia sem erros: `mvn spring-boot:run`

---

## Story 1.2: Configuração Docker & PostgreSQL

Como desenvolvedor,
Eu quero containerização Docker configurada com PostgreSQL 15+,
Para que possamos ter ambiente de desenvolvimento consistente e reproduzível.

**Acceptance Criteria:**

1. `Dockerfile` multi-stage criado usando `eclipse-temurin:21-jdk-alpine` para build e `eclipse-temurin:21-jre-alpine` para runtime
2. `docker-compose.yml` configurado com dois serviços: `postgres` (PostgreSQL 15-alpine) e `app` (aplicação Spring Boot)
3. PostgreSQL configurado com database `ecf_db`, user `ecf_user`, password via variável de ambiente
4. Volume Docker criado para persistência de dados PostgreSQL: `postgres_data`
5. Network bridge configurada: `ecf-network` para comunicação app ↔ DB
6. Variáveis de ambiente configuradas no service `app`: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_PROFILES_ACTIVE=dev`
7. Aplicação expõe porta 8080, PostgreSQL expõe porta 5432
8. Comando `docker-compose up` sobe ambos containers sem erros
9. Aplicação conecta com sucesso ao PostgreSQL (log confirma conexão)
10. Health check básico disponível: `GET /api/v1/actuator/health` retorna status UP

---

## Story 1.3: Configuração JPA e Auditoria Automática

Como desenvolvedor,
Eu quero JPA configurado com auditoria automática e entidade base reutilizável,
Para que todas entidades tenham campos de auditoria (createdAt, updatedAt, createdBy, updatedBy) e soft delete automaticamente.

**Acceptance Criteria:**

1. Configuração JPA no `application.yml`: `spring.jpa.hibernate.ddl-auto=update` (dev), `validate` (prod)
2. `@EnableJpaAuditing` configurado na classe de configuração
3. Interface `AuditorAware<String>` implementada retornando email do contexto Spring Security (ou "system" se não autenticado)
4. Classe abstrata `BaseEntity` criada com:
   - `@Id @GeneratedValue(strategy = IDENTITY) Long id`
   - `@Enumerated(STRING) Status status` (ACTIVE/INACTIVE) - default ACTIVE
   - `@CreatedDate LocalDateTime createdAt`
   - `@LastModifiedDate LocalDateTime updatedAt`
   - `@CreatedBy String createdBy`
   - `@LastModifiedBy String updatedBy`
5. Enum `Status` criado com valores ACTIVE e INACTIVE
6. Teste de integração valida que ao criar uma entidade de exemplo, campos de auditoria são populados automaticamente
7. Teste valida que ao atualizar entidade, `updatedAt` e `updatedBy` são atualizados

---

## Story 1.4: Entidade User e Repository

Como desenvolvedor,
Eu quero entidade User com repository JPA implementando port,
Para que possamos persistir usuários no banco de dados seguindo arquitetura hexagonal.

**Acceptance Criteria:**

1. Entidade JPA `UserEntity` criada em `infrastructure/adapter/out/persistence/entity/` estendendo `BaseEntity`:
   - `@Column(name="primeiro_nome", nullable=false) String firstName` (nome)
   - `@Column(name="sobrenome", nullable=false) String lastName` (sobrenome)
   - `@Column(nullable=false, unique=true) String email` (usado para login)
   - `@Column(name="senha", nullable=false) String password` (hash BCrypt)
   - `@Enumerated(STRING) @Column(name="funcao", nullable=false) UserRole role` (ADMIN ou CONTADOR)
   - `@Column(name="deve_mudar_senha", nullable=false) Boolean mustChangePassword` - default true
   - **Nota:** Tabela de banco = `tb_usuario`, colunas em snake_case conforme ADR-001
2. Enum `UserRole` criado com valores ADMIN e CONTADOR
3. Interface `UserRepositoryPort` criada em `application/port/out/`:
   - `Optional<User> findByEmail(String email)`
   - `User save(User user)`
   - `Optional<User> findById(Long id)`
   - `List<User> findAll()`
   - Métodos seguem convenção de domain objects (não JPA entities)
4. Interface `UserJpaRepository` criada estendendo `JpaRepository<UserEntity, Long>`:
   - `Optional<UserEntity> findByEmail(String email)`
5. Classe `UserRepositoryAdapter` implementa `UserRepositoryPort` usando `UserJpaRepository` e MapStruct para conversão Entity ↔ Domain
6. Model `User` (domain) criado em `domain/model/` como POJO puro sem annotations JPA
7. Mapper MapStruct `UserMapper` criado para conversão `UserEntity` ↔ `User`
8. Teste de integração (TestContainers) valida:
   - Salvar usuário e recuperar por email
   - Unique constraint em email (tentativa de duplicata lança exception)
   - Soft delete funciona corretamente (status INACTIVE)

---

## Story 1.5: Configuração Spring Security & JWT

Como desenvolvedor,
Eu quero Spring Security configurado com autenticação JWT,
Para que endpoints protegidos exijam token válido e role apropriado.

**Acceptance Criteria:**

1. Dependência `jjwt` (Java JWT) adicionada ao `pom.xml`
2. Classe `JwtTokenProvider` criada em `infrastructure/security/`:
   - `generateAccessToken(String email, UserRole role)` - expira em 15min
   - `generateRefreshToken(String email)` - expira em 7 dias
   - `validateToken(String token)` - retorna boolean
   - `getEmailFromToken(String token)`
   - `getRoleFromToken(String token)`
   - Secret key configurável via variável de ambiente `JWT_SECRET`
3. Classe `JwtAuthenticationFilter` (extends OncePerRequestFilter) criada:
   - Extrai token do header `Authorization: Bearer {token}`
   - Valida token via `JwtTokenProvider`
   - Popula `SecurityContext` com `UsernamePasswordAuthenticationToken`
4. Classe `SecurityConfig` criada com:
   - `@EnableWebSecurity` e `@EnableMethodSecurity`
   - BCryptPasswordEncoder bean (strength 12)
   - SecurityFilterChain configurado:
     - `/api/v1/auth/**` permitAll
     - `/api/v1/actuator/health` permitAll
     - `/swagger-ui/**`, `/v3/api-docs/**` permitAll
     - Todos outros endpoints require authentication
   - `JwtAuthenticationFilter` adicionado antes de UsernamePasswordAuthenticationFilter
5. Endpoint de health check `/api/v1/actuator/health` acessível sem autenticação
6. Endpoints protegidos retornam 401 Unauthorized se token ausente/inválido
7. Teste valida que token válido permite acesso a endpoints protegidos
8. Teste valida que token expirado é rejeitado

---

## Story 1.6: Endpoint de Login (Autenticação)

Como usuário (ADMIN ou CONTADOR),
Eu quero fazer login com email e senha,
Para que eu receba tokens JWT (access + refresh) para acessar o sistema.

**Acceptance Criteria:**

1. Controller `AuthController` criado em `infrastructure/adapter/in/rest/` com endpoint `POST /api/v1/auth/login`
2. DTO `LoginRequest` criado: `email` (obrigatório), `password` (obrigatório)
3. DTO `LoginResponse` criado: `accessToken`, `refreshToken`, `email`, `firstName`, `lastName`, `role`, `mustChangePassword`
4. Use case `AuthenticateUserUseCase` (port in) e implementação `AuthService` criados:
   - Busca usuário por email
   - Valida senha usando BCryptPasswordEncoder
   - Gera access token e refresh token
   - Retorna tokens + dados do usuário + flag `mustChangePassword`
5. Response retorna status 200 OK com tokens se credenciais válidas
6. Response retorna status 401 Unauthorized com mensagem "Credenciais inválidas" se email ou senha incorretos
7. Response retorna status 400 Bad Request se campos obrigatórios ausentes (validação Bean Validation)
8. Teste de API valida login bem-sucedido retorna tokens válidos e dados do usuário
9. Teste valida login com senha incorreta retorna 401
10. Teste valida login com email inexistente retorna 401

---

## Story 1.7: Endpoint de Troca de Senha Obrigatória

Como usuário,
Eu quero trocar minha senha quando obrigado (primeiro acesso ou reset por ADMIN),
Para que eu possa acessar o sistema com minha própria senha segura.

**Acceptance Criteria:**

1. Endpoint `POST /api/v1/auth/change-password` criado (autenticado)
2. DTO `ChangePasswordRequest`: `currentPassword` (obrigatório), `newPassword` (obrigatório, mín 8 caracteres)
3. DTO `ChangePasswordResponse`: `success` (boolean), `message`
4. Use case `ChangePasswordUseCase` implementado:
   - Valida senha atual do usuário autenticado
   - Valida nova senha (mínimo 8 caracteres, não pode ser igual à atual)
   - Faz hash BCrypt da nova senha
   - Atualiza senha e seta `mustChangePassword = false`
5. Response 200 OK com `{"success": true, "message": "Senha alterada com sucesso"}`
6. Response 400 Bad Request se senha atual incorreta: `{"success": false, "message": "Senha atual inválida"}`
7. Response 400 Bad Request se nova senha não atende requisitos
8. Teste valida troca de senha bem-sucedida atualiza `mustChangePassword` para false
9. Teste valida que senha atual incorreta é rejeitada
10. Teste valida que nova senha muito curta (< 8 chars) é rejeitada

---

## Story 1.8: CRUD de Usuários (ADMIN apenas)

Como ADMIN,
Eu quero criar, listar, visualizar, editar e inativar usuários,
Para que eu possa gerenciar centralizadamente todos os acessos ao sistema.

**Acceptance Criteria:**

1. Controller `UserController` criado com endpoints:
   - `POST /api/v1/users` - criar usuário (ADMIN only)
   - `GET /api/v1/users` - listar usuários com paginação (ADMIN only)
   - `GET /api/v1/users/{id}` - visualizar usuário (ADMIN only)
   - `PUT /api/v1/users/{id}` - editar usuário (ADMIN only)
   - `PATCH /api/v1/users/{id}/status` - alternar status do usuário (ativar/inativar, ADMIN only)
2. DTOs criados: `CreateUserRequest`, `UpdateUserRequest`, `UserResponse`
3. `CreateUserRequest`: `firstName`, `lastName`, `email`, `password`, `role` (ADMIN ou CONTADOR)
4. `UserResponse`: `id`, `firstName`, `lastName`, `email`, `role`, `status`, `mustChangePassword`, `createdAt`, `updatedAt`
5. Use cases implementados: `CreateUserUseCase`, `ListUsersUseCase`, `GetUserUseCase`, `UpdateUserUseCase`, `ToggleUserStatusUseCase`
6. Ao criar usuário: senha é hashada com BCrypt, `mustChangePassword` setado para true
7. Todos endpoints protegidos com `@PreAuthorize("hasRole('ADMIN')")`
8. Listagem suporta paginação: `?page=0&size=50&sort=createdAt,desc`
9. Listagem suporta busca por nome: `?search=João` (busca em firstName e lastName)
10. Listagem por padrão retorna apenas usuários ACTIVE, aceita `?include_inactive=true`
11. DTO adicional `ToggleStatusRequest`: `status` (obrigatório, enum: ACTIVE ou INACTIVE)
12. DTO `ToggleStatusResponse`: `success` (boolean), `message`, `newStatus`
13. Toggle status alterna entre ACTIVE e INACTIVE (soft delete)
14. Response 403 Forbidden se CONTADOR tentar acessar endpoints
15. Teste valida que ADMIN consegue criar usuário com firstName e lastName
16. Teste valida que email duplicado retorna 400 Bad Request
17. Teste valida que CONTADOR recebe 403 ao tentar criar usuário
18. Teste valida toggle status: ACTIVE → INACTIVE funciona
19. Teste valida toggle status: INACTIVE → ACTIVE funciona
20. Teste valida que usuário inativado não aparece na listagem padrão
21. Teste valida que usuário inativado aparece com include_inactive=true

---

## Story 1.9: Endpoint de Reset de Senha (ADMIN)

Como ADMIN,
Eu quero redefinir a senha de um usuário,
Para que o usuário receba nova senha temporária e seja obrigado a trocá-la no próximo login.

**Acceptance Criteria:**

1. Endpoint `POST /api/v1/users/{id}/reset-password` criado (ADMIN only)
2. DTO `ResetPasswordRequest`: `temporaryPassword` (obrigatório, mín 8 caracteres)
3. DTO `ResetPasswordResponse`: `success`, `message`
4. Use case `ResetUserPasswordUseCase` implementado:
   - Valida que usuário existe e está ACTIVE
   - Atualiza senha (hash BCrypt da senha temporária)
   - Seta `mustChangePassword = true`
   - Registra em auditoria (updatedBy = ADMIN email)
5. Response 200 OK: `{"success": true, "message": "Senha redefinida. Usuário deve trocar no próximo login."}`
6. Response 404 Not Found se usuário não existe
7. Response 400 Bad Request se senha temporária não atende requisitos
8. Endpoint protegido com `@PreAuthorize("hasRole('ADMIN')")`
9. Teste valida que após reset, `mustChangePassword` é true
10. Teste valida que CONTADOR não pode resetar senhas (403)

---

## Story 1.10: Configuração Swagger/OpenAPI

Como desenvolvedor ou consumidor da API,
Eu quero documentação Swagger interativa,
Para que eu possa explorar e testar endpoints facilmente.

**Acceptance Criteria:**

1. Dependência `springdoc-openapi-starter-webmvc-ui` adicionada ao `pom.xml`
2. Classe `OpenApiConfig` criada com:
   - `@OpenAPIDefinition` com título "Sistema ECF - API", versão "1.0", descrição
   - Configuração de security scheme JWT: `@SecurityScheme(type=HTTP, scheme=bearer, bearerFormat=JWT)`
3. Controllers anotados com `@Tag` para agrupamento (ex: `@Tag(name = "Authentication")`)
4. Endpoints anotados com `@Operation` descrevendo funcionalidade
5. DTOs anotados com `@Schema` para documentação de campos
6. Swagger UI acessível em `http://localhost:8080/swagger-ui.html`
7. OpenAPI JSON disponível em `http://localhost:8080/v3/api-docs`
8. Swagger UI exibe corretamente todos endpoints criados até agora
9. Botão "Authorize" permite inserir JWT token para testar endpoints protegidos
10. Teste manual: conseguir fazer login via Swagger UI e usar token para acessar endpoint protegido

---

## Story 1.11: CI/CD GitHub Actions Básico

Como desenvolvedor,
Eu quero pipeline CI/CD no GitHub Actions,
Para que commits sejam validados automaticamente (build + testes).

**Acceptance Criteria:**

1. Arquivo `.github/workflows/ci.yml` criado com:
   - Trigger em push para `main` e `develop`, e em pull requests
   - Job `build`: checkout, setup Java 21, `mvn clean package -DskipTests`
   - Job `test`: `mvn test`, upload de JaCoCo coverage report como artifact
   - Job `docker`: build da imagem Docker (apenas se branch `main`)
2. Pipeline executa em runner `ubuntu-latest`
3. Secrets configurados no repositório: `JWT_SECRET`
4. Build falha se compilação falhar
5. Build falha se cobertura de testes < 70% (configuração JaCoCo no `pom.xml`)
6. Artefatos gerados: JAR, relatório de coverage
7. Badge de status do pipeline adicionado ao README.md
8. Teste: fazer push em branch `develop` e validar que pipeline executa com sucesso

---

## 📋 Ajustes de Nomenclatura (ADR-001)

**Referência:** [ADR-001: Simplificação do Modelo de Dados](../architecture/adr-001-simplificacao-modelo-dados.md)

Este épico foi atualizado para refletir a decisão arquitetural de usar **snake_case** nas colunas de banco de dados.

### Mudanças Aplicadas

**Story 1.4 (User Entity):**
- Tabela: `tb_usuario` (não `users` ou `user`)
- Mapeamento de colunas adicionado:
  - `firstName` → `primeiro_nome`
  - `lastName` → `sobrenome`
  - `password` → `senha`
  - `role` → `funcao`
  - `mustChangePassword` → `deve_mudar_senha`

**Story 1.3 (Base Entity):**
- Colunas de auditoria seguem snake_case (configuradas em `BaseEntity`):
  - `createdAt` → `criado_em`
  - `updatedAt` → `atualizado_em`
  - `createdBy` → `criado_por`
  - `updatedBy` → `atualizado_por`

### Impacto

✅ **MÍNIMO** - Apenas ajustes de `@Column(name="...")` nas entities JPA.

Lógica de negócio, testes e endpoints **não são afetados**.
