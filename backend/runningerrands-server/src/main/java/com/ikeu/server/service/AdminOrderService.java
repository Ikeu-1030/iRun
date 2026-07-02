package com.ikeu.server.service;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderManageVO;

/**
 * 管理端订单管理服务接口，提供订单列表、详情和强制状态更新功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminOrderService {

    /**
     * 分页查询所有订单列表，支持按状态筛选。
     *
     * @param status 订单状态（null 表示查询全部状态）
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 订单管理列表分页结果
     */
    PageResult<OrderManageVO> listAllOrders(Integer status, int page, int size);

    /**
     * 根据订单 ID 查询订单详情（管理端），含完整任务信息、发布者和跑腿员信息及凭证图片，
     * 管理端可查看全部字段，不受手机号脱敏限制。
     *
     * @param orderId 订单 ID
     * @return 完整订单详情 VO
     * @throws NotFoundException 订单不存在时抛出
     */
    OrderDetailVO getOrderDetail(Long orderId);

    /**
     * 强制更新订单状态（经 OrderStateMachine 校验），分布式锁防并发覆盖，
     * 同步更新关联任务状态，终态自动触发资金结算或退款并清除相关缓存。
     *
     * @param orderId 订单 ID
     * @param status  目标状态码（见 StatusConstant）
     * @throws NotFoundException 订单不存在时抛出
     * @throws BusinessException 系统繁忙（分布式锁获取失败）或状态变更不合法时抛出
     */
    void updateOrderStatus(Long orderId, Integer status);
}
