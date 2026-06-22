package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.utils.PiiMaskUtil;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.UserInfoVO;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端用户管理服务实现，处理用户列表、详情、状态切换和认证审核。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final RunnerProfileMapper runnerProfileMapper;

    /**
     * 分页查询用户列表，支持按状态、认证状态和关键词（用户名/手机号/昵称）筛选。
     */
    @Override
    public PageResult<UserInfoVO> listUsers(Integer status, Integer isCertify, String keyword, int page, int size) {
        Page<User> p = userMapper.selectUsersWithKeyword(new Page<>(page, size), status, isCertify, keyword);
        boolean mask = !Integer.valueOf(1).equals(BaseContext.getCurrentRole());
        List<UserInfoVO> records = p.getRecords().stream()
                .map(u -> BeanUtil.copyProperties(u, UserInfoVO.class))
                .peek(vo -> { if (mask) maskPii(vo); })
                .collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 启用/禁用用户，操作后清除仪表盘缓存。
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void toggleUserStatus(Long userId, Boolean enabled) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        user.setStatus(enabled ? StatusConstant.ENABLE : StatusConstant.DISABLE);
        userMapper.updateById(user);
        log.info("管理员切换用户 {} 状态为 {}", userId, enabled ? "启用" : "禁用");
    }

    /**
     * 查询用户详情，含跑腿员认证状态（若已申请）。
     */
    @Override
    public UserInfoVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        UserInfoVO vo = BeanUtil.copyProperties(user, UserInfoVO.class);
        RunnerProfile rp = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, userId));
        if (rp != null) {
            vo.setVerifyStatus(rp.getVerifyStatus());
        }
        if (!Integer.valueOf(1).equals(BaseContext.getCurrentRole())) maskPii(vo);
        return vo;
    }

    /**
     * 审核用户实名认证，仅“审核中”状态可操作，操作后清除仪表盘缓存。
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void reviewUserCertification(Long userId, Integer isCertify, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (!Objects.equals(user.getIsCertify(), StatusConstant.CERTIFY_AUDITING)) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_AUDITING_STATUS);
        }
        user.setIsCertify(isCertify);
        user.setCertifyRemark(remark != null ? remark : "");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("管理员审核用户 {} 实名认证，结果：{}，备注：{}", userId, isCertify, remark);
    }

    private void maskPii(UserInfoVO vo) {
        vo.setPhone(PiiMaskUtil.maskPhone(vo.getPhone()));
        vo.setRealName(PiiMaskUtil.maskRealName(vo.getRealName()));
        vo.setStudentId(PiiMaskUtil.maskStudentId(vo.getStudentId()));
    }
}
