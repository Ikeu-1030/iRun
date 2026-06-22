package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.CreditConstant;
import com.ikeu.model.entity.CreditLog;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.CreditLogMapper;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CreditServiceImpl 单元测试。
 * 覆盖 processCreditOnComplete、deductCredit、addCredit 的全部决策分支。
 */
@ExtendWith(MockitoExtension.class)
class CreditServiceImplTest {

    @Mock
    private TaskOrderMapper taskOrderMapper;

    @Mock
    private RunnerProfileMapper runnerProfileMapper;

    @Mock
    private CreditLogMapper creditLogMapper;

    private CreditServiceImpl creditService;

    @BeforeEach
    void setUp() {
        creditService = new CreditServiceImpl(taskOrderMapper, runnerProfileMapper, creditLogMapper);
    }

    // ========== processCreditOnComplete — 空值 / 边界 ==========

    @Test
    void processCreditOnComplete_orderIsNull_shouldReturnWithoutAction() {
        when(taskOrderMapper.selectById(anyLong())).thenReturn(null);

        creditService.processCreditOnComplete(1L);

        verify(taskOrderMapper).selectById(1L);
        verifyNoInteractions(runnerProfileMapper, creditLogMapper);
    }

    @Test
    void processCreditOnComplete_expectFinishTimeIsNull_shouldReturnWithoutAction() {
        TaskOrder order = TaskOrder.builder().id(1L).build();
        when(taskOrderMapper.selectById(1L)).thenReturn(order);

        creditService.processCreditOnComplete(1L);

        verify(taskOrderMapper).selectById(1L);
        verifyNoInteractions(runnerProfileMapper, creditLogMapper);
    }

