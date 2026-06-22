package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.CancelOrderDTO;
import com.ikeu.model.dto.ProofImageDTO;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderListVO;

/**
 * 订单服务接口，提供接单、取货、送达、确认完成、详情查询和删除等订单全生命周期操作。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface TaskOrderService {

    /**
     * 配送员接单，使用 Redis 分布式锁防止并发抢单。
     *
     * <p>前置校验：任务存在且为"待接单"、任务未过期、配送员非发布者、性别要求匹配、
     * 配送员已认证、在线、未达接单上限、信用分达标、未被封禁。
     * 持锁后二次校验任务状态和订单存在性，防止并发冲突。
     *
     * @param runnerId 配送员ID
     * @param taskId 任务ID
     * @throws BusinessException 任务不存在/已过期/已接单/性别不匹配/配送员未认证/离线/订单满/信用低/被封禁/系统繁忙
     * @return 新建的订单ID
     */
    Long acceptOrder(Long runnerId, Long taskId);

    /**
     * 配送员确认取货，上传取货凭证并更新订单状态。
     *
     * @param runnerId 配送员ID
     * @param orderId 订单ID
     * @param proof 取货凭证图片信息
     */
    void confirmPickup(Long runnerId, Long orderId, ProofImageDTO proof);

    /**
     * 配送员确认送达，上传送达凭证并更新订单状态。
     *
     * @param runnerId 配送员ID
     * @param orderId 订单ID
     * @param proof 送达凭证图片信息
     */
    void confirmDeliver(Long runnerId, Long orderId, ProofImageDTO proof);

    /**
     * 发布者确认订单完成，触发报酬结算和信用分更新。
     *
     * <p>校验当前用户为任务发布者、订单状态为"已送达"，
     * 更新订单和任务状态为"已完成"，向跑腿员支付报酬。
     *
     * @param publisherId 发布者ID
     * @param orderId 订单ID
     */
    void confirmComplete(Long publisherId, Long orderId);

    /**
     * 通过订单ID查询订单详情，校验查看权限。
     *
     * <p>非参与方查看时业务信息脱敏。
     *
     * @param orderId 订单ID
     * @param currentUserId 当前登录用户ID
     * @return 订单详情VO，含发布者/配送员信息、地址、凭证图片等
     */
    OrderDetailVO getOrderDetailByOrderId(Long orderId, Long currentUserId);

    /**
     * 通过任务ID查询关联订单详情，校验查看权限。
     *
     * <p>若无有效订单且任务已取消，构建已取消的订单详情返回。
     *
     * @param taskId 任务ID
     * @param currentUserId 当前登录用户ID
     * @return 订单详情VO
     */
    OrderDetailVO getOrderDetailByTaskId(Long taskId, Long currentUserId);

    /**
     * 配送员查看已接订单列表，支持按状态筛选。
     *
     * @param userId 配送员用户ID
     * @param status 订单状态筛选（可选，null为全部）
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 配送员已接订单列表分页结果
     */
    PageResult<OrderListVO> listMyAcceptOrders(Long userId, Integer status, int page, int size);

    /**
     * 配送员接单后5分钟内取消订单，关联任务回退至待接单状态。
     *
     * @param runnerId 配送员ID
     * @param orderId 订单ID
     * @param dto 取消订单DTO（包含取消原因）
     */
    void cancelOrder(Long runnerId, Long orderId, CancelOrderDTO dto);

    /**
     * 发布者或配送员软删除已完成超过7天的订单。
     *
     * <p>仅允许订单发布者或配送员操作，且订单状态必须为"已完成"且完成时间超过7天。
     *
     * @param userId 当前用户ID
     * @param orderId 订单ID
     */
    void deleteOrder(Long userId, Long orderId);
}
