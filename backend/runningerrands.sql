SET NAMES utf8mb4;
DROP DATABASE IF EXISTS runningerrands;
CREATE DATABASE IF NOT EXISTS runningerrands DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE runningerrands;

# ========================================== user表 ==========================================
# 用户主表(user)-存放核心登录信息及账户余额，用户可以发布任务
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(32)     NOT NULL UNIQUE COMMENT '用户名',
    `password`        VARCHAR(128)    NOT NULL COMMENT '密码（BCrypt 加密存储）',
    `pay_password`    VARCHAR(128)             DEFAULT NULL COMMENT '支付密码',
    `nickname`        VARCHAR(64)              DEFAULT '' COMMENT '用户昵称',
    `avatar_url`      VARCHAR(512)             DEFAULT '' COMMENT '头像URL',
    `phone`           CHAR(11)                 DEFAULT NULL COMMENT '手机号',
    `real_name`       VARCHAR(32)              DEFAULT '' COMMENT '真实姓名',
    `balance`         DECIMAL(10, 2)           DEFAULT 0.00 COMMENT '账户余额（元）',
    `campus`          VARCHAR(32)              DEFAULT '' COMMENT '学院名',
    `signature`       VARCHAR(128)             DEFAULT '' COMMENT '个性签名',
    `lng`             DECIMAL(10, 7)           DEFAULT NULL COMMENT '用户所在经度',
    `lat`             DECIMAL(10, 7)           DEFAULT NULL COMMENT '用户所在纬度',
    `status`          TINYINT                  DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `is_certify`      TINYINT                  DEFAULT 0 COMMENT '认证状态：0-未认证，1-审核中，2-已认证，3-认证驳回',
    `last_login_time` DATETIME                 DEFAULT NULL COMMENT '最后登录时间',
    `created_at`      DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    UNIQUE KEY `uk_username` (`username`), -- 用户名唯一
    UNIQUE KEY `uk_phone` (`phone`)        -- 手机号唯一
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

ALTER TABLE `user`
    ADD COLUMN `openid` VARCHAR(64) DEFAULT '' COMMENT '微信openid' AFTER `phone`;
ALTER TABLE `user`
    ADD COLUMN `unionid` VARCHAR(64) DEFAULT '' COMMENT '微信unionid' AFTER `openid`;
ALTER TABLE `user`
    ADD COLUMN `register_type` TINYINT DEFAULT 1 COMMENT '注册类型：1-手机号，2-微信' AFTER `lat`;
ALTER TABLE `user`
    ADD COLUMN `student_id` VARCHAR(32) DEFAULT '' COMMENT '学号' AFTER `real_name`;
ALTER TABLE `user`
    ADD COLUMN `certify_img` VARCHAR(512) DEFAULT '' COMMENT '认证照片URL' AFTER `student_id`;
ALTER TABLE `user`
    ADD COLUMN `certify_remark` VARCHAR(255) DEFAULT '' COMMENT '认证审核备注（驳回原因）' AFTER `certify_img`;
ALTER TABLE `user`
    ADD COLUMN `sex` VARCHAR(2) DEFAULT NULL COMMENT '性别：男/女' AFTER `student_id`;


# ========================================== user_address表 ==========================================
# 用户地址簿(user_address)-发布任务时的常用收货地址
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联的用户ID', -- 关联user表的id
    `contact_name`  VARCHAR(32)     NOT NULL COMMENT '联系人',
    `contact_phone` CHAR(11)        NOT NULL COMMENT '联系电话',
    `sex`           VARCHAR(2)     DEFAULT NULL COMMENT '性别',
    `detail`        VARCHAR(255)    NOT NULL COMMENT '详细地址，如：XX宿舍楼XXX室',
    `lng`           DECIMAL(10, 7) DEFAULT NULL COMMENT '地址经度',
    `lat`           DECIMAL(10, 7) DEFAULT NULL COMMENT '地址纬度',
    `is_default`    TINYINT        DEFAULT 0 COMMENT '是否默认地址',
    `created_at`    DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户地址簿';



