Próximos-Passos

### Para o Desenvolvedor (Dev Agent)

**Roteiro de Implementação:**

1. **Setup Inicial**
   - Epic 1, Story 1.1: Configuração Inicial Spring Boot
   - Validar `mvn clean package` compila
   - Estrutura hexagonal conforme Seção 10

2. **Docker & Database**
   - Epic 1, Story 1.2: Docker & PostgreSQL
   - `docker-compose up` funcional
   - Health check: `/actuator/health` retorna UP

3. **Ordem de Implementação**
   - Domain layer primeiro (entities, calculators)
   - Application layer (use cases, ports)
   - Infrastructure layer (adapters)

4. **Referências Críticas**
   - Seção 3: Versões de dependências
   - Seção 4: Entidades com refinamentos
   - Seção 8: Contratos OpenAPI
   - Seção 9: DDL completo
   - Seção 13: Coding Standards
   - Seção 15: Padrões de Segurança

5. **Testes Obrigatórios**
   - 70% coverage geral, 80% calculators
   - TestContainers para integration
   - JaCoCo no CI/CD

### Prompt para Dev Agent (Primeira Story)

```
Olá Dev Agent! Implementação do Sistema LALUR V2 ECF.

DOCUMENTOS OBRIGATÓRIOS:
1. docs/architecture.md - Arquitetura completa
2. docs/prd.md - Requirements
3. docs/epics/epic-01-fundacao-autenticacao.md - Epic 1

PRIMEIRA TAREFA:
Epic 1, Story 1.1: Configuração Inicial Spring Boot

CRITÉRIOS DE ACEITAÇÃO:
- Java 21 + Spring Boot 3.2.1
- Estrutura hexagonal: domain/application/infrastructure
- Dependências conforme Seção 3
- application.yml (dev, prod)
- Compila: mvn clean package
- Inicia: mvn spring-boot:run

REGRAS CRÍTICAS (Seção 13):
- NUNCA importar Spring no domain
- SEMPRE SLF4J Logger
- SEMPRE validar em DTOs
- NUNCA expor Entities
- Seguir nomenclatura (PascalCase/camelCase)

Prossiga com Story 1.1.
```

### Monitoramento Pós-Deploy (Futuro)

**Quando em produção:**

1. **Logs Centralizados:** ELK/Loki, JSON, 90 dias retention
2. **Métricas:** Prometheus + Grafana (JVM, HTTP, DB)
3. **Tracing:** Jaeger + OpenTelemetry
4. **Health Checks:** `/actuator/health` a cada 30s

### Compliance

- **LGPD:** Audit trail, soft delete + anonimização, direito ao esquecimento
- **Documentação Fiscal:** Referências legais em código (Lei 9.249/1995)
- **API Docs:** Swagger UI com autenticação, changelog versionado

---

**FIM DO DOCUMENTO DE ARQUITETURA**

---

Este documento foi gerado por **Winston the Architect** (BMad Framework) em 2025-01-19.

Para dúvidas ou atualizações, consultar:
- PRD: `docs/prd.md`
- Epics: `docs/epics/`
- Código: Seguir estrutura em Seção 10 (Source Tree)
