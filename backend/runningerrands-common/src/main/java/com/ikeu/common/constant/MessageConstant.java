package com.ikeu.common.constant;

/**
 * 统一消息提示常量，涵盖认证、用户、任务、订单、配送员、余额、评价、
 * 支付密码、管理员、聊天、文件上传、系统配置和微信等所有业务域的提示文本。
 *
 * @author ikeu
 * @since 2025/05/21
 */
public final class MessageConstant {

    private MessageConstant() {}

    // ========== 通用消息 ==========
    public static final String SUCCESS = "操作成功";
    public static final String ERROR = "操作失败，请稍后重试";
    public static final String OPERATION_NOT_ALLOWED = "不允许的操作";
    public static final String PARAM_ERROR = "参数错误";
    public static final String SYSTEM_BUSY = "系统繁忙，请稍候再试";
    public static final String FORBIDDEN = "权限不足";
    public static final String NOT_FOUND = "资源不存在";

    // ========== 认证 / 令牌 ==========
    public static final String TOKEN_NOT_FOUND = "请先登录";
    public static final String TOKEN_EXPIRED = "登录已过期，请重新登录";
    public static final String TOKEN_INVALID = "无效的令牌";
    public static final String REFRESH_TOKEN_INVALID = "无效的刷新令牌";
    public static final String REFRESH_TOKEN_EXPIRED = "刷新令牌已失效，请重新登录";
    public static final String ACCOUNT_DISABLED_OR_NOT_EXIST = "账号已禁用或不存在";


    // ========== 用户相关 ==========
    public static final String USER_NOT_EXIST = "用户不存在";
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String PHONE_ALREADY_EXISTS = "手机号已被注册";
    public static final String USERNAME_ALREADY_EXISTS = "用户名已被占用";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String USERNAME_CANNOT_BE_NULL = "用户名不能为空";
    public static final String PASSWORD_CANNOT_BE_NULL = "密码不能为空";
    public static final String INVALID_CREDENTIALS = "用户名或密码错误";
    public static final String LOGIN_FAIL_LOCKED_USER = "登录失败次数过多，请5分钟后再试";
    public static final String INVALID_LOGIN_TYPE = "无效的登录方式";
    public static final String WECHAT_USER_NO_PASSWORD = "微信注册用户请使用验证码或微信一键登录";
    public static final String USER_DISABLED = "账号已被禁用";
    public static final String CODE_SEND_SUCCESS = "短信验证码发送成功";
    public static final String CODE_SEND_FAILED = "短信验证码发送失败，请稍后重试";
    public static final String CODE_ERROR = "验证码错误或已过期";
    public static final String SMS_RATE_LIMITED = "短信发送频率过高，请稍后再试";
    public static final String LOGOUT_SUCCESS = "退出登录成功";
    public static final String ACCOUNT_DELETED = "账户已注销";
    public static final String PHONE_ALREADY_BIND_TO_OTHER = "该手机号已被其他用户绑定";
    public static final String OLD_PASSWORD_INCORRECT = "旧密码不正确";
    public static final String PHONE_CHANGE_SUCCESS = "手机号修改成功";
    public static final String PASSWORD_CHANGE_SUCCESS = "密码修改成功";
    public static final String PHONE_NOT_MATCH = "手机号与当前账户不匹配";
    public static final String PASSWORD_RESET_SUCCESS = "登录密码重置成功";
    public static final String PAY_PASSWORD_RESET_SUCCESS = "支付密码重置成功";
    public static final String RESET_PWD_FAIL_LOCKED = "密码重置尝试次数过多，请5分钟后再试";
    public static final String CERTIFY_SUBMITTED = "实名认证提交成功，请等待审核";
    public static final String CERTIFY_CHECKING = "实名认证正在审核中，请勿重复提交";
    public static final String CERTIFY_SUCCESS = "您已完成实名认证";
    public static final String USER_NOT_CERTIFIED = "请先完成实名认证后再使用此功能";
    public static final String CERTIFY_REVIEWED = "实名认证审核完成";
    public static final String USER_NOT_IN_AUDITING_STATUS = "该用户不在审核中状态";


    // ========== 地址相关 ==========
    public static final String ADDRESS_NOT_EXISTS = "地址不存在";
    public static final String ADDRESS_SAVE_SUCCESS = "地址添加成功";
    public static final String ADDRESS_DELETE_SUCCESS = "地址删除成功";
    public static final String ADDRESS_UPDATE_SUCCESS = "地址修改成功";
    public static final String ADDRESS_SET_DEFAULT_SUCCESS = "默认地址设置成功";