    @Test
    void processCreditOnComplete_runnerProfileNotFound_shouldLogWarningAndSkip() {
        TaskOrder order = TaskOrder.builder()
                .id(1L).runnerId(100L)
                .expectFinishTime(LocalDateTime.now().minusMinutes(15))
                .build();

        when(taskOrderMapper.selectById(1L)).thenReturn(order);
        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        creditService.processCreditOnComplete(1L);

        verify(creditLogMapper, never()).insert(any());
        verify(runnerProfileMapper, never()).updateCreditScoreAndFreeze(anyLong(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    // ========== processCreditOnComplete — 各时段 ==========

    @Test
    void processCreditOnComplete_early_shouldReward5() {
        TaskOrder order = TaskOrder.builder()
                .id(1L).runnerId(100L)
                .expectFinishTime(LocalDateTime.now().plusHours(1))
                .build();
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(80).isBanned(0).build();

        when(taskOrderMapper.selectById(1L)).thenReturn(order);
        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.processCreditOnComplete(1L);

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(100L, creditLog.getRunnerId());
        assertEquals(CreditConstant.REWARD_EARLY, creditLog.getDelta());
        assertEquals(80, creditLog.getScoreBefore());
        assertEquals(85, creditLog.getScoreAfter());
        assertEquals(CreditConstant.ReasonType.REWARD, creditLog.getReasonType());
        assertEquals("提前完成", creditLog.getReasonDetail());
        assertEquals(1L, creditLog.getRelatedOrderId());

        verify(runnerProfileMapper).updateCreditScoreAndFreeze(
                eq(100L), eq(CreditConstant.REWARD_EARLY),
                eq(CreditConstant.CREDIT_FREEZE_THRESHOLD), eq(CreditConstant.CREDIT_FREEZE_DAYS),
                eq(CreditConstant.CREDIT_MAX));
    }

    @Test
    void processCreditOnComplete_onTime_shouldReward1() {
        TaskOrder order = TaskOrder.builder()
                .id(1L).runnerId(100L)
                .expectFinishTime(LocalDateTime.now().minusSeconds(30))
                .build();
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(90).isBanned(0).build();

        when(taskOrderMapper.selectById(1L)).thenReturn(order);
        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.processCreditOnComplete(1L);

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(100L, creditLog.getRunnerId());
        assertEquals(CreditConstant.REWARD_ON_TIME, creditLog.getDelta());
        assertEquals(90, creditLog.getScoreBefore());
        assertEquals(91, creditLog.getScoreAfter());
        assertEquals(CreditConstant.ReasonType.REWARD, creditLog.getReasonType());
        assertEquals("按时完成", creditLog.getReasonDetail());
        assertEquals(1L, creditLog.getRelatedOrderId());
    }

    @Test
    void processCreditOnComplete_late15min_shouldDeduct2() {
        TaskOrder order = TaskOrder.builder()
                .id(1L).runnerId(100L)
                .expectFinishTime(LocalDateTime.now().minusMinutes(15))
                .build();
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(85).isBanned(0).build();

        when(taskOrderMapper.selectById(1L)).thenReturn(order);
        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.processCreditOnComplete(1L);

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(-2, creditLog.getDelta());
        assertEquals(85, creditLog.getScoreBefore());
        assertEquals(83, creditLog.getScoreAfter());
        assertEquals(CreditConstant.ReasonType.TIMEOUT, creditLog.getReasonType());
        assertTrue(creditLog.getReasonDetail().contains("配送超时"));
        assertEquals(1L, creditLog.getRelatedOrderId());
    }

    @Test
    void processCreditOnComplete_late45min_shouldDeduct5() {
        TaskOrder order = TaskOrder.builder()
                .id(1L).runnerId(100L)
                .expectFinishTime(LocalDateTime.now().minusMinutes(45))
                .build();
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(80).isBanned(0).build();

        when(taskOrderMapper.selectById(1L)).thenReturn(order);
        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.processCreditOnComplete(1L);

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(-5, creditLog.getDelta());
        assertEquals(80, creditLog.getScoreBefore());
        assertEquals(75, creditLog.getScoreAfter());
        assertEquals(CreditConstant.ReasonType.TIMEOUT, creditLog.getReasonType());
    }

    @Test
    void processCreditOnComplete_late90min_shouldDeduct10() {
        TaskOrder order = TaskOrder.builder()
                .id(1L).runnerId(100L)
                .expectFinishTime(LocalDateTime.now().minusMinutes(90))
                .build();
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(100).isBanned(0).build();

        when(taskOrderMapper.selectById(1L)).thenReturn(order);
        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.processCreditOnComplete(1L);

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(-10, creditLog.getDelta());
        assertEquals(100, creditLog.getScoreBefore());
        assertEquals(90, creditLog.getScoreAfter());
        assertEquals(CreditConstant.ReasonType.TIMEOUT, creditLog.getReasonType());
    }

    // ========== deductCredit ==========

    @Test
    void deductCredit_nullRunnerId_shouldReturnWithoutAction() {
        creditService.deductCredit(null, 5, "差评");
        verifyNoInteractions(runnerProfileMapper, taskOrderMapper, creditLogMapper);
    }

    @Test
    void deductCredit_nonPositivePenalty_shouldReturnWithoutAction() {
        creditService.deductCredit(100L, 0, "test");
        creditService.deductCredit(100L, -1, "test");
        verifyNoInteractions(runnerProfileMapper, taskOrderMapper, creditLogMapper);
    }

    @Test
    void deductCredit_normal_shouldDeductWithManualReason() {
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(80).isBanned(0).build();

        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.deductCredit(100L, 10, "违规操作");

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(100L, creditLog.getRunnerId());
        assertEquals(-10, creditLog.getDelta());
        assertEquals(80, creditLog.getScoreBefore());
        assertEquals(70, creditLog.getScoreAfter());
        assertEquals(CreditConstant.ReasonType.MANUAL, creditLog.getReasonType());
        assertEquals("违规操作", creditLog.getReasonDetail());
        assertNull(creditLog.getRelatedOrderId());

        verify(runnerProfileMapper).updateCreditScoreAndFreeze(
                eq(100L), eq(-10), eq(CreditConstant.CREDIT_FREEZE_THRESHOLD), eq(CreditConstant.CREDIT_FREEZE_DAYS),
                eq(CreditConstant.CREDIT_MAX));
    }

    // ========== addCredit ==========

    @Test
    void addCredit_nullRunnerId_shouldReturnWithoutAction() {
        creditService.addCredit(null, 5, "好评奖励");
        verifyNoInteractions(runnerProfileMapper, taskOrderMapper, creditLogMapper);
    }

    @Test
    void addCredit_nonPositiveBonus_shouldReturnWithoutAction() {
        creditService.addCredit(100L, 0, "test");
        creditService.addCredit(100L, -1, "test");
        verifyNoInteractions(runnerProfileMapper, taskOrderMapper, creditLogMapper);
    }

    @Test
    void addCredit_normal_shouldAddWithRewardReason() {
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(75).isBanned(0).build();

        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.addCredit(100L, 5, "好评奖励");

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(100L, creditLog.getRunnerId());
        assertEquals(5, creditLog.getDelta());
        assertEquals(75, creditLog.getScoreBefore());
        assertEquals(80, creditLog.getScoreAfter());
        assertEquals(CreditConstant.ReasonType.REWARD, creditLog.getReasonType());
        assertEquals("好评奖励", creditLog.getReasonDetail());
        assertNull(creditLog.getRelatedOrderId());
    }

    // ========== 冻结 / 解冻 阈值 ==========

    @Test
    void deductCredit_belowFreezeThreshold_shouldSetBanUntil() {
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(65).isBanned(0).build();

        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.deductCredit(100L, 10, "违规");

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(-10, creditLog.getDelta());
        assertEquals(65, creditLog.getScoreBefore());
        assertEquals(55, creditLog.getScoreAfter());

        // freezeDays passed to SQL; freeze logic now computed atomically in DB
        verify(runnerProfileMapper).updateCreditScoreAndFreeze(
                eq(100L), eq(-10), eq(CreditConstant.CREDIT_FREEZE_THRESHOLD), eq(CreditConstant.CREDIT_FREEZE_DAYS),
                eq(CreditConstant.CREDIT_MAX));
    }

    @Test
    void addCredit_aboveFreezeThreshold_shouldClearBan() {
        // score was 55 (below threshold, currently banned), add 10 -> 65 (>= 60)
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(55).isBanned(1)
                .banUntil(LocalDateTime.now().plusDays(1)).build();

        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.addCredit(100L, 10, "恢复奖励");

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(10, creditLog.getDelta());
        assertEquals(55, creditLog.getScoreBefore());
        assertEquals(65, creditLog.getScoreAfter());

        // freeze logic now computed atomically in SQL
        verify(runnerProfileMapper).updateCreditScoreAndFreeze(
                eq(100L), eq(10), eq(CreditConstant.CREDIT_FREEZE_THRESHOLD), eq(CreditConstant.CREDIT_FREEZE_DAYS),
                eq(CreditConstant.CREDIT_MAX));
    }

    // ========== 分数下限 ==========

    @Test
    void deductCredit_scoreFloor_shouldNotGoBelowZero() {
        RunnerProfile profile = RunnerProfile.builder()
                .userId(100L).creditScore(3).isBanned(0).build();

        when(runnerProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

        creditService.deductCredit(100L, 10, "penalty");

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(logCaptor.capture());
        CreditLog creditLog = logCaptor.getValue();

        assertEquals(-10, creditLog.getDelta());
        assertEquals(3, creditLog.getScoreBefore());
        assertEquals(0, creditLog.getScoreAfter());  // floor at 0, not -7
    }
}