# ========================================== runner_profile表 ==========================================
# 跑腿员表(runner_profile)-用户认证后成为跑腿员，成为跑腿员后可接取任务
DROP TABLE IF EXISTS `runner_profile`;
CREATE TABLE `runner_profile`
(
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`               BIGINT UNSIGNED NOT NULL COMMENT '关联的用户ID',
    `real_name`             VARCHAR(32)   DEFAULT '' COMMENT '真实姓名',
    `verify_status`         TINYINT       DEFAULT 0 COMMENT '认证状态：0-未认证，1-审核中，2-已认证，3-认证驳回',
    `verify_remark`         VARCHAR(255)  DEFAULT '' COMMENT '审核备注（驳回原因）',
    `credit_score`          TINYINT       DEFAULT 100 COMMENT '信用分',
    `total_orders`          INT UNSIGNED  DEFAULT 0 COMMENT '历史总接单数',
    `success_orders`        INT UNSIGNED  DEFAULT 0 COMMENT '成功完成单数',
    `avg_rating`            DECIMAL(2, 1) DEFAULT 5.0 COMMENT '平均评分（1.0~5.0）',
    `is_online`             TINYINT       DEFAULT 0 COMMENT '是否在线接单：0-离线，1-在线',
    `max_concurrent_orders` TINYINT       DEFAULT 3 COMMENT '最大同时接单数',
    `current_orders`        TINYINT       DEFAULT 0 COMMENT '当前进行中的订单数',
    `created_at`            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_verify_status` (`verify_status`),
    KEY `idx_online` (`is_online`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='跑腿员档案表';

ALTER TABLE `runner_profile`
    ADD COLUMN `is_banned` TINYINT DEFAULT 0 COMMENT '是否禁止接单：0-正常，1-禁止' AFTER `current_orders`;
ALTER TABLE `runner_profile`
    ADD COLUMN `ban_until` DATETIME DEFAULT NULL COMMENT '禁止接单截止时间' AFTER `is_banned`;



# ========================================== credit_log表 ==========================================
# credit_log表-信用分变动日志
DROP TABLE IF EXISTS `credit_log`;
CREATE TABLE `credit_log`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `runner_id`        BIGINT UNSIGNED NOT NULL COMMENT '跑腿员 user_id',
    `delta`            INT             NOT NULL COMMENT '信用分变动（正加负扣）',
    `score_before`     INT             NOT NULL COMMENT '变动前分值',
    `score_after`      INT             NOT NULL COMMENT '变动后分值',
    `reason_type`      VARCHAR(32)     NOT NULL COMMENT '原因类型：TIMEOUT/COMPLAINT/MANUAL/REWARD/RECOVER',
    `reason_detail`    VARCHAR(255)    DEFAULT '' COMMENT '原因描述',
    `related_order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联订单ID',
    `created_at`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_runner_time` (`runner_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='信用分变动日志';



# ========================================== admin表 ==========================================
# 管理员表(admin)-用于后台管理端管理所有用户及任务
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`
(
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(32)        NOT NULL COMMENT '姓名',
    `username`        VARCHAR(32) UNIQUE NOT NULL COMMENT '用户名',
    `password`        VARCHAR(128)       NOT NULL COMMENT '密码',
    `phone`           VARCHAR(11)        NOT NULL COMMENT '手机号',
    `sex`             VARCHAR(2)         NOT NULL COMMENT '性别',
    `id_number`       VARCHAR(18)        NOT NULL COMMENT '身份证号',
    `role`            TINYINT            NOT NULL COMMENT '角色：1-超级管理员 2-普通管理员',
    `status`          TINYINT  DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='管理员表';



# ========================================== task表 ==========================================
# 任务主表(task)-发布者创建的任务记录，包含 地理坐标 用于附近搜索
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_no`          VARCHAR(32)     NOT NULL COMMENT '任务编号（业务唯一，如T202601010001）',
    `publisher_id`     BIGINT UNSIGNED NOT NULL COMMENT '发布者ID', -- 关联user表的id
    `type`             VARCHAR(32)     NOT NULL COMMENT '任务大类',
    `sub_type`         VARCHAR(32)    DEFAULT NULL COMMENT '任务小类',
    `description`      VARCHAR(512)   DEFAULT '' COMMENT '任务详细描述',
    `pickup_code`      VARCHAR(64)    DEFAULT '' COMMENT '取件码',
    `reward`           DECIMAL(10, 2)  NOT NULL COMMENT '报酬金额',
    `pickup_address`   VARCHAR(255)   DEFAULT '' COMMENT '取件地址',
    `pickup_lng`       DECIMAL(10, 7) DEFAULT NULL COMMENT '取件地址经度',
    `pickup_lat`       DECIMAL(10, 7) DEFAULT NULL COMMENT '取件地址纬度',
    `delivery_address` VARCHAR(255)    NOT NULL COMMENT '送达地址',
    `delivery_lng`     DECIMAL(10, 7) DEFAULT NULL COMMENT '送达地址经度',
    `delivery_lat`     DECIMAL(10, 7) DEFAULT NULL COMMENT '送达地址纬度',
    `image_urls`       JSON           DEFAULT NULL COMMENT '任务图片URL数组',
    `expire_time`      DATETIME        NOT NULL COMMENT '任务过期时间',
    `status`           TINYINT        DEFAULT 1 COMMENT '状态：1-待接单，2-已接单，3-配送中，4-待确认，5-已完成，6-已取消',
    `cancel_reason`    VARCHAR(255)   DEFAULT '' COMMENT '取消原因',
    `created_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_no` (`task_no`),
    KEY `idx_publisher_status` (`publisher_id`, `status`),
    KEY `idx_geo` (`pickup_lng`, `pickup_lat`),
    KEY `idx_geo_delivery` (`delivery_lng`, `delivery_lat`),
    KEY `idx_type` (`type`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

ALTER TABLE `task`
    ADD COLUMN `require_sex` VARCHAR(8) DEFAULT '' COMMENT '要求接单人性别：男/女，空字符串表示不限' AFTER `pickup_code`;
ALTER TABLE `task`
    ADD COLUMN `task_specs` JSON DEFAULT NULL COMMENT '任务规格JSON' AFTER `sub_type`;
ALTER TABLE `task`
    ADD COLUMN `contact_name` VARCHAR(32) DEFAULT '' COMMENT '收货联系人' AFTER `delivery_address`;
ALTER TABLE `task`
    ADD COLUMN `contact_phone` CHAR(11) DEFAULT '' COMMENT '收货联系电话' AFTER `contact_name`;
ALTER TABLE `task`
    DROP COLUMN `description`;
ALTER TABLE `task`
    ADD COLUMN `public_desc` VARCHAR(256) DEFAULT '' COMMENT '任务公开描述' AFTER `sub_type`;
ALTER TABLE `task`
    ADD COLUMN `private_note` VARCHAR(256) DEFAULT '' COMMENT '任务私密备注' AFTER `public_desc`;
-- 费用明细拆分（小费 + 配送费 + 预估商品费 = reward）
ALTER TABLE `task`
    ADD COLUMN `tip`          DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '小费' AFTER `reward`,
    ADD COLUMN `delivery_fee` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '配送费' AFTER `tip`,
    ADD COLUMN `product_cost` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '预估商品费' AFTER `delivery_fee`;

-- 任务大厅列表/搜索/筛选（覆盖 listTasksHall, searchTasks, filterTasks）
ALTER TABLE `task`
    ADD INDEX `idx_task_hall` (`status`, `expire_time`, `created_at`);



# ========================================== task_order表 ==========================================
# 订单执行表(task_order)-记录跑腿员与任务的匹配关系
DROP TABLE IF EXISTS `task_order`;
CREATE TABLE `task_order`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`            BIGINT UNSIGNED NOT NULL COMMENT '关联的任务ID',
    `runner_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联的跑腿员ID',
    `status`             TINYINT         NOT NULL COMMENT '订单状态：1-待取货，2-配送中，3-待确认，4-已完成，5-已取消',
    `accept_time`        DATETIME DEFAULT NULL COMMENT '接单时间',
    `pickup_time`        DATETIME DEFAULT NULL COMMENT '取货时间',
    `deliver_time`       DATETIME DEFAULT NULL COMMENT '送达时间',
    `confirm_time`       DATETIME DEFAULT NULL COMMENT '确认完成时间',
    `expect_finish_time` DATETIME DEFAULT NULL COMMENT '预计送达时间',
    `pickup_proof_img`   JSON COMMENT '取货凭证图片JSON集合',
    `deliver_proof_img`  JSON COMMENT '送达凭证图片JSON集合',
    `is_deleted`         TINYINT  DEFAULT 0 COMMENT '0-未删除 1-已删除',
    `cancel_reason`      VARCHAR(255) DEFAULT '' COMMENT '取消原因',
    `cancel_time`        DATETIME DEFAULT NULL COMMENT '取消时间',

    PRIMARY KEY (`id`),
    KEY `idx_runner_status` (`runner_id`, `status`),
    KEY `idx_accept_time` (`accept_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务执行订单表';

-- 按任务ID查询（覆盖 getOrderDetailByTaskId, cancelTask 关联订单查询）
ALTER TABLE `task_order`
    ADD INDEX `idx_task_deleted` (task_id, is_deleted);
-- 按配送员查询未删除订单（覆盖 listMyAcceptOrders 状态筛选）
ALTER TABLE `task_order`
    ADD INDEX `idx_runner_deleted` (runner_id, is_deleted);
-- 配送员订单列表分页排序（覆盖 listMyAcceptOrders 按接单时间倒序）
ALTER TABLE `task_order`
    ADD INDEX `idx_runner_accept` (`runner_id`, `is_deleted`, `accept_time`);



# ========================================== transaction_record表 ==========================================
# 资金流水表(transaction_record)-记录每一笔资金的变动，用于对账
DROP TABLE IF EXISTS `transaction_record`;
CREATE TABLE `transaction_record`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联的用户ID',
    `task_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '关联的任务ID',
    `amount`         DECIMAL(10, 2)  NOT NULL COMMENT '变动金额',
    `type`           TINYINT         NOT NULL COMMENT '类型：1-任务悬赏支出，2-跑腿收入，3-充值，4-提现，5-退款',
    `balance_before` DECIMAL(10, 2)  NOT NULL COMMENT '变动前余额',
    `balance_after`  DECIMAL(10, 2)  NOT NULL COMMENT '变动后余额',
    `created_at`     DATETIME        DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='资金流水表';



# ========================================== review表 ==========================================
# 评价表(review)-任务完成后的互评
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联的任务ID',
    `reviewer_id`    BIGINT UNSIGNED NOT NULL COMMENT '评价人ID',
    `target_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被评价人ID',
    `rating`         TINYINT         NOT NULL COMMENT '评分 1-5',
    `content`        VARCHAR(255) DEFAULT '' COMMENT '评价备注',
    `tags`           JSON         DEFAULT NULL COMMENT '评价标签，如["态度好","速度快"]',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_target_user` (`target_user_id`),
    UNIQUE KEY `uk_task_reviewer` (`task_id`, `reviewer_id`) -- 同一任务每人只能评一次
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评价表';

# 评价表追加评价支持
ALTER TABLE `review`
    ADD COLUMN `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评价ID，NULL表示根评价';
ALTER TABLE `review`
    MODIFY COLUMN `rating` TINYINT NULL COMMENT '评分 1-5，追加评价可为空';

-- 父评价查询（追加评价列表）
ALTER TABLE `review`
    ADD KEY `idx_parent` (`parent_id`);
-- 用户评价列表 / 平均分计算（覆盖 listUserReviews, selectAvgRatingByTargetUser）
ALTER TABLE `review`
    ADD INDEX `idx_target_parent` (`target_user_id`, `parent_id`);
-- 任务评价查询 / 去重校验（覆盖 getTaskReviews, createReview 幂等检查）
ALTER TABLE `review`
    ADD INDEX `idx_task_reviewer_parent` (`task_id`, `reviewer_id`, `parent_id`);



# ========================================== notification表 ==========================================
# 站内信表(notification)-用于存储系统推送的消息记录
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `type`       TINYINT         NOT NULL COMMENT '1-系统通知，2-订单状态，3-活动提醒',
    `title`      VARCHAR(64)     NOT NULL COMMENT '标题',
    `content`    VARCHAR(512)    NOT NULL COMMENT '内容',
    `is_read`    TINYINT  DEFAULT 0 COMMENT '0-未读，1-已读',
    `target_id`  BIGINT   DEFAULT NULL COMMENT '关联的业务ID', -- type = 2时
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_user_unread` (`user_id`, `is_read`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内信表';

ALTER TABLE `notification`
    ADD INDEX `idx_created_at` (`created_at`);



# ========================================== chat_message表 ==========================================
# 聊天消息表(chat_message)-用户与配送员实时聊天记录
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `sender_id`    BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
    `receiver_id`  BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    `content`      VARCHAR(500)    NOT NULL COMMENT '消息内容',
    `message_type` TINYINT  DEFAULT 1 COMMENT '1-文字 2-图片',
    `is_read`      TINYINT  DEFAULT 0 COMMENT '0-未读 1-已读',
    `is_deleted`   TINYINT  DEFAULT 0 COMMENT '0-未删除 1-已删除',
    `is_recalled`  TINYINT  DEFAULT 0 COMMENT '0-正常 1-已撤回',
    `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_conversation` (`sender_id`, `receiver_id`),
    KEY `idx_receiver_unread` (`receiver_id`, `is_read`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='聊天消息表';

-- 未读消息计数 / 标记已读（覆盖 countUnreadsBatch, markRead）
ALTER TABLE `chat_message`
    ADD INDEX `idx_receiver_read_del` (`receiver_id`, `is_read`, `is_deleted`);


# ========================================== task_image表 ==========================================
DROP TABLE IF EXISTS `task_image`;
CREATE TABLE IF NOT EXISTS `task_image`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联任务ID',
    `url`        VARCHAR(512)    NOT NULL COMMENT '图片URL',
    `sort_order` TINYINT  DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`)
) COMMENT '任务图片表';



# ========================================== payment_idempotent表 ==========================================
# 幂等校验表(payment_idempotent)-存储订单幂等校验key
DROP TABLE IF EXISTS `payment_idempotent`;
CREATE TABLE IF NOT EXISTS `payment_idempotent`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `idempotent_key` VARCHAR(64)     NOT NULL,
    `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`)
);



