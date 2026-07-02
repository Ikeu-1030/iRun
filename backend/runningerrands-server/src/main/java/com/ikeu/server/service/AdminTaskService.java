package com.ikeu.server.service;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;

/**
 * 管理端任务管理服务接口，提供任务列表、详情和强制状态更新功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminTaskService {

    /**
     * 分页查询所有任务列表，支持按任务状态筛选。
     *
     * @param status 任务状态（null 表示查询全部状态）
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 任务列表分页结果
     */
    PageResult<TaskListVO> listAllTasks(Integer status, int page, int size);

    /**
     * 获取任务详情（管理端），含发布者信息和图片列表。
     *
     * @param taskId 任务 ID
     * @return 任务详情 VO
     * @throws NotFoundException 任务不存在时抛出
     */
    TaskDetailVO getTaskDetail(Long taskId);

    /**
     * 管理员强制更新任务状态，经状态机校验后执行变更。
     *
     * @param taskId 任务 ID
     * @param status 目标任务状态值
     * @throws NotFoundException 任务不存在时抛出
     * @throws BusinessException 系统繁忙（分布式锁获取失败）或状态变更不合法时抛出
     */
    void updateTaskStatus(Long taskId, Integer status);
}
