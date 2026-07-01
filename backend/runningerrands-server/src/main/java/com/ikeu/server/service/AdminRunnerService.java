package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.RunnerManageVO;

/**
 * 管理端跑腿员管理服务接口，提供跑腿员列表、详情、审核和封禁功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminRunnerService {

    /**
     * 分页查询配送员管理列表，支持按认证状态和关键词（昵称/手机号）筛选。
     *
     * @param verifyStatus 认证状态（null 表示查询全部状态）
     * @param keyword      搜索关键词（null 表示不筛选）
     * @param page         页码（从 1 开始）
     * @param size         每页条数
     * @return 跑腿员管理分页结果
     */
    PageResult<RunnerManageVO> listRunners(Integer verifyStatus, String keyword, int page, int size);

    /**
     * 获取跑腿员详情，包含用户基本信息及累计收入。
     *
     * @param profileId 跑腿员档案 ID
     * @return 跑腿员详情 VO
     * @throws NotFoundException 跑腿员不存在时抛出
     */
    RunnerManageVO getRunnerDetail(Long profileId);

    /**
     * 审核配送员认证申请，更新认证状态和审核备注。
     *
     * @param runnerProfileId 跑腿员档案 ID
     * @param verifyStatus    审核目标状态（如通过/拒绝）
     * @param remark          审核备注说明
     * @throws NotFoundException 跑腿员档案不存在时抛出
     * @throws BusinessException 跑腿员不在审核中状态时抛出
     */
    void reviewRunnerCertification(Long runnerProfileId, Integer verifyStatus, String remark);

    /**
     * 禁止或恢复跑腿员接单权限。
     *
     * @param profileId 跑腿员档案 ID
     * @param banned    true 禁止接单，false 恢复接单
     * @throws NotFoundException 跑腿员档案不存在时抛出
     */
    void toggleRunnerBan(Long profileId, boolean banned);
}
