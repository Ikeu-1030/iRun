package com.ikeu.server.controller.user;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.CancelTaskDTO;
import com.ikeu.model.dto.TaskPublishDTO;
import com.ikeu.model.entity.Task;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.model.vo.TaskPublishVO;
import com.ikeu.model.vo.TaskStatisticsVO;
import com.ikeu.server.annotation.RequireCertify;
import com.ikeu.server.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

/**
 * 任务模块接口，提供任务发布、大厅浏览、详情查看、搜索筛选和取消等功能。
 * @author ikeu
 * @since 2025/05/23
 */
@Tag(name = "用户端-任务模块接口", description = "任务发布、浏览、详情")
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    /**
     * 发布新任务。
     *
     * <p>委托 {@link TaskService#publishTask} 校验任务参数（类型、报酬、地址等），
     * 保存任务记录和图片列表到 task 和 task_image 表，任务状态初始为"待接单"。
     *
     * @param taskPublishDTO 任务发布DTO（类型、报酬、地址、描述、图片等）
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "发布新任务")
    @PostMapping("/publish")
    public Result<TaskPublishVO> publish(@Valid @RequestBody TaskPublishDTO taskPublishDTO) {
        Long userId = BaseContext.getCurrentId();
        Task task = taskService.publishTask(userId, taskPublishDTO);
        return Result.success(TaskPublishVO.builder()
                .taskId(task.getId())
                .taskNo(task.getTaskNo())
                .build());
    }

    /**
     * 查看任务大厅列表，支持按类型、子类型、报酬范围和地理位置筛选。
     *
     * <p>委托 {@link TaskService#listTasksHall} 构建多条件查询，按距离或发布时间排序，
     * 结果通过 Spring Cache 缓存。返回的 TaskListVO 包含发布者昵称、头像和任务摘要信息。
     *
     * @param type 任务类型（可选）
     * @param subType 任务子类型（可选）
     * @param minReward 最小报酬（可选）
     * @param maxReward 最大报酬（可选）
     * @param lng 经度（可选，用于距离排序）
     * @param lat 纬度（可选，用于距离排序）
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 任务列表分页结果
     */
    @Operation(summary = "查看任务大厅列表")
    @GetMapping("/list")
    public Result<PageResult<TaskListVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String subType,
            @RequestParam(required = false) BigDecimal minReward,
            @RequestParam(required = false) BigDecimal maxReward,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResult<TaskListVO> result = taskService.listTasksHall(type, subType, minReward, maxReward, lng, lat, page, size);
        return Result.success(result);
    }

    /**
     * 查看当前用户发布的任务列表，支持按状态筛选。
     *
     * <p>委托 {@link TaskService#listMyPublishedTasks} 按 publisherId 和可选的
     * status 条件查询 task 表，按创建时间倒序分页返回。
     *
     * @param status 任务状态（可选）
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 任务列表分页结果
     */
    @Operation(summary = "查看我的任务列表")
    @GetMapping("/mine")
    public Result<PageResult<TaskListVO>> myPublished(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(taskService.listMyPublishedTasks(userId, status, page, size));
    }

    /**
     * 查看指定任务的详细信息。
     *
     * <p>委托 {@link TaskService#getTaskDetail} 查询任务完整信息并关联发布者信息、
     * 图片列表，返回 TaskDetailVO。同时判断当前用户是否为该任务的发布者。
     *
     * @param taskId 任务ID
     * @return 任务详细信息
     */
    @Operation(summary = "查看任务详情")
    @GetMapping("/{taskId}")
    public Result<TaskDetailVO> detail(@PathVariable Long taskId) {
        Long userId = BaseContext.getCurrentId();
        TaskDetailVO detail = taskService.getTaskDetail(taskId, userId);
        return Result.success(detail);
    }

    /**
     * 取消任务（仅待接单状态可取消）。
     *
     * <p>委托 {@link TaskService#cancelTask} 校验任务归属当前用户且状态为"待接单"，
     * 更新任务状态为"已取消"并记录取消原因。
     *
     * @param taskId 任务ID
     * @param cancelDTO 取消任务DTO（取消原因）
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "取消任务（仅待接单状态）")
    @PutMapping("/{taskId}/cancel")
    public Result<Void> cancel(@PathVariable Long taskId,
                               @Valid @RequestBody CancelTaskDTO cancelDTO) {
        Long userId = BaseContext.getCurrentId();
        taskService.cancelTask(userId, taskId, cancelDTO);
        return Result.success(MessageConstant.TASK_CANCEL_SUCCESS);
    }

    /**
     * 根据关键词搜索任务，支持按类型和报酬范围筛选。
     *
     * <p>委托 {@link TaskService#searchTasks} 对任务标题、描述进行模糊匹配，
     * 结合可选的类型和报酬范围筛选条件，分页返回匹配结果。
     *
     * @param keyword 搜索关键词（可选）
     * @param type 任务类型（可选）
     * @param minReward 最小报酬（可选）
     * @param maxReward 最大报酬（可选）
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 任务列表分页结果
     */
    @Operation(summary = "关键词搜索任务")
    @GetMapping("/search")
    public Result<PageResult<TaskListVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minReward,
            @RequestParam(required = false) BigDecimal maxReward,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(taskService.searchTasks(keyword, type, minReward, maxReward, page, size));
    }

    /**
     * 按多条件筛选任务（地点、性别、报酬范围等）。
     *
     * <p>委托 {@link TaskService#filterTasks} 构建组合条件查询：
     * 类型精确匹配、地址模糊匹配、性别要求匹配、报酬区间筛选，分页返回结果。
     *
     * @param type 任务类型（可选）
     * @param pickupAddress 取货地址（可选，模糊匹配）
     * @param deliveryAddress 送达地址（可选，模糊匹配）
     * @param requireSex 性别要求（可选）
     * @param minReward 最小报酬（可选）
     * @param maxReward 最大报酬（可选）
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 任务列表分页结果
     */
    @Operation(summary = "条件筛选任务（地点、性别、报酬范围）")
    @GetMapping("/filter")
    public Result<PageResult<TaskListVO>> filter(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String pickupAddress,
            @RequestParam(required = false) String deliveryAddress,
            @RequestParam(required = false) String requireSex,
            @RequestParam(required = false) BigDecimal minReward,
            @RequestParam(required = false) BigDecimal maxReward,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(taskService.filterTasks(type, pickupAddress, deliveryAddress,
                requireSex, minReward, maxReward, page, size));
    }

    /**
     * 根据经纬度搜索附近任务。
     *
     * <p>委托 {@link TaskService#listTasksNearby} 使用经纬度计算任务与当前位置的距离，
     * 筛选距离在 radiusKm 范围内的任务，按距离升序排列分页返回。
     *
     * @param lng 当前经度
     * @param lat 当前纬度
     * @param radiusKm 搜索半径（公里），默认5
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 附近任务列表分页结果
     */
    @Operation(summary = "附近任务搜索")
    @GetMapping("/nearby")
    public Result<PageResult<TaskListVO>> nearby(
            @RequestParam BigDecimal lng,
            @RequestParam BigDecimal lat,
            @RequestParam(defaultValue = "5") Double radiusKm,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(taskService.listTasksNearby(lng, lat, radiusKm, page, size));
    }

    /**
     * 获取当前用户的任务统计数据。
     *
     * <p>委托 {@link TaskService#getTaskStatistics} 统计用户发布的任务总数、
     * 各状态的任务数量等汇总数据，构造 TaskStatisticsVO 返回。
     *
     * @return 任务统计数据
     */
    @Operation(summary = "我的任务统计")
    @GetMapping("/statistics")
    public Result<TaskStatisticsVO> statistics() {
        Long userId = BaseContext.getCurrentId();
        return Result.success(taskService.getTaskStatistics(userId));
    }
}
