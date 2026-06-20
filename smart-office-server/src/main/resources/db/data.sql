USE smart_office;

INSERT INTO sys_user (id, username, password, real_name, phone, email, status)
VALUES
    (1, 'admin', '{noop}123456', 'System Admin', '13800000001', 'admin@example.com', 1),
    (2, 'manager', '{noop}123456', 'Department Manager', '13800000002', 'manager@example.com', 1),
    (3, 'employee', '{noop}123456', 'Employee', '13800000003', 'employee@example.com', 1),
    (4, 'finance', '{noop}123456', 'Finance', '13800000004', 'finance@example.com', 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_role (id, role_code, role_name, sort, status, remark)
VALUES
    (1, 'ADMIN', 'Administrator', 1, 1, 'System administrator'),
    (2, 'MANAGER', 'Department Manager', 2, 1, 'Department approver'),
    (3, 'EMPLOYEE', 'Employee', 3, 1, 'Common employee'),
    (4, 'FINANCE', 'Finance', 4, 1, 'Expense approver')
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES
    (1, 1, 1),
    (2, 2, 2),
    (3, 3, 3),
    (4, 4, 4)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, visible, status)
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
    (40, 0, '消息中心', 'MENU', '/messages', 'message/index', 'message:list', 'bell', 40, 1, 1),
    (41, 40, '发布公告', 'BUTTON', NULL, NULL, 'message:announcement:send', NULL, 1, 0, 1),
    (50, 0, '文件中心', 'MENU', '/files', 'file/index', 'file:list', 'folder', 50, 1, 1),
    (51, 50, '删除文件', 'BUTTON', NULL, NULL, 'file:delete', NULL, 1, 0, 1),
    (60, 0, '制度文档', 'MENU', '/policies', 'policy/index', 'policy:list', 'document', 60, 1, 1),
    (61, 60, '维护制度', 'BUTTON', NULL, NULL, 'policy:manage', NULL, 1, 0, 1),
    (70, 0, '考勤打卡', 'MENU', '/attendance', 'attendance/index', 'attendance:list', 'clock', 70, 1, 1),
    (71, 70, '部门考勤', 'BUTTON', NULL, NULL, 'attendance:department:list', NULL, 1, 0, 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES
    (1, 1, 1), (2, 1, 10), (3, 1, 11), (4, 1, 12), (5, 1, 13), (6, 1, 14), (7, 1, 15),
    (8, 1, 20), (9, 1, 30), (10, 1, 40), (11, 1, 41), (12, 1, 50), (13, 1, 51),
    (14, 1, 60), (15, 1, 61), (16, 1, 70), (17, 1, 71),
    (18, 2, 1), (19, 2, 20), (20, 2, 30), (21, 2, 40), (22, 2, 41), (23, 2, 50),
    (24, 2, 60), (25, 2, 61), (26, 2, 70), (27, 2, 71),
    (28, 3, 1), (29, 3, 30), (30, 3, 40), (31, 3, 50), (32, 3, 60), (33, 3, 70),
    (34, 4, 1), (35, 4, 30), (36, 4, 40), (37, 4, 50), (38, 4, 60), (39, 4, 70)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO org_company (id, company_code, company_name, contact_name, contact_phone, address, status)
VALUES (1, 'SO', 'Smart Office Demo Company', 'System Admin', '13800000001', 'Shanghai', 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO org_department (id, parent_id, department_code, department_name, leader_user_id, sort, status)
VALUES
    (1, 0, 'HQ', 'Headquarters', 1, 1, 1),
    (2, 1, 'RD', 'Research and Development', 2, 1, 1),
    (3, 1, 'FIN', 'Finance Department', 4, 2, 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO org_position (id, department_id, position_code, position_name, sort, status)
VALUES
    (1, 2, 'RD-MANAGER', 'R&D Manager', 1, 1),
    (2, 2, 'RD-ENGINEER', 'Engineer', 2, 1),
    (3, 3, 'FINANCE', 'Finance Specialist', 1, 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO org_employee (id, user_id, employee_no, department_id, position_id, hire_date, employment_status)
VALUES
    (1, 1, 'E0001', 1, NULL, '2024-01-01', 'ACTIVE'),
    (2, 2, 'E0002', 2, 1, '2024-01-01', 'ACTIVE'),
    (3, 3, 'E0003', 2, 2, '2024-01-01', 'ACTIVE'),
    (4, 4, 'E0004', 3, 3, '2024-01-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO attendance_rule (id, rule_name, work_start_time, work_end_time, late_minutes, early_leave_minutes, status)
VALUES (1, 'Default Rule', '09:00:00', '18:00:00', 0, 0, 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