# ========================================== operation_log表 ==========================================
# 操作日志表(operation_log)-记录管理员操作行为
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `admin_id`       BIGINT UNSIGNED NOT NULL COMMENT '操作管理员ID',
    `admin_name`     VARCHAR(32)     NOT NULL COMMENT '操作管理员姓名',
    `module`         VARCHAR(32)     NOT NULL COMMENT '操作模块',
    `action`         VARCHAR(32)     NOT NULL COMMENT '操作类型',
    `description`    VARCHAR(255) DEFAULT '' COMMENT '操作描述',
    `request_method` VARCHAR(8)   DEFAULT '' COMMENT '请求方法',
    `request_url`    VARCHAR(255) DEFAULT '' COMMENT '请求URL',
    `request_params` TEXT         DEFAULT NULL COMMENT '请求参数',
    `ip`             VARCHAR(45)  DEFAULT '' COMMENT '操作IP',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_admin` (`admin_id`),
    KEY `idx_module` (`module`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';



# ========================================== system_config表 ==========================================
# 系统配置表(system_config)-键值对存储平台运行参数，仅超管可修改
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `config_key`   VARCHAR(64)     NOT NULL UNIQUE COMMENT '配置键',
    `config_value` VARCHAR(1024)   NOT NULL DEFAULT '' COMMENT '配置值',
    `config_group` VARCHAR(32)     NOT NULL COMMENT '分组：基础设置/订单规则/跑腿限制/费率设置',
    `value_type`   VARCHAR(16)              DEFAULT 'string' COMMENT '值类型：string/int/decimal',
    `description`  VARCHAR(256)             DEFAULT '' COMMENT '配置说明',
    `updated_at`   DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at`   DATETIME                 DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表';

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `value_type`, `description`)
VALUES ('platform.hotline', '400-000-0000', '基础设置', 'string', '平台客服热线电话'),
       ('platform.announcement', '', '基础设置', 'string', '系统公告文本（显示在首页）'),
       ('order.task_expiry_hours', '24', '订单规则', 'int', '任务发布后无人接单自动过期时间（小时）'),
       ('order.auto_confirm_hours', '72', '订单规则', 'int', '配送员确认送达后自动确认收货时间（小时）'),
       ('order.min_withdrawal', '10.00', '订单规则', 'decimal', '用户最低提现金额（元）'),
       ('runner.max_concurrent', '5', '跑腿限制', 'int', '跑腿员最大同时接单数'),
       ('runner.min_credit_score', '60', '跑腿限制', 'int', '跑腿员最低接单信用分'),
       ('platform.fee_rate', '5.00', '费率设置', 'decimal', '平台服务费抽成比例（%）'),
       ('upload.max_daily', '20', '上传限制', 'int', '每用户每日最大上传文件次数'),
       ('banner.interval_seconds', '3', '轮播图', 'int', '首页轮播图切换间隔（秒），1-10'),
       ('banner.images', '', '轮播图', 'string', '首页轮播图图片URL，逗号分隔，最多5张');



