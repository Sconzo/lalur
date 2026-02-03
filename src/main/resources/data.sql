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
-- Tipos de Parâmetros Tributários
-- ============================================================================
INSERT INTO tb_tipos_parametros_tributarios (descricao, natureza, status, criado_em, atualizado_em)
VALUES
    ('FORMA_TRIB_LUCRO_REAL', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PERIODO_DE_APURACAO', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('QUALIFICACAO_PJ', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CRITERIO_RECONHECIMENTO__RECEITA', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ESTIMATIVA_MENSAL', 'MONTHLY', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FORMA_TRIBUTACAO', 'QUARTERLY', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('NATUREZA_JURIDICA', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CNAE', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (descricao) DO NOTHING;

-- ============================================================================
-- Parâmetros Tributários - Regimes de Tributação do IRPJ
-- ============================================================================
-- Inserir regimes de tributação padrão conforme tabela ECF
INSERT INTO tb_parametros_tributarios (
    codigo,
    tipo_parametro_id,
    descricao,
    status,
    criado_em,
    criado_por,
    atualizado_em
)
SELECT v.codigo, t.id, v.descricao, 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
FROM (VALUES
    ('1', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real'),
    ('2', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real/Arbitrado'),
    ('3', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real'),
    ('4', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real/Arbitrado'),
    ('5', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido'),
    ('6', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Arbitrado'),
    ('7', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Arbitrado'),
    ('8', 'FORMA_TRIB_LUCRO_REAL', 'Imune de IRPJ'),
    ('9', 'FORMA_TRIB_LUCRO_REAL', 'Isento do IRPJ'),
    ('A', 'PERIODO_DE_APURACAO', 'Anual'),
    ('T', 'PERIODO_DE_APURACAO', 'Trimestral'),
    ('01', 'QUALIFICACAO_PJ', 'PJ em Geral'),
    ('02', 'QUALIFICACAO_PJ', 'PJ Componente do Sistema Financeiro'),
    ('03', 'QUALIFICACAO_PJ', 'Sociedades Seguradoras, de Capitalização e Previdência'),
    ('1', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de caixa'),
    ('2', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de competência'),
    ('1', 'ESTIMATIVA_MENSAL', 'Receita Bruta e Acréscimos'),
    ('2', 'ESTIMATIVA_MENSAL', 'Balanço/Balancete de Suspensão/Redução'),
    ('P', 'FORMA_TRIBUTACAO', 'Presumido'),
    ('R', 'FORMA_TRIBUTACAO', 'Real'),
    ('A', 'FORMA_TRIBUTACAO', 'Arbitrado'),
    ('1015', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Federal'),
    ('1023', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Estadual ou do Distrito Federal'),
    ('1031', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Municipal'),
    ('1040', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Federal'),
    ('1058', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Estadual ou do Distrito Federal'),
    ('1066', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Municipal'),
    ('1074', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Federal'),
    ('1082', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Estadual'),
    ('1104', 'NATUREZA_JURIDICA', 'Autarquia Federal'),
    ('1112', 'NATUREZA_JURIDICA', 'Autarquia Estadual ou do Distrito Federal'),
    ('1120', 'NATUREZA_JURIDICA', 'Autarquia Municipal'),
    ('0111301', 'CNAE', 'Cultivo de arroz'),
    ('0111302', 'CNAE', 'Cultivo de milho'),
    ('0111303', 'CNAE', 'Cultivo de trigo'),
    ('0111399', 'CNAE', 'Cultivo de outros cereais não especificados anteriormente'),
    ('0112101', 'CNAE', 'Cultivo de algodão herbáceo'),
    ('0112102', 'CNAE', 'Cultivo de juta'),
    ('0112199', 'CNAE', 'Cultivo de outras fibras de lavoura temporária não especificadas anteriormente'),
    ('0113000', 'CNAE', 'Cultivo de cana-de-açúcar'),
    ('0114800', 'CNAE', 'Cultivo de fumo'),
    ('0115600', 'CNAE', 'Cultivo de soja'),
    ('0116401', 'CNAE', 'Cultivo de amendoim'),
    ('0116402', 'CNAE', 'Cultivo de girassol')
) AS v(codigo, tipo, descricao)
JOIN tb_tipos_parametros_tributarios t ON t.descricao = v.tipo
ON CONFLICT (codigo, tipo_parametro_id) DO NOTHING;

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
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
WHERE e.cnpj = '12345678000199'
  AND t.descricao = 'ESTIMATIVA_MENSAL'
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
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
WHERE e.cnpj = '12345678000199'
  AND t.descricao = 'FORMA_TRIBUTACAO'
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
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
CROSS JOIN (
    SELECT 1 AS mes UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) m
WHERE e.cnpj = '12345678000199'
  AND t.descricao = 'ESTIMATIVA_MENSAL'
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
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
CROSS JOIN (
    SELECT 1 AS mes UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) m
WHERE e.cnpj = '12345678000199'
  AND t.descricao = 'ESTIMATIVA_MENSAL'
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
JOIN tb_tipos_parametros_tributarios tp ON p.tipo_parametro_id = tp.id
CROSS JOIN (SELECT 2023 AS ano UNION ALL SELECT 2024 UNION ALL SELECT 2025) y
CROSS JOIN (SELECT 1 AS trimestre UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t
WHERE e.cnpj = '12345678000199'
  AND tp.descricao = 'FORMA_TRIBUTACAO'
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
JOIN tb_tipos_parametros_tributarios tp ON p.tipo_parametro_id = tp.id
CROSS JOIN (SELECT 2023 AS ano UNION ALL SELECT 2024 UNION ALL SELECT 2025) y
CROSS JOIN (SELECT 1 AS trimestre UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t
WHERE e.cnpj = '12345678000199'
  AND tp.descricao = 'FORMA_TRIBUTACAO'
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
JOIN tb_tipos_parametros_tributarios tp ON p.tipo_parametro_id = tp.id
CROSS JOIN (SELECT 2023 AS ano UNION ALL SELECT 2024 UNION ALL SELECT 2025) y
CROSS JOIN (SELECT 1 AS trimestre UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t
WHERE e.cnpj = '12345678000199'
  AND tp.descricao = 'FORMA_TRIBUTACAO'
  AND p.codigo = 'A'
ON CONFLICT (empresa_parametros_tributarios_id, ano, mes, trimestre) DO NOTHING;
