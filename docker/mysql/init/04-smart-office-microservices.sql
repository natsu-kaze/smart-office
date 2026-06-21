SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `smart_office_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_office_org` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_office_approval` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_office_attendance` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_office_message` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_office_file` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_office_search` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `smart_office_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smart_office_system`.`sys_user` LIKE `smart_office`.`sys_user`;
CREATE TABLE IF NOT EXISTS `smart_office_system`.`sys_role` LIKE `smart_office`.`sys_role`;
CREATE TABLE IF NOT EXISTS `smart_office_system`.`sys_menu` LIKE `smart_office`.`sys_menu`;
CREATE TABLE IF NOT EXISTS `smart_office_system`.`sys_user_role` LIKE `smart_office`.`sys_user_role`;
CREATE TABLE IF NOT EXISTS `smart_office_system`.`sys_role_menu` LIKE `smart_office`.`sys_role_menu`;
INSERT IGNORE INTO `smart_office_system`.`sys_user` SELECT * FROM `smart_office`.`sys_user`;
INSERT IGNORE INTO `smart_office_system`.`sys_role` SELECT * FROM `smart_office`.`sys_role`;
INSERT IGNORE INTO `smart_office_system`.`sys_menu` SELECT * FROM `smart_office`.`sys_menu`;
INSERT IGNORE INTO `smart_office_system`.`sys_user_role` SELECT * FROM `smart_office`.`sys_user_role`;
INSERT IGNORE INTO `smart_office_system`.`sys_role_menu` SELECT * FROM `smart_office`.`sys_role_menu`;

DELETE FROM `smart_office_system`.`sys_menu` WHERE `id` IN (2, 3, 4, 5);

INSERT INTO `smart_office_system`.`sys_role` (`id`, `role_code`, `role_name`, `sort`, `status`, `remark`)
VALUES
    (1, 'ADMIN', 'Administrator', 1, 1, 'System administrator'),
    (2, 'MANAGER', 'Department Manager', 2, 1, 'Department approver'),
    (3, 'EMPLOYEE', 'Employee', 3, 1, 'Common employee'),
    (4, 'FINANCE', 'Finance', 4, 1, 'Expense approver')
ON DUPLICATE KEY UPDATE
    `role_code` = VALUES(`role_code`),
    `role_name` = VALUES(`role_name`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`),
    `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `smart_office_system`.`sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `visible`, `status`)
