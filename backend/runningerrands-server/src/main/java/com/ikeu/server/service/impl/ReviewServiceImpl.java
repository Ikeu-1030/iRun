package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.exception.ForbiddenException;
import com.ikeu.common.exception.ParamErrorException;
import com.ikeu.model.dto.ReviewCreateDTO;
import com.ikeu.model.dto.ReviewUpdateDTO;
import com.ikeu.model.entity.*;
import com.ikeu.model.vo.ReviewVO;
import com.ikeu.server.mapper.*;
import com.ikeu.server.service.CreditService;
import com.ikeu.server.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评价服务实现，处理评价的创建、修改、删除、查询及追加评价等功能。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final TaskMapper taskMapper;
    private final TaskOrderMapper taskOrderMapper;
    private final UserMapper userMapper;
    private final RunnerProfileMapper runnerProfileMapper;
    private final CreditService creditService;

    private static final int RATING_MIN = 1;
    private static final int RATING_MAX = 5;
    private static final int REVIEW_EDIT_HOURS = 24 * 7; // 评价允许修改的时间间隔-7天


    /**
     * 将评价实体转换为评价VO
     *
     * @param review 评价实体
     * @param userMap 用户Map映射
     * @return ReviewVO 评价VO
     */
    private ReviewVO convertToVO(Review review, Map<Long, User> userMap) {
        ReviewVO vo = BeanUtil.copyProperties(review, ReviewVO.class, "tags");
        vo.setReviewId(review.getId());

        User reviewer = userMap.get(review.getReviewerId());
        vo.setReviewerNickname(reviewer != null ? reviewer.getNickname() : "匿名");
        vo.setReviewerAvatar(reviewer != null ? reviewer.getAvatarUrl() : "");

        vo.setTags(review.getTags() != null
                ? JSONUtil.toList(review.getTags(), String.class)
                : Collections.emptyList());

        return vo;
    }

    /**
     * 将评价实体列表转换为评价VO列表，使用预加载的 userMap，避免重复查询
     *
     * @param reviews 评价实体列表
     * @return List<ReviewVO> 评价VO列表
     */
    private List<ReviewVO> convertToVOList(List<Review> reviews, Map<Long, User> userMap) {
        if (reviews == null || reviews.isEmpty()) return Collections.emptyList();
        return reviews.stream()
                .map(review -> convertToVO(review, userMap))
                .collect(Collectors.toList());
    }

    /** 批量加载评论中的用户信息，返回 userMap */
    private Map<Long, User> loadReviewerUserMap(List<Review> reviews) {
        List<Long> reviewerIds = reviews.stream()
                .map(Review::getReviewerId).distinct().collect(Collectors.toList());
        return userMapper.selectBatchIds(reviewerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * 重新计算并更新跑腿员平均评分
     *  逻辑：查询该跑腿员所有根评价的 rating 平均值，更新 runner_profile.avg_rating
     *
     * @param runnerId 跑腿员用户ID
     */
    private void recalculateAvgRating(Long runnerId) {
        Double avg = reviewMapper.selectAvgRatingByTargetUser(runnerId);
        runnerProfileMapper.updateAvgRating(java.math.BigDecimal.valueOf(Math.round(avg * 10.0) / 10.0), runnerId);
    }

    /**
     * 创建追加评价方法
     *  校验：父评价存在，只有原评价人或被评价人可以追加
     *  逻辑：创建追加评价记录，关联父评价ID
     *
     * @param reviewerId 评价人ID
     * @param reviewDTO 追加评价DTO
     */
    private void createFollowUpInternal(Long reviewerId, ReviewCreateDTO reviewDTO) {
        Review parent = getById(reviewDTO.getParentId());
        if (parent == null) throw new NotFoundException(MessageConstant.PARENT_REVIEW_NOT_EXIST);

        // 只有原评价的评价人或被评价人可以追加
        if (!reviewerId.equals(parent.getReviewerId()) && !reviewerId.equals(parent.getTargetUserId())) {
            throw new ForbiddenException(MessageConstant.FOLLOWUP_NOT_ALLOWED);
        }

        Review followUp = Review.builder()
                .taskId(parent.getTaskId())
                .reviewerId(reviewerId)
                .targetUserId(reviewerId.equals(parent.getReviewerId()) ? parent.getTargetUserId() : parent.getReviewerId())
                .rating(null)
                .content(reviewDTO.getContent() != null ? reviewDTO.getContent() : "")
                .tags(null)
                .parentId(reviewDTO.getParentId())
                .createdAt(LocalDateTime.now())
                .build();
        reviewMapper.insert(followUp);
    }

    /**
     * 创建评价方法
     *  校验：任务存在且已完成，关联订单已完成，评价人为此任务的参与者，未重复评价
     *  逻辑：确定被评价人（发布者评价跑腿员或跑腿员评价发布者），保存评价信息，
     *  差评（评分1分）时扣除跑腿员信用分，支持追加评价（parentId不为空时走追加分支）
     *
     * @param reviewerId 评价人ID
     * @param reviewDTO 创建评价DTO
     */
    @Override
    @Transactional
    public void createReview(Long reviewerId, ReviewCreateDTO reviewDTO) {
        // 追加评价分支
        if (reviewDTO.getParentId() != null) {
            createFollowUpInternal(reviewerId, reviewDTO);
            return;
        }

        // 1. 确认任务存在且已完成
        Task task = taskMapper.selectById(reviewDTO.getTaskId());
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
        if (!Objects.equals(task.getStatus(), StatusConstant.TASK_COMPLETED)) {
            throw new ForbiddenException(MessageConstant.TASK_NOT_COMPLETED);
        }

        // 2. 获取关联订单，确认评价人是否为参与者
        TaskOrder order = taskOrderMapper.selectOne(
                new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getTaskId, reviewDTO.getTaskId()));
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
        if (!Objects.equals(order.getStatus(), StatusConstant.ORDER_COMPLETED)) {
            throw new ForbiddenException(MessageConstant.ORDER_NOT_COMPLETED);
        }

        // 只有发布者可以首次评价，跑腿员不能创建根评价
        if (!reviewerId.equals(task.getPublisherId())) {
            throw new ForbiddenException(MessageConstant.OPERATION_NOT_ALLOWED);
        }

        // 确定被评价人（此处 reviewerId 一定是发布者）
        Long targetUserId = order.getRunnerId();

        // 3. 检查是否已评价过（唯一约束 + 业务判断）
        boolean alreadyExists = reviewMapper.exists(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTaskId, reviewDTO.getTaskId())
                        .eq(Review::getReviewerId, reviewerId)
                        .isNull(Review::getParentId));
        if (alreadyExists) {
            throw new BusinessException(MessageConstant.REVIEW_ALREADY_EXISTS);
        }

        // 4. 保存评价
        Review review = Review.builder()
                .taskId(reviewDTO.getTaskId())
                .reviewerId(reviewerId)
                .targetUserId(targetUserId)
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent() != null ? reviewDTO.getContent() : "")
                .tags(reviewDTO.getTags() != null ? JSONUtil.toJsonStr(reviewDTO.getTags()) : null)
                .createdAt(LocalDateTime.now())
                .build();
        reviewMapper.insert(review);

        boolean targetIsRunner = targetUserId.equals(order.getRunnerId());

        // 5. 更新跑腿员信用分: 好评 +2, 差评 -5
        if (targetIsRunner) {
            if (reviewDTO.getRating() == 1) {
                creditService.deductCredit(order.getRunnerId(), 5, "收到差评");
            } else if (reviewDTO.getRating() >= 4) {
                creditService.addCredit(order.getRunnerId(), 2, "收到好评");
            }
            // 重新计算平均评分
            recalculateAvgRating(order.getRunnerId());
        }
    }

    /**
     * 修改评价方法
     *  校验：评价存在且为当前用户所写，未超过7天修改期限
     *  逻辑：支持修改评分、内容和标签，评分变更时触发信用分调整（差评恢复或改为差评扣分）
     *
     * @param reviewerId 评价人ID
     * @param reviewId 评价ID
     * @param reviewUpdateDTO 修改评价DTO
     */
    @Override
    @Transactional
    public void updateReview(Long reviewerId, Long reviewId, ReviewUpdateDTO reviewUpdateDTO) {
        Review review = getById(reviewId);
        if (review == null) throw new NotFoundException(MessageConstant.REVIEW_NOT_EXIST);
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new ForbiddenException(MessageConstant.REVIEW_NOT_ALLOWED_TO_MODIFY);
        }

        long hoursSinceCreated = Duration.between(review.getCreatedAt(), LocalDateTime.now()).toHours();
        if (hoursSinceCreated > REVIEW_EDIT_HOURS) {
            throw new ForbiddenException(MessageConstant.REVIEW_EDIT_TIMEOUT);
        }

        Integer oldRating = review.getRating();
        Integer newRating = reviewUpdateDTO.getRating();
        if (newRating != null) {
            if (newRating < RATING_MIN || newRating > RATING_MAX) {
                throw new ParamErrorException(MessageConstant.REVIEW_RATING_OUT_OF_RANGE);
            }
            review.setRating(newRating);
        }

        if (reviewUpdateDTO.getContent() != null) {
            review.setContent(reviewUpdateDTO.getContent());
        }
        if (reviewUpdateDTO.getTags() != null) {
            review.setTags(JSONUtil.toJsonStr(reviewUpdateDTO.getTags()));
        }
        updateById(review);

        if (oldRating != null && newRating != null && !Objects.equals(oldRating, newRating)) {
            TaskOrder order = taskOrderMapper.selectOne(
                    new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getTaskId, review.getTaskId()));
            if (order != null && review.getTargetUserId().equals(order.getRunnerId())) {
                if (oldRating == 1 && newRating != 1) {
                    creditService.addCredit(order.getRunnerId(), 5, "差评已修改");
                } else if (oldRating != 1 && newRating == 1) {
                    creditService.deductCredit(order.getRunnerId(), 5, "评价修改为差评");
                }
                recalculateAvgRating(order.getRunnerId());
            }
        }
    }

    /**
     * 删除评价方法
     *  校验：评价存在且为当前用户所写，未超过7天修改期限
     *  逻辑：删除评价，如果是差评（评分1分）则恢复跑腿员信用分
     *
     * @param reviewerId 评价人ID
     * @param reviewId 评价ID
     */
    @Override
    @Transactional
    public void deleteReview(Long reviewerId, Long reviewId) {
        Review review = getById(reviewId);
        if (review == null) throw new NotFoundException(MessageConstant.REVIEW_NOT_EXIST);
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new ForbiddenException(MessageConstant.REVIEW_NOT_ALLOWED_TO_DELETE);
        }

        long hoursSinceCreated = Duration.between(review.getCreatedAt(), LocalDateTime.now()).toHours();
        if (hoursSinceCreated > REVIEW_EDIT_HOURS) {
            throw new ForbiddenException(MessageConstant.REVIEW_EDIT_TIMEOUT);
        }

        Long runnerId = null;
        if (review.getRating() == 1) {
            TaskOrder order = taskOrderMapper.selectOne(
                    new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getTaskId, review.getTaskId()));
            if (order != null && review.getTargetUserId().equals(order.getRunnerId())) {
                runnerId = order.getRunnerId();
                creditService.addCredit(runnerId, 5, "差评已删除");
            }
        }

        removeById(reviewId);

        if (runnerId != null) recalculateAvgRating(runnerId);
    }

    /**
     * 查看某个用户收到的评价列表方法
     *  逻辑：查询指定用户收到的所有评价，按创建时间倒序排列
     *
     * @param targetUserId 目标用户ID
     * @return List<ReviewVO> 评价VO列表
     */
    @Override
    public List<ReviewVO> listUserReviews(Long targetUserId) {
        List<Review> rootReviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTargetUserId, targetUserId)
                        .isNull(Review::getParentId)
                        .orderByDesc(Review::getCreatedAt));
        if (rootReviews.isEmpty()) return Collections.emptyList();

        // 迭代查询所有层级的追加评价
        List<Review> allFollowUps = new ArrayList<>();
        List<Long> currentParentIds = rootReviews.stream().map(Review::getId).collect(Collectors.toList());
        while (!currentParentIds.isEmpty()) {
            List<Review> level = reviewMapper.selectList(
                    new LambdaQueryWrapper<Review>()
                            .in(Review::getParentId, currentParentIds)
                            .orderByAsc(Review::getCreatedAt));
            if (level.isEmpty()) break;
            allFollowUps.addAll(level);
            currentParentIds = level.stream().map(Review::getId).collect(Collectors.toList());
        }

        // 合并所有 review 的 reviewerId，只查一次 User
        List<Review> allReviews = new ArrayList<>(rootReviews);
        if (!allFollowUps.isEmpty()) allReviews.addAll(allFollowUps);
        Map<Long, User> userMap = loadReviewerUserMap(allReviews);

        List<ReviewVO> rootVOs = convertToVOList(rootReviews, userMap);
        if (!allFollowUps.isEmpty()) {
            List<ReviewVO> followUpVOs = convertToVOList(allFollowUps, userMap);
            Map<Long, List<ReviewVO>> childrenMap = followUpVOs.stream()
                    .collect(Collectors.groupingBy(ReviewVO::getParentId));
            for (ReviewVO root : rootVOs) {
                attachDescendants(root, childrenMap);
            }
        }
        return rootVOs;
    }

    /**
     * 创建追加评价方法（对外接口）
     *  逻辑：构建ReviewCreateDTO并调用createReview方法处理追加评价
     *
     * @param reviewerId 评价人ID
     * @param parentReviewId 父评价ID
     * @param content 追加评价内容
     */
    @Override
    public void createFollowUp(Long reviewerId, Long parentReviewId, String content) {
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setParentId(parentReviewId);
        dto.setContent(content);
        createReview(reviewerId, dto);
    }

    /**
     * 查看某个任务下的所有评价方法（含嵌套追加评价）
     *  逻辑：先查根评价列表，再查询所有追加评价按父评价ID分组，
     *  将追加评价挂载到对应根评价的followUps字段中
     *
     * @param taskId 任务ID
     * @return List<ReviewVO> 评价VO列表（含追加评价）
     */
    @Override
    public List<ReviewVO> getTaskReviews(Long taskId) {
        List<Review> rootReviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTaskId, taskId)
                        .isNull(Review::getParentId)
                        .orderByDesc(Review::getCreatedAt));
        if (rootReviews.isEmpty()) return Collections.emptyList();

        // 迭代查询所有层级的追加评价
        List<Review> allFollowUps = new ArrayList<>();
        List<Long> currentParentIds = rootReviews.stream().map(Review::getId).collect(Collectors.toList());
        while (!currentParentIds.isEmpty()) {
            List<Review> level = reviewMapper.selectList(
                    new LambdaQueryWrapper<Review>()
                            .in(Review::getParentId, currentParentIds)
                            .orderByAsc(Review::getCreatedAt));
            if (level.isEmpty()) break;
            allFollowUps.addAll(level);
            currentParentIds = level.stream().map(Review::getId).collect(Collectors.toList());
        }

        // 合并所有 review 的 reviewerId，只查一次 User
        List<Review> allReviews = new ArrayList<>(rootReviews);
        if (!allFollowUps.isEmpty()) allReviews.addAll(allFollowUps);
        Map<Long, User> userMap = loadReviewerUserMap(allReviews);

        List<ReviewVO> rootVOs = convertToVOList(rootReviews, userMap);
        if (!allFollowUps.isEmpty()) {
            List<ReviewVO> followUpVOs = convertToVOList(allFollowUps, userMap);
            Map<Long, List<ReviewVO>> childrenMap = followUpVOs.stream()
                    .collect(Collectors.groupingBy(ReviewVO::getParentId));
            for (ReviewVO root : rootVOs) {
                attachDescendants(root, childrenMap);
            }
        }
        return rootVOs;
    }

    /**
     * 递归挂载子孙评价到父评价的 followUps 字段中，构建树形嵌套结构
     *
     * @param parent 父评价 VO
     * @param childrenMap parentId → 子评价列表的映射
     */
    private void attachDescendants(ReviewVO parent, Map<Long, List<ReviewVO>> childrenMap) {
        List<ReviewVO> children = childrenMap.getOrDefault(parent.getReviewId(), Collections.emptyList());
        if (!children.isEmpty()) {
            parent.setFollowUps(children);
            for (ReviewVO child : children) {
                attachDescendants(child, childrenMap);
            }
        }
    }

}