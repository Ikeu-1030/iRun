package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.CategoryPieVO;
import com.ikeu.model.vo.DashboardVO;
import com.ikeu.model.vo.TrendPointVO;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端仪表盘服务实现，按统计维度拆分为独立缓存方法。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final TaskOrderMapper taskOrderMapper;
    private final RunnerProfileMapper runnerProfileMapper;

    private static final Map<String, String> TYPE_LABEL_MAP = Map.ofEntries(
            Map.entry("代取快递", "代取快递"), Map.entry("daiqukuaidi", "代取快递"),
            Map.entry("代拿餐食", "代拿餐食"), Map.entry("dana canshi", "代拿餐食"),
            Map.entry("校内代办", "校内代办"), Map.entry("xiaonei daiban", "校内代办"),
            Map.entry("代购物品", "代购物品"), Map.entry("daigou wupin", "代购物品"),
            Map.entry("通用代办", "通用代办"), Map.entry("tongyong daiban", "通用代办"));

    /**
     * 统计卡片摘要：用户/任务/订单总数、认证/在线跑腿员数、今日新增/完成量、今日交易额。
     *
     * <p>全部从数据库 COUNT 或 SUM 查询，今日维度以当天 00:00 为起始；已完成订单的
     * 交易额汇总自 {@code taskMapper.sumRewardByStatusSince()}。结果缓存 3 分钟，
     * 涉及写操作（订单/任务/用户/跑腿员变更）通过 {@code @CacheEvict(allEntries = true)} 清除。
     *
     * @return 仪表盘统计摘要 VO，含 10 项统计指标
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'summary'")
    public DashboardVO getDashboardSummary() {
        Long userCount = userMapper.selectCount(null);
        Long taskCount = taskMapper.selectCount(null);
        Long orderCount = taskOrderMapper.selectCount(null);
        Long runnerCount = runnerProfileMapper.selectCount(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getVerifyStatus, StatusConstant.CERTIFY_APPROVED));
        Long onlineRunnerCount = runnerProfileMapper.selectCount(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getIsOnline, 1));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, todayStart));
        Long todayNewTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().ge(Task::getCreatedAt, todayStart));
        Long todayCompletedOrders = taskOrderMapper.selectCount(
                new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getStatus, StatusConstant.ORDER_COMPLETED)
                        .ge(TaskOrder::getConfirmTime, todayStart));

        BigDecimal todayRevenue = taskMapper.sumRewardByStatusSince(StatusConstant.TASK_COMPLETED, todayStart);

        return DashboardVO.builder()
                .userCount(userCount).taskCount(taskCount)
                .orderCount(orderCount).runnerCount(runnerCount)
                .onlineRunnerCount(onlineRunnerCount)
                .todayRevenue(todayRevenue).todayNewUsers(todayNewUsers)
                .todayNewTasks(todayNewTasks).todayCompletedOrders(todayCompletedOrders)
                .build();
    }

    /**
     * 近 7 天新增用户趋势，逐日统计 {@code created_at} 落入当天的用户数，补零填充无数据日期。
     *
     * <p>从当天往前推 6 天共 7 天，每一天独立 COUNT；无新增的日期返回 0。
     * 日期格式为 "MM-dd"。结果单独缓存 3 分钟。
     *
     * @return 包含 7 个日期-数量点的趋势列表
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'userTrend'")
    public List<TrendPointVO> getUserTrend() {
        LocalDateTime sevenDaysAgo = LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.MIN);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<TrendPointVO> result = new ArrayList<>();
        LocalDate cursor = sevenDaysAgo.toLocalDate();
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay();
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getCreatedAt, dayStart).lt(User::getCreatedAt, dayEnd));
            result.add(TrendPointVO.builder().date(cursor.format(fmt)).value(count).build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * 近 7 天收入趋势，调用 {@code taskMapper.sumRewardPerDay()} 聚合已完成任务报酬，补零填充。
     *
     * <p>从当天往前推 6 天共 7 天，按日分组 SUM {@code reward}；无收入的日期返回 0。
     * 日期格式为 "MM-dd"。结果单独缓存 3 分钟。
     *
     * @return 包含 7 个日期-金额点的趋势列表
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'revenueTrend'")
    public List<TrendPointVO> getRevenueTrend() {
        LocalDateTime sevenDaysAgo = LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.MIN);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> rows = taskMapper.sumRewardPerDay(StatusConstant.TASK_COMPLETED, sevenDaysAgo);
        Map<String, BigDecimal> dateSum = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String date = row.get("date").toString();
            BigDecimal val = row.get("value") != null ? new BigDecimal(row.get("value").toString()) : BigDecimal.ZERO;
            dateSum.put(date, val);
        }
        List<TrendPointVO> result = new ArrayList<>();
        LocalDate cursor = sevenDaysAgo.toLocalDate();
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            String key = cursor.toString();
            Long value = dateSum.containsKey(key) ? dateSum.get(key).longValue() : 0L;
            result.add(TrendPointVO.builder().date(cursor.format(fmt)).value(value).build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * 任务分类占比，调用 {@code taskMapper.countTasksByType()} 按类型分组统计。
     *
     * <p>统计所有任务（不区分状态）按 {@code type} 字段分组 COUNT，
     * 用于管理端饼图展示任务分布情况。结果单独缓存 3 分钟。
     *
     * @return 任务类型-数量列表，每项包含分类名称和数量
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'taskCategories'")
    public List<CategoryPieVO> getTaskCategories() {
        List<Map<String, Object>> rows = taskMapper.countTasksByType();
        // 按中文标签聚合，兜底未识别 type 避免英文 code 暴露到前端
        Map<String, Long> grouped = new LinkedHashMap<>();
        for (String label : List.of("代取快递", "代拿餐食", "校内代办", "代购物品", "通用代办")) {
            grouped.put(label, 0L);
        }
        for (Map<String, Object> row : rows) {
            String raw = row.get("name") != null ? row.get("name").toString() : "";
            Long value = row.get("value") != null ? Long.parseLong(row.get("value").toString()) : 0L;
            String label = TYPE_LABEL_MAP.getOrDefault(raw, raw);
            grouped.merge(label, value, Long::sum);
        }
        return grouped.entrySet().stream()
                .map(e -> CategoryPieVO.builder().name(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());
    }

    /**
     * 订单状态分布，遍历 1~5 状态值统计各状态未删除订单数。
     *
     * <p>固化的状态标签依次为：待取货、配送中、待确认、已完成、已取消。
     * 仅统计 {@code is_deleted = 0} 的有效订单，用于管理端饼图展示。
     * 结果单独缓存 3 分钟。
     *
     * @return 订单状态-数量列表，每项包含状态名称和数量
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'orderStatusDistribution'")
    public List<CategoryPieVO> getOrderStatusDistribution() {
        String[] labels = {"待取货", "配送中", "待确认", "已完成", "已取消"};
        List<CategoryPieVO> result = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Long count = taskOrderMapper.selectCount(
                    new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getStatus, i)
                            .eq(TaskOrder::getIsDeleted, 0));
            result.add(CategoryPieVO.builder().name(labels[i - 1]).value(count).build());
        }
        return result;
    }
}
