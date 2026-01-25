-- Migration: Add natureza column to tb_parametros_tributarios
-- Story: 2.11 - Enum de Natureza do Parâmetro Tributário

-- Add column with default value for existing records
ALTER TABLE tb_parametros_tributarios
ADD COLUMN natureza VARCHAR(20) NOT NULL DEFAULT 'GLOBAL';

-- Add CHECK constraint for valid values
ALTER TABLE tb_parametros_tributarios
ADD CONSTRAINT chk_natureza CHECK (natureza IN ('GLOBAL', 'MONTHLY', 'QUARTERLY'));

-- Create index for filtering by nature
CREATE INDEX idx_parametros_tributarios_natureza ON tb_parametros_tributarios(natureza);
