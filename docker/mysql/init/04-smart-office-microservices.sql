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
ALTER TABLE `smart_office_approval`.`approval_form` MODIFY COLUMN `content` TEXT DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `smart_office_attendance`.`attendance_rule` LIKE `smart_office`.`attendance_rule`;
CREATE TABLE IF NOT EXISTS `smart_office_attendance`.`attendance_record` LIKE `smart_office`.`attendance_record`;
CREATE TABLE IF NOT EXISTS `smart_office_attendance`.`attendance_summary` LIKE `smart_office`.`attendance_summary`;
INSERT IGNORE INTO `smart_office_attendance`.`attendance_rule` SELECT * FROM `smart_office`.`attendance_rule`;

CREATE TABLE IF NOT EXISTS `smart_office_message`.`message_notice` LIKE `smart_office`.`message_notice`;
CREATE TABLE IF NOT EXISTS `smart_office_message`.`message_todo` LIKE `smart_office`.`message_todo`;

CREATE TABLE IF NOT EXISTS `smart_office_file`.`file_record` LIKE `smart_office`.`file_record`;

CREATE TABLE IF NOT EXISTS `smart_office_search`.`policy_document` LIKE `smart_office`.`policy_document`;
CREATE TABLE IF NOT EXISTS `smart_office_search`.`policy_document_chunk` LIKE `smart_office`.`policy_document_chunk`;

CREATE TABLE IF NOT EXISTS `smart_office_ai`.`ai_conversation` LIKE `smart_office`.`ai_conversation`;
CREATE TABLE IF NOT EXISTS `smart_office_ai`.`ai_message` LIKE `smart_office`.`ai_message`;
CREATE TABLE IF NOT EXISTS `smart_office_ai`.`ai_prompt_template` LIKE `smart_office`.`ai_prompt_template`;
