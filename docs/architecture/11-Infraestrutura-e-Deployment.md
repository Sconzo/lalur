Infraestrutura-e-Deployment

### Infraestrutura como Código (IaC)

- **Ferramenta:** Docker Compose 2.24.0 (desenvolvimento local) / Terraform 1.7.0+ (produção - futuro)
- **Localização:** `docker-compose.yml` (raiz do projeto), `terraform/` (futuro)
- **Abordagem:**
  - **Desenvolvimento:** Docker Compose para ambiente completo (PostgreSQL + App)
  - **Produção (futuro):** Terraform para provisionamento cloud-agnostic (AWS/GCP/Azure)

### Estratégia de Deployment

- **Estratégia:**
  - **MVP/Dev:** Docker Compose manual (`docker-compose up`)
  - **Produção (futuro):** Blue-Green Deployment com containers Docker

- **Plataforma CI/CD:** GitHub Actions
- **Configuração de Pipeline:** `.github/workflows/ci.yml`

### Ambientes

- **dev (Desenvolvimento Local):**
  - Docker Compose (`docker-compose up`)
  - PostgreSQL 15.5 em container local
  - `application-dev.yml`, JPA DDL `ddl-auto: update`

- **test (CI/CD - GitHub Actions):**
  - Runners GitHub Actions (ubuntu-latest)
  - TestContainers PostgreSQL (efêmero)
  - `application-test.yml`

- **staging (Homologação - futuro):**
  - Cloud provider container service
  - PostgreSQL gerenciado
  - Dados sintéticos

- **prod (Produção - futuro):**
  - Cloud provider com alta disponibilidade
  - PostgreSQL gerenciado com replicação
  - SSL/TLS obrigatório

### Fluxo de Promoção

```
develop → CI Build + Tests → Docker Registry → staging (futuro) → main (PR) → prod (manual approval)
```

### Estratégia de Rollback

- **Método Primário:** Blue-Green Deployment (futuro)
- **RTO Target:** < 5 minutos
- **Database migrations:** Sempre backward-compatible

---

