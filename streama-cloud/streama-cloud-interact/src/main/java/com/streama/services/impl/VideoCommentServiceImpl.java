package com.streama.services.impl;

import com.streama.annotation.GlobalInterceptor;
import com.streama.api.consumer.VideoClient;
import com.streama.entity.constants.Constants;
import com.streama.entity.po.UserInfo;
import com.streama.entity.po.VideoComment;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.query.SimplePage;
import com.streama.entity.query.VideoCommentQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.CommentTopTypeEnum;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.enums.UserActionTypeEnum;
import com.streama.exception.BusinessException;
import com.streama.mappers.VideoCommentMapper;
import com.streama.services.VideoCommentService;
import jakarta.annotation.Resource;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description:评论Service
 * @author:孙将斌
 * @date:2026/03/11
 */
@Service("videoCommentService")
public class VideoCommentServiceImpl implements VideoCommentService{

	@Resource
	private VideoCommentMapper<VideoComment,VideoCommentQuery> videoCommentMapper;

    @Resource
	private VideoClient videoClient;

	/**
	 * 根据条件查询列表
	 */
	public List<VideoComment> findListByParam(VideoCommentQuery query) {
		if(query.getLoadChildren() != null && query.getLoadChildren()) {
			return this.videoCommentMapper.selectListWithChildren(query);
		}
		return videoCommentMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(VideoCommentQuery query) {
		return videoCommentMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<VideoComment> findListByPage(VideoCommentQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<VideoComment> list = findListByParam(query);
		PaginationResultVO<VideoComment> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(VideoComment bean) {
		return videoCommentMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<VideoComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoCommentMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(VideoComment bean) {
		return videoCommentMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<VideoComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoCommentMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据CommentId查询
	 */
	public VideoComment getVideoCommentByCommentId(Integer commentId) {
		return videoCommentMapper.selectByCommentId(commentId);
	}

	/**
	 * 根据CommentId更新
	 */
	public Integer updateVideoCommentByCommentId(VideoComment bean, Integer commentId) {
		return videoCommentMapper.updateByCommentId(bean, commentId);
	}

	/**
	 * 根据CommentId删除
	 */
	public Integer deleteVideoCommentByCommentId(Integer commentId) {
		return videoCommentMapper.deleteByCommentId(commentId);
	}

	@Override
	public Integer deleteByParam(VideoCommentQuery query) {
		return videoCommentMapper.deleteByParam(query);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void postComment(VideoComment bean, Integer replyCommentId) throws BusinessException {
		if(bean.getUserId() ==  null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
 		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(bean.getVideoId());
		if(videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())) {
			throw new BusinessException("博主已关闭评论区");
		}
		if(replyCommentId != null) {
			//获取回复评论
			VideoComment replyComment = getVideoCommentByCommentId(replyCommentId);
			if(replyComment == null || !replyComment.getVideoId().equals(bean.getVideoId())) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			//回复评论（0，则代表回复父级评论，否则回复子级评论）
			if(replyComment.getPCommentId() == 0) {
				//设置pcommendId为回复评论的ID
				bean.setPCommentId(replyComment.getCommentId());
			} else {
				//设置pcommendId为回复评论的父级评论ID
				bean.setPCommentId(replyComment.getPCommentId());
				bean.setReplyUserId(replyComment.getUserId());
			}
			UserInfo userInfo = videoClient.getUserInfoByUserId(replyComment.getUserId());
			bean.setReplyNickName(userInfo.getNickName());
			bean.setReplyAvatar(userInfo.getAvatar());
		} else {
			bean.setPCommentId(0);
		}
		bean.setPostTime(new Date());
		bean.setVideoUserId(videoInfo.getUserId());
		videoCommentMapper.insert(bean);
		if(bean.getPCommentId() == 0) {
			videoClient.updateCountInfo(bean.getVideoId(), UserActionTypeEnum.VIDEO_COMMENT.getField(), 1);
		}
	}

	@Override
	public void topComment(Integer commentId, String userId) throws BusinessException {
		cancelTopComment(commentId, userId);
		VideoComment videoComment = new VideoComment();
		videoComment.setTopType(CommentTopTypeEnum.TOP.getType());
		videoCommentMapper.updateByCommentId(videoComment, commentId);
	}

	@Override
	public void cancelTopComment(Integer commentId, String userId) throws BusinessException {
		VideoComment dbVideoComment = videoCommentMapper.selectByCommentId(commentId);
		if(dbVideoComment == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(dbVideoComment.getVideoId());
		if(videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(!videoInfo.getUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoComment videoComment = new VideoComment();
		videoComment.setTopType(CommentTopTypeEnum.NO_TOP.getType());

		VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
		videoCommentQuery.setVideoId(dbVideoComment.getVideoId());
		videoCommentQuery.setTopType(CommentTopTypeEnum.TOP.getType());
		videoCommentMapper.updateByParam(videoComment, videoCommentQuery);
	}

	@Override
	public void deleteComment(Integer commentId, String userId) throws BusinessException {
		VideoComment comment = videoCommentMapper.selectByCommentId(commentId);
		if(comment == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(comment.getVideoId());
		if(videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//判断删除评论者是否为作者本身还是评论者本身，若都不是则报错
		if(!videoInfo.getUserId().equals(userId) && !comment.getUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		videoCommentMapper.deleteByCommentId(commentId);
		if(comment.getPCommentId() == 0) {
			videoClient.updateCountInfo(videoInfo.getVideoId(), UserActionTypeEnum.VIDEO_COMMENT.getField(), -1);
			//删除二级评论
			VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
			videoCommentQuery.setPCommentId(commentId);
			videoCommentMapper.deleteByParam(videoCommentQuery);
		}
	}


}
