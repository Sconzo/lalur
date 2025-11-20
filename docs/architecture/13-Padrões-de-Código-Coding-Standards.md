Padrões-de-Código-Coding-Standards

> **IMPORTANTE PARA AGENTES DE IA:** Regras OBRIGATÓRIAS durante geração de código.

### Padrões Centrais

- **Linguagem:** Java 21 LTS (OpenJDK Temurin 21.0.1)
- **Build:** Maven 3.9.6
- **Style:** Google Java Style Guide
- **Indentação:** 4 espaços (nunca tabs)
- **Line length:** Máximo 120 caracteres

### Convenções de Nomenclatura

| Elemento | Convenção | Exemplo |
|----------|-----------|---------|
| Classes | PascalCase | `TaxCalculationService` |
| Interfaces | PascalCase | `CalculateIrpjUseCase` |
| Métodos | camelCase | `calculateIrpj()` |
| Variáveis | camelCase | `fiscalYear` |
| Constantes | UPPER_SNAKE_CASE | `MAX_RETRY_ATTEMPTS` |
| Pacotes | lowercase | `br.com.lalurecf.domain` |
| Enums | PascalCase + UPPER | `enum UserRole { ADMIN }` |

### Regras Críticas

1. **Arquitetura Hexagonal - Domain NUNCA importa Spring/JPA**
2. **Logging - SEMPRE SLF4J (nunca `System.out.println`)**
3. **API Responses - SEMPRE DTOs (nunca expor Entities)**
4. **Transações - SEMPRE em Application Services**
5. **Exceptions - Traduzir exceptions técnicas**
6. **Validação - Bean Validation em DTOs, lógica em Domain**
7. **Null Safety - Preferir `Optional<T>` para retornos**
8. **Database - Repository Pattern obrigatório**
9. **JSON - camelCase (nunca snake_case)**
10. **Imports - NUNCA wildcard (`import java.util.*`)**

---

