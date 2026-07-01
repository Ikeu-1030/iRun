package com.ikeu.server.service;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.ForbiddenException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.model.dto.SetMaxOrdersDTO;
import com.ikeu.model.vo.RunnerInfoVO;
import com.ikeu.model.vo.RunnerRankingVO;
import com.ikeu.model.vo.RunnerPerformanceVO;

import java.util.List;

/**
 * 配送员档案服务接口，提供档案查询、上下线、接单数设置、排行榜和申请等功能。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface RunnerProfileService {

    /**
     * 申请成为配送员，需已通过学生实名认证。
     *
     * <p>审核中或已认证的申请会阻止重复提交，驳回后可重新申请。
     * 首次申请创建跑腿员档案并设置初始信用分 100 分。
     *
     * @param userId 用户 ID
     * @throws NotFoundException 用户不存在时抛出
     * @throws ForbiddenException 用户未通过实名认证时抛出
     * @throws BusinessException 申请审核中或已认证时抛出
     */
    void applyForRunner(Long userId);

    /**
     * 获取当前用户的配送员档案信息。
     *
     * @param userId 用户 ID
     * @return 配送员档案信息 VO
     * @throws ForbiddenException 配送员档案不存在时抛出
     */
    RunnerInfoVO getProfile(Long userId);

    /**
     * 配送员上线，允许接收新订单。
     *
     * @param userId 用户 ID
     * @throws ForbiddenException 配送员未通过认证时抛出
     * @throws BusinessException 配送员已在线时抛出
     */
    void goOnline(Long userId);

    /**
     * 配送员下线，停止接收新订单。
     *
     * @param userId 用户 ID
     * @throws BusinessException 配送员已离线时抛出
     */
    void goOffline(Long userId);

    /**
     * 设置最大同时接单数量。
     *
     * @param userId 用户 ID
     * @param dto 设置最大接单数 DTO（包含最大单数值）
     * @throws ForbiddenException 配送员档案不存在时抛出
     */
    void setMaxOrders(Long userId, SetMaxOrdersDTO dto);

    /**
     * 原子减少当前接单数（完成或取消订单后调用）。
     *
     * @param userId 用户 ID
     */
    void decrementCurrentOrders(Long userId);

    /**
     * 获取指定配送员的表现数据（接单统计、评分等）。
     *
     * @param runnerId 跑腿员用户 ID
     * @return 配送员表现数据 VO，包含总订单数、成功率、平均评分、信用分、收入等
     */
    RunnerPerformanceVO getRunnerPerformance(Long runnerId);

    /**
     * 获取配送员排行榜，支持按接单数、评分或完成率排序。
     *
     * @param sortBy 排序方式（rating-按评分，completion-按完成率，默认按成功单数）
     * @param limit 返回条数限制
     * @return 配送员排行榜列表
     */
    List<RunnerRankingVO> getLeaderboard(String sortBy, int limit);
}
