package com.ikeu.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.AdminCreateDTO;
import com.ikeu.model.dto.AdminPasswordResetDTO;
import com.ikeu.model.dto.AdminUpdateDTO;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.vo.AdminListVO;

/**
 * 管理员员工管理服务接口，提供管理员员工的 CRUD 和状态管理功能。
 * @author ikeu
 * @since 2026/06/03
 */
public interface AdminEmployeeService extends IService<Admin> {

    /**
     * 分页查询管理员员工列表，按创建时间倒序排列。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 管理员员工分页结果
     */
    PageResult<AdminListVO> listEmployees(int page, int size);

    /**
     * 根据 ID 查询管理员员工详情。
     *
     * @param id 管理员员工 ID
     * @return 管理员员工详情 VO
     * @throws NotFoundException 管理员员工不存在时抛出
     */
    AdminListVO getEmployee(Long id);

    /**
     * 创建普通管理员，强制设置 role=2，禁止创建超级管理员。
     *
     * @param dto 管理员创建请求 DTO
     * @throws BusinessException 用户名已存在或未指定角色为普通管理员时抛出
     */
    void createEmployee(AdminCreateDTO dto);

    /**
     * 修改管理员员工信息（姓名、手机号、性别）。
     *
     * @param id  管理员员工 ID
     * @param dto 管理员信息更新 DTO
     * @throws NotFoundException 管理员员工不存在时抛出
     */
    void updateEmployee(Long id, AdminUpdateDTO dto);

    /**
     * 启用或停用管理员员工，不能停用自己的账号。
     *
     * @param id      管理员员工 ID
     * @param enabled true 启用，false 停用
     * @throws NotFoundException 管理员员工不存在时抛出
     * @throws BusinessException 停用自己的账号时抛出
     */
    void toggleStatus(Long id, Boolean enabled);

    /**
     * 重置管理员员工密码。
     *
     * @param id  管理员员工 ID
     * @param dto 密码重置请求 DTO（含新密码）
     * @throws NotFoundException 管理员员工不存在时抛出
     */
    void resetPassword(Long id, AdminPasswordResetDTO dto);

    /**
     * 删除管理员员工。禁止删除超级管理员，禁止删除自己的账号。
     *
     * @param id 管理员员工 ID
     * @throws NotFoundException 管理员员工不存在时抛出
     * @throws BusinessException 删除自己的账号或删除超级管理员时抛出
     */
    void deleteEmployee(Long id);
}
