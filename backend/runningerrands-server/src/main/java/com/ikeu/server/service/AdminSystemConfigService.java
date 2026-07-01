package com.ikeu.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.model.dto.SystemConfigBatchUpdateDTO;
import com.ikeu.model.entity.SystemConfig;
import com.ikeu.model.vo.SystemConfigVO;

import java.util.List;

/**
 * 管理端系统配置服务接口，提供系统配置项的全量查询和批量更新功能。
 *
 * <p>配置项按分组（configGroup）和配置键（configKey）组织，支持 string/int/decimal 三种值类型校验。
 * 批量更新时自动去重合并，后值覆盖前值。
 * @author ikeu
 * @since 2026/06/18
 */
public interface AdminSystemConfigService extends IService<SystemConfig> {

    /**
     * 获取全部分组配置项
     * @return 配置项 VO 列表
     */
    List<SystemConfigVO> listAll();

    /**
     * 批量更新配置值，写入后清除缓存
     * @param dto 批量更新请求 DTO
     * @throws BusinessException 配置项不存在或值类型不匹配时抛出
     */
    void batchUpdate(SystemConfigBatchUpdateDTO dto);
}
