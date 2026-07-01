package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端仪表盘VO")
public class DashboardVO implements Serializable {

    @Schema(description = "用户总数")
    private Long userCount;

    @Schema(description = "任务总数")
    private Long taskCount;

    @Schema(description = "订单总数")
    private Long orderCount;

    @Schema(description = "认证跑腿员数")
    private Long runnerCount;

    @Schema(description = "在线跑腿员数")
    private Long onlineRunnerCount;

    @Schema(description = "今日交易额")
    private BigDecimal todayRevenue;

    @Schema(description = "今日新增用户")
    private Long todayNewUsers;

    @Schema(description = "今日新增任务")
    private Long todayNewTasks;

    @Schema(description = "今日完成订单")
    private Long todayCompletedOrders;

    @Schema(description = "近7天新增用户趋势")
    private List<TrendPointVO> userTrend;

    @Schema(description = "近7天收入趋势")
    private List<TrendPointVO> revenueTrend;

    @Schema(description = "任务分类占比")
    private List<CategoryPieVO> taskCategories;

    @Schema(description = "订单状态分布")
    private List<CategoryPieVO> orderStatusDistribution;
}
