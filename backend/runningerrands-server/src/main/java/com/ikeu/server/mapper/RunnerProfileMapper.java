package com.ikeu.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.model.entity.RunnerProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 配送员档案 Mapper，提供档案 CRUD 及信用分、订单统计的原子更新。
 * @author ikeu
 * @since 2025/05/21
 */
@Mapper
public interface RunnerProfileMapper extends BaseMapper<RunnerProfile> {

    /**
     * 单次 JOIN 查询替代双阶段关键词搜索，关联 user 表匹配昵称或手机号。
     *
     * @param page 分页对象
     * @param verifyStatus 认证状态（可选）
     * @param keyword 关键词（可选，匹配昵称、手机号）
     * @return 分页结果
     */
    IPage<RunnerProfile> selectRunnersWithKeyword(Page<RunnerProfile> page,
                                                  @Param("verifyStatus") Integer verifyStatus,
                                                  @Param("keyword") String keyword);

    /**
     * 原子更新信用分并自动冻结/解冻（仅信用分冻结，不影响管理员手动封禁）。
     * 新信用分低于冻结阈值时自动设置 is_banned=1 + ban_until，
     * 回到阈值以上时仅清除由信用分冻结触发的封禁（ban_until IS NOT NULL）。
     * ban_until 完全在 SQL 内计算，消除 TOCTOU 竞态。
     *
     * @param userId          用户ID
     * @param delta           信用分增量（可为负数）
     * @param freezeThreshold 冻结阈值（低于该值触发冻结）
     * @param freezeDays      冻结天数
     * @param maxScore        信用分上限（通常为100）
     */
    @Update("UPDATE runner_profile SET " +
            "credit_score = LEAST(#{maxScore}, GREATEST(0, credit_score + #{delta})), " +
            "is_banned = CASE " +
            "WHEN LEAST(#{maxScore}, GREATEST(0, credit_score + #{delta})) < #{freezeThreshold} THEN 1 " +
            "WHEN LEAST(#{maxScore}, GREATEST(0, credit_score + #{delta})) >= #{freezeThreshold} AND is_banned = 1 AND ban_until IS NOT NULL THEN 0 " +
            "ELSE is_banned END, " +
            "ban_until = CASE " +
            "WHEN LEAST(#{maxScore}, GREATEST(0, credit_score + #{delta})) < #{freezeThreshold} THEN " +
            "  CASE WHEN is_banned = 1 AND ban_until IS NOT NULL AND ban_until > NOW() THEN ban_until " +
            "       ELSE DATE_ADD(NOW(), INTERVAL #{freezeDays} DAY) END " +
            "WHEN LEAST(#{maxScore}, GREATEST(0, credit_score + #{delta})) >= #{freezeThreshold} AND is_banned = 1 AND ban_until IS NOT NULL THEN NULL " +
            "ELSE ban_until END " +
            "WHERE user_id = #{userId}")
    void updateCreditScoreAndFreeze(@Param("userId") Long userId, @Param("delta") int delta,
                                    @Param("freezeThreshold") int freezeThreshold,
                                    @Param("freezeDays") int freezeDays,
                                    @Param("maxScore") int maxScore);

    /**
     * 原子恢复信用分并解冻（仅对冻结期满的跑腿员生效）。
     * score ⇐ targetScore 时设为 targetScore，否则保持原值，
     * 防止并发读写导致 TOCTOU 竞态。
     *
     * @param userId 用户ID
     * @param targetScore 目标恢复分值
     * @return 影响的记录行数（0 表示该跑腿员未处于冻结期满状态）
     */
    @Update("UPDATE runner_profile SET " +
            "credit_score = CASE WHEN credit_score < #{targetScore} THEN #{targetScore} ELSE credit_score END, " +
            "is_banned = 0, ban_until = NULL " +
            "WHERE user_id = #{userId} AND is_banned = 1 AND ban_until IS NOT NULL AND ban_until < NOW()")
    int restoreCreditAndUnfreeze(@Param("userId") Long userId, @Param("targetScore") int targetScore);

    /**
     * 原子切换跑腿员封禁状态，仅修改 is_banned 和 ban_until，
     * 不触及 credit_score 等其他字段，防止与信用分更新产生丢失更新。
     *
     * @param userId 用户ID
     * @param banned 是否封禁（1-封禁，0-解封）
     */
    @Update("UPDATE runner_profile SET is_banned = #{banned}, ban_until = NULL WHERE user_id = #{userId}")
    void toggleBan(@Param("userId") Long userId, @Param("banned") int banned);

    /**
     * 原子更新配送员订单统计（总单+1、成功+1、当前接单-1）。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET total_orders = total_orders + 1, success_orders = success_orders + 1, " +
            "current_orders = GREATEST(current_orders - 1, 0) WHERE user_id = #{userId}")
    void incrementOrderStats(@Param("userId") Long userId);

    /**
     * 原子增加完成单数统计（不含 current_orders 变动）。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET total_orders = total_orders + 1, success_orders = success_orders + 1 " +
            "WHERE user_id = #{userId}")
    void incrementCompletedStats(@Param("userId") Long userId);

    /**
     * 原子增加当前接单数。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET current_orders = current_orders + 1 WHERE user_id = #{userId}")
    void incrementCurrentOrders(@Param("userId") Long userId);

    /**
     * 原子减少当前接单数（最小为0）。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET current_orders = GREATEST(current_orders - 1, 0) WHERE user_id = #{userId}")
    void decrementCurrentOrders(@Param("userId") Long userId);

    /**
     * 更新跑腿员平均评分。
     *
     * @param avgRating 新的平均评分
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET avg_rating = #{avgRating} WHERE user_id = #{userId}")
    void updateAvgRating(@Param("avgRating") java.math.BigDecimal avgRating, @Param("userId") Long userId);
}
