ALTER TABLE apps_versions ADD COLUMN IF NOT EXISTS dtn_system_input_dir TEXT;
ALTER TABLE apps_versions ADD COLUMN IF NOT EXISTS dtn_system_output_dir TEXT;