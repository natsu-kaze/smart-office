-- V002: Approval rule enhancement
-- Adds name, priority, applicant_role_code, dept_id, timeout_hours, timeout_action columns

-- Use stored procedure for safe ALTER TABLE (skip if column already exists)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_ar_column(IN col_name VARCHAR(64), IN col_def VARCHAR(256))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_rule' AND COLUMN_NAME = col_name
    ) THEN
        SET @stmt = CONCAT('ALTER TABLE approval_rule ADD COLUMN ', col_name, ' ', col_def);
        PREPARE stmt FROM @stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL add_ar_column('name', 'VARCHAR(128) DEFAULT NULL COMMENT ''rule display name'' AFTER approval_type');
CALL add_ar_column('priority', 'INT NOT NULL DEFAULT 0 COMMENT ''higher = checked first'' AFTER name');
CALL add_ar_column('applicant_role_code', 'VARCHAR(64) DEFAULT NULL COMMENT ''applicant must have this role'' AFTER amount_limit');
CALL add_ar_column('dept_id', 'BIGINT DEFAULT NULL COMMENT ''scope to department, null=global'' AFTER applicant_role_code');
CALL add_ar_column('timeout_hours', 'INT DEFAULT NULL COMMENT ''hours before timeout triggers'' AFTER required_roles');
CALL add_ar_column('timeout_action', 'VARCHAR(32) DEFAULT NULL COMMENT ''AUTO_APPROVE|AUTO_REJECT|ESCALATE'' AFTER timeout_hours');

DROP PROCEDURE IF EXISTS add_ar_column;

UPDATE approval_rule SET priority = 10 WHERE priority IS NULL OR priority = 0;
