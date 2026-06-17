package com.natsukaze.smartoffice.orgservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.orgservice.dto.CompanyUpdateRequest;
import com.natsukaze.smartoffice.orgservice.dto.DepartmentLeaderRequest;
import com.natsukaze.smartoffice.orgservice.dto.DepartmentSaveRequest;
import com.natsukaze.smartoffice.orgservice.dto.EmployeePageQuery;
import com.natsukaze.smartoffice.orgservice.dto.EmployeeSaveRequest;
import com.natsukaze.smartoffice.orgservice.dto.PositionPageQuery;
import com.natsukaze.smartoffice.orgservice.dto.PositionSaveRequest;
import com.natsukaze.smartoffice.orgservice.entity.OrgCompany;
import com.natsukaze.smartoffice.orgservice.entity.OrgDepartment;
import com.natsukaze.smartoffice.orgservice.entity.OrgEmployee;
import com.natsukaze.smartoffice.orgservice.entity.OrgPosition;
import com.natsukaze.smartoffice.orgservice.mapper.OrgCompanyMapper;
import com.natsukaze.smartoffice.orgservice.mapper.OrgDepartmentMapper;
import com.natsukaze.smartoffice.orgservice.mapper.OrgEmployeeMapper;
import com.natsukaze.smartoffice.orgservice.mapper.OrgPositionMapper;
import com.natsukaze.smartoffice.orgservice.vo.CompanyVO;
import com.natsukaze.smartoffice.orgservice.vo.DepartmentTreeVO;
import com.natsukaze.smartoffice.orgservice.vo.EmployeeVO;
import com.natsukaze.smartoffice.orgservice.vo.PositionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgCompanyMapper companyMapper;

    private final OrgDepartmentMapper departmentMapper;

    private final OrgPositionMapper positionMapper;

    private final OrgEmployeeMapper employeeMapper;

    private final SystemUserClient systemUserClient;

    public CompanyVO getCompany() {
        OrgCompany company = companyMapper.selectList(new LambdaQueryWrapper<OrgCompany>()
                        .orderByAsc(OrgCompany::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("company not found"));
        return toCompanyVO(company);
    }

    @Transactional
    public CompanyVO updateCompany(Long id, CompanyUpdateRequest request) {
        OrgCompany company = requireCompany(id);
        company.setCompanyName(request.getCompanyName());
        company.setContactName(request.getContactName());
        company.setContactPhone(request.getContactPhone());
        company.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            ensureStatus(request.getStatus());
            company.setStatus(request.getStatus());
        }
        companyMapper.updateById(company);
        return toCompanyVO(requireCompany(id));
    }

    public List<DepartmentTreeVO> departmentTree() {
        List<OrgDepartment> departments = departmentMapper.selectList(new LambdaQueryWrapper<OrgDepartment>()
                .orderByAsc(OrgDepartment::getSort)
                .orderByAsc(OrgDepartment::getId));
        Map<Long, CurrentUserDTO> users = usersByIds(departments.stream()
                .map(OrgDepartment::getLeaderUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<DepartmentTreeVO> nodes = departments.stream()
                .map(dept -> toDepartmentTreeVO(dept, userRealName(users.get(dept.getLeaderUserId()))))
                .toList();
        Map<Long, DepartmentTreeVO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(DepartmentTreeVO::getId, node -> node));
        nodes.forEach(node -> {
            if (node.getParentId() != null && node.getParentId() != 0) {
                DepartmentTreeVO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        });
        return nodes.stream()
                .filter(node -> node.getParentId() == null || node.getParentId() == 0)
                .sorted(Comparator.comparing(DepartmentTreeVO::getSort, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Transactional
    public DepartmentTreeVO createDepartment(DepartmentSaveRequest request) {
        ensureDepartmentCodeAvailable(request.getDepartmentCode(), null);
        if (request.getParentId() != null && request.getParentId() != 0) {
            requireDepartment(request.getParentId());
        }
        if (request.getLeaderUserId() != null) {
            requireUser(request.getLeaderUserId());
        }
        OrgDepartment department = new OrgDepartment();
        fillDepartment(department, request, true);
        departmentMapper.insert(department);
        return toDepartmentTreeVO(department, leaderName(department.getLeaderUserId()));
    }

    @Transactional
    public DepartmentTreeVO updateDepartment(Long id, DepartmentSaveRequest request) {
        OrgDepartment department = requireDepartment(id);
        ensureDepartmentCodeAvailable(request.getDepartmentCode(), id);
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new BusinessException("department parent cannot be itself");
        }
        if (request.getParentId() != null && request.getParentId() != 0) {
            requireDepartment(request.getParentId());
        }
        if (request.getLeaderUserId() != null) {
            requireUser(request.getLeaderUserId());
        }
        fillDepartment(department, request, false);
        departmentMapper.updateById(department);
        return toDepartmentTreeVO(requireDepartment(id), leaderName(department.getLeaderUserId()));
    }

    @Transactional
    public void deleteDepartment(Long id) {
        requireDepartment(id);
        Long childCount = departmentMapper.selectCount(new LambdaQueryWrapper<OrgDepartment>()
                .eq(OrgDepartment::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("department has children");
        }
        Long employeeCount = employeeMapper.selectCount(new LambdaQueryWrapper<OrgEmployee>()
                .eq(OrgEmployee::getDepartmentId, id));
        if (employeeCount > 0) {
            throw new BusinessException("department has employees");
        }
        departmentMapper.deleteById(id);
    }

    @Transactional
    public void setDepartmentLeader(Long id, DepartmentLeaderRequest request) {
        OrgDepartment department = requireDepartment(id);
        requireUser(request.getLeaderUserId());
        department.setLeaderUserId(request.getLeaderUserId());
        departmentMapper.updateById(department);
    }

    public PageResult<PositionVO> pagePositions(PositionPageQuery query) {
        LambdaQueryWrapper<OrgPosition> wrapper = new LambdaQueryWrapper<OrgPosition>()
                .eq(query.getDepartmentId() != null, OrgPosition::getDepartmentId, query.getDepartmentId())
                .eq(query.getStatus() != null, OrgPosition::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(OrgPosition::getPositionCode, query.getKeyword())
                        .or()
                        .like(OrgPosition::getPositionName, query.getKeyword()))
                .orderByAsc(OrgPosition::getSort)
                .orderByDesc(OrgPosition::getCreateTime);
        Page<OrgPosition> page = positionMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toPositionVO));
    }

    @Transactional
    public PositionVO createPosition(PositionSaveRequest request) {
        ensurePositionCodeAvailable(request.getPositionCode(), null);
        if (request.getDepartmentId() != null) {
            requireDepartment(request.getDepartmentId());
        }
        OrgPosition position = new OrgPosition();
        fillPosition(position, request, true);
        positionMapper.insert(position);
        return toPositionVO(position);
    }

    @Transactional
    public PositionVO updatePosition(Long id, PositionSaveRequest request) {
        OrgPosition position = requirePosition(id);
        ensurePositionCodeAvailable(request.getPositionCode(), id);
        if (request.getDepartmentId() != null) {
            requireDepartment(request.getDepartmentId());
        }
        fillPosition(position, request, false);
        positionMapper.updateById(position);
        return toPositionVO(requirePosition(id));
    }

    @Transactional
    public void deletePosition(Long id) {
        requirePosition(id);
        Long employeeCount = employeeMapper.selectCount(new LambdaQueryWrapper<OrgEmployee>()
                .eq(OrgEmployee::getPositionId, id));
        if (employeeCount > 0) {
            throw new BusinessException("position has employees");
        }
        positionMapper.deleteById(id);
    }

    public PageResult<EmployeeVO> pageEmployees(EmployeePageQuery query) {
        LambdaQueryWrapper<OrgEmployee> wrapper = new LambdaQueryWrapper<OrgEmployee>()
                .eq(query.getDepartmentId() != null, OrgEmployee::getDepartmentId, query.getDepartmentId())
                .eq(StringUtils.hasText(query.getEmploymentStatus()), OrgEmployee::getEmploymentStatus, query.getEmploymentStatus())
                .like(StringUtils.hasText(query.getKeyword()), OrgEmployee::getEmployeeNo, query.getKeyword())
                .orderByDesc(OrgEmployee::getCreateTime);
        Page<OrgEmployee> page = employeeMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toEmployeeVO));
    }

    public List<EmployeeVO> departmentEmployees(Long departmentId) {
        requireDepartment(departmentId);
        return employeeMapper.selectList(new LambdaQueryWrapper<OrgEmployee>()
                        .eq(OrgEmployee::getDepartmentId, departmentId)
                        .orderByAsc(OrgEmployee::getEmployeeNo))
                .stream()
                .map(this::toEmployeeVO)
                .toList();
    }

    public OrgEmployeeDTO getEmployeeByUserId(Long userId) {
        OrgEmployee employee = employeeMapper.selectOne(new LambdaQueryWrapper<OrgEmployee>()
                .eq(OrgEmployee::getUserId, userId)
                .last("LIMIT 1"));
        if (employee == null) {
            throw new BusinessException("employee not found");
        }
        OrgDepartment department = departmentMapper.selectById(employee.getDepartmentId());
        return new OrgEmployeeDTO(
                employee.getId(),
                employee.getUserId(),
                employee.getDepartmentId(),
                department == null ? null : department.getDepartmentName(),
                department == null ? null : department.getLeaderUserId());
    }

    @Transactional
    public EmployeeVO createEmployee(EmployeeSaveRequest request) {
        ensureEmployeeNoAvailable(request.getEmployeeNo(), null);
        ensureUserAvailableForEmployee(request.getUserId(), null);
        requireUser(request.getUserId());
        requireDepartment(request.getDepartmentId());
        if (request.getPositionId() != null) {
            requirePosition(request.getPositionId());
        }
        OrgEmployee employee = new OrgEmployee();
        fillEmployee(employee, request);
        employeeMapper.insert(employee);
        return toEmployeeVO(employee);
    }

    @Transactional
    public EmployeeVO updateEmployee(Long id, EmployeeSaveRequest request) {
        OrgEmployee employee = requireEmployee(id);
        ensureEmployeeNoAvailable(request.getEmployeeNo(), id);
        ensureUserAvailableForEmployee(request.getUserId(), id);
        requireUser(request.getUserId());
        requireDepartment(request.getDepartmentId());
        if (request.getPositionId() != null) {
            requirePosition(request.getPositionId());
        }
        fillEmployee(employee, request);
        employeeMapper.updateById(employee);
        return toEmployeeVO(requireEmployee(id));
    }

    @Transactional
    public void deleteEmployee(Long id) {
        requireEmployee(id);
        employeeMapper.deleteById(id);
    }

    private void fillDepartment(OrgDepartment department, DepartmentSaveRequest request, boolean creating) {
        department.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        department.setDepartmentCode(request.getDepartmentCode());
        department.setDepartmentName(request.getDepartmentName());
        department.setLeaderUserId(request.getLeaderUserId());
        department.setSort(request.getSort() == null ? (creating ? 0 : department.getSort()) : request.getSort());
        department.setStatus(request.getStatus() == null ? (creating ? 1 : department.getStatus()) : request.getStatus());
        ensureStatus(department.getStatus());
    }

    private void fillPosition(OrgPosition position, PositionSaveRequest request, boolean creating) {
        position.setDepartmentId(request.getDepartmentId());
        position.setPositionCode(request.getPositionCode());
        position.setPositionName(request.getPositionName());
        position.setSort(request.getSort() == null ? (creating ? 0 : position.getSort()) : request.getSort());
        position.setStatus(request.getStatus() == null ? (creating ? 1 : position.getStatus()) : request.getStatus());
        ensureStatus(position.getStatus());
    }

    private void fillEmployee(OrgEmployee employee, EmployeeSaveRequest request) {
        employee.setUserId(request.getUserId());
        employee.setEmployeeNo(request.getEmployeeNo());
        employee.setDepartmentId(request.getDepartmentId());
        employee.setPositionId(request.getPositionId());
        employee.setHireDate(request.getHireDate());
        employee.setEmploymentStatus(StringUtils.hasText(request.getEmploymentStatus()) ? request.getEmploymentStatus() : "ACTIVE");
    }

    private OrgCompany requireCompany(Long id) {
        OrgCompany company = companyMapper.selectById(id);
        if (company == null) {
            throw new BusinessException("company not found");
        }
        return company;
    }

    private OrgDepartment requireDepartment(Long id) {
        OrgDepartment department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException("department not found");
        }
        return department;
    }

    private OrgPosition requirePosition(Long id) {
        OrgPosition position = positionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException("position not found");
        }
        return position;
    }

    private OrgEmployee requireEmployee(Long id) {
        OrgEmployee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException("employee not found");
        }
        return employee;
    }

    private CurrentUserDTO requireUser(Long id) {
        Result<CurrentUserDTO> result = systemUserClient.getById(id);
        if (result.code() != ErrorCode.SUCCESS.getCode() || result.data() == null) {
            throw new BusinessException("user not found");
        }
        return result.data();
    }

    private void ensureDepartmentCodeAvailable(String code, Long excludeId) {
        Long count = departmentMapper.selectCount(new LambdaQueryWrapper<OrgDepartment>()
                .eq(OrgDepartment::getDepartmentCode, code)
                .ne(excludeId != null, OrgDepartment::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("department code already exists");
        }
    }

    private void ensurePositionCodeAvailable(String code, Long excludeId) {
        Long count = positionMapper.selectCount(new LambdaQueryWrapper<OrgPosition>()
                .eq(OrgPosition::getPositionCode, code)
                .ne(excludeId != null, OrgPosition::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("position code already exists");
        }
    }

    private void ensureEmployeeNoAvailable(String employeeNo, Long excludeId) {
        Long count = employeeMapper.selectCount(new LambdaQueryWrapper<OrgEmployee>()
                .eq(OrgEmployee::getEmployeeNo, employeeNo)
                .ne(excludeId != null, OrgEmployee::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("employee no already exists");
        }
    }

    private void ensureUserAvailableForEmployee(Long userId, Long excludeEmployeeId) {
        Long count = employeeMapper.selectCount(new LambdaQueryWrapper<OrgEmployee>()
                .eq(OrgEmployee::getUserId, userId)
                .ne(excludeEmployeeId != null, OrgEmployee::getId, excludeEmployeeId));
        if (count > 0) {
            throw new BusinessException("user already has employee profile");
        }
    }

    private void ensureStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException("status must be 0 or 1");
        }
    }

    private String leaderName(Long leaderUserId) {
        return leaderUserId == null ? null : userRealName(requireUser(leaderUserId));
    }

    private Map<Long, CurrentUserDTO> usersByIds(List<Long> userIds) {
        return userIds.stream()
                .distinct()
                .map(this::safeUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CurrentUserDTO::userId, Function.identity(), (left, right) -> left));
    }

    private CurrentUserDTO safeUser(Long userId) {
        Result<CurrentUserDTO> result = systemUserClient.getById(userId);
        return result.code() == ErrorCode.SUCCESS.getCode() ? result.data() : null;
    }

    private String userRealName(CurrentUserDTO user) {
        return user == null ? null : user.realName();
    }

    private CompanyVO toCompanyVO(OrgCompany company) {
        return CompanyVO.builder()
                .id(company.getId())
                .companyCode(company.getCompanyCode())
                .companyName(company.getCompanyName())
                .contactName(company.getContactName())
                .contactPhone(company.getContactPhone())
                .address(company.getAddress())
                .status(company.getStatus())
                .build();
    }

    private DepartmentTreeVO toDepartmentTreeVO(OrgDepartment department, String leaderName) {
        return DepartmentTreeVO.builder()
                .id(department.getId())
                .parentId(department.getParentId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .leaderUserId(department.getLeaderUserId())
                .leaderName(leaderName)
                .sort(department.getSort())
                .status(department.getStatus())
                .build();
    }

    private PositionVO toPositionVO(OrgPosition position) {
        OrgDepartment department = position.getDepartmentId() == null ? null : departmentMapper.selectById(position.getDepartmentId());
        return PositionVO.builder()
                .id(position.getId())
                .departmentId(position.getDepartmentId())
                .departmentName(department == null ? null : department.getDepartmentName())
                .positionCode(position.getPositionCode())
                .positionName(position.getPositionName())
                .sort(position.getSort())
                .status(position.getStatus())
                .build();
    }

    private EmployeeVO toEmployeeVO(OrgEmployee employee) {
        CurrentUserDTO user = safeUser(employee.getUserId());
        OrgDepartment department = departmentMapper.selectById(employee.getDepartmentId());
        OrgPosition position = employee.getPositionId() == null ? null : positionMapper.selectById(employee.getPositionId());
        return EmployeeVO.builder()
                .id(employee.getId())
                .userId(employee.getUserId())
                .username(user == null ? null : user.username())
                .realName(user == null ? null : user.realName())
                .employeeNo(employee.getEmployeeNo())
                .departmentId(employee.getDepartmentId())
                .departmentName(department == null ? null : department.getDepartmentName())
                .positionId(employee.getPositionId())
                .positionName(position == null ? null : position.getPositionName())
                .hireDate(employee.getHireDate())
                .employmentStatus(employee.getEmploymentStatus())
                .build();
    }
}
