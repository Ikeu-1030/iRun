package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.AdminCreateDTO;
import com.ikeu.model.dto.AdminPasswordResetDTO;
import com.ikeu.model.dto.AdminUpdateDTO;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.vo.AdminListVO;
import com.ikeu.server.mapper.AdminMapper;
import com.ikeu.server.service.AdminEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员员工服务实现，处理管理员员工的 CRUD 操作及权限校验（禁止操作超管账号、禁止停用自己）。
 * @author ikeu
 * @since 2026/06/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEmployeeServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminEmployeeService {

    private static final int ROLE_SUPER_ADMIN = 1;   // 超级管理员
    private static final int ROLE_NORMAL_ADMIN = 2;  // 普通管理员

    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询管理员员工列表，按创建时间降序排列。
     *
     * <p>返回所有管理员（含超管和普通管理员），按创建时间倒序排列，
     * 转换为 {@link AdminListVO} 视图对象，不暴露密码等敏感字段。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 管理员员工分页列表
     */
    @Override
    public PageResult<AdminListVO> listEmployees(int page, int size) {
        Page<Admin> p = page(new Page<>(page, size),
                new LambdaQueryWrapper<Admin>().orderByDesc(Admin::getCreatedAt));
        List<AdminListVO> records = p.getRecords().stream().map(a -> AdminListVO.builder()
                .id(a.getId())
                .username(a.getUsername())
                .name(a.getName())
                .phone(a.getPhone())
                .sex(a.getSex())
                .role(a.getRole())
                .status(a.getStatus())
                .lastLoginTime(a.getLastLoginTime())
                .createdAt(a.getCreatedAt())
                .build()).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 根据 ID 查询单个管理员员工详情。
     *
     * @param id 管理员 ID
     * @return 管理员员工详情 VO
     * @throws NotFoundException 管理员不存在时抛出
     */
    @Override
    public AdminListVO getEmployee(Long id) {
        Admin a = getById(id);
        if (a == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        return AdminListVO.builder()
                .id(a.getId()).username(a.getUsername()).name(a.getName())
                .phone(a.getPhone()).sex(a.getSex()).role(a.getRole())
                .status(a.getStatus()).lastLoginTime(a.getLastLoginTime())
                .createdAt(a.getCreatedAt()).build();
    }

    /**
     * 创建普通管理员员工。
     *
     * <p>校验用户名唯一性，强制设置角色为普通管理员（role=2），
     * 禁止创建超级管理员。密码使用 BCrypt 加密存储。初始状态为启用。
     *
     * @param dto 创建参数，包含用户名、姓名、密码、手机号、性别、身份证号
     * @throws BusinessException 用户名已存在或角色不是普通管理员时抛出
     */
    @Override
    @Transactional
    public void createEmployee(AdminCreateDTO dto) {
        boolean exists = lambdaQuery().eq(Admin::getUsername, dto.getUsername()).exists();
        if (exists) throw new BusinessException(MessageConstant.ADMIN_USERNAME_EXISTS);

        // 只能创建普通管理员，禁止创建超管
        if (dto.getRole() == null || dto.getRole() != ROLE_NORMAL_ADMIN) {
            throw new BusinessException(MessageConstant.ADMIN_MUST_BE_NORMAL);
        }

        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setName(dto.getName());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setPhone(dto.getPhone());
        admin.setSex(dto.getSex() != null ? dto.getSex() : "");
        admin.setIdNumber(dto.getIdNumber() != null ? dto.getIdNumber() : "");
        admin.setRole(ROLE_NORMAL_ADMIN);
        admin.setStatus(StatusConstant.ENABLE);

        save(admin);
        log.info("管理员 {} 创建了普通管理员 {}", BaseContext.getCurrentId(), dto.getUsername());
    }

    /**
     * 更新管理员员工信息。
     *
     * <p>仅允许修改姓名、手机号和性别，角色保持不变（不可提升为超管）。
     *
     * @param id 管理员 ID
     * @param dto 更新参数，包含姓名、手机号、性别
     * @throws NotFoundException 管理员不存在时抛出
     */
    @Override
    @Transactional
    public void updateEmployee(Long id, AdminUpdateDTO dto) {
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        // 仅允许修改姓名、手机号、性别，角色保持不变
        admin.setName(dto.getName());
        admin.setPhone(dto.getPhone());
        admin.setSex(dto.getSex() != null ? dto.getSex() : admin.getSex());

        updateById(admin);
    }

    /**
     * 启用或禁用管理员账号。
     *
     * <p>禁止禁用自己的账号（当前登录管理员 ID 与目标 ID 相同则抛出异常）。
     * 此方法不校验是否为超管角色，由调用方 {@code @RequireRole} 注解确保仅超管可操作。
     *
     * @param id 管理员 ID
     * @param enabled true 启用，false 禁用
     * @throws NotFoundException 管理员不存在时抛出
     * @throws BusinessException 试图禁用自己时抛出
     */
    @Override
    @Transactional
    public void toggleStatus(Long id, Boolean enabled) {
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        Long currentId = BaseContext.getCurrentId();
        // 不能停用自己的账号
        if (!enabled && currentId != null && currentId.equals(id)) {
            throw new BusinessException(MessageConstant.CANNOT_DISABLE_SELF);
        }
        admin.setStatus(enabled ? StatusConstant.ENABLE : StatusConstant.DISABLE);

        updateById(admin);
    }

    /**
     * 重置管理员密码。
     *
     * <p>直接使用 BCrypt 加密新密码后更新。不校验新旧密码是否相同，
     * 超管可以在员工忘记密码时直接重置。
     *
     * @param id 管理员 ID
     * @param dto 重置参数，包含新密码
     * @throws NotFoundException 管理员不存在时抛出
     */
    @Override
    @Transactional
    public void resetPassword(Long id, AdminPasswordResetDTO dto) {
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        updateById(admin);
    }

    /**
     * 删除普通管理员员工。
     *
     * <p>禁止删除自己（当前登录管理员 ID 与目标 ID 相同则抛出异常）。
     * 禁止删除超级管理员（role=1）。仅超管可通过此方法删除普通管理员。
     *
     * @param id 管理员 ID
     * @throws NotFoundException 管理员不存在时抛出
     * @throws BusinessException 试图删除自己或删除超管时抛出
     */
    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null && currentId.equals(id)) {
            throw new BusinessException(MessageConstant.CANNOT_DELETE_SELF);
        }
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        // 禁止删除超管
        if (admin.getRole() == ROLE_SUPER_ADMIN) {
            throw new BusinessException(MessageConstant.CANNOT_DELETE_SUPER_ADMIN);
        }

        removeById(id);
        log.info("管理员 {} 删除了普通管理员 {}", currentId, admin.getUsername());
    }
}
