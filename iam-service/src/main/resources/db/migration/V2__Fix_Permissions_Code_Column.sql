-- Fix permissions table: change 'name' column to 'code' to match entity
-- This migration handles the case where V1 created the table with 'name' instead of 'code'

-- Check if 'name' column exists and migrate to 'code'
DO $$
BEGIN
    -- If 'name' column exists but 'code' doesn't, rename it
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = current_schema() 
        AND table_name = 'permissions' 
        AND column_name = 'name'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = current_schema() 
        AND table_name = 'permissions' 
        AND column_name = 'code'
    ) THEN
        -- Rename name column to code
        ALTER TABLE permissions RENAME COLUMN name TO code;
        
        -- Update the unique constraint/index
        DROP INDEX IF EXISTS idx_permission_name;
        CREATE INDEX IF NOT EXISTS idx_permission_code ON permissions(code) WHERE deleted = FALSE;
        
        -- Update the unique constraint name if it exists
        ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_name_key;
        ALTER TABLE permissions ADD CONSTRAINT permissions_code_key UNIQUE (code);
    END IF;
END $$;

