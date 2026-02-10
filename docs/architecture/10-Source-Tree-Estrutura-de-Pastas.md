Source-Tree-Estrutura-de-Pastas

### Estrutura Completa do Projeto

```
LALUR-V2-ECF/
│
├── .github/
│   └── workflows/
│       └── ci.yml                          # GitHub Actions CI/CD
│
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/
│   │   │       └── com/
│   │   │           └── lalurecf/
│   │   │               │
│   │   │               ├── EcfApplication.java              # Main class @SpringBootApplication
│   │   │               │
│   │   │               ├── domain/                          # CAMADA DE DOMÍNIO (core business)
│   │   │               │   ├── model/                       # Entidades de domínio (POJOs puros)
│   │   │               │   │   ├── User.java
│   │   │               │   │   ├── Company.java
│   │   │               │   │   ├── PlanoDeContas.java
│   │   │               │   │   ├── AccountingData.java
│   │   │               │   │   ├── TaxParameter.java
│   │   │               │   │   ├── FiscalMovement.java
│   │   │               │   │   ├── TaxCalculationResult.java
│   │   │               │   │   └── EcfFile.java
│   │   │               │   │
│   │   │               │   ├── enums/                       # Enumerações de domínio
│   │   │               │   │   ├── UserRole.java            # ADMIN, CONTADOR
│   │   │               │   │   ├── Status.java              # ACTIVE, INACTIVE
│   │   │               │   │   ├── RegimeTributario.java    # LUCRO_REAL, LUCRO_PRESUMIDO, etc
│   │   │               │   │   ├── AccountType.java         # ATIVO, PASSIVO, etc
│   │   │               │   │   ├── MovementType.java        # LALUR, LACS
│   │   │               │   │   ├── Classification.java      # ADICAO, EXCLUSAO
│   │   │               │   │   ├── CalculationType.java     # IRPJ, CSLL
│   │   │               │   │   ├── EcfFileType.java         # IMPORTED_ECF, GENERATED_M_FILE, COMPLETE_ECF
│   │   │               │   │   └── ValidationStatus.java    # NOT_VALIDATED, VALID, INVALID
│   │   │               │   │
│   │   │               │   ├── exception/                   # Domain exceptions
│   │   │               │   │   ├── DomainException.java
│   │   │               │   │   ├── InvalidPeriodoContabilException.java
│   │   │               │   │   ├── CalculationPrerequisitesNotMetException.java
│   │   │               │   │   └── EcfValidationException.java
│   │   │               │   │
│   │   │               │   └── service/                     # Domain services (calculators)
│   │   │               │       ├── calculator/
│   │   │               │       │   ├── IrpjCalculator.java  # Pure domain logic - cálculo IRPJ
│   │   │               │       │   └── CsllCalculator.java  # Pure domain logic - cálculo CSLL
│   │   │               │       │
│   │   │               │       └── validator/
│   │   │               │           ├── CnpjValidator.java
│   │   │               │           └── EcfFileValidator.java
│   │   │               │
│   │   │               ├── application/                     # CAMADA DE APLICAÇÃO (orquestração)
│   │   │               │   │
│   │   │               │   ├── port/
│   │   │               │   │   ├── in/                      # Ports IN (use cases)
│   │   │               │   │   │   ├── auth/
│   │   │               │   │   │   │   ├── AuthenticateUserUseCase.java
│   │   │               │   │   │   │   └── ChangePasswordUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   ├── user/
│   │   │               │   │   │   │   ├── CreateUserUseCase.java
│   │   │               │   │   │   │   ├── ListUsersUseCase.java
│   │   │               │   │   │   │   ├── GetUserUseCase.java
│   │   │               │   │   │   │   ├── UpdateUserUseCase.java
│   │   │               │   │   │   │   ├── ToggleUserStatusUseCase.java
│   │   │               │   │   │   │   └── ResetUserPasswordUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   ├── company/
│   │   │               │   │   │   │   ├── CreateCompanyUseCase.java
│   │   │               │   │   │   │   ├── SearchCnpjUseCase.java
│   │   │               │   │   │   │   ├── UpdatePeriodoContabilUseCase.java
│   │   │               │   │   │   │   └── ListCompaniesUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   ├── chartofaccount/
│   │   │               │   │   │   │   ├── ImportPlanoDeContassUseCase.java
│   │   │               │   │   │   │   └── ExportPlanoDeContassUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   ├── accountingdata/
│   │   │               │   │   │   │   ├── ImportAccountingDataUseCase.java
│   │   │               │   │   │   │   └── ExportAccountingDataUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   ├── taxparameter/
│   │   │               │   │   │   │   └── ManageTaxParametersUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   ├── fiscalmovement/
│   │   │               │   │   │   │   ├── CreateFiscalMovementUseCase.java
│   │   │               │   │   │   │   ├── UpdateFiscalMovementUseCase.java
│   │   │               │   │   │   │   └── DeleteFiscalMovementUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   ├── calculation/
│   │   │               │   │   │   │   ├── CalculateIrpjUseCase.java
│   │   │               │   │   │   │   ├── CalculateCsllUseCase.java
│   │   │               │   │   │   │   ├── RecalculateAllUseCase.java
│   │   │               │   │   │   │   └── GetOutdatedCalculationsUseCase.java
│   │   │               │   │   │   │
│   │   │               │   │   │   └── ecf/
│   │   │               │   │   │       ├── UploadImportedEcfUseCase.java
│   │   │               │   │   │       ├── GenerateMFileUseCase.java
│   │   │               │   │   │       ├── GenerateCompleteEcfUseCase.java
│   │   │               │   │   │       ├── ValidateEcfUseCase.java
│   │   │               │   │   │       └── DownloadEcfUseCase.java
│   │   │               │   │   │
│   │   │               │   │   └── out/                     # Ports OUT (interfaces para infraestrutura)
│   │   │               │   │       ├── persistence/
│   │   │               │   │       │   ├── UserRepositoryPort.java
│   │   │               │   │       │   ├── CompanyRepositoryPort.java
│   │   │               │   │       │   ├── PlanoDeContasRepositoryPort.java
│   │   │               │   │       │   ├── AccountingDataRepositoryPort.java
│   │   │               │   │       │   ├── TaxParameterRepositoryPort.java
│   │   │               │   │       │   ├── FiscalMovementRepositoryPort.java
│   │   │               │   │       │   ├── TaxCalculationResultRepositoryPort.java
│   │   │               │   │       │   └── EcfFileRepositoryPort.java
│   │   │               │   │       │
│   │   │               │   │       ├── external/
│   │   │               │   │       │   ├── CnpjApiPort.java           # Interface para BrasilAPI/ReceitaWS
│   │   │               │   │       │   └── FileStoragePort.java       # Interface para armazenamento de ECF
│   │   │               │   │       │
│   │   │               │   │       └── event/
│   │   │               │   │           └── CalculationInvalidationEventPublisher.java
│   │   │               │   │
│   │   │               │   └── service/                     # Application services (implementação use cases)
│   │   │               │       ├── AuthService.java
│   │   │               │       ├── UserService.java
│   │   │               │       ├── CompanyService.java
│   │   │               │       ├── PlanoDeContasService.java
│   │   │               │       ├── AccountingDataService.java
│   │   │               │       ├── TaxParameterService.java
│   │   │               │       ├── FiscalMovementService.java
│   │   │               │       ├── TaxCalculationService.java
│   │   │               │       └── EcfService.java
│   │   │               │
│   │   │               └── infrastructure/                  # CAMADA DE INFRAESTRUTURA (adapters)
│   │   │                   │
│   │   │                   ├── adapter/
│   │   │                   │   │
│   │   │                   │   ├── in/
│   │   │                   │   │   └── rest/                # REST Controllers (adapter IN)
│   │   │                   │   │       ├── AuthController.java
│   │   │                   │   │       ├── UserController.java
│   │   │                   │   │       ├── CompanyController.java
│   │   │                   │   │       ├── PlanoDeContasController.java
│   │   │                   │   │       ├── AccountingDataController.java
│   │   │                   │   │       ├── TaxParameterController.java
│   │   │                   │   │       ├── FiscalMovementController.java
│   │   │                   │   │       ├── TaxCalculationController.java
│   │   │                   │   │       └── EcfController.java
│   │   │                   │   │
│   │   │                   │   └── out/
│   │   │                   │       ├── persistence/         # JPA adapters (adapter OUT)
│   │   │                   │       │   ├── entity/          # JPA Entities
│   │   │                   │       │   │   ├── BaseEntity.java
│   │   │                   │       │   │   ├── UserEntity.java
│   │   │                   │       │   │   ├── CompanyEntity.java
│   │   │                   │       │   │   ├── PlanoDeContasEntity.java
│   │   │                   │       │   │   ├── AccountingDataEntity.java
│   │   │                   │       │   │   ├── TaxParameterEntity.java
│   │   │                   │       │   │   ├── FiscalMovementEntity.java
│   │   │                   │       │   │   ├── TaxCalculationResultEntity.java
│   │   │                   │       │   │   └── EcfFileEntity.java
│   │   │                   │       │   │
│   │   │                   │       │   ├── repository/      # Spring Data JPA repositories
│   │   │                   │       │   │   ├── UserJpaRepository.java
│   │   │                   │       │   │   ├── CompanyJpaRepository.java
│   │   │                   │       │   │   ├── PlanoDeContasJpaRepository.java
│   │   │                   │       │   │   ├── AccountingDataJpaRepository.java
│   │   │                   │       │   │   ├── TaxParameterJpaRepository.java
│   │   │                   │       │   │   ├── FiscalMovementJpaRepository.java
│   │   │                   │       │   │   ├── TaxCalculationResultJpaRepository.java
│   │   │                   │       │   │   └── EcfFileJpaRepository.java
│   │   │                   │       │   │
│   │   │                   │       │   ├── adapter/         # Repository adapters (implementam RepositoryPort)
│   │   │                   │       │   │   ├── UserRepositoryAdapter.java
│   │   │                   │       │   │   ├── CompanyRepositoryAdapter.java
│   │   │                   │       │   │   ├── PlanoDeContasRepositoryAdapter.java
│   │   │                   │       │   │   ├── AccountingDataRepositoryAdapter.java
│   │   │                   │       │   │   ├── TaxParameterRepositoryAdapter.java
│   │   │                   │       │   │   ├── FiscalMovementRepositoryAdapter.java
│   │   │                   │       │   │   ├── TaxCalculationResultRepositoryAdapter.java
│   │   │                   │       │   │   └── EcfFileRepositoryAdapter.java
│   │   │                   │       │   │
│   │   │                   │       │   ├── mapper/          # MapStruct mappers (Entity ↔ Domain)
│   │   │                   │       │   │   ├── UserMapper.java
│   │   │                   │       │   │   ├── CompanyMapper.java
│   │   │                   │       │   │   ├── PlanoDeContasMapper.java
│   │   │                   │       │   │   ├── AccountingDataMapper.java
│   │   │                   │       │   │   ├── TaxParameterMapper.java
│   │   │                   │       │   │   ├── FiscalMovementMapper.java
│   │   │                   │       │   │   ├── TaxCalculationResultMapper.java
│   │   │                   │       │   │   └── EcfFileMapper.java
│   │   │                   │       │   │
│   │   │                   │       │   └── converter/       # JPA Attribute Converters
│   │   │                   │       │       └── YearMonthConverter.java
│   │   │                   │       │
│   │   │                   │       ├── external/            # External API adapters
│   │   │                   │       │   ├── BrasilApiCnpjAdapter.java
│   │   │                   │       │   ├── ReceitaWsCnpjAdapter.java
│   │   │                   │       │   └── LocalFileStorageAdapter.java
│   │   │                   │       │
│   │   │                   │       └── event/               # Event publishers
│   │   │                   │           └── SpringCalculationInvalidationEventPublisher.java
│   │   │                   │
│   │   │                   ├── config/                      # Spring Configuration classes
│   │   │                   │   ├── SecurityConfig.java      # Spring Security + JWT
│   │   │                   │   ├── OpenApiConfig.java       # Swagger/OpenAPI
│   │   │                   │   ├── JpaConfig.java           # JPA Auditing
│   │   │                   │   ├── WebClientConfig.java     # WebClient beans (BrasilAPI, ReceitaWS)
│   │   │                   │   └── CorsConfig.java          # CORS (futuro frontend)
│   │   │                   │
│   │   │                   ├── security/                    # Security components
│   │   │                   │   ├── JwtTokenProvider.java
│   │   │                   │   ├── JwtAuthenticationFilter.java
│   │   │                   │   ├── SpringSecurityAuditorAware.java
│   │   │                   │   └── SecurityContextHelper.java
│   │   │                   │
│   │   │                   ├── dto/                         # DTOs (request/response)
│   │   │                   │   ├── auth/
│   │   │                   │   │   ├── LoginRequest.java
│   │   │                   │   │   ├── LoginResponse.java
│   │   │                   │   │   ├── ChangePasswordRequest.java
│   │   │                   │   │   └── ChangePasswordResponse.java
│   │   │                   │   │
│   │   │                   │   ├── user/
│   │   │                   │   │   ├── CreateUserRequest.java
│   │   │                   │   │   ├── UpdateUserRequest.java
│   │   │                   │   │   ├── UserResponse.java
│   │   │                   │   │   ├── ToggleStatusRequest.java
│   │   │                   │   │   ├── ToggleStatusResponse.java
│   │   │                   │   │   ├── ResetPasswordRequest.java
│   │   │                   │   │   └── ResetPasswordResponse.java
│   │   │                   │   │
│   │   │                   │   ├── company/
│   │   │                   │   │   ├── CreateCompanyRequest.java
│   │   │                   │   │   ├── UpdateCompanyRequest.java
│   │   │                   │   │   ├── CompanyResponse.java
│   │   │                   │   │   └── CnpjSearchResponse.java
│   │   │                   │   │
│   │   │                   │   ├── chartofaccount/
│   │   │                   │   │   ├── PlanoDeContasResponse.java
│   │   │                   │   │   └── ImportChartResponse.java
│   │   │                   │   │
│   │   │                   │   ├── accountingdata/
│   │   │                   │   │   ├── AccountingDataResponse.java
│   │   │                   │   │   └── ImportAccountingDataResponse.java
│   │   │                   │   │
│   │   │                   │   ├── taxparameter/
│   │   │                   │   │   ├── TaxParameterRequest.java
│   │   │                   │   │   └── TaxParameterResponse.java
│   │   │                   │   │
│   │   │                   │   ├── fiscalmovement/
│   │   │                   │   │   ├── FiscalMovementRequest.java
│   │   │                   │   │   └── FiscalMovementResponse.java
│   │   │                   │   │
│   │   │                   │   ├── calculation/
│   │   │                   │   │   ├── CalculateIrpjRequest.java
│   │   │                   │   │   ├── CalculateCsllRequest.java
│   │   │                   │   │   ├── TaxCalculationResponse.java
│   │   │                   │   │   └── OutdatedCalculationsResponse.java
│   │   │                   │   │
│   │   │                   │   ├── ecf/
│   │   │                   │   │   ├── GenerateMFileRequest.java
│   │   │                   │   │   ├── GenerateMFileResponse.java
│   │   │                   │   │   ├── GenerateCompleteEcfRequest.java
│   │   │                   │   │   ├── GenerateCompleteEcfResponse.java
│   │   │                   │   │   ├── ValidateEcfResponse.java
│   │   │                   │   │   └── EcfFileResponse.java
│   │   │                   │   │
│   │   │                   │   ├── common/
│   │   │                   │   │   ├── PageResponse.java    # Generic pagination
│   │   │                   │   │   └── ErrorResponse.java   # RFC 7807 format
│   │   │                   │   │
│   │   │                   │   └── mapper/                  # DTO ↔ Domain mappers
│   │   │                   │       ├── UserDtoMapper.java
│   │   │                   │       ├── CompanyDtoMapper.java
│   │   │                   │       ├── PlanoDeContasDtoMapper.java
│   │   │                   │       ├── AccountingDataDtoMapper.java
│   │   │                   │       ├── TaxParameterDtoMapper.java
│   │   │                   │       ├── FiscalMovementDtoMapper.java
│   │   │                   │       ├── TaxCalculationDtoMapper.java
│   │   │                   │       └── EcfFileDtoMapper.java
│   │   │                   │
│   │   │                   ├── exception/                   # Exception handling
│   │   │                   │   ├── GlobalExceptionHandler.java
│   │   │                   │   ├── ResourceNotFoundException.java
│   │   │                   │   ├── BusinessRuleViolationException.java
│   │   │                   │   ├── InvalidCredentialsException.java
│   │   │                   │   └── AccountLockedException.java
│   │   │                   │
│   │   │                   └── util/                        # Utilities
│   │   │                       ├── CsvParser.java
│   │   │                       ├── EcfFileParser.java
│   │   │                       └── DateTimeUtil.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                  # Default profile
│   │       ├── application-dev.yml              # Dev profile
│   │       ├── application-prod.yml             # Prod profile
│   │       │
│   │       ├── db/
│   │       │   └── schema.sql                   # Schema SQL (opcional - JPA DDL automático)
│   │       │
│   │       ├── static/                          # Static resources (se necessário)
│   │       │
│   │       └── templates/                       # Templates (não usado em API-only)
│   │
│   └── test/
│       ├── java/
│       │   └── br/
│       │       └── com/
│       │           └── lalurecf/
│       │               │
│       │               ├── domain/
│       │               │   └── service/
│       │               │       └── calculator/
│       │               │           ├── IrpjCalculatorTest.java       # Unit test - cálculo IRPJ
│       │               │           └── CsllCalculatorTest.java       # Unit test - cálculo CSLL
│       │               │
│       │               ├── application/
│       │               │   └── service/
│       │               │       ├── AuthServiceTest.java              # Unit test
│       │               │       ├── TaxCalculationServiceTest.java    # Unit test
│       │               │       └── EcfServiceTest.java               # Unit test
│       │               │
│       │               ├── infrastructure/
│       │               │   ├── adapter/
│       │               │   │   ├── in/
│       │               │   │   │   └── rest/
│       │               │   │   │       ├── AuthControllerTest.java   # Integration test (MockMvc)
│       │               │   │   │       ├── UserControllerTest.java
│       │               │   │   │       ├── CompanyControllerTest.java
│       │               │   │   │       ├── TaxCalculationControllerTest.java
│       │               │   │   │       └── EcfControllerTest.java
│       │               │   │   │
│       │               │   │   └── out/
│       │               │   │       └── persistence/
│       │               │   │           └── adapter/
│       │               │   │               ├── UserRepositoryAdapterTest.java    # TestContainers
│       │               │   │               ├── CompanyRepositoryAdapterTest.java
│       │               │   │               └── AccountingDataRepositoryAdapterTest.java
│       │               │   │
│       │               │   └── security/
│       │               │       └── JwtTokenProviderTest.java          # Unit test
│       │               │
│       │               ├── integration/                               # Full integration tests
│       │               │   ├── AuthenticationFlowIntegrationTest.java
│       │               │   ├── TaxCalculationFlowIntegrationTest.java
│       │               │   └── EcfGenerationFlowIntegrationTest.java
│       │               │
│       │               └── util/                                      # Test utilities
│       │                   ├── TestDataBuilder.java
│       │                   └── TestContainersConfig.java
│       │
│       └── resources/
│           ├── application-test.yml              # Test profile
│           │
│           └── test-data/                        # Sample CSV/ECF files for tests
│               ├── sample-chart-of-accounts.csv
│               ├── sample-accounting-data.csv
│               └── sample-ecf-imported.txt
│
├── docker/
│   ├── Dockerfile                                # Multi-stage build (JDK build + JRE runtime)
│   └── init-db/                                  # PostgreSQL initialization scripts
│       └── 01-init-schema.sql                    # Schema inicial + seed data
│
├── docker-compose.yml                            # CONFIGURAÇÃO DOCKER (PostgreSQL + App)
│
├── .gitignore
├── mvnw                                          # Maven wrapper (Unix)
├── mvnw.cmd                                      # Maven wrapper (Windows)
├── pom.xml                                       # Maven dependencies
└── README.md
```

