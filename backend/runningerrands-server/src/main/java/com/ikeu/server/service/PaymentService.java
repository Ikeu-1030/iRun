package com.ikeu.server.service;

import java.math.BigDecimal;

/**
 * 支付服务接口，提供支付密码校验、任务支付退款、报酬结算和充值提现等功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface PaymentService {

    /**
     * 校验支付密码是否正确。使用 BCrypt 匹配，依次校验密码非空、用户存在、
     * 已设置支付密码、密码匹配。
     *
     * @param userId      用户 ID
     * @param rawPassword 原始支付密码（明文）
     * @throws BusinessException 密码为空、未设置支付密码或密码错误时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    void verifyPayPassword(Long userId, String rawPassword);

    /**
     * 发布任务时扣减用户余额，含幂等校验、行锁查询及支出流水记录。
     *
     * @param userId 用户 ID
     * @param taskId 任务 ID
     * @param amount 支付金额
     * @throws BusinessException 金额不合法或余额不足时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    void payForTask(Long userId, Long taskId, BigDecimal amount);

    /**
     * 取消任务时退款给发布者，使用数据库唯一键幂等记录防止重复退款。
     *
     * @param userId 用户 ID
     * @param taskId 任务 ID
     * @param amount 退款金额
     * @throws BusinessException 金额不合法时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    void refundForTask(Long userId, Long taskId, BigDecimal amount);

    /**
     * 任务完成时支付报酬给跑腿员，幂等校验后增加余额并记录收入流水，
     * 返回是否首次支付，调用方据此决定是否更新跑腿员完成统计。
     *
     * @param runnerId 跑腿员用户 ID
     * @param taskId   任务 ID
     * @param amount   报酬金额
     * @return true 首次支付，false 幂等拦截（重复请求）
     * @throws BusinessException 金额不合法时抛出
     * @throws NotFoundException 跑腿员用户不存在时抛出
     */
    boolean payToRunner(Long runnerId, Long taskId, BigDecimal amount);

    /**
     * 用户账户充值，使用 1 秒时间窗口幂等 + SELECT FOR UPDATE 防止重复提交。
     *
     * @param userId 用户 ID
     * @param amount 充值金额
     * @throws BusinessException 金额不合法时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    void recharge(Long userId, BigDecimal amount);

    /**
     * 用户账户提现，使用 1 秒时间窗口幂等 + SELECT FOR UPDATE，校验余额充足。
     *
     * @param userId 用户 ID
     * @param amount 提现金额
     * @throws BusinessException 金额不合法或余额不足时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    void withdraw(Long userId, BigDecimal amount);
}
