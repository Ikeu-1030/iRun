package com.ikeu.common.constant;

/**
 * Redis 键前缀与过期时间常量，统一管理认证令牌、缓存名称、分布式锁、
 * 缓存穿透防护、速率限制和支付幂等等所有 Redis 键命名空间。
 *
 * @author ikeu
 * @since 2025/05/21
 */
public final class RedisConstant {

    private RedisConstant() {}

    // ========== 认证 / 令牌 ==========
    /** 用户 refresh token，实际 key = {@code user:refresh:token:{userId}:{jti}} */
    public static final String USER_REFRESH_TOKEN_PREFIX = "user:refresh:token:";
    /** 管理员 refresh token，实际 key = {@code admin:refresh:token:{adminId}:{jti}} */
    public static final String ADMIN_REFRESH_TOKEN_PREFIX = "admin:refresh:token:";

    // ========== 用户相关 ==========
    public static final String USER_INFO_PREFIX = "user:info:";                     // 用户基本信息
    public static final String USER_CERTIFY_CODE = "user:code:";                    // 验证码
    public static final String USER_LOGIN_FAIL_PREFIX = "user:login:fail:";         // 用户登录失败计数
    public static final String ADMIN_LOGIN_FAIL_PREFIX = "admin:login:fail:";       // 管理员登录失败计数
    public static final String USER_RESET_PWD_FAIL_PREFIX = "user:reset:pwd:fail:";         // 用户重置登录密码失败计数
    public static final String USER_RESET_PAY_PWD_FAIL_PREFIX = "user:reset:paypwd:fail:";  // 用户重置支付密码失败计数
    public static final long LOGIN_LOCK_SECONDS = 300;                              // 登录锁定时间-5分钟
    public static final int LOGIN_MAX_FAIL_COUNT = 5;                               // 登录最大失败次数

    // ========== 订单/任务操作锁 ==========
    // 所有订单&任务状态变更统一加锁 key，保证管理员、用户端、定时任务互斥
    public static final String ORDER_LOCK_KEY = "order:lock:";
    public static final String ORDER_DELAY_REMINDED = "order:delay:reminded:"; // 配送延迟已提醒标记

    // ========== 定时任务锁 ==========
    public static final String TASK_TIMEOUT_LOCK_KEY = "task:timeout:lock";                 // 任务超时取消检查锁
    public static final String ORDER_TIMEOUT_LOCK_KEY = "order:timeout:lock";               // 订单超时取消检查锁
    public static final String ORDER_AUTO_COMPLETE_LOCK_KEY = "order:autoComplete:lock";    // 24h自动结算锁
    public static final String NOTIFICATION_CLEANUP_LOCK_KEY = "notification:cleanup:lock"; // 通知清理锁
    public static final String CREDIT_RECOVERY_LOCK_KEY = "credit:recovery:lock"; // 信用分恢复定时任务锁
    public static final String PAYMENT_IDEMPOTENT_CLEANUP_LOCK_KEY = "payment:idempotent:cleanup:lock"; // 支付幂等清理锁

    // ========== 缓存击穿防护锁 ==========
    public static final String TASK_HALL_LOCK_KEY = "task:hall:lock:";              // 任务大厅分页缓存互斥锁前缀
    public static final String TASK_DETAIL_LOCK_PREFIX = "task:detail:lock:";       // 任务详情缓存互斥锁前缀

    // ========== 缓存穿透防护 ==========
    public static final String TASK_NOT_EXIST_PREFIX = "task:notExist:";            // 任务不存在标记前缀
    public static final String TASK_HALL_NULL_PREFIX = "task:notExist:hall:";       // 任务大厅空结果标记
    public static final Long TASK_NOT_EXIST_TTL = 60L;                              // 空标记过期时间-60秒

    // ========== Spring Cache 缓存名称 ==========
    public static final String CACHE_TASK_HALL = "task:hall";               // 接单大厅列表缓存
    public static final String CACHE_TASK_DETAIL = "task:detail";           // 任务详情缓存
    public static final String CACHE_LEADERBOARD = "runner:leaderboard";    // 跑腿员排行榜缓存
    public static final String CACHE_DASHBOARD = "admin:dashboard";         // 管理员仪表盘缓存

    // ========== 速率限制 ==========
    public static final String USER_SMS_RATE_KEY = "user:sms:rate:";           // 用户短信发送速率限制
    public static final String USER_LOGIN_RATE_KEY = "user:login:rate:";       // 用户登录速率限制
    public static final String USER_REFRESH_RATE_KEY = "user:refresh:rate:";   // 用户刷新令牌速率限制
    public static final String ADMIN_LOGIN_RATE_KEY = "admin:login:rate:";     // 管理员登录速率限制
    public static final String ADMIN_REFRESH_RATE_KEY = "admin:refresh:rate:"; // 管理员刷新速率限制
    public static final String USER_UPLOAD_DAILY_KEY = "user:upload:daily:";   // 用户每日上传次数

    // ========== 支付幂等 ==========
    public static final String PAY_TASK_KEY = "PAY:task:";                     // 支付幂等-任务付款
    public static final String REFUND_TASK_KEY = "REFUND:task:";               // 支付幂等-退款
    public static final String INCOME_TASK_KEY = "INCOME:task:";               // 支付幂等-跑腿员收入
    public static final String RECHARGE_USER_KEY = "RECHARGE:user:";           // 支付幂等-充值
    public static final String WITHDRAW_USER_KEY = "WITHDRAW:user:";           // 支付幂等-提现

    // ========== 通用过期时间（秒） ==========
    public static final Long CODE_EXPIRE = 300L;                            // 验证码过期时间-5分钟
    public static final Long TASK_CACHE_EXPIRE = 600L;                      // 任务缓存过期时间-10分钟
    public static final Integer LOCK_WAIT_TIME = 3;                         // 分布式锁等待时间-3秒
    public static final Integer LOCK_EXPIRE = 10;                           // 分布式锁持有时间-10秒
}