package com.ikeu.server.controller.user;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.SetMaxOrdersDTO;
import com.ikeu.model.vo.RunnerInfoVO;
import com.ikeu.model.vo.RunnerRankingVO;
import com.ikeu.model.vo.RunnerPerformanceVO;
import com.ikeu.server.annotation.RequireCertify;
import com.ikeu.server.service.RunnerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 跑腿员相关接口，提供申请成为跑腿员、上下线、设置最大接单数、排行榜和表现数据等功能。
 * @author ikeu
 * @since 2025/05/22
 */
@Tag(name = "用户端-跑腿员接口", description = "跑腿员相关接口")
@RestController
@RequestMapping("/runner")
@RequiredArgsConstructor
@Validated
public class RunnerController {

    private final RunnerProfileService runnerProfileService;

    /**
     * 申请成为跑腿员。
     *
     * <p>委托 {@link RunnerProfileService#applyForRunner} 校验用户已通过学生实名认证、
     * 尚未提交过跑腿员申请后，创建 runner_profile 记录并将认证状态设为"审核中"。
     *
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "申请成为跑腿员（需已通过学生实名认证）")
    @PostMapping("/apply")
    public Result<Void> apply() {
        Long userId = BaseContext.getCurrentId();
        runnerProfileService.applyForRunner(userId);
        return Result.success("跑腿员申请已提交，请等待审核");
    }

    /**
     * 获取当前用户的跑腿员档案信息。
     *
     * <p>委托 {@link RunnerProfileService#getProfile} 根据 userId 查询 runner_profile 表，
     * 返回包含认证状态、信用分、接单统计、在线状态等的 RunnerInfoVO。
     *
     * @return 跑腿员档案信息
     */
    @RequireCertify
    @Operation(summary = "获取跑腿员档案信息")
    @GetMapping("/profile")
    public Result<RunnerInfoVO> getProfile() {
        Long userId = BaseContext.getCurrentId();
        RunnerInfoVO profile = runnerProfileService.getProfile(userId);
        return Result.success(profile);
    }

    /**
     * 跑腿员上线，开始接收订单。
     *
     * <p>委托 {@link RunnerProfileService#goOnline} 将 runner_profile 的 is_online 设为 1，
     * 使该跑腿员出现在可接单列表中。
     *
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "上线接单")
    @PostMapping("/online")
    public Result<Void> online() {
        Long userId = BaseContext.getCurrentId();
        runnerProfileService.goOnline(userId);
        return Result.success(MessageConstant.RUNNER_GO_ONLINE);
    }

    /**
     * 跑腿员下线，停止接收新订单。
     *
     * <p>委托 {@link RunnerProfileService#goOffline} 将 runner_profile 的 is_online 设为 0，
     * 已有订单可继续处理但不再接收新订单。
     *
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "下线")
    @PostMapping("/offline")
    public Result<Void> offline() {
        Long userId = BaseContext.getCurrentId();
        runnerProfileService.goOffline(userId);
        return Result.success(MessageConstant.RUNNER_GO_OFFLINE);
    }

    /**
     * 设置跑腿员最大同时接单数量。
     *
     * <p>委托 {@link RunnerProfileService#setMaxOrders} 更新 runner_profile 的
     * max_orders 字段，用于限制跑腿员同时处理的订单数。
     *
     * @param setMaxOrdersDTO 最大接单数DTO
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "设置最大同时接单数")
    @PutMapping("/max-orders")
    public Result<Void> setMaxOrders(@Valid @RequestBody SetMaxOrdersDTO setMaxOrdersDTO) {
        Long userId = BaseContext.getCurrentId();
        runnerProfileService.setMaxOrders(userId, setMaxOrdersDTO);
        return Result.success();
    }

    /**
     * 获取跑腿员排行榜。
     *
     * <p>委托 {@link RunnerProfileService#getLeaderboard} 按指定指标（接单数或评分）
     * 倒序排列跑腿员，返回前 N 名的 RunnerRankingVO 列表。结果通过 Spring Cache 缓存。
     *
     * @param sortBy 排序字段（默认 "orders"，可选 "rating"）
     * @param limit 返回条数，默认10
     * @return 排行榜列表
     */
    @Operation(summary = "跑腿员排行榜")
    @GetMapping("/leaderboard")
    public Result<List<RunnerRankingVO>> leaderboard(
            @RequestParam(defaultValue = "orders") String sortBy,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return Result.success(runnerProfileService.getLeaderboard(sortBy, limit));
    }

    /**
     * 获取指定跑腿员的表现数据。
     *
     * <p>委托 {@link RunnerProfileService#getRunnerPerformance} 统计该跑腿员的
     * 总接单数、完成率、平均评分等指标，构造 RunnerPerformanceVO 返回。
     *
     * @param runnerId 跑腿员用户ID
     * @return 跑腿员表现数据
     */
    @Operation(summary = "跑腿员表现数据")
    @GetMapping("/performance/{runnerId}")
    public Result<RunnerPerformanceVO> performance(@PathVariable Long runnerId) {
        return Result.success(runnerProfileService.getRunnerPerformance(runnerId));
    }
}