    // ========== 任务相关 ==========
    public static final String TASK_NOT_EXIST = "任务不存在";
    public static final String TASK_PUBLISH_SUCCESS = "任务发布成功";
    public static final String TASK_EXPIRED = "任务已过期";
    public static final String TASK_CANNOT_ACCEPT_SELF = "您不能接取自己发布的任务";
    public static final String TASK_STATUS_ILLEGAL = "任务状态异常，操作失败";
    public static final String TASK_STATUS_NEED_REFRESH = "任务状态异常，请刷新后重试";
    public static final String TASK_STATUS_NOT_ALLOW = "当前任务状态不允许此操作";
    public static final String TASK_CANCEL_FAILED = "任务取消失败";
    public static final String TASK_CANCEL_NOT_BE_ALLOWED = "任务已被接单，请与接单员联系";
    public static final String TASK_WAIT_CONFIRM_CANNOT_CANCEL = "任务正在等待确认收货，暂无法取消";
    public static final String TASK_COMPLETED_CANNOT_CANCEL = "任务已完成，无法取消";
    public static final String TASK_ALREADY_CANCELLED = "任务已取消，请勿重复操作";
    public static final String TASK_TIMEOUT_CANCEL = "任务超时无人接单，已自动取消";
    public static final String TASK_CANCEL_SUCCESS = "任务取消成功";
    public static final String NEED_SELECT_DELIVERY_ADDRESS = "该任务类型必须选择送达地址";


    // ========== 订单相关 ==========
    public static final String ORDER_NOT_EXIST = "订单不存在";
    public static final String ORDER_CANCEL_NOT_ALLOWED = "当前订单不允许取消";
    public static final String ORDER_ACCEPTED_FAIL = "接单失败，当前订单可能已被接取或已取消";
    public static final String ORDER_STATUS_CHANGED = "订单状态已变更";
    public static final String ORDER_STATUS_ILLEGAL = "订单状态异常，操作失败";
    public static final String ORDER_STATUS_NEED_REFRESH = "订单状态异常，请刷新后重试";
    public static final String ORDER_STATUS_NOT_ALLOW = "当前订单状态不允许此操作";
    public static final String ORDER_ACCEPT_SUCCESS = "接单成功";
    public static final String ORDER_PICKUP_SUCCESS = "已确认取货";
    public static final String ORDER_DELIVERED_SUCCESS = "订单已确认送达";
    public static final String ORDER_COMPLETED_SUCCESS = "订单已确认完成";
    public static final String ORDER_CANCEL_SUCCESS = "订单取消成功";
    public static final String ORDER_PICKUP_TIMEOUT = "配送员未按时取货，订单自动取消";
    public static final String ORDER_CANNOT_VIEW = "您无权限查看此订单";
    public static final String ORDER_BELONGS_TO_NULL_TASK = "该任务暂无有效订单";
    public static final String ORDER_DELETED = "订单已删除";
    public static final String ORDER_CANCEL_TIMEOUT_RUNNER = "接单已超过5分钟，无法取消";


    // ========== 配送员相关 ==========
    public static final String RUNNER_NOT_EXIST = "配送员档案不存在";
    public static final String RUNNER_NOT_CERTIFIED = "您还未通过配送员认证";
    public static final String RUNNER_ONLINE = "您已在线";
    public static final String RUNNER_GO_ONLINE = "您已上线，可以接单了";
    public static final String RUNNER_GO_OFFLINE = "您已离线";
    public static final String RUNNER_OFFLINE = "您已离线，请先上线";
    public static final String RUNNER_MAX_ORDERS = "当前接单已达上限";
    public static final String RUNNER_LOW_CREDIT = "信用分不足，已暂停接单3天";
    public static final String CREDIT_FROZEN_TOAST = "您的信用分不足，已暂停接单";
    public static final String RUNNER_IS_BANNED = "您已被禁止接单，请联系管理员";
    public static final String GENDER_RESTRICTION_NOT_MATCH = "任务对配送员性别有要求，您不符合条件";
    public static final String RUNNER_APPLY_AUDITING = "配送员申请正在审核中，请勿重复提交";
    public static final String RUNNER_ALREADY_CERTIFIED = "您已是认证配送员";


    // ========== 余额相关 ==========
    public static final String AMOUNT_MUST_GREATER_THAN_ZERO = "操作金额必须大于0";
    public static final String BALANCE_NOT_ENOUGH = "账户余额不足，请先充值";
    public static final String RECHARGE_SUCCESS = "充值成功";
    public static final String WITHDRAW_SUCCESS = "提现申请已提交";


