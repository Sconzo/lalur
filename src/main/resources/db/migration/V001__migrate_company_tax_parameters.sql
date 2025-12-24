-- ============================================================================
-- Migration: Migrate CNAE, Qualificação PJ, Natureza Jurídica to Tax Parameters
-- Version: V001
-- Date: 2025-12-07
-- ADR: ADR-001 v2.0
-- ============================================================================
--
-- This migration implements the architectural change to move CNAE,
-- Qualificação PJ, and Natureza Jurídica from direct columns in tb_empresa
-- to tax parameters managed through tb_empresa_parametros_tributarios.
--
-- IMPORTANT: Run this migration when the database is NOT in use by the application.
-- The migration will fail if there are companies with NULL values in the 3 fields.
--
-- ============================================================================

-- Step 1: Create tb_parametros_tributarios if it doesn't exist
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_parametros_tributarios (
    id BIGSERIAL PRIMARY KEY,

    -- Identificação
    codigo VARCHAR(100) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    descricao TEXT,

    -- Campos de auditoria
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT,

    -- Composite unique constraint (codigo + tipo must be unique together)
    CONSTRAINT uk_parametros_codigo_tipo UNIQUE (codigo, tipo)
);

CREATE INDEX IF NOT EXISTS idx_parametros_tributarios_tipo
    ON tb_parametros_tributarios(tipo);
CREATE INDEX IF NOT EXISTS idx_parametros_tributarios_codigo
    ON tb_parametros_tributarios(codigo);
CREATE INDEX IF NOT EXISTS idx_parametros_tributarios_status
    ON tb_parametros_tributarios(status);

COMMENT ON TABLE tb_parametros_tributarios IS
    'Parâmetros tributários (estrutura flat sem hierarquia - ADR-001 v2.0)';
COMMENT ON COLUMN tb_parametros_tributarios.tipo IS
    'Tipo/categoria: CNAE, QUALIFICACAO_PJ, NATUREZA_JURIDICA, IRPJ, CSLL, etc.';

-- Step 2: Create tb_empresa_parametros_tributarios if it doesn't exist
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_empresa_parametros_tributarios (
    id BIGSERIAL PRIMARY KEY,

    empresa_id BIGINT NOT NULL REFERENCES tb_empresa(id) ON DELETE CASCADE,
    parametro_tributario_id BIGINT NOT NULL
        REFERENCES tb_parametros_tributarios(id) ON DELETE RESTRICT,

    -- Campos de auditoria (simplificados)
    criado_por BIGINT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Garantir unicidade da associação
    CONSTRAINT uk_empresa_parametro UNIQUE (empresa_id, parametro_tributario_id)
);

CREATE INDEX IF NOT EXISTS idx_empresa_param_empresa_id
    ON tb_empresa_parametros_tributarios(empresa_id);
CREATE INDEX IF NOT EXISTS idx_empresa_param_parametro_id
    ON tb_empresa_parametros_tributarios(parametro_tributario_id);

COMMENT ON TABLE tb_empresa_parametros_tributarios IS
    'Associação entre empresas e parâmetros tributários com auditoria';

-- Step 3: Migrate existing data from tb_empresa columns to tax parameters
-- ============================================================================
-- This step creates tax parameters from existing company data and establishes
-- the associations. It only runs if the columns still exist.

DO $$
DECLARE
    column_exists BOOLEAN;
    companies_count INTEGER;
