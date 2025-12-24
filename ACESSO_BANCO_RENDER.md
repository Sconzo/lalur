# Como Acessar o Banco PostgreSQL no Render

## Credenciais do Banco

Você já tem as credenciais do seu banco:
- **Host**: `dpg-d4rim8e3jp1c7391naog-a.oregon-postgres.render.com`
- **Port**: `5432`
- **Database**: `lalur_ecf`
- **Username**: `lalur_ecf_user`
- **Password**: `kASkCCeC9YYZCnLDOdRrZCxLefrl17H2`

---

## Opção 1: pgAdmin (Interface Gráfica - Recomendado)

### 1. Instalar pgAdmin
- Download: https://www.pgadmin.org/download/
- Escolha a versão para Windows

### 2. Configurar Conexão
1. Abra o pgAdmin
2. Clique com botão direito em **"Servers"** → **"Register"** → **"Server"**
3. **Aba General**:
   - Name: `Render - LALUR ECF`
4. **Aba Connection**:
   - Host: `dpg-d4rim8e3jp1c7391naog-a.oregon-postgres.render.com`
   - Port: `5432`
   - Maintenance database: `lalur_ecf`
   - Username: `lalur_ecf_user`
   - Password: `kASkCCeC9YYZCnLDOdRrZCxLefrl17H2`
   - ✅ Marque "Save password"
5. **Aba SSL** (IMPORTANTE):
   - SSL mode: `Require`
6. Clique em **"Save"**

### 3. Acessar
- Expanda: Servers → Render - LALUR ECF → Databases → lalur_ecf → Schemas → public → Tables
- Clique com botão direito em uma tabela → "View/Edit Data" → "All Rows"

---

## Opção 2: DBeaver (Interface Gráfica - Alternativa)

### 1. Instalar DBeaver
- Download: https://dbeaver.io/download/
- Community Edition (gratuita)

### 2. Configurar Conexão
1. Abra o DBeaver
2. Database → New Database Connection
3. Selecione **PostgreSQL**
4. **Connection Settings**:
   - Host: `dpg-d4rim8e3jp1c7391naog-a.oregon-postgres.render.com`
   - Port: `5432`
   - Database: `lalur_ecf`
   - Username: `lalur_ecf_user`
   - Password: `kASkCCeC9YYZCnLDOdRrZCxLefrl17H2`
5. **SSL**:
   - Use SSL: ✅ Yes
   - SSL mode: `require`
6. Clique em **"Test Connection"**
7. Se pedir para baixar drivers, clique em **"Download"**
8. Clique em **"Finish"**

---

## Opção 3: psql (Linha de Comando)

### Windows (se tiver PostgreSQL instalado):

```bash
psql "postgresql://lalur_ecf_user:kASkCCeC9YYZCnLDOdRrZCxLefrl17H2@dpg-d4rim8e3jp1c7391naog-a.oregon-postgres.render.com:5432/lalur_ecf?sslmode=require"
```

### Comandos úteis no psql:
```sql
-- Listar tabelas
\dt

-- Descrever estrutura de uma tabela
\d tb_empresa

-- Ver dados de uma tabela
SELECT * FROM tb_empresa;

-- Contar registros
SELECT COUNT(*) FROM tb_empresa;

-- Sair
\q
```

---

## Opção 4: DataGrip (JetBrains - Pago)

Se você usa IntelliJ IDEA Ultimate ou tem DataGrip:

1. Database → New → Data Source → PostgreSQL
2. Preencha as credenciais acima
3. ✅ Use SSL
4. Test Connection → OK

---

## Opção 5: VSCode + PostgreSQL Extension

1. Instale a extensão: **PostgreSQL** (by Chris Kolkman)
2. PostgreSQL Explorer → Add Connection
3. Preencha:
   ```
   Host: dpg-d4rim8e3jp1c7391naog-a.oregon-postgres.render.com
   Port: 5432
   Database: lalur_ecf
   Username: lalur_ecf_user
   Password: kASkCCeC9YYZCnLDOdRrZCxLefrl17H2
   SSL: Standard
   ```

---

## Tabelas que você deve encontrar:

Após conectar, você verá as tabelas criadas pela aplicação:

- `tb_empresa` - Empresas
- `tb_user` - Usuários
- `tb_periodo_contabil_audit` - Auditoria de período contábil
- `tb_tax_parameter` - Parâmetros tributários
- (outras tabelas...)

---

## Troubleshooting

### Erro: "Connection refused"
- ✅ Verifique que está usando a URL **INTERNA** (dpg-xxxxx.oregon-postgres.render.com)
- ❌ NÃO use a URL externa (se houver)

### Erro: "SSL required"
- Configure SSL mode como `require` na sua ferramenta

### Erro: "Password authentication failed"
- Copie e cole a senha exatamente como está
- Password: `kASkCCeC9YYZCnLDOdRrZCxLefrl17H2`

### Banco vazio (sem tabelas)
- Verifique se a aplicação já fez deploy com sucesso
- A variável `SPRING_JPA_HIBERNATE_DDL_AUTO=update` deve estar configurada

---

## Recomendação

**Para iniciantes**: Use **pgAdmin** (Opção 1)
- Interface gráfica amigável
- Específico para PostgreSQL
- Gratuito e completo

**Para desenvolvedores**: Use **DBeaver** (Opção 2)
- Suporta múltiplos bancos
- Mais leve que pgAdmin
- Ótima interface

---

## URL de Conexão Rápida (Copy-Paste)

Para ferramentas que aceitam connection string:

```
postgresql://lalur_ecf_user:kASkCCeC9YYZCnLDOdRrZCxLefrl17H2@dpg-d4rim8e3jp1c7391naog-a.oregon-postgres.render.com:5432/lalur_ecf?sslmode=require
```

---

**Última atualização**: 2025-12-08
