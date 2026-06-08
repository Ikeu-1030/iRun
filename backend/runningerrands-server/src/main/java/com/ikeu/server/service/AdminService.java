package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.vo.*;

/**
 * 管理员服务接口，提供登录认证、用户管理、任务订单管理和仪表盘数据等功能。
 * @author ikeu
 * @since 2026/05/21
 */
public interface AdminService {

    /** 管理员登录，校验用户名密码后生成双令牌。 */
    AdminLoginVO login(AdminLoginDTO dto);

    /** 刷新管理员访问令牌（令牌轮换）。 */
    AdminLoginVO refreshAccessToken(String refreshToken);

    /** 获取当前登录管理员信息（不含令牌）。 */
    AdminLoginVO getAdminInfo();

    /** 管理员退出登录，清除所有 refresh token。 */
    void logout(Long adminId);

    /** 获取仪表盘统计数据（用户数、任务数、订单数、今日收益等）。 */
    DashboardVO getDashboard();

    /** 管理员强制更新任务状态。 */
    void updateTaskStatus(Long taskId, Integer status);

    /** 获取任务详情。 */
    TaskDetailVO getTaskDetail(Long taskId);

    /** 分页查询所有任务列表，支持按状态筛选。 */
    PageResult<TaskListVO> listAllTasks(Integer status, int page, int size);

    /** 分页查询所有订单列表，支持按状态筛选。 */
    PageResult<OrderManageVO> listAllOrders(Integer status, int page, int size);

    /** 根据订单ID查询订单详情。 */
    OrderDetailVO getOrderDetail(Long orderId);

    /** 强制更新订单状态（状态机校验）。 */
    void updateOrderStatus(Long orderId, Integer status);

    /** 分页查询配送员管理列表，支持按认证状态和关键词筛选。 */
    PageResult<RunnerManageVO> listRunners(Integer verifyStatus, String keyword, int page, int size);

    /** 分页查询用户管理列表，支持按状态、认证状态和关键词筛选。 */
    PageResult<UserInfoVO> listUsers(Integer status, Integer isCertify, String keyword, int page, int size);

    /** 封禁或解封指定用户账户。 */
    void toggleUserStatus(Long userId, Boolean enabled);

    /** 审核配送员认证申请，更新认证状态和备注。 */
    void reviewRunnerCertification(Long runnerProfileId, Integer verifyStatus, String remark);

    /** 审核用户实名认证（学生身份）申请。 */
    void reviewUserCertification(Long userId, Integer isCertify, String remark);

    /** 获取单个用户详细信息。 */
    UserInfoVO getUserDetail(Long userId);

    /** 获取跑腿员详情（含累计收入）。 */
    RunnerManageVO getRunnerDetail(Long profileId);

    /** 禁止/恢复跑腿员接单。 */
    void toggleRunnerBan(Long profileId, boolean banned);
}
