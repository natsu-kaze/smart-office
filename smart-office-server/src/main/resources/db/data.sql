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
    (1, 0, 'System Management', 'CATALOG', '/system', NULL, NULL, 'setting', 1, 1, 1),
    (2, 1, 'User Management', 'MENU', '/system/users', 'system/user/index', 'sys:user:list', 'user', 1, 1, 1),
    (3, 0, 'Approval Center', 'CATALOG', '/approval', NULL, NULL, 'tickets', 2, 1, 1),
    (4, 3, 'My Applications', 'MENU', '/approval/my', 'approval/my/index', 'approval:form:list', 'document', 1, 1, 1),
    (5, 3, 'My Todos', 'MENU', '/approval/todo', 'approval/todo/index', 'approval:todo:list', 'todo', 2, 1, 1)
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
