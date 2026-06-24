package com.ikeu.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.model.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务 Mapper，提供任务 CRUD 及地理位置查询、统计等自定义 SQL。
 * @author ikeu
 * @since 2025/05/21
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 使用 Haversine 公式计算距离，查询指定半径内的待接单任务，按距离升序排列。
     *
     * @param lng 当前经度
     * @param lat 当前纬度
     * @param radiusKm 搜索半径（公里）
     * @param offset 分页偏移
     * @param size 分页大小
     * @return 附近任务列表
     */
    @Select("SELECT *, (6371 * acos(cos(radians(#{lat})) * cos(radians(pickup_lat)) * " +
            "cos(radians(pickup_lng) - radians(#{lng})) + sin(radians(#{lat})) * sin(radians(pickup_lat)))) " +
            "AS distance FROM task WHERE status = 1 AND expire_time > NOW() " +
            "AND pickup_lng IS NOT NULL AND pickup_lat IS NOT NULL " +
            "HAVING distance < #{radiusKm} ORDER BY distance LIMIT #{offset}, #{size}")
    List<Task> selectNearbyTasks(@Param("lng") BigDecimal lng, @Param("lat") BigDecimal lat,
                                 @Param("radiusKm") Double radiusKm,
                                 @Param("offset") int offset, @Param("size") int size);

    /**
     * 统计指定半径内的待接单任务总数。
     */
    @Select("SELECT COUNT(*) FROM task WHERE status = 1 AND expire_time > NOW() " +
            "AND pickup_lng IS NOT NULL AND pickup_lat IS NOT NULL " +
            "AND (6371 * acos(cos(radians(#{lat})) * cos(radians(pickup_lat)) * " +
            "cos(radians(pickup_lng) - radians(#{lng})) + sin(radians(#{lat})) * sin(radians(pickup_lat)))) < #{radiusKm}")
    Long countNearbyTasks(@Param("lng") BigDecimal lng, @Param("lat") BigDecimal lat,
                          @Param("radiusKm") Double radiusKm);

    /**
     * 按状态分组统计用户发布的任务数量。
     *
     * @param userId 用户ID
     * @return 状态到计数的映射列表
     */
    @Select("SELECT COALESCE(status, 0), COUNT(*) FROM task " +
            "WHERE publisher_id = #{userId} GROUP BY status")
    List<Map<String, Object>> countTasksByStatus(@Param("userId") Long userId);

    /**
     * 统计指定时间之后完成的任务报酬总额（通过关联订单确认时间避免 updated_at 漂移）。
     *
     * @param status 任务状态
     * @param since 订单确认起始时间
     * @return 报酬总额
     */
    @Select("SELECT COALESCE(SUM(t.reward), 0) FROM task t " +
            "JOIN task_order o ON o.task_id = t.id " +
            "WHERE t.status = #{status} AND o.confirm_time >= #{since}")
    BigDecimal sumRewardByStatusSince(@Param("status") Integer status,
                                                @Param("since") java.time.LocalDateTime since);

    /** 统计各任务类型的数量 */
    @Select("SELECT type AS name, COUNT(*) AS value FROM task GROUP BY type")
    List<Map<String, Object>> countTasksByType();

    /** 统计近N天每日完成任务报酬总额（按订单确认时间分组，避免 updated_at 漂移） */
    @Select("SELECT DATE(o.confirm_time) AS date, COALESCE(SUM(t.reward), 0) AS value " +
            "FROM task t JOIN task_order o ON o.task_id = t.id " +
            "WHERE t.status = #{status} AND o.confirm_time >= #{since} " +
            "GROUP BY DATE(o.confirm_time) ORDER BY date")
    List<Map<String, Object>> sumRewardPerDay(@Param("status") Integer status,
                                              @Param("since") LocalDateTime since);

    /**
     * 关键词搜索任务，支持按类型和报酬范围筛选。
     *
     * <p>仅查询状态为待接单（status=1）且未过期（expire_time &gt; NOW()）的任务，
     * 按创建时间倒序排列。所有筛选条件均可选，为 null 时跳过对应条件。
     *
     * @param page      分页对象
     * @param keyword   搜索关键词（模糊匹配 public_desc），可为 null
     * @param type      任务类型，可为 null
     * @param minReward 最低报酬，可为 null
     * @param maxReward 最高报酬，可为 null
     * @return 分页任务列表
     */
    Page<Task> searchTasks(Page<Task> page,
                           @Param("keyword") String keyword,
                           @Param("type") String type,
                           @Param("minReward") BigDecimal minReward,
                           @Param("maxReward") BigDecimal maxReward);

    /**
     * 多条件筛选任务，支持取件/送达地址模糊匹配、性别要求、报酬范围。
     *
     * <p>仅查询状态为待接单（status=1）且未过期（expire_time &gt; NOW()）的任务，
     * 按创建时间倒序排列。所有筛选条件均可选，为 null 时跳过对应条件。
     *
     * @param page            分页对象
     * @param type            任务类型，可为 null
     * @param pickupAddress   取件地址（模糊匹配），可为 null
     * @param deliveryAddress 送达地址（模糊匹配），可为 null
     * @param requireSex      性别要求，可为 null
     * @param minReward       最低报酬，可为 null
     * @param maxReward       最高报酬，可为 null
     * @return 分页任务列表
     */
    Page<Task> filterTasks(Page<Task> page,
                           @Param("type") String type,
                           @Param("pickupAddress") String pickupAddress,
                           @Param("deliveryAddress") String deliveryAddress,
                           @Param("requireSex") String requireSex,
                           @Param("minReward") BigDecimal minReward,
                           @Param("maxReward") BigDecimal maxReward);
}
