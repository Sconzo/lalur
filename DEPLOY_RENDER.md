# Deploy no Render - LALUR V2 ECF API

## Pré-requisitos

1. Conta no [Render](https://render.com)
2. Repositório Git com o código (GitHub, GitLab, etc.)

## Passo 1: Criar PostgreSQL Database

1. No dashboard do Render, clique em **"New +"** → **"PostgreSQL"**
2. Configure:
   - **Name**: `lalur-ecf-db` (ou nome de sua preferência)
   - **Database**: `lalur_ecf`
   - **User**: será gerado automaticamente
   - **Region**: escolha a mesma região do web service (ex: Oregon)
   - **PostgreSQL Version**: 15 ou superior
   - **Plan**: Free (para teste) ou Starter
3. Clique em **"Create Database"**
4. Aguarde a criação (pode levar alguns minutos)
5. **IMPORTANTE**: Anote as seguintes informações da aba "Info":
   - **Internal Database URL** (formato: `postgres://user:password@dpg-xxxxx-a.oregon-postgres.render.com/dbname`) postgresql://lalur_ecf_user:kASkCCeC9YYZCnLDOdRrZCxLefrl17H2@dpg-d4rim8e3jp1c7391naog-a/lalur_ecf
   - **Username** lalur_ecf_user
   - **Password** kASkCCeC9YYZCnLDOdRrZCxLefrl17H2

## Passo 2: Criar Web Service

1. No dashboard do Render, clique em **"New +"** → **"Web Service"**
2. Conecte seu repositório Git
3. Configure:
   - **Name**: `lalur-ecf-api`
   - **Region**: mesma do database (ex: Oregon)
   - **Branch**: `main` (ou sua branch principal)
   - **Root Directory**: deixe vazio ou `/` se o Dockerfile está em `docker/`
   - **Environment**: `Docker`
   - **Dockerfile Path**: `docker/Dockerfile`
   - **Docker Build Context Directory**: `/`
   - **Plan**: Free (para teste) ou Starter

## Passo 3: Configurar Variáveis de Ambiente

Na seção **"Environment Variables"**, adicione:

### 3.1. Database (URL Completa)

```
SPRING_DATASOURCE_URL = jdbc:postgresql://dpg-xxxxx-a.oregon-postgres.render.com:5432/lalur_ecf
```

**IMPORTANTE**:
- Copie a **Internal Database URL** do passo 1
- Converta de formato PostgreSQL para JDBC:
  - De: `postgres://user:password@host:5432/dbname`
  - Para: `jdbc:postgresql://host:5432/dbname`
- Use o hostname INTERNO (dpg-xxxxx.oregon-postgres.render.com)

### 3.2. Database (Credenciais)

```
SPRING_DATASOURCE_USERNAME = [copie do database info]
SPRING_DATASOURCE_PASSWORD = [copie do database info]
```

### 3.3. JWT Secret

Gere uma chave secreta forte:

```bash
openssl rand -base64 64
```

Adicione:
```
JWT_SECRET = [cole a chave gerada]
```

### 3.4. Profile (Opcional - já configurado no Dockerfile)

```
SPRING_PROFILES_ACTIVE = prod
```

## Passo 4: Deploy

1. Clique em **"Create Web Service"**
2. O Render fará automaticamente:
   - Clone do repositório
   - Build da imagem Docker
   - Deploy da aplicação
3. Acompanhe os logs na aba "Logs"

## Passo 5: Testar a Aplicação

Após o deploy bem-sucedido, teste:

### Health Check
```
https://seu-app.onrender.com/api/v1/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP"
}
```

### Swagger UI
```
https://seu-app.onrender.com/api/v1/swagger-ui.html
```

## Troubleshooting

### Erro: "Connection refused" ao PostgreSQL

**Causa**: URL do database incorreta ou uso de URL externa

**Solução**:
1. Verifique que está usando a **Internal Database URL**
2. Confirme o formato JDBC: `jdbc:postgresql://host:5432/dbname`
3. Verifique que database e web service estão na MESMA REGIÃO

### Erro: "failed to solve: exit code 126"

**Causa**: Maven Wrapper sem permissão de execução

**Solução**: Já corrigido no Dockerfile com `chmod +x ./mvnw`

### Erro: Aplicação inicia mas não conecta ao banco

**Checklist**:
- [ ] Database está rodando (status "Available")
- [ ] Variáveis de ambiente configuradas corretamente
- [ ] Usando hostname INTERNO do database
- [ ] Username e password corretos
- [ ] Database e web service na mesma região

### Logs para verificar

No Render, vá em "Logs" e procure por:
- ✅ `Started LalurecfApplication` - aplicação iniciou
- ✅ `HikariPool-1 - Start completed` - pool de conexões OK
- ❌ `Connection refused` - problema de conectividade
- ❌ `Access denied` - credenciais incorretas

## Configurações Adicionais

### Auto-Deploy

No Render, por padrão, cada push na branch principal faz deploy automático.

Para desabilitar:
1. Settings → Build & Deploy
2. Desmarque "Auto-Deploy"

### Health Check Customizado

O Dockerfile já inclui um health check:
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1
```

### Migrations (Flyway/Liquibase)

Se estiver usando migrations, certifique-se de que:
- O database foi criado com encoding UTF-8
- O usuário tem permissões de DDL
- `ddl-auto: validate` em produção (já configurado)

## Variáveis de Ambiente - Resumo

| Variável | Valor | Obrigatória |
|----------|-------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://dpg-xxxxx.oregon-postgres.render.com:5432/lalur_ecf` | ✅ |
| `SPRING_DATASOURCE_USERNAME` | Username do database | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | Password do database | ✅ |
| `JWT_SECRET` | Chave base64 de 64+ caracteres | ✅ |
| `SPRING_PROFILES_ACTIVE` | `prod` | ⚠️ Já no Dockerfile |

## Suporte

Em caso de problemas:
1. Verifique os logs no Render
2. Confirme que o database está "Available"
3. Teste a conexão usando a Internal URL
4. Verifique se as variáveis de ambiente estão todas configuradas

---

**Última atualização**: 2025-12-07
