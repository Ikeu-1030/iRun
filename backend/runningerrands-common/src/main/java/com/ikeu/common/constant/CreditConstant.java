package com.ikeu.common.constant;

/**
 * 信用分常量，定义阈值、冻结时长、原因类型。
 * @author ikeu
 * @since 2026/06/17
 */
public final class CreditConstant {

    private CreditConstant() {}

    /** 初始信用分 */
    public static final int CREDIT_INITIAL = 100;

    /** 信用分上限 */
    public static final int CREDIT_MAX = 100;

    /** 冻结阈值（低于该值触发冻结，恢复时也恢复至此值） */
    public static final int CREDIT_FREEZE_THRESHOLD = 60;

    /** 冻结天数 */
    public static final int CREDIT_FREEZE_DAYS = 3;

    /** 按时完成加分 */
    public static final int REWARD_ON_TIME = 1;

    /** 提前完成加分 */
    public static final int REWARD_EARLY = 5;

    /** 超时 0-30min 扣分 */
    public static final int PENALTY_TIMEOUT_30 = -2;
    /** 超时 30-60min 扣分 */
    public static final int PENALTY_TIMEOUT_60 = -5;
    /** 超时 60min+ 扣分 */
    public static final int PENALTY_TIMEOUT_OVER60 = -10;

    /** 好评加分 */
    public static final int REWARD_GOOD_REVIEW = 2;
    /** 差评扣分 */
    public static final int PENALTY_BAD_REVIEW = -5;

    /** 信用变动原因类型 */
    public static final class ReasonType {
        private ReasonType() {}
        public static final String TIMEOUT = "TIMEOUT";
        public static final String COMPLAINT = "COMPLAINT";
        public static final String MANUAL = "MANUAL";
        public static final String REWARD = "REWARD";
        public static final String RECOVER = "RECOVER";
    }

    /** 信用变动原因详情 */
    public static final class ReasonDetail {
        private ReasonDetail() {}
        public static final String EARLY = "提前完成";
        public static final String ON_TIME = "按时完成";
        public static final String TIMEOUT_PREFIX = "配送超时";
        public static final String GOOD_REVIEW = "好评";
        public static final String BAD_REVIEW = "差评";
        public static final String RECOVER_FROM_FREEZE = "冻结期满，自动恢复";
    }
}
