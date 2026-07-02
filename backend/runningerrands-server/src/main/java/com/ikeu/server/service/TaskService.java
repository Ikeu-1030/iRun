package com.ikeu.server.service;

import com.ikeu.model.dto.CancelTaskDTO;
import com.ikeu.model.dto.TaskPublishDTO;
import com.ikeu.model.entity.Task;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.model.vo.TaskStatisticsVO;
import com.ikeu.common.result.PageResult;

import java.math.BigDecimal;

/**
 * 任务服务接口，提供任务发布、大厅浏览、搜索筛选、取消和统计等功能。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface TaskService {

    /**
     * 用户发布新任务，保存任务记录和图片列表。
     *
     * @param userId 当前登录用户ID
     * @param dto 任务发布请求参数（类型、报酬、地址、描述、图片等）
     * @return 新建的任务实体（含 taskId 和 taskNo）
     */
    Task publishTask(Long userId, TaskPublishDTO dto);

    /**
     * 分页查询任务大厅列表。
     *
     * <p>仅查询状态为"待接单"且未过期的任务，支持按类型、报酬范围和地理位置筛选。
     * 无筛选条件时走缓存（@RedisDefend 防穿透+防击穿）。
     *
     * @param type 任务类型（可选，精确匹配）
     * @param subType 任务子类型（可选，精确匹配）
     * @param minReward 最小报酬（可选）
     * @param maxReward 最大报酬（可选）
     * @param lng 当前经度（可选，用于距离排序预留）
     * @param lat 当前纬度（可选，用于距离排序预留）
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 任务大厅列表分页结果
     */
    PageResult<TaskListVO> listTasksHall(String type, String subType, BigDecimal minReward,
                                     BigDecimal maxReward, BigDecimal lng, BigDecimal lat,
                                     int page, int size);

    /**
     * 查询当前用户发布的任务列表，支持按状态筛选。
     *
     * @param userId 当前用户ID
     * @param status 任务状态筛选（可选，null为全部）
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 当前用户发布的任务列表分页结果
     */
    PageResult<TaskListVO> listMyPublishedTasks(Long userId, Integer status, int page, int size);

    /**
     * 查询任务详细信息，关联发布者信息和图片列表。
     *
     * <p>使用 Redis 缓存防穿透+防击穿，非发布者返回脱敏数据。
     *
     * @param taskId 任务ID
     * @param currentUserId 当前登录用户ID
     * @return 任务详情VO，含发布者信息和脱敏处理
     */
    TaskDetailVO getTaskDetail(Long taskId, Long currentUserId);

    /**
     * 取消任务。
     *
     * <p>仅发布者可在待接单状态下取消，已接单/配送中的任务需配送员先取消订单。
     * 取消后退还任务报酬给发布者并清除缓存。
     *
     * @param userId 当前用户ID
     * @param taskId 任务ID
     * @param cancelTaskDTO 取消原因DTO
     */
    void cancelTask(Long userId, Long taskId, CancelTaskDTO cancelTaskDTO);

    /**
     * 按关键词搜索任务。
     *
     * <p>对任务描述进行模糊匹配，支持按类型和报酬范围筛选，
     * 仅查询状态为"待接单"且未过期的任务。
     *
     * @param keyword 搜索关键词（可选）
     * @param type 任务类型（可选）
     * @param minReward 最小报酬（可选）
     * @param maxReward 最大报酬（可选）
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 匹配的任务列表分页结果
     */
    PageResult<TaskListVO> searchTasks(String keyword, String type, BigDecimal minReward,
                                       BigDecimal maxReward, int page, int size);

    /**
     * 按多条件筛选任务。
     *
     * <p>支持类型精确匹配、地址模糊匹配、性别要求匹配、报酬区间筛选，
     * 仅查询状态为"待接单"且未过期的任务。
     *
     * @param type 任务类型（可选）
     * @param pickupAddress 取件地址（可选，模糊匹配）
     * @param deliveryAddress 送达地址（可选，模糊匹配）
     * @param requireSex 性别要求（可选）
     * @param minReward 最小报酬（可选）
     * @param maxReward 最大报酬（可选）
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 筛选后的任务列表分页结果
     */
    PageResult<TaskListVO> filterTasks(String type, String pickupAddress, String deliveryAddress,
                                       String requireSex, BigDecimal minReward, BigDecimal maxReward,
                                       int page, int size);

    /**
     * 按经纬度搜索附近任务。
     *
     * <p>使用空间计算筛选距离在 radiusKm 范围内的任务，按距离升序排列。
     *
     * @param lng 当前经度
     * @param lat 当前纬度
     * @param radiusKm 搜索半径（公里）
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 附近任务列表分页结果
     */
    PageResult<TaskListVO> listTasksNearby(BigDecimal lng, BigDecimal lat, Double radiusKm,
                                           int page, int size);

    /**
     * 统计当前用户发布任务的数量分布。
     *
     * <p>包含待接单、进行中、已完成、已取消各状态的数量统计。
     *
     * @param userId 当前用户ID
     * @return 任务统计VO（总数及各状态计数）
     */
    TaskStatisticsVO getTaskStatistics(Long userId);
}
