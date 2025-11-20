# Sistema LALUR V2 ECF - API Backend

![CI Pipeline](https://github.com/USERNAME/REPOSITORY/actions/workflows/ci.yml/badge.svg)
![Coverage](https://img.shields.io/badge/coverage-%E2%89%A570%25-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen)

Sistema de escrituração contábil fiscal (ECF) para cálculos de IRPJ (Imposto de Renda Pessoa Jurídica) e CSLL (Contribuição Social sobre o Lucro Líquido). O sistema permite importação de balancetes contábeis, parametrização fiscal, lançamentos de ajustes (adições e exclusões), cálculo automatizado de tributos e geração de arquivos ECF compatíveis com o SPED.

Desenvolvido com arquitetura hexagonal (Ports & Adapters) para garantir separação de responsabilidades, testabilidade e manutenibilidade de longo prazo.

## 📋 Prerequisites

Certifique-se de ter as seguintes ferramentas instaladas:

- **Java 21 LTS** (OpenJDK Temurin 21.0.1 ou superior)
- **Maven 3.9.6** ou superior
- **PostgreSQL 15.5** ou superior
- **Git** (para clonar o repositório)

## 🚀 Quick Start

### 1. Clone the repository

```bash
git clone <repository-url>
cd "LALUR V2"
```

### 2. Configure PostgreSQL

Crie o banco de dados e usuário:

```sql
CREATE DATABASE ecf_db;
CREATE USER ecf_user WITH PASSWORD 'ecf_password_dev';
GRANT ALL PRIVILEGES ON DATABASE ecf_db TO ecf_user;
```

### 3. Build the project

```bash
mvn clean install
```

### 4. Run the application

```bash
mvn spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080/api/v1`

## 🐳 Running with Docker (Recommended)

Docker provides a consistent development environment with Java 21 and PostgreSQL preconfigured.

### Prerequisites

- **Docker** 25.0.0 ou superior
- **Docker Compose** 2.24.0 ou superior

### Docker Commands

```bash
# First run (build + start)
docker-compose up --build

# Subsequent runs
docker-compose up

# Start in background (detached mode)
docker-compose up -d

# View logs
docker-compose logs -f app
docker-compose logs -f postgres

# Stop containers
docker-compose down

# Stop and remove ALL data (complete reset)
docker-compose down -v

# Access PostgreSQL via psql
docker exec -it lalurecf-postgres psql -U ecf_user -d ecf_db

# Rebuild only the app (without PostgreSQL)
docker-compose up --build app
```

### Health Check

After starting the containers, verify the application is healthy:

```bash
curl http://localhost:8080/api/v1/actuator/health
# Expected: {"status":"UP"}
```

### Docker Configuration

- **PostgreSQL**: Runs on `localhost:5432` with database `ecf_db`, user `ecf_user`
- **Application**: Runs on `localhost:8080` with context path `/api/v1`
- **Data persistence**: PostgreSQL data is persisted in Docker volume `postgres_data`
- **Application storage**: ECF files will be stored in Docker volume `app_storage`
- **Network**: Both services communicate via bridge network `ecf-network`

### Environment Variables

Create a `.env` file in the project root (gitignored) for custom configuration:

```env
POSTGRES_PASSWORD=ecf_password_dev
JWT_SECRET=dev-secret-key-change-in-production-minimum-256-bits
SPRING_PROFILES_ACTIVE=dev
```

## 🏗️ Architecture

Este projeto segue os princípios da **Arquitetura Hexagonal (Ports & Adapters)**:

- **Domain Layer** (`domain/`): Lógica de negócio pura, sem dependências de frameworks
  - Models: Entidades de domínio (POJOs)
  - Enums: Enumerações de domínio
  - Exceptions: Exceções específicas de domínio
  - Services: Serviços de domínio (calculadoras, validadores)

- **Application Layer** (`application/`): Casos de uso e orquestração
  - Ports IN: Interfaces de casos de uso
  - Ports OUT: Interfaces para repositórios e serviços externos
  - Services: Implementação dos casos de uso

- **Infrastructure Layer** (`infrastructure/`): Adaptadores e integrações
  - Adapters IN: Controllers REST
  - Adapters OUT: Repositórios JPA, integrações externas
  - Config: Configurações Spring
  - Security: JWT, filtros de segurança
  - DTOs: Objetos de transferência de dados

### Key Features

- 🔐 **Autenticação JWT** com tokens de acesso e refresh
- 👥 **Controle de acesso baseado em roles** (ADMIN, CONTADOR)
- 📊 **Multi-tenant** por empresa (CNPJ)
- 🧮 **Cálculos automatizados** de IRPJ e CSLL
- 📁 **Import/Export CSV** de balancetes e planos de contas
- 📄 **Geração de arquivos ECF** compatíveis com SPED
- 📝 **Documentação OpenAPI/Swagger**
- ✅ **Cobertura de testes** ≥70%

## 📚 Documentation

Para documentação completa do projeto, consulte:

- **PRD (Product Requirements Document)**: `docs/prd.md`
- **Architecture Documentation**: `docs/architecture/`
- **API Documentation**: Disponível em `/swagger-ui.html` após iniciar a aplicação

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 3.2.1, Spring Security 6.2.1
- **Persistence**: Spring Data JPA 3.2.1, Hibernate 6.4.1, PostgreSQL 15.5
- **Security**: JWT (Auth0 java-jwt 4.4.0), BCrypt
- **API Documentation**: Springdoc OpenAPI 2.3.0
- **Build**: Maven 3.9.6
- **Testing**: JUnit 5, Mockito, TestContainers
- **Code Quality**: JaCoCo (coverage), Checkstyle (Google Style)
- **Utilities**: Lombok, MapStruct, Apache Commons CSV

## 📝 Development Commands

```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Package the application
mvn clean package

# Run the application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Check code style
mvn checkstyle:check
```

## 🧪 Testing

Execute all tests:

```bash
mvn test
```

View coverage report:

```bash
mvn jacoco:report
# Open: target/site/jacoco/index.html
```

## 📦 Build and Deploy

Create production JAR:

```bash
mvn clean package -DskipTests
# Output: target/lalur-ecf-api-1.0.0-SNAPSHOT.jar
```

Run production JAR:

```bash
java -jar target/lalur-ecf-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🔒 Security

- Passwords are hashed using BCrypt (strength 12)
- JWT tokens for stateless authentication
- Access tokens expire in 15 minutes
- Refresh tokens expire in 7 days
- HTTPS required in production
- CORS configured for frontend integration

## 📄 License

Proprietary - All rights reserved

## 👥 Team

Developed for fiscal and accounting professionals.

---

For questions or support, refer to the complete documentation in the `docs/` directory.
