package com.ikeu.server.aspect;

import com.ikeu.server.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 信用分切面，在跑腿员确认送达事务提交后自动清算履约信用分。
 * @author ikeu
 * @since 2026/05/14
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@RequiredArgsConstructor
public class CreditScoreAspect {

    private final CreditService creditService;

    /**
     * 后置通知，在跑腿员确认送达后注册事务同步回调清算信用分。
     *
     * <p>清算时机选在送达而非发布者确认：
     * <ul>
     *   <li>`deliverTime` 是跑腿员真实履约时刻，不受发布者操作延迟污染</li>
     *   <li>自动完成订单在送达时已完成清算，无需等待发布者确认</li>
     * </ul>
     * 通过 {@link TransactionSynchronizationManager#registerSynchronization} 注册 afterCommit 回调，
     * 等外层事务提交释放 runner_profile 行锁后，由 {@link CreditService#processCreditOnDelivered}
     *（REQUIRES_NEW 事务）在独立事务中处理信用分变更。
     *
     * @param runnerId 跑腿员ID
     * @param orderId 订单ID
     */
    @AfterReturning("execution(* com.ikeu.server.service.TaskOrderService.confirmDeliver(..)) && args(runnerId, orderId, proof)")
    public void afterConfirmDeliver(Long runnerId, Long orderId, Object proof) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                creditService.processCreditOnDelivered(orderId);
            }
        });
    }

}
