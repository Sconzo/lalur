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
    criado_por
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
    1
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
    criado_por
) VALUES (
    'Admin',
    'Sistema',
    'admin@gmail.com',
    '$2a$12$wVByGsG.Ko94ePxBn/dTt.sTzh7RRXYkRH.P2TYKEo.8HjhyvOI9.',
    'ADMIN',
    'ACTIVE',
    true,
    CURRENT_TIMESTAMP,
    1
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
    criado_por
) VALUES
    ('1', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('2', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real/Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('3', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('4', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real/Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('5', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('6', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('7', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('8', 'FORMA_TRIB_LUCRO_REAL', 'Imune de IRPJ', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('9', 'FORMA_TRIB_LUCRO_REAL', 'Isento do IRPJ', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('A', 'PERIODO_DE_APURACAO', 'Anual', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('T', 'PERIODO_DE_APURACAO', 'Trimestral', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('01', 'QUALIFICACAO_PESSOA_JURIDICA', 'PJ em Geral', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('02', 'QUALIFICACAO_PESSOA_JURIDICA', 'PJ Componente do Sistema Financeiro', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('03', 'QUALIFICACAO_PESSOA_JURIDICA', 'Sociedades Seguradoras, de Capitalização e Previdência', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de caixa', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('2', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de competência', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1', 'ESTIMATIVA_MENSAL', 'Receita Bruta e Acréscimos', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('2', 'ESTIMATIVA_MENSAL', 'Balanço/Balancete de Suspensão/Redução', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('P', 'FORMA_TRIBUTACAO', 'Presumido', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('R', 'FORMA_TRIBUTACAO', 'Real', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('A', 'FORMA_TRIBUTACAO', 'Arbitrado', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1015', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1023', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Estadual ou do Distrito Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1031', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Municipal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1040', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1058', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Estadual ou do Distrito Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1066', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Municipal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1074', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1082', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Estadual', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1104', 'NATUREZA_JURIDICA', 'Autarquia Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1112', 'NATUREZA_JURIDICA', 'Autarquia Estadual ou do Distrito Federal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('1120', 'NATUREZA_JURIDICA', 'Autarquia Municipal', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0111301', 'CNAE', 'Cultivo de arroz', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0111302', 'CNAE', 'Cultivo de milho', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0111303', 'CNAE', 'Cultivo de trigo', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0111399', 'CNAE', 'Cultivo de outros cereais não especificados anteriormente', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0112101', 'CNAE', 'Cultivo de algodão herbáceo', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0112102', 'CNAE', 'Cultivo de juta', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0112199', 'CNAE', 'Cultivo de outras fibras de lavoura temporária não especificadas anteriormente', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0113000', 'CNAE', 'Cultivo de cana-de-açúcar', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0114800', 'CNAE', 'Cultivo de fumo', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0115600', 'CNAE', 'Cultivo de soja', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0116401', 'CNAE', 'Cultivo de amendoim', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('0116402', 'CNAE', 'Cultivo de girassol', 'ACTIVE', CURRENT_TIMESTAMP, 1)
ON CONFLICT (codigo, tipo) DO NOTHING;

-- ============================================================================
-- Valores Parametros Temporais - Seed Data
-- ============================================================================
-- Nota: Seed data for tb_valores_parametros_temporais can be added manually
-- or through application logic as needed. PL/pgSQL blocks in data.sql
-- cause parsing issues with Spring Boot's script executor.