BEGIN
    -- Check if the old columns still exist
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_empresa'
        AND column_name = 'cnae'
    ) INTO column_exists;

    IF column_exists THEN
        RAISE NOTICE 'Old columns found. Starting migration...';

        -- Count companies to migrate
        EXECUTE 'SELECT COUNT(*) FROM tb_empresa' INTO companies_count;
        RAISE NOTICE 'Found % companies to migrate', companies_count;

        -- Migrate CNAE values
        INSERT INTO tb_parametros_tributarios (codigo, tipo, descricao, status)
        SELECT DISTINCT
            cnae,
            'CNAE',
            'CNAE: ' || cnae,
            'ACTIVE'
        FROM tb_empresa
        WHERE cnae IS NOT NULL
        AND cnae NOT IN (
            SELECT codigo FROM tb_parametros_tributarios WHERE tipo = 'CNAE'
        );

        RAISE NOTICE 'CNAE parameters created';

        -- Migrate Qualificação PJ values
        INSERT INTO tb_parametros_tributarios (codigo, tipo, descricao, status)
        SELECT DISTINCT
            qualificacao_pessoa_juridica,
            'QUALIFICACAO_PJ',
            qualificacao_pessoa_juridica,
            'ACTIVE'
        FROM tb_empresa
        WHERE qualificacao_pessoa_juridica IS NOT NULL
        AND qualificacao_pessoa_juridica NOT IN (
            SELECT codigo FROM tb_parametros_tributarios WHERE tipo = 'QUALIFICACAO_PJ'
        );

        RAISE NOTICE 'Qualificação PJ parameters created';

        -- Migrate Natureza Jurídica values
        INSERT INTO tb_parametros_tributarios (codigo, tipo, descricao, status)
        SELECT DISTINCT
            natureza_juridica,
            'NATUREZA_JURIDICA',
            natureza_juridica,
            'ACTIVE'
        FROM tb_empresa
        WHERE natureza_juridica IS NOT NULL
        AND natureza_juridica NOT IN (
            SELECT codigo FROM tb_parametros_tributarios WHERE tipo = 'NATUREZA_JURIDICA'
        );

        RAISE NOTICE 'Natureza Jurídica parameters created';

        -- Create associations for CNAE
        INSERT INTO tb_empresa_parametros_tributarios (empresa_id, parametro_tributario_id)
        SELECT
            e.id,
            p.id
        FROM tb_empresa e
        INNER JOIN tb_parametros_tributarios p
            ON p.codigo = e.cnae AND p.tipo = 'CNAE'
        WHERE e.cnae IS NOT NULL;

        RAISE NOTICE 'CNAE associations created';

        -- Create associations for Qualificação PJ
        INSERT INTO tb_empresa_parametros_tributarios (empresa_id, parametro_tributario_id)
        SELECT
            e.id,
            p.id
        FROM tb_empresa e
        INNER JOIN tb_parametros_tributarios p
            ON p.codigo = e.qualificacao_pessoa_juridica
            AND p.tipo = 'QUALIFICACAO_PJ'
        WHERE e.qualificacao_pessoa_juridica IS NOT NULL;

        RAISE NOTICE 'Qualificação PJ associations created';

        -- Create associations for Natureza Jurídica
        INSERT INTO tb_empresa_parametros_tributarios (empresa_id, parametro_tributario_id)
        SELECT
            e.id,
            p.id
        FROM tb_empresa e
        INNER JOIN tb_parametros_tributarios p
            ON p.codigo = e.natureza_juridica
            AND p.tipo = 'NATUREZA_JURIDICA'
        WHERE e.natureza_juridica IS NOT NULL;

        RAISE NOTICE 'Natureza Jurídica associations created';
        RAISE NOTICE 'Migration completed successfully';
    ELSE
        RAISE NOTICE 'Old columns not found. Migration already applied or not needed.';
    END IF;
END $$;

-- Step 4: Remove old columns from tb_empresa
-- ============================================================================
-- Only drop columns if they exist

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_empresa'
        AND column_name = 'cnae'
    ) THEN
        ALTER TABLE tb_empresa DROP COLUMN cnae;
        RAISE NOTICE 'Column cnae dropped';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_empresa'
        AND column_name = 'qualificacao_pessoa_juridica'
    ) THEN
        ALTER TABLE tb_empresa DROP COLUMN qualificacao_pessoa_juridica;
        RAISE NOTICE 'Column qualificacao_pessoa_juridica dropped';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_empresa'
        AND column_name = 'natureza_juridica'
    ) THEN
        ALTER TABLE tb_empresa DROP COLUMN natureza_juridica;
        RAISE NOTICE 'Column natureza_juridica dropped';
    END IF;
END $$;

-- Step 5: Add helpful comment to tb_empresa
-- ============================================================================

COMMENT ON TABLE tb_empresa IS
    'Empresas gerenciadas no sistema. CNAE, Qualificação PJ e Natureza Jurídica '
    'são gerenciados via tb_empresa_parametros_tributarios (ADR-001 v2.0)';

-- ============================================================================
-- Migration Complete
-- ============================================================================
--
-- Verification queries:
--
-- 1. Check tax parameters were created:
--    SELECT tipo, COUNT(*) FROM tb_parametros_tributarios GROUP BY tipo;
--
-- 2. Check associations were created:
--    SELECT COUNT(*) FROM tb_empresa_parametros_tributarios;
--
-- 3. Verify each company has 3 required parameters:
--    SELECT empresa_id, COUNT(*)
--    FROM tb_empresa_parametros_tributarios
--    GROUP BY empresa_id
--    HAVING COUNT(*) < 3;
--    (Should return 0 rows for existing companies)
--
-- 4. Confirm old columns are gone:
--    SELECT column_name FROM information_schema.columns
--    WHERE table_name = 'tb_empresa';
--
-- ============================================================================
