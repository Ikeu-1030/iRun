package com.ikeu.server.service;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.UserInfoVO;

/**
 * 管理端用户管理服务接口，提供用户列表、详情、状态切换和认证审核功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminUserService {

    /**
     * 分页查询用户管理列表，支持按状态、认证状态和关键词（用户名/手机号/昵称）筛选。
     *
     * @param status     用户状态（null 表示查询全部）
     * @param isCertify  实名认证状态（null 表示查询全部）
     * @param keyword    搜索关键词（null 表示不筛选）
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @return 用户管理分页结果
     */
    PageResult<UserInfoVO> listUsers(Integer status, Integer isCertify, String keyword, int page, int size);

    /**
     * 封禁或解封指定用户账户，操作后清除仪表盘缓存。
     *
     * @param userId  用户 ID
     * @param enabled true 启用，false 禁用
     * @throws NotFoundException 用户不存在时抛出
     */
    void toggleUserStatus(Long userId, Boolean enabled);

    /**
     * 获取单个用户详细信息，含跑腿员认证状态（若已申请）。
     *
     * @param userId 用户 ID
     * @return 用户详情 VO
     * @throws NotFoundException 用户不存在时抛出
     */
    UserInfoVO getUserDetail(Long userId);

    /**
     * 审核用户实名认证（学生身份）申请。
     *
     * @param userId    用户 ID
     * @param isCertify 审核后的认证状态（通过/拒绝）
     * @param remark    审核备注说明
     * @throws NotFoundException 用户不存在时抛出
     * @throws BusinessException 用户不在审核中状态时抛出
     */
    void reviewUserCertification(Long userId, Integer isCertify, String remark);
}
