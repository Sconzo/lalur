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
-- natureza: GLOBAL (valor único), MONTHLY (mensal), QUARTERLY (trimestral)
INSERT INTO tb_parametros_tributarios (
    codigo,
    tipo,
    descricao,
    natureza,
    status,
    criado_em,
    criado_por,
    atualizado_em
) VALUES
    ('1', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('2', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real/Arbitrado', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('3', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('4', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real/Arbitrado', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('5', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('6', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Arbitrado', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('7', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Arbitrado', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('8', 'FORMA_TRIB_LUCRO_REAL', 'Imune de IRPJ', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('9', 'FORMA_TRIB_LUCRO_REAL', 'Isento do IRPJ', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('A', 'PERIODO_DE_APURACAO', 'Anual', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('T', 'PERIODO_DE_APURACAO', 'Trimestral', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('01', 'QUALIFICACAO_PJ', 'PJ em Geral', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('02', 'QUALIFICACAO_PJ', 'PJ Componente do Sistema Financeiro', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('03', 'QUALIFICACAO_PJ', 'Sociedades Seguradoras, de Capitalização e Previdência', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de caixa', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('2', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de competência', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1', 'ESTIMATIVA_MENSAL', 'Receita Bruta e Acréscimos', 'MONTHLY', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('2', 'ESTIMATIVA_MENSAL', 'Balanço/Balancete de Suspensão/Redução', 'MONTHLY', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('P', 'FORMA_TRIBUTACAO', 'Presumido', 'QUARTERLY', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('R', 'FORMA_TRIBUTACAO', 'Real', 'QUARTERLY', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('A', 'FORMA_TRIBUTACAO', 'Arbitrado', 'QUARTERLY', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1015', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Federal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1023', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Estadual ou do Distrito Federal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1031', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Municipal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1040', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Federal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1058', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Estadual ou do Distrito Federal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1066', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Municipal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1074', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Federal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1082', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Estadual', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1104', 'NATUREZA_JURIDICA', 'Autarquia Federal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1112', 'NATUREZA_JURIDICA', 'Autarquia Estadual ou do Distrito Federal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('1120', 'NATUREZA_JURIDICA', 'Autarquia Municipal', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111301', 'CNAE', 'Cultivo de arroz', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111302', 'CNAE', 'Cultivo de milho', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111303', 'CNAE', 'Cultivo de trigo', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0111399', 'CNAE', 'Cultivo de outros cereais não especificados anteriormente', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0112101', 'CNAE', 'Cultivo de algodão herbáceo', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0112102', 'CNAE', 'Cultivo de juta', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0112199', 'CNAE', 'Cultivo de outras fibras de lavoura temporária não especificadas anteriormente', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0113000', 'CNAE', 'Cultivo de cana-de-açúcar', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0114800', 'CNAE', 'Cultivo de fumo', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0115600', 'CNAE', 'Cultivo de soja', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0116401', 'CNAE', 'Cultivo de amendoim', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('0116402', 'CNAE', 'Cultivo de girassol', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
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