    // ========== 评价相关 ==========
    public static final String REVIEW_NOT_EXIST = "评价不存在";
    public static final String REVIEW_CREATE_SUCCESS = "评价成功";
    public static final String TASK_NOT_COMPLETED = "任务未完成，无法评价";
    public static final String ORDER_NOT_COMPLETED = "订单未完成，无法评价";
    public static final String REVIEW_NOT_ALLOWED = "您不能对此订单进行评价";
    public static final String REVIEW_ALREADY_EXISTS = "您已评价过此订单";
    public static final String REVIEW_DELETED = "评价已删除";
    public static final String REVIEW_NOT_ALLOWED_TO_DELETE = "您不能删除此评价";
    public static final String REVIEW_NOT_ALLOWED_TO_MODIFY = "您不能修改此评价";
    public static final String REVIEW_MODIFIED = "评价已修改";
    public static final String REVIEW_RATING_OUT_OF_RANGE = "评分需在1-5之间";
    public static final String REVIEW_EDIT_TIMEOUT = "评价已经过期，无法修改";
    public static final String FOLLOWUP_CREATE_SUCCESS = "追加评价成功";
    public static final String FOLLOWUP_NOT_ALLOWED = "您不能对此评价进行追加";
    public static final String PARENT_REVIEW_NOT_EXIST = "原评价不存在";


    // ========== 支付密码相关 ==========
    public static final String PAY_PASSWORD_NOT_SET = "请先设置支付密码";
    public static final String PAY_PASSWORD_ERROR = "支付密码错误";
    public static final String PAY_PASSWORD_SET_SUCCESS = "支付密码设置成功";
    public static final String PAY_PASSWORD_CHANGE_SUCCESS = "支付密码修改成功";
    public static final String PAY_PASSWORD_ALREADY_SET = "支付密码已设置";
    public static final String OLD_PAY_PASSWORD_INCORRECT = "原支付密码不正确";
    public static final String PAY_PASSWORD_CANNOT_BE_NULL = "支付密码不能为空";


    // ========== 管理员相关 ==========
    public static final String LOGIN_FAIL_LOCKED = "登录失败次数过多，请5分钟后再试";
    public static final String ACCESS_DENIED = "权限不足，无法执行此操作";
    public static final String RUNNER_NOT_IN_AUDITING_STATUS = "该跑腿员不在审核中状态";
    public static final String ADMIN_LOGIN_FAILED = "用户名或密码错误";
    public static final String ADMIN_DISABLED = "管理员账号已被禁用";
    public static final String ADMIN_NOT_EXIST = "管理员不存在";
    public static final String ADMIN_USERNAME_EXISTS = "管理员用户名已存在";
    public static final String ADMIN_CREATE_SUCCESS = "管理员创建成功";
    public static final String ADMIN_UPDATE_SUCCESS = "管理员信息已更新";
    public static final String ADMIN_DELETE_SUCCESS = "管理员已删除";
    public static final String ADMIN_PASSWORD_RESET = "密码重置成功";
    public static final String CANNOT_DELETE_SELF = "不能删除自己的账户";
    public static final String CANNOT_DELETE_SUPER_ADMIN = "不能删除超级管理员";
    public static final String CANNOT_DISABLE_SELF = "不能停用自己的账户";
    public static final String ADMIN_MUST_BE_NORMAL = "只能创建普通管理员";
    public static final String TASK_STATUS_UPDATED = "任务状态已更新";

    // ========== 聊天消息相关 ==========
    public static final String SENDER_ID_NULL = "发送者ID不能为空";
    public static final String MESSAGE_NOT_FOUND = "消息不存在";
    public static final String MESSAGE_NOT_YOURS = "只能操作自己发送的消息";
    public static final String MESSAGE_ALREADY_DELETED = "消息已被删除";
    public static final String MESSAGE_RECALL_TIMEOUT = "消息发送已超过5分钟，无法撤回";
    public static final String MESSAGE_RECALLED_CONTENT = "[消息已撤回]";


    // ========== 文件上传 ==========
    public static final String UPLOAD_SUCCESS = "上传成功";
    public static final String UPLOAD_FAILED = "上传失败";
    public static final String FILE_EMPTY = "文件不能为空";
    public static final String FILE_SIZE_EXCEED = "文件大小不能超过 5MB";
    public static final String FILE_TYPE_NOT_SUPPORTED = "不支持的文件类型";
    public static final String UPLOAD_LIMIT_EXCEEDED = "今日上传次数已达上限";
    public static final String CONFIG_VALUE_TYPE_UNKNOWN = "未知的配置值类型";

    // ========== 系统配置相关 ==========
    public static final String SYSTEM_CONFIG_NOT_EXIST = "系统配置不存在";
    public static final String SYSTEM_CONFIG_UPDATE_SUCCESS = "系统配置更新成功";

    // ========== 微信相关 ==========
    public static final String WECHAT_SERVICE_UNAVAILABLE = "微信服务不可用，请稍后重试";
    public static final String WECHAT_LOGIN_FAILED = "微信登录失败";

}