VALUES
    (1, 0, '工作台', 'MENU', '/dashboard', 'dashboard/index', 'dashboard:view', 'data-board', 1, 1, 1),
    (10, 0, '系统管理', 'CATALOG', '/system', NULL, NULL, 'setting', 10, 1, 1),
    (11, 10, '用户管理', 'MENU', '/system/users', 'system/user/index', 'sys:user:list', 'user', 1, 1, 1),
    (12, 10, '角色权限', 'MENU', '/system/roles', 'system/role/index', 'sys:role:list', 'lock', 2, 1, 1),
    (13, 11, '分配用户角色', 'BUTTON', NULL, NULL, 'sys:user:role', NULL, 1, 0, 1),
    (14, 12, '维护角色', 'BUTTON', NULL, NULL, 'sys:role:save', NULL, 1, 0, 1),
    (15, 12, '分配角色菜单', 'BUTTON', NULL, NULL, 'sys:role:menu', NULL, 2, 0, 1),
    (20, 0, '组织架构', 'MENU', '/org', 'org/index', 'org:manage', 'office-building', 20, 1, 1),
    (30, 0, '审批中心', 'MENU', '/approvals', 'approval/index', 'approval:list', 'tickets', 30, 1, 1),
    (31, 30, '维护审批规则', 'BUTTON', NULL, NULL, 'approval:rule:manage', NULL, 1, 0, 1),
    (40, 0, '消息中心', 'MENU', '/messages', 'message/index', 'message:list', 'bell', 40, 1, 1),
    (41, 40, '发布公告', 'BUTTON', NULL, NULL, 'message:announcement:send', NULL, 1, 0, 1),
    (50, 0, '文件中心', 'MENU', '/files', 'file/index', 'file:list', 'folder', 50, 1, 1),
    (51, 50, '删除文件', 'BUTTON', NULL, NULL, 'file:delete', NULL, 1, 0, 1),
    (60, 0, '制度文档', 'MENU', '/policies', 'policy/index', 'policy:list', 'document', 60, 1, 1),
    (61, 60, '维护制度', 'BUTTON', NULL, NULL, 'policy:manage', NULL, 1, 0, 1),
    (70, 0, '考勤打卡', 'MENU', '/attendance', 'attendance/index', 'attendance:list', 'clock', 70, 1, 1),
    (71, 70, '部门考勤', 'BUTTON', NULL, NULL, 'attendance:department:list', NULL, 1, 0, 1)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `menu_name` = VALUES(`menu_name`),
    `menu_type` = VALUES(`menu_type`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `permission` = VALUES(`permission`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `update_time` = CURRENT_TIMESTAMP;

DELETE FROM `smart_office_system`.`sys_user_role` WHERE `user_id` IN (1, 2, 3, 4);
INSERT INTO `smart_office_system`.`sys_user_role` (`id`, `user_id`, `role_id`)
VALUES
    (1, 1, 1),
    (2, 2, 2),
    (3, 3, 3),
    (4, 4, 4)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`), `update_time` = CURRENT_TIMESTAMP;

DELETE FROM `smart_office_system`.`sys_role_menu` WHERE `role_id` IN (1, 2, 3, 4);
INSERT INTO `smart_office_system`.`sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (1, 1, 1), (2, 1, 10), (3, 1, 11), (4, 1, 12), (5, 1, 13), (6, 1, 14), (7, 1, 15),
    (8, 1, 20), (9, 1, 30), (10, 1, 40), (11, 1, 41), (12, 1, 50), (13, 1, 51),
    (14, 1, 60), (15, 1, 61), (16, 1, 70), (17, 1, 71), (18, 1, 31),
    (19, 2, 1), (20, 2, 20), (21, 2, 30), (22, 2, 31), (23, 2, 40), (24, 2, 41), (25, 2, 50),
    (26, 2, 60), (27, 2, 61), (28, 2, 70), (29, 2, 71),
    (30, 3, 1), (31, 3, 30), (32, 3, 40), (33, 3, 50), (34, 3, 60), (35, 3, 70),
    (36, 4, 1), (37, 4, 30), (38, 4, 40), (39, 4, 50), (40, 4, 60), (41, 4, 70)
ON DUPLICATE KEY UPDATE `menu_id` = VALUES(`menu_id`), `update_time` = CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS `smart_office_org`.`org_company` LIKE `smart_office`.`org_company`;
CREATE TABLE IF NOT EXISTS `smart_office_org`.`org_department` LIKE `smart_office`.`org_department`;
CREATE TABLE IF NOT EXISTS `smart_office_org`.`org_position` LIKE `smart_office`.`org_position`;
CREATE TABLE IF NOT EXISTS `smart_office_org`.`org_employee` LIKE `smart_office`.`org_employee`;
INSERT IGNORE INTO `smart_office_org`.`org_company` SELECT * FROM `smart_office`.`org_company`;
INSERT IGNORE INTO `smart_office_org`.`org_department` SELECT * FROM `smart_office`.`org_department`;
INSERT IGNORE INTO `smart_office_org`.`org_position` SELECT * FROM `smart_office`.`org_position`;
INSERT IGNORE INTO `smart_office_org`.`org_employee` SELECT * FROM `smart_office`.`org_employee`;

CREATE TABLE IF NOT EXISTS `smart_office_approval`.`approval_form` LIKE `smart_office`.`approval_form`;
CREATE TABLE IF NOT EXISTS `smart_office_approval`.`approval_record` LIKE `smart_office`.`approval_record`;
CREATE TABLE IF NOT EXISTS `smart_office_approval`.`approval_process` LIKE `smart_office`.`approval_process`;
CREATE TABLE IF NOT EXISTS `smart_office_approval`.`approval_rule` LIKE `smart_office`.`approval_rule`;
CREATE TABLE IF NOT EXISTS `smart_office_approval`.`approval_attachment` LIKE `smart_office`.`approval_attachment`;
INSERT INTO `smart_office_approval`.`approval_rule` (`id`, `approval_type`, `name`, `priority`, `amount_limit`, `applicant_role_code`, `dept_id`, `required_roles`, `timeout_hours`, `timeout_action`, `status`, `remark`)
VALUES
    (1, 'LEAVE', 'Default Leave Rule', 10, NULL, NULL, NULL, 'DEPARTMENT_LEADER', NULL, NULL, 1, '请假默认由申请人所在部门主管审批'),
    (2, 'EXPENSE', 'Default Expense Rule', 10, 1000.00, NULL, NULL, 'DEPARTMENT_LEADER,ROLE:FINANCE', NULL, NULL, 1, '报销金额达到 1000 元后追加财务审批')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `priority` = VALUES(`priority`),
    `amount_limit` = VALUES(`amount_limit`),
    `applicant_role_code` = VALUES(`applicant_role_code`),
    `dept_id` = VALUES(`dept_id`),
    `required_roles` = VALUES(`required_roles`),
    `timeout_hours` = VALUES(`timeout_hours`),
    `timeout_action` = VALUES(`timeout_action`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`),
    `update_time` = CURRENT_TIMESTAMP;
ALTER TABLE `smart_office_approval`.`approval_form` MODIFY COLUMN `content` TEXT DEFAULT NULL;
SET @add_leave_start_date = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `smart_office_approval`.`approval_form` ADD COLUMN `leave_start_date` DATE DEFAULT NULL AFTER `amount`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smart_office_approval'
      AND TABLE_NAME = 'approval_form'
      AND COLUMN_NAME = 'leave_start_date'
);
PREPARE stmt FROM @add_leave_start_date;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_leave_end_date = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `smart_office_approval`.`approval_form` ADD COLUMN `leave_end_date` DATE DEFAULT NULL AFTER `leave_start_date`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smart_office_approval'
      AND TABLE_NAME = 'approval_form'
      AND COLUMN_NAME = 'leave_end_date'
);
PREPARE stmt FROM @add_leave_end_date;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `smart_office_attendance`.`attendance_rule` LIKE `smart_office`.`attendance_rule`;
CREATE TABLE IF NOT EXISTS `smart_office_attendance`.`attendance_record` LIKE `smart_office`.`attendance_record`;
CREATE TABLE IF NOT EXISTS `smart_office_attendance`.`attendance_summary` LIKE `smart_office`.`attendance_summary`;
INSERT IGNORE INTO `smart_office_attendance`.`attendance_rule` SELECT * FROM `smart_office`.`attendance_rule`;

CREATE TABLE IF NOT EXISTS `smart_office_message`.`message_notice` LIKE `smart_office`.`message_notice`;
CREATE TABLE IF NOT EXISTS `smart_office_message`.`message_todo` LIKE `smart_office`.`message_todo`;

SET @add_sender_name = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `smart_office_message`.`message_notice` ADD COLUMN `sender_name` VARCHAR(64) DEFAULT NULL AFTER `content`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smart_office_message'
      AND TABLE_NAME = 'message_notice'
      AND COLUMN_NAME = 'sender_name'
);
PREPARE stmt FROM @add_sender_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `smart_office_file`.`file_record` LIKE `smart_office`.`file_record`;

CREATE TABLE IF NOT EXISTS `smart_office_search`.`policy_document` LIKE `smart_office`.`policy_document`;
CREATE TABLE IF NOT EXISTS `smart_office_search`.`policy_document_chunk` LIKE `smart_office`.`policy_document_chunk`;

CREATE TABLE IF NOT EXISTS `smart_office_ai`.`ai_conversation` LIKE `smart_office`.`ai_conversation`;
CREATE TABLE IF NOT EXISTS `smart_office_ai`.`ai_message` LIKE `smart_office`.`ai_message`;
CREATE TABLE IF NOT EXISTS `smart_office_ai`.`ai_prompt_template` LIKE `smart_office`.`ai_prompt_template`;