# ========================================== 索引优化补充 ==========================================

-- user表：微信OAuth登录按openid查询
ALTER TABLE `user`
    ADD INDEX `idx_openid` (`openid`);
-- user表：管理端按状态筛选用户
ALTER TABLE `user`
    ADD INDEX `idx_status` (`status`);

-- runner_profile表：管理端筛选禁止接单的跑腿员
ALTER TABLE `runner_profile`
    ADD INDEX `idx_is_banned` (`is_banned`);

-- transaction_record表：按类型筛选资金流水（如只看提现记录）
ALTER TABLE `transaction_record`
    ADD INDEX `idx_user_type_time` (`user_id`, `type`, `created_at`);

-- chat_message表：会话消息分页查询（按时间排序）
ALTER TABLE `chat_message`
    ADD INDEX `idx_conversation_time` (`sender_id`, `receiver_id`, `created_at`);

# ========================================== 1.0 发版前约束修复 ==========================================

-- task表：补全NOT NULL约束（应用层已保证非空）
ALTER TABLE `task`
    MODIFY COLUMN `pickup_address` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '取件地址',
    MODIFY COLUMN `contact_name` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '收货联系人',
    MODIFY COLUMN `contact_phone` CHAR(11) NOT NULL DEFAULT '' COMMENT '收货联系电话',
    MODIFY COLUMN `public_desc` VARCHAR(256) NOT NULL DEFAULT '' COMMENT '任务公开描述',
    MODIFY COLUMN `private_note` VARCHAR(256) NOT NULL DEFAULT '' COMMENT '任务私密备注';

