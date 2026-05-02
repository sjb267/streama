package com.streama.services;

import com.streama.entity.po.VideoComment;
import com.streama.entity.query.VideoCommentQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.exception.BusinessException;

import java.util.List;

/**
 * @Description:评论Service
 * @author:孙将斌
 * @date:2026/03/11
 */
public interface VideoCommentService{

	/**
	 * 根据条件查询列表
	 */
	List<VideoComment> findListByParam(VideoCommentQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(VideoCommentQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<VideoComment> findListByPage(VideoCommentQuery query);

	/**
	 * 新增
	 */
	Integer add(VideoComment bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<VideoComment> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(VideoComment bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<VideoComment> listBean);

	/**
	 * 根据CommentId查询
	 */
	VideoComment getVideoCommentByCommentId(Integer commentId);

	/**
	 * 根据CommentId更新
	 */
	Integer updateVideoCommentByCommentId(VideoComment bean, Integer commentId);

	/**
	 * 根据CommentId删除
	 */
	Integer deleteVideoCommentByCommentId(Integer commentId);

	Integer deleteByParam(VideoCommentQuery query);

	void postComment(VideoComment bean, Integer replyCommentId) throws BusinessException;

	void topComment(Integer commentId, String userId) throws BusinessException;

	void cancelTopComment(Integer commentId, String userId) throws BusinessException;

	void deleteComment(Integer commentId, String userId) throws BusinessException;
}
