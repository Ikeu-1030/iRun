package com.ikeu.server.service;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.ForbiddenException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.exception.ParamErrorException;
import com.ikeu.model.dto.ReviewCreateDTO;
import com.ikeu.model.dto.ReviewUpdateDTO;
import com.ikeu.model.vo.ReviewVO;

import java.util.List;

/**
 * 评价服务接口，提供评价创建、修改、删除、查看和追加评价等功能。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface ReviewService {

    /**
     * 创建评价，校验订单已完成且未重复评价。
     *
     * <p>仅发布者可创建根评价，跑腿员可在根评价基础上追加追评。差评（评分 1 分）时
     * 自动扣除跑腿员信用分，好评（评分 >= 4 分）自动增加信用分。
     *
     * @param reviewerId 评价人 ID
     * @param reviewCreateDTO 创建评价 DTO（包含任务 ID、评分、内容、标签等）
     * @throws NotFoundException 任务或订单不存在时抛出
     * @throws ForbiddenException 任务或订单未完成、评价人非参与者时抛出
     * @throws BusinessException 重复评价时抛出
     */
    void createReview(Long reviewerId, ReviewCreateDTO reviewCreateDTO);

    /**
     * 修改评价，仅本人可修改且未超修改时限（7 天）。
     *
     * <p>评分变更时自动调整跑腿员信用分（差评改好评恢复、好评改差评扣分）。
     *
     * @param reviewerId 评价人 ID
     * @param reviewId 评价 ID
     * @param reviewUpdateDTO 修改评价 DTO（包含评分、内容、标签等）
     * @throws NotFoundException 评价不存在时抛出
     * @throws ForbiddenException 非本人评价或已超修改时限时抛出
     * @throws ParamErrorException 评分超出 1-5 范围时抛出
     */
    void updateReview(Long reviewerId, Long reviewId, ReviewUpdateDTO reviewUpdateDTO);

    /**
     * 删除评价，仅本人可删除且未超修改时限（7 天）。
     *
     * <p>差评被删除时自动恢复跑腿员信用分。
     *
     * @param reviewerId 评价人 ID
     * @param reviewId 评价 ID
     * @throws NotFoundException 评价不存在时抛出
     * @throws ForbiddenException 非本人评价或已超修改时限时抛出
     */
    void deleteReview(Long reviewerId, Long reviewId);

    /**
     * 查看指定用户收到的所有评价（含嵌套追加评价），按创建时间倒序排列。
     *
     * @param targetUserId 目标用户 ID
     * @return 评价 VO 列表（含追加评价树形结构）
     */
    List<ReviewVO> listUserReviews(Long targetUserId);

    /**
     * 查看指定任务关联的所有评价（含嵌套追加评价）。
     *
     * @param taskId 任务 ID
     * @return 评价 VO 列表（含追加评价树形结构）
     */
    List<ReviewVO> getTaskReviews(Long taskId);

    /**
     * 对已有评价进行追加评论，需为评价目标用户且未重复追加。
     *
     * @param reviewerId 评价人 ID
     * @param parentReviewId 父评价 ID
     * @param content 追加评价内容
     * @throws NotFoundException 父评价不存在时抛出
     * @throws ForbiddenException 当前用户非评价相关方时抛出
     */
    void createFollowUp(Long reviewerId, Long parentReviewId, String content);
}
