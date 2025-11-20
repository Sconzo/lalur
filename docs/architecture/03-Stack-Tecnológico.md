Stack-Tecnológico

### Infraestrutura Cloud

- **Provider:** Cloud-agnostic (deploy inicial Docker Compose local, futuro: AWS/GCP/Azure)
- **Serviços Principais:** Docker Compose 2.24.0 (dev), Container Service gerenciado (prod futuro), PostgreSQL gerenciado (RDS/Cloud SQL/Azure DB)
- **Regiões de Deploy:** Local (dev), Brasil Sul/São Paulo (prod futuro para menor latência)

### Tabela de Tecnologias

| Categoria | Tecnologia | Versão | Propósito | Justificativa |
|-----------|------------|--------|-----------|---------------|
| **Linguagem** | Java | 21 LTS (OpenJDK Temurin 21.0.1) | Linguagem principal backend | LTS até setembro 2029, performance superior (GraalVM-ready), Virtual Threads (Project Loom), records, pattern matching |
| **Framework Backend** | Spring Boot | 3.2.1 | Framework web e DI | Versão estável 3.x com suporte Java 21, ecossistema maduro, produtividade alta |
| **Framework Web** | Spring Web MVC | 6.1.2 | REST API | MVC tradicional (não WebFlux), síncrono, simplicidade para MVP |
| **Persistência** | Spring Data JPA | 3.2.1 | Abstração de repositórios | Repository pattern out-of-the-box, reduz boilerplate |
| **ORM** | Hibernate | 6.4.1 | Mapeamento objeto-relacional | ORM maduro, integração nativa com Spring Data JPA |
| **Segurança** | Spring Security | 6.2.1 | Autenticação e autorização | RBAC, JWT integration, CORS, CSRF protection |
| **Banco de Dados** | PostgreSQL | 15.5 | Database relacional ACID | Conformidade ACID crítica para cálculos fiscais, constraints robustos, JSON support (calculationMemory), open-source |
| **Build Tool** | Maven | 3.9.6 | Gerenciamento de build | Padrão Java enterprise, plugins robustos (JaCoCo, Checkstyle), dependency management |
| **Containerização** | Docker | 25.0.0 | Containerização de app | Portabilidade, isolamento, reprodutibilidade de ambientes |
| **Orquestração Local** | Docker Compose | 2.24.0 | Orquestração multi-container | Setup completo (PostgreSQL + App) com um comando, ideal para dev |
| **JWT** | Auth0 java-jwt | 4.4.0 | Geração e validação de tokens | Biblioteca leve e madura para JWT, suporte HMAC256/RSA |
| **Password Hashing** | BCrypt (Spring Security) | Embutido | Hashing seguro de senhas | Strength 12, resistance a rainbow tables, padrão da indústria |
| **Documentação API** | Springdoc OpenAPI | 2.3.0 | Swagger UI + OpenAPI 3.0 | Auto-geração de docs, UI interativa, especificação OpenAPI exportável |
| **Mapeamento Objetos** | MapStruct | 1.5.5.Final | Entity ↔ Domain ↔ DTO | Performance (compile-time), type-safe, reduz boilerplate vs manual mapping |
| **Redução Boilerplate** | Lombok | 1.18.30 | Getters/setters/builders via annotations | Reduz verbosidade, @Data, @Builder, @Slf4j |
| **Validação** | Hibernate Validator | 8.0.1 (Bean Validation 3.0.2) | Validação de entrada (DTOs) | @NotBlank, @Email, @Size, integração com Spring MVC |
| **Logging** | SLF4J + Logback | 2.0.9 + 1.4.14 | Logging estruturado | SLF4J facade + Logback impl, JSON logs em prod, MDC support |
| **CSV Parsing** | Apache Commons CSV | 1.10.0 | Import/export de balancetes | Parsing robusto de CSV, escape de caracteres, streaming para grandes arquivos |
| **HTTP Client** | WebClient (Spring WebFlux) | 6.1.2 | Chamadas a APIs externas | Assíncrono, timeout configurável, retry policies (BrasilAPI/ReceitaWS) |
| **Testing Framework** | JUnit 5 (Jupiter) | 5.10.1 | Testes unitários e integração | Annotations modernas (@DisplayName), assertions fluentes, extensões |
| **Mocking** | Mockito | 5.8.0 | Mocks em testes unitários | Mature mocking library, integração com JUnit 5 |
| **Integration Testing** | TestContainers | 1.19.3 | PostgreSQL real em testes | Containers efêmeros, testes confiáveis com DB real, CI/CD friendly |
| **Code Coverage** | JaCoCo | 0.8.11 | Relatórios de cobertura | Coverage reports HTML, threshold enforcement (≥70%) |
| **CI/CD** | GitHub Actions | N/A | Pipeline de build/test/deploy | Free para repos públicos, integração nativa GitHub, runners Ubuntu |
| **Code Quality** | Checkstyle | 10.12.5 | Linting e style enforcement | Enforce Google Java Style, falha build se violações |
| **Dependency Check** | OWASP Dependency Check | 8.4.3 | Scan de vulnerabilidades | CVE database, alerta CRITICAL/HIGH vulnerabilities |

**Observações Importantes:**

- **Todas as versões são pinadas** (não usar "latest") para garantir reprodutibilidade de builds
- **Java 21:** Virtual Threads (Project Loom) não serão usados no MVP, mas mantém opção futura
- **Spring Boot 3.x:** Requer Java 17+, utiliza Jakarta EE 9+ (namespace `jakarta.*` vs `javax.*`)
- **PostgreSQL 15.5:** Versão estável com JSONB performance, melhorias em indexes, suporte a `YearMonth` via custom converter
- **Não usar Flyway/Liquibase no MVP:** JPA DDL automático (`ddl-auto: update` em dev, `validate` em prod) - migrar para Flyway se schema evoluir muito
- **WebClient vs RestTemplate:** WebClient é não-bloqueante, mas usado de forma síncrona (`.block()`) no MVP para simplicidade
- **JSON Convention:** camelCase (padrão Java/Jackson) - não forçar snake_case

---

