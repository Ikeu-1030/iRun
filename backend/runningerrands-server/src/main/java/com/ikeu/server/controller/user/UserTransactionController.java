package com.ikeu.server.controller.user;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.RechargeDTO;
import com.ikeu.model.vo.TransactionVO;
import com.ikeu.server.annotation.RequireCertify;
import com.ikeu.server.service.PaymentService;
import com.ikeu.server.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户收支明细与充值提现接口，提供流水查询、账户充值和余额提现功能。
 * @author ikeu
 * @since 2025/05/26
 */
@Tag(name = "用户端-收支账本接口", description = "收支明细查询")
@RestController
@RequestMapping("/user/transactions")
@RequiredArgsConstructor
@Validated
public class UserTransactionController {

    private final TransactionService transactionService;
    private final PaymentService paymentService;

    /**
     * 查询当前用户的收支汇总，包含总收入和总支出，不受分页限制。
     *
     * <p>从安全上下文中提取当前用户 ID，委托 {@link TransactionService#getSummary}
     * 执行聚合查询，返回用户全部流水记录的收入与支出合计值。
     *
     * @return 包含 total_income 和 total_expense 的收支汇总
     */
    @RequireCertify
    @Operation(summary = "查询收支汇总（全量，不受分页限制）")
    @GetMapping("/summary")
    public Result<Map<String, BigDecimal>> summary() {
        Long userId = BaseContext.getCurrentId();
        return Result.success(transactionService.getSummary(userId));
    }

    /**
     * 查询当前用户的收支明细，支持按类型和时间范围筛选。
     *
     * <p>委托 {@link TransactionService#listUserTransactions} 按 userId 精确匹配，
     * 可选按 type、时间区间筛选，按交易时间倒序分页返回 TransactionVO。
     *
     * @param type 流水类型（可选，1-支出 2-收入 3-充值 4-提现 5-退款）
     * @param start 开始日期（可选，格式 yyyy-MM-dd HH:mm:ss）
     * @param end 结束日期（可选，格式 yyyy-MM-dd HH:mm:ss）
     * @param page 页码，默认 1
     * @param size 每页条数，默认 10
     * @return 流水分页结果
     */
    @RequireCertify
    @Operation(summary = "查询我的收支明细")
    @GetMapping
    public Result<PageResult<TransactionVO>> list(
            @Parameter(description = "流水类型：1-支出 2-收入 3-充值 4-提现 5-退款")
            @RequestParam(required = false) Integer type,
            @Parameter(description = "开始日期（yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @Parameter(description = "结束日期（yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Long userId = BaseContext.getCurrentId();
        PageResult<TransactionVO> result = transactionService.listUserTransactions(userId, type, start, end, page, size);
        return Result.success(result);
    }

    /**
     * 向用户账户充值。
     *
     * <p>先调用 {@link PaymentService#verifyPayPassword} 校验支付密码，
     * 通过后调用 {@link PaymentService#recharge} 增加用户余额并记录充值流水。
     *
     * @param rechargeDTO 充值DTO（金额、支付密码）
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "账户充值")
    @PostMapping("/recharge")
    public Result<Void> recharge(@Valid @RequestBody RechargeDTO rechargeDTO) {
        Long userId = BaseContext.getCurrentId();
        paymentService.verifyPayPassword(userId, rechargeDTO.getPayPassword());
        paymentService.recharge(userId, rechargeDTO.getAmount());
        return Result.success(MessageConstant.RECHARGE_SUCCESS);
    }

    /**
     * 从用户账户提现。
     *
     * <p>先调用 {@link PaymentService#verifyPayPassword} 校验支付密码，
     * 通过后调用 {@link PaymentService#withdraw} 扣减用户余额并记录提现流水。
     *
     * @param rechargeDTO 提现DTO（金额、支付密码）
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "账户提现")
    @PostMapping("/withdraw")
    public Result<Void> withdraw(@Valid @RequestBody RechargeDTO rechargeDTO) {
        Long userId = BaseContext.getCurrentId();
        paymentService.verifyPayPassword(userId, rechargeDTO.getPayPassword());
        paymentService.withdraw(userId, rechargeDTO.getAmount());
        return Result.success(MessageConstant.WITHDRAW_SUCCESS);
    }
}
