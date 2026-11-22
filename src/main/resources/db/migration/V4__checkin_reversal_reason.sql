-- Reason entered by staff when a check-in is reversed (optional)
ALTER TABLE check_in_records ADD COLUMN IF NOT EXISTS reversal_reason TEXT;
