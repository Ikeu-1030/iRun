package com.ikeu.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.OperationLog;

import java.time.LocalDateTime;

/**
 * 操作日志服务接口，提供操作日志的分页查询功能。
 *
 * @author ikeu
 * @since 2026/06/03
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 分页查询操作日志，支持按模块、管理员 ID、时间范围筛选。
     *
     * @param module 操作模块名称（可选，null 表示不限制）
     * @param adminId 管理员 ID（可选，null 表示不限制）
     * @param start 开始时间（可选，null 表示不限制）
     * @param end 结束时间（可选，null 表示不限制）
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 操作日志分页结果
     */
    PageResult<OperationLog> listLogs(String module, Long adminId, LocalDateTime start, LocalDateTime end, int page, int size);
}
