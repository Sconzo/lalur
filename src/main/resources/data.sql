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
    status
) VALUES
    ('1', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real', 'ACTIVE'),
    ('2', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real/Arbitrado', 'ACTIVE'),
    ('3', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real', 'ACTIVE'),
    ('4', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real/Arbitrado', 'ACTIVE'),
    ('5', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido', 'ACTIVE'),
    ('6', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Arbitrado', 'ACTIVE'),
    ('7', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Arbitrado', 'ACTIVE'),
    ('8', 'FORMA_TRIB_LUCRO_REAL', 'Imune de IRPJ', 'ACTIVE'),
    ('9', 'FORMA_TRIB_LUCRO_REAL', 'Isento do IRPJ', 'ACTIVE'),
    ('A', 'PERIODO_DE_APURACAO', 'Anual', 'ACTIVE'),
    ('T', 'PERIODO_DE_APURACAO', 'Trimestral', 'ACTIVE'),
    ('01', 'QUALIFICACAO_PESSOA_JURIDICA', 'PJ em Geral', 'ACTIVE'),
    ('02', 'QUALIFICACAO_PESSOA_JURIDICA', 'PJ Componente do Sistema Financeiro', 'ACTIVE'),
    ('03', 'QUALIFICACAO_PESSOA_JURIDICA', 'Sociedades Seguradoras, de Capitalização e Previdência', 'ACTIVE'),
    ('1', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de caixa', 'ACTIVE'),
    ('2', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de competência', 'ACTIVE'),
    ('1', 'ESTIMATIVA_MENSAL', 'Receita Bruta e Acréscimos', 'ACTIVE'),
    ('2', 'ESTIMATIVA_MENSAL', 'Balanço/Balancete de Suspensão/Redução', 'ACTIVE'),
    ('P', 'FORMA_TRIBUTACAO', 'Presumido', 'ACTIVE'),
    ('R', 'FORMA_TRIBUTACAO', 'Real', 'ACTIVE'),
    ('A', 'FORMA_TRIBUTACAO', 'Arbitrado', 'ACTIVE'),
    ('1015', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Federal', 'ACTIVE'),
    ('1023', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Estadual ou do Distrito Federal', 'ACTIVE'),
    ('1031', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Municipal', 'ACTIVE'),
    ('1040', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Federal', 'ACTIVE'),
    ('1058', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Estadual ou do Distrito Federal', 'ACTIVE'),
    ('1066', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Municipal', 'ACTIVE'),
    ('1074', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Federal', 'ACTIVE'),
    ('1082', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Estadual', 'ACTIVE'),
    ('1104', 'NATUREZA_JURIDICA', 'Autarquia Federal', 'ACTIVE'),
    ('1112', 'NATUREZA_JURIDICA', 'Autarquia Estadual ou do Distrito Federal', 'ACTIVE'),
    ('1120', 'NATUREZA_JURIDICA', 'Autarquia Municipal', 'ACTIVE'),
    ('0111301', 'CNAE', 'Cultivo de arroz', 'ACTIVE'),
    ('0111302', 'CNAE', 'Cultivo de milho', 'ACTIVE'),
    ('0111303', 'CNAE', 'Cultivo de trigo', 'ACTIVE'),
    ('0111399', 'CNAE', 'Cultivo de outros cereais não especificados anteriormente', 'ACTIVE'),
    ('0112101', 'CNAE', 'Cultivo de algodão herbáceo', 'ACTIVE'),
    ('0112102', 'CNAE', 'Cultivo de juta', 'ACTIVE'),
    ('0112199', 'CNAE', 'Cultivo de outras fibras de lavoura temporária não especificadas anteriormente', 'ACTIVE'),
    ('0113000', 'CNAE', 'Cultivo de cana-de-açúcar', 'ACTIVE'),
    ('0114800', 'CNAE', 'Cultivo de fumo', 'ACTIVE'),
    ('0115600', 'CNAE', 'Cultivo de soja', 'ACTIVE'),
    ('0116401', 'CNAE', 'Cultivo de amendoim', 'ACTIVE'),
    ('0116402', 'CNAE', 'Cultivo de girassol', 'ACTIVE')
ON CONFLICT (codigo, tipo) DO NOTHING;

-- ============================================================================
-- Valores Parametros Temporais - Seed Data
-- ============================================================================
-- Nota: Seed data for tb_valores_parametros_temporais can be added manually
-- or through application logic as needed. PL/pgSQL blocks in data.sql
-- cause parsing issues with Spring Boot's script executor.
