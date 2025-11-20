Segurança

> **ATENÇÃO CRÍTICA:** Requisitos de segurança OBRIGATÓRIOS.

### Validação de Entrada

- **Biblioteca:** Bean Validation 3.0.2 + Hibernate Validator 8.0.1
- **Onde:** DTOs (estrutural), Domain (negócio)
- **Regras:**
  - TODAS entradas externas DEVEM ser validadas
  - Whitelist approach
  - NUNCA confiar em dados do cliente

### Autenticação & Autorização

- **Método:** JWT (Auth0 java-jwt 4.4.0)
- **Tokens:**
  - Access: 15 minutos
  - Refresh: 7 dias
- **Password:** BCrypt strength 12
- **Brute Force:** Lock após 5 tentativas (15min)

**Padrões Obrigatórios:**
1. NUNCA senhas em plaintext
2. Proteção contra brute force
3. `@PreAuthorize` em TODOS endpoints protegidos
4. Validação multi-tenant (X-Company-Id)

### Gestão de Secrets

- **Dev:** `.env` (gitignored)
- **Prod:** Variáveis de ambiente
- **Regras:**
  - NUNCA hardcodar secrets
  - NUNCA logar secrets
  - Acessar via `@Value` ou `Environment`

### Segurança de APIs

- **Rate Limiting:** Futuro (Bucket4j + Redis)
- **CORS:** Lista específica de origins
- **Security Headers:**
  - Content-Security-Policy
  - X-XSS-Protection
  - X-Content-Type-Options: nosniff
  - X-Frame-Options: DENY
  - HSTS
- **HTTPS:** Obrigatório em produção

### Proteção de Dados

**Criptografia:**
- Em repouso: PostgreSQL TDE
- Em trânsito: HTTPS/TLS 1.3
- Database connections: SSL/TLS

**PII (Mascaramento em Logs):**
- CNPJ: `12.345.678/****-**`
- Email: `cont****@empresa.com.br`
- NUNCA logar: senhas, tokens completos, API keys

### Segurança de Dependências

- **Tool:** OWASP Dependency Check (GitHub Actions)
- **Política:**
  - CRITICAL: Patch < 24h
  - HIGH: Patch < 7 dias
  - MEDIUM/LOW: Trimestral

---

