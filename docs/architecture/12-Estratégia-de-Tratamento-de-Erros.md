Estratégia-de-Tratamento-de-Erros

### Abordagem Geral

- **Modelo de Erro:** RFC 7807 Problem Details for HTTP APIs
- **Hierarquia de Exceções:**
  - `DomainException` (domain layer)
  - `ApplicationException` (application layer)
  - `InfrastructureException` (infrastructure layer)

### Padrões de Logging

- **Biblioteca:** SLF4J 2.0.9 + Logback 1.4.14
- **Formato:** JSON estruturado (produção), texto legível (dev)
- **Níveis:** ERROR, WARN, INFO, DEBUG, TRACE
- **Contexto Obrigatório:**
  - Correlation ID (UUID)
  - User email (quando autenticado)
  - Company ID (se CONTADOR)

### Padrões de Tratamento

**APIs Externas:**
- Retry: 3 tentativas com exponential backoff
- Timeout: 10s (BrasilAPI), 15s (ReceitaWS)
- Fallback: ReceitaWS quando BrasilAPI falha

**Lógica de Negócio:**
- Mensagens em português brasileiro
- Códigos de erro: `CATEGORIA_DESCRICAO` (ex: `AUTH_INVALID_CREDENTIALS`)

**Consistência de Dados:**
- `@Transactional` em Application Services
- Rollback automático em exceptions
- UPSERT via unique constraints

### GlobalExceptionHandler (RFC 7807)

```json
{
  "type": "https://api.lalurecf.com.br/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "CNPJ inválido",
  "instance": "/api/v1/companies",
  "timestamp": "2024-01-15T10:30:00Z",
  "correlationId": "a3f5b2c1-...",
  "errors": [{"field": "cnpj", "message": "CNPJ deve ter 14 dígitos"}]
}
```

---

