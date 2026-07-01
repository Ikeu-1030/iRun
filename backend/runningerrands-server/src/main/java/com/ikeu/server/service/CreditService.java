package com.ikeu.server.service;

/**
 * 信用分服务接口，提供订单完成信用分处理及通用加减分操作。
 * @author ikeu
 * @since 2026/05/14
 */
public interface CreditService {

    /**
     * 根据订单完成情况自动处理跑腿员信用分变更（提前完成 +5，按时 +1，超时扣分）。
     *
     * @param orderId 订单 ID
     */
    void processCreditOnDelivered(Long orderId);

    /**
     * 通用信用扣分（投诉、差评、违约或管理员手动操作），写入 CreditLog 并原子更新跑腿员档案，
     * 信用分低于阈值时自动冻结接单权限。
     *
     * @param runnerId 跑腿员用户 ID
     * @param penalty  扣分数值（正数）
     * @param reason   扣除原因描述
     */
    void deductCredit(Long runnerId, int penalty, String reason);

    /**
     * 通用信用加分（好评、提前完成奖励或管理员手动操作），写入 CreditLog 并原子更新跑腿员档案，
     * 信用分回升至阈值以上时自动解冻接单权限。
     *
     * @param runnerId 跑腿员用户 ID
     * @param reward   加分数值（正数）
     * @param reason   加分原因描述
     */
    void addCredit(Long runnerId, int reward, String reason);
}
