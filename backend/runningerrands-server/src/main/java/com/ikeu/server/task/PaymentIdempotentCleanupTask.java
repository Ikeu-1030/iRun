package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.model.entity.PaymentIdempotent;
import com.ikeu.server.mapper.PaymentIdempotentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 支付幂等记录清理定时任务，每日凌晨4点删除7天前的过期幂等记录。
 * @author ikeu
 * @since 2026/06/03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIdempotentCleanupTask {

    private final PaymentIdempotentMapper paymentIdempotentMapper;
    private final RedissonClient redissonClient;

    /**
     * 每日凌晨4点清理7天前的支付幂等记录，避免表数据无限膨胀。
     * 使用 Redisson 分布式锁防止多实例并发清理。
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanExpiredRecords() {
        RLock lock = redissonClient.getLock(RedisConstant.PAYMENT_IDEMPOTENT_CLEANUP_LOCK_KEY);
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return;
            }
            LocalDateTime deadline = LocalDateTime.now().minusDays(7);
            int deleted = paymentIdempotentMapper.delete(
                    new LambdaQueryWrapper<PaymentIdempotent>()
                            .lt(PaymentIdempotent::getCreatedAt, deadline));
            if (deleted > 0) {
                log.info("清理了 {} 条支付幂等记录", deleted);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}