-- task_order表：cancel_reason仅在取消时有值，应允许NULL
ALTER TABLE `task_order`
    MODIFY COLUMN `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因';

-- task_order表：删除冗余索引（idx_runner_deleted被idx_runner_accept前缀覆盖）
ALTER TABLE `task_order`
    DROP INDEX `idx_runner_deleted`;

-- task_order表：配送超时检查器按预计送达时间查询
ALTER TABLE `task_order`
    ADD INDEX `idx_expect_finish` (`expect_finish_time`, `status`);

-- runner_profile表：并发接单数扩宽为SMALLINT（TINYINT上限127）
ALTER TABLE `runner_profile`
    MODIFY COLUMN `max_concurrent_orders` SMALLINT DEFAULT 3 COMMENT '最大同时接单数',
    MODIFY COLUMN `current_orders` SMALLINT DEFAULT 0 COMMENT '当前进行中的订单数';

-- chat_message表：删除冗余索引（idx_conversation被idx_conversation_time前缀覆盖）
ALTER TABLE `chat_message`
    DROP INDEX `idx_conversation`;

-- payment_idempotent表：清理任务按时间范围查询
ALTER TABLE `payment_idempotent`
    ADD INDEX `idx_created_at` (`created_at`);

-- system_config表：config_value扩容至1024，支持轮播图等多URL长配置
ALTER TABLE `system_config`
    MODIFY COLUMN `config_value` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '配置值';