### Arquivos Chave de Configuração

#### docker-compose.yml

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:12-alpine
    container_name: lalurecf-postgres
    environment:
      POSTGRES_DB: ecf_db
      POSTGRES_USER: ecf_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-ecf_password_dev}
      POSTGRES_INITDB_ARGS: "-E UTF8 --locale=pt_BR.UTF-8"
      TZ: America/Sao_Paulo
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/init-db:/docker-entrypoint-initdb.d
    networks:
      - ecf-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ecf_user -d ecf_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  app:
    build:
      context: .
      dockerfile: docker/Dockerfile
    container_name: lalurecf-app
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ecf_db
      SPRING_DATASOURCE_USERNAME: ecf_user
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-ecf_password_dev}
      JWT_SECRET: ${JWT_SECRET:-dev-secret-key-change-in-production-minimum-256-bits}
      TZ: America/Sao_Paulo
    ports:
      - "8080:8080"
    volumes:
      - app_storage:/app/storage
    networks:
      - ecf-network
    restart: unless-stopped

networks:
  ecf-network:
    driver: bridge

volumes:
  postgres_data:
    driver: local
  app_storage:
    driver: local
```

**Comandos de Uso:**

```bash
# Desenvolvimento local (IDE)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Docker Compose (PostgreSQL + App)
docker-compose up --build

# Rebuild apenas app
docker-compose up --build app

# Parar containers
docker-compose down

# Parar e remover volumes (RESET completo)
docker-compose down -v

# Logs
docker-compose logs -f app
docker-compose logs -f postgres

# Acessar banco via psql
docker exec -it lalurecf-postgres psql -U ecf_user -d ecf_db
```

---

