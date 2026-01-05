-- ============================================================================
-- Script de inicialização de dados - LALUR V2 ECF
-- ============================================================================
-- Este script é executado automaticamente pelo Spring Boot após a criação
-- do schema (quando spring.jpa.defer-datasource-initialization=true)
--
-- ATENÇÃO: Este script usa INSERT ... ON CONFLICT DO NOTHING (PostgreSQL)
-- para evitar duplicação de dados em reinicializações.
-- ============================================================================

-- Criar usuário SYSTEM com ID fixo = 1 (usado para auditoria sem autenticação)
-- Senha desabilitada (hash inválido) - usuário não pode fazer login
INSERT INTO tb_usuario (
    id,
    primeiro_nome,
    sobrenome,
    email,
    senha,
    funcao,
    status,
    deve_mudar_senha,
    criado_em,
    criado_por,
    atualizado_em
) VALUES (
    1,
    'Sistema',
    'Automático',
    'system@lalurecf.com.br',
    '$2a$12$disabled.password.hash.not.usable',
    'ADMIN',
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Resetar sequence para não conflitar com ID fixo
SELECT setval('tb_usuario_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tb_usuario), true);

-- Criar usuário ADMIN padrão se não existir
-- Senha padrão: Admin@123 (hash BCrypt com strength 12)
-- IMPORTANTE: Altere a senha após o primeiro login!
INSERT INTO tb_usuario (
    primeiro_nome,
    sobrenome,
    email,
    senha,
    funcao,
    status,
    deve_mudar_senha,
    criado_em,
    criado_por,
    atualizado_em
) VALUES (
    'Admin',
    'Sistema',
    'admin@gmail.com',
    '$2a$12$wVByGsG.Ko94ePxBn/dTt.sTzh7RRXYkRH.P2TYKEo.8HjhyvOI9.',
    'ADMIN',
    'ACTIVE',
    true,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- ============================================================================
-- Parâmetros Tributários - Regimes de Tributação do IRPJ
-- ============================================================================
-- Inserir regimes de tributação padrão conforme tabela ECF
INSERT INTO tb_parametros_tributarios (
    codigo,
    tipo,
    descricao,
    status,
    criado_em,
    criado_por,
    atualizado_em
) VALUES
    ('1', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('2', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real/Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('3', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('4', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real/Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('5', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('6', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('7', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('8', 'FORMA_TRIB_LUCRO_REAL', 'Imune de IRPJ', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('9', 'FORMA_TRIB_LUCRO_REAL', 'Isento do IRPJ', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('A', 'PERIODO_DE_APURACAO', 'Anual', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('T', 'PERIODO_DE_APURACAO', 'Trimestral', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('01', 'QUALIFICACAO_PESSOA_JURIDICA', 'PJ em Geral', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('02', 'QUALIFICACAO_PESSOA_JURIDICA', 'PJ Componente do Sistema Financeiro', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('03', 'QUALIFICACAO_PESSOA_JURIDICA', 'Sociedades Seguradoras, de Capitalização e Previdência', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de caixa', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('2', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de competência', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1', 'ESTIMATIVA_MENSAL', 'Receita Bruta e Acréscimos', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('2', 'ESTIMATIVA_MENSAL', 'Balanço/Balancete de Suspensão/Redução', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('P', 'FORMA_TRIBUTACAO', 'Presumido', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('R', 'FORMA_TRIBUTACAO', 'Real', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('A', 'FORMA_TRIBUTACAO', 'Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1015', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1023', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Estadual ou do Distrito Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1031', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Municipal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1040', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1058', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Estadual ou do Distrito Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1066', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Municipal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1074', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1082', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Estadual', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1104', 'NATUREZA_JURIDICA', 'Autarquia Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1112', 'NATUREZA_JURIDICA', 'Autarquia Estadual ou do Distrito Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1120', 'NATUREZA_JURIDICA', 'Autarquia Municipal', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111301', 'CNAE', 'Cultivo de arroz', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111302', 'CNAE', 'Cultivo de milho', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111303', 'CNAE', 'Cultivo de trigo', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111399', 'CNAE', 'Cultivo de outros cereais não especificados anteriormente', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0112101', 'CNAE', 'Cultivo de algodão herbáceo', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0112102', 'CNAE', 'Cultivo de juta', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0112199', 'CNAE', 'Cultivo de outras fibras de lavoura temporária não especificadas anteriormente', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0113000', 'CNAE', 'Cultivo de cana-de-açúcar', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0114800', 'CNAE', 'Cultivo de fumo', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0115600', 'CNAE', 'Cultivo de soja', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0116401', 'CNAE', 'Cultivo de amendoim', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0116402', 'CNAE', 'Cultivo de girassol', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
ON CONFLICT (codigo, tipo) DO NOTHING;

-- ============================================================================
-- Valores Parametros Temporais - Seed Data
-- ============================================================================
-- Popula tb_valores_parametros_temporais com dados iniciais para empresa exemplo
-- Inclui períodos mensais para ESTIMATIVA_MENSAL e trimestrais para FORMA_TRIBUTACAO

-- Criar empresa exemplo se não existir
INSERT INTO tb_empresa (
    razao_social,
    cnpj,
    periodo_contabil,
    status,
    criado_em,
    criado_por,
    atualizado_em
) VALUES (
    'Empresa Exemplo LTDA',
    '12345678000199',
    '2024-01-01',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
)
ON CONFLICT (cnpj) DO NOTHING;

-- Associar parâmetros ESTIMATIVA_MENSAL à empresa exemplo
INSERT INTO tb_empresa_parametros_tributarios (empresa_id, parametro_tributario_id, criado_por, criado_em)
SELECT
    e.id,
    p.id,
    1,
    CURRENT_TIMESTAMP
FROM tb_empresa e
CROSS JOIN tb_parametros_tributarios p
WHERE e.cnpj = '12345678000199'
  AND p.tipo = 'ESTIMATIVA_MENSAL'
  AND p.codigo IN ('1', '2')
ON CONFLICT (empresa_id, parametro_tributario_id) DO NOTHING;

-- Associar parâmetros FORMA_TRIBUTACAO à empresa exemplo
INSERT INTO tb_empresa_parametros_tributarios (empresa_id, parametro_tributario_id, criado_por, criado_em)
SELECT
    e.id,
    p.id,
    1,
    CURRENT_TIMESTAMP
FROM tb_empresa e
CROSS JOIN tb_parametros_tributarios p
WHERE e.cnpj = '12345678000199'
  AND p.tipo = 'FORMA_TRIBUTACAO'
  AND p.codigo IN ('P', 'R', 'A')
ON CONFLICT (empresa_id, parametro_tributario_id) DO NOTHING;

-- Valores temporais MENSAIS para ESTIMATIVA_MENSAL (código 1) - Ano 2024
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    2024,
    m.mes,
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
CROSS JOIN (
    SELECT 1 AS mes UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) m
WHERE e.cnpj = '12345678000199'
  AND p.tipo = 'ESTIMATIVA_MENSAL'
  AND p.codigo = '1'
ON CONFLICT (empresa_parametros_tributarios_id, ano, mes, trimestre) DO NOTHING;

-- Valores temporais MENSAIS para ESTIMATIVA_MENSAL (código 2) - Ano 2024
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    2024,
    m.mes,
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
CROSS JOIN (
    SELECT 1 AS mes UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) m
WHERE e.cnpj = '12345678000199'
  AND p.tipo = 'ESTIMATIVA_MENSAL'
  AND p.codigo = '2'
ON CONFLICT (empresa_parametros_tributarios_id, ano, mes, trimestre) DO NOTHING;

-- Valores temporais TRIMESTRAIS para FORMA_TRIBUTACAO (Presumido) - Anos 2023-2025
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    y.ano,
    NULL,
    t.trimestre,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
CROSS JOIN (SELECT 2023 AS ano UNION ALL SELECT 2024 UNION ALL SELECT 2025) y
CROSS JOIN (SELECT 1 AS trimestre UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t
WHERE e.cnpj = '12345678000199'
  AND p.tipo = 'FORMA_TRIBUTACAO'
  AND p.codigo = 'P'
ON CONFLICT (empresa_parametros_tributarios_id, ano, mes, trimestre) DO NOTHING;

-- Valores temporais TRIMESTRAIS para FORMA_TRIBUTACAO (Real) - Anos 2023-2025
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    y.ano,
    NULL,
    t.trimestre,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
CROSS JOIN (SELECT 2023 AS ano UNION ALL SELECT 2024 UNION ALL SELECT 2025) y
CROSS JOIN (SELECT 1 AS trimestre UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t
WHERE e.cnpj = '12345678000199'
  AND p.tipo = 'FORMA_TRIBUTACAO'
  AND p.codigo = 'R'
ON CONFLICT (empresa_parametros_tributarios_id, ano, mes, trimestre) DO NOTHING;

-- Valores temporais TRIMESTRAIS para FORMA_TRIBUTACAO (Arbitrado) - Anos 2023-2025
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    y.ano,
    NULL,
    t.trimestre,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
CROSS JOIN (SELECT 2023 AS ano UNION ALL SELECT 2024 UNION ALL SELECT 2025) y
CROSS JOIN (SELECT 1 AS trimestre UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t
WHERE e.cnpj = '12345678000199'
  AND p.tipo = 'FORMA_TRIBUTACAO'
  AND p.codigo = 'A'
ON CONFLICT (empresa_parametros_tributarios_id, ano, mes, trimestre) DO NOTHING;
