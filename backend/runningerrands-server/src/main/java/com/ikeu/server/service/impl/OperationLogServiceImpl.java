package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.entity.OperationLog;
import com.ikeu.server.mapper.AdminMapper;
import com.ikeu.server.mapper.OperationLogMapper;
import com.ikeu.server.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志服务实现，提供操作日志的分页条件查询。
 * 超管可查看所有日志，普通管理员仅可查看自己的日志。
 * @author ikeu
 * @since 2026/06/03
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private static final int ROLE_SUPER_ADMIN = 1;

    private final AdminMapper adminMapper;

    @Override
    public PageResult<OperationLog> listLogs(String module, Long adminId, LocalDateTime start, LocalDateTime end, int page, int size) {
        // 普通管理员只能查看自己的操作日志，忽略前端传入的 adminId 参数
        Long currentAdminId = BaseContext.getCurrentId();
        if (currentAdminId != null) {
            Admin admin = adminMapper.selectById(currentAdminId);
            if (admin != null && admin.getRole() != ROLE_SUPER_ADMIN) {
                adminId = currentAdminId;
            }
        }

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(module != null && !module.isBlank(), OperationLog::getModule, module)
                .eq(adminId != null, OperationLog::getAdminId, adminId)
                .ge(start != null, OperationLog::getCreatedAt, start)
                .le(end != null, OperationLog::getCreatedAt, end)
                .orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> p = page(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }
}
