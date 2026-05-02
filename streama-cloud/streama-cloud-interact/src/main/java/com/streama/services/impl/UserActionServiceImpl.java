package com.streama.services.impl;

import com.streama.api.consumer.VideoClient;
import com.streama.entity.constants.Constants;
import com.streama.entity.po.UserAction;
import com.streama.entity.po.VideoComment;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.query.*;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.enums.SearchOrderTypeEnum;
import com.streama.entity.enums.UserActionTypeEnum;
import com.streama.exception.BusinessException;
import com.streama.mappers.UserActionMapper;
import com.streama.mappers.VideoCommentMapper;
import com.streama.services.UserActionService;
import jakarta.annotation.Resource;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description:用户行为Service
 * @author:孙将斌
 * @date:2026/03/11
 */
@Service("userActionService")
public class UserActionServiceImpl implements UserActionService{

	@Resource
	private UserActionMapper<UserAction,UserActionQuery> userActionMapper;

	@Resource
	private VideoCommentMapper<VideoComment, VideoCommentQuery> videoCommentMapper;

	@Resource
	private VideoClient videoClient;

	/**
	 * 根据条件查询列表
	 */
	public List<UserAction> findListByParam(UserActionQuery query) {
		return userActionMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserActionQuery query) {
		return userActionMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserAction> findListByPage(UserActionQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserAction> list = findListByParam(query);
		PaginationResultVO<UserAction> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(UserAction bean) {
		return userActionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return userActionMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(UserAction bean) {
		return userActionMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return userActionMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据ActionId查询
	 */
	public UserAction getUserActionByActionId(Integer actionId) {
		return userActionMapper.selectByActionId(actionId);
	}

	/**
	 * 根据ActionId更新
	 */
	public Integer updateUserActionByActionId(UserAction bean, Integer actionId) {
		return userActionMapper.updateByActionId(bean, actionId);
	}

	/**
	 * 根据ActionId删除
	 */
	public Integer deleteUserActionByActionId(Integer actionId) {
		return userActionMapper.deleteByActionId(actionId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId查询
	 */
	public UserAction getUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId更新
	 */
	public Integer updateUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(UserAction bean, String videoId, Integer commentId, Integer actionType, String userId) {
		return userActionMapper.updateByVideoIdAndCommentIdAndActionTypeAndUserId(bean, videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId删除
	 */
	public Integer deleteUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return userActionMapper.deleteByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void saveAction(UserAction userAction) throws BusinessException {
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(userAction.getVideoId());
		if (videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		userAction.setVideoUserId(videoInfo.getVideoId());
		UserActionTypeEnum userActionTypeEnum = UserActionTypeEnum.getByType(userAction.getActionType());
		if (userActionTypeEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserAction dbAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(
				userAction.getVideoId(), userAction.getCommentId(), userAction.getActionType(), userAction.getUserId());
		userAction.setActionTime(new Date());

		switch (userActionTypeEnum) {
			case VIDEO_LIKE:
			case VIDEO_COLLECT:
				if(dbAction != null) {
					userActionMapper.deleteByActionId(dbAction.getActionId());
				} else {
					userActionMapper.insert(userAction);
				}
				Integer changeCount = dbAction==null ? Constants.ONE: -Constants.ONE;
				videoClient.updateCountInfo(userAction.getVideoId(), userActionTypeEnum.getField(), changeCount);
				if(userActionTypeEnum == UserActionTypeEnum.VIDEO_COLLECT) {
					videoClient.updateDocCount(userAction.getVideoId(), SearchOrderTypeEnum.VIDEO_COLLECT, changeCount);
				}
				break;
			case VIDEO_COIN:
				if(videoInfo.getUserId().equals(userAction.getUserId())) {
					throw new BusinessException("不能对自己的视频投币");
				}
				if(dbAction != null) {
					throw new BusinessException("已对视频投币");
				}
				// 检查用户是否有足够的币
				Integer updateCount = videoClient.updateCoinCountInfo(userAction.getUserId(), -userAction.getActionCount());
				if(updateCount==0) {
					throw new BusinessException("币不足");
				}
				// 给博主加币
				updateCount = videoClient.updateCoinCountInfo(videoInfo.getUserId(), userAction.getActionCount());
				if(updateCount==0) {
					throw new BusinessException("给博主加币失败");
				}
				userActionMapper.insert(userAction);
				videoClient.updateCountInfo(userAction.getVideoId(), userActionTypeEnum.getField(), userAction.getActionCount());
				break;
			case COMMENT_LIKE:
			case COMMENT_HATE:
				//对立行为
				UserActionTypeEnum oActionTypeEnum = UserActionTypeEnum.COMMENT_LIKE == userActionTypeEnum? UserActionTypeEnum.COMMENT_HATE: UserActionTypeEnum.COMMENT_LIKE;
				UserAction oAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(
						userAction.getVideoId(), userAction.getCommentId(), oActionTypeEnum.getType(), userAction.getUserId());
				//对立行为若存在，删除对立行为
				if(oAction != null) {
					userActionMapper.deleteByActionId(oAction.getActionId());
				}

				//行为若存在，删除行为
				if(dbAction != null) {
					userActionMapper.deleteByActionId(dbAction.getActionId());
				} else {
					userActionMapper.insert(userAction);
				}

				changeCount = dbAction == null ? Constants.ONE: -Constants.ONE;
				Integer oChangeCount = -changeCount;
				videoCommentMapper.updateCountInfo(userAction.getCommentId(), userActionTypeEnum.getField(), changeCount, oAction==null?null:oActionTypeEnum.getField(), oChangeCount);
				break;
		}
	}
}
