-- ============================================================================
-- Alter PlanoDeContas and ContaReferencial
-- Version: V012
-- Date: 2026-02-17
-- ============================================================================
--
-- Changes:
-- 1. Make conta_referencial_id nullable in tb_plano_de_contas
-- 2. Add 'modelo' column to tb_conta_referencial
--
-- ============================================================================

-- 1. Make conta_referencial_id nullable in tb_plano_de_contas
ALTER TABLE tb_plano_de_contas ALTER COLUMN conta_referencial_id DROP NOT NULL;

-- 2. Add 'modelo' column to tb_conta_referencial (for future use)
ALTER TABLE tb_conta_referencial ADD COLUMN IF NOT EXISTS modelo VARCHAR(50);
