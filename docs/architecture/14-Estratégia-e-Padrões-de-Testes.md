Estratégia-e-Padrões-de-Testes

### Filosofia

- **Abordagem:** Test-After (TDD para calculators críticos)
- **Cobertura:**
  - Geral: ≥70%
  - Calculators: ≥80%
  - Domain: ≥75%
- **Pirâmide:** 70% Unit / 25% Integration / 5% E2E

### Tipos de Testes

**1. Unit Tests (JUnit 5 + Mockito)**
- Framework: JUnit 5.10.1
- Localização: `src/test/java/{mesmo.pacote}`
- Padrão AAA (Arrange-Act-Assert)
- Mockar TODAS dependências

**2. Integration Tests (TestContainers)**
- PostgreSQL real em containers
- `@SpringBootTest` + MockMvc
- Base class: `IntegrationTestBase`

**3. E2E Tests**
- Manual via Swagger UI (MVP)
- Futuro: Postman/REST Assured

### Test Data Management

- **Builders:** `TestDataBuilder` (factory pattern)
- **Fixtures:** CSV samples em `src/test/resources/test-data/`
- **Cleanup:** `@BeforeEach` + rollback automático

### Coverage (JaCoCo)

```xml
<execution>
    <id>jacoco-check</id>
    <configuration>
        <rules>
            <rule>
                <element>PACKAGE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

---

