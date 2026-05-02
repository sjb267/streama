package com.streama.entity.query;

import java.util.Date;

/**
 * @Description:用户行为查询对象
 * @author:孙将斌
 * @date:2026/03/11
 */
public class UserActionQuery extends BaseQuery {
	/**
	 * 自增ID
	 */
	private Integer actionId;

	/**
	 * 视频ID
	 */
	private String videoId;

	private String videoIdFuzzy;

	/**
	 * 视频用户ID
	 */
	private String videoUserId;

	private String videoUserIdFuzzy;

	/**
	 * 评论ID
	 */
	private Integer commentId;

	/**
	 * 0:评论喜欢点赞 1:讨厌评论 2:视频点赞 3:视频收藏 4:视频投币
	 */
	private Integer actionType;

	/**
	 * 数量
	 */
	private Integer actionCount;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 操作时间
	 */
	private Date actionTime;

	private String actionTimeStart;

	private String actionTimeEnd;

	private Integer[] actionTypeArray;

	public Boolean getQueryVideoInfo() {
		return queryVideoInfo;
	}

	public void setQueryVideoInfo(Boolean queryVideoInfo) {
		this.queryVideoInfo = queryVideoInfo;
	}

	private Boolean queryVideoInfo;

	public Integer[] getActionTypeArray() {
		return actionTypeArray;
	}

	public void setActionTypeArray(Integer[] actionTypeArray) {
		this.actionTypeArray = actionTypeArray;
	}

	public Integer getActionId() {
		return actionId;
	}

	public void setActionId(Integer actionId) {
		this.actionId = actionId;
	}

	public String getVideoId() {
		return videoId;
	}

	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}

	public String getVideoUserId() {
		return videoUserId;
	}

	public void setVideoUserId(String videoUserId) {
		this.videoUserId = videoUserId;
	}

	public Integer getCommentId() {
		return commentId;
	}

	public void setCommentId(Integer commentId) {
		this.commentId = commentId;
	}

	public Integer getActionType() {
		return actionType;
	}

	public void setActionType(Integer actionType) {
		this.actionType = actionType;
	}

	public Integer getActionCount() {
		return actionCount;
	}

	public void setActionCount(Integer actionCount) {
		this.actionCount = actionCount;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getActionTime() {
		return actionTime;
	}

	public void setActionTime(Date actionTime) {
		this.actionTime = actionTime;
	}

	public String getVideoIdFuzzy() {
		return videoIdFuzzy;
	}

	public void setVideoIdFuzzy(String videoIdFuzzy) {
		this.videoIdFuzzy = videoIdFuzzy;
	}

	public String getVideoUserIdFuzzy() {
		return videoUserIdFuzzy;
	}

	public void setVideoUserIdFuzzy(String videoUserIdFuzzy) {
		this.videoUserIdFuzzy = videoUserIdFuzzy;
	}

	public String getUserIdFuzzy() {
		return userIdFuzzy;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getActionTimeStart() {
		return actionTimeStart;
	}

	public void setActionTimeStart(String actionTimeStart) {
		this.actionTimeStart = actionTimeStart;
	}

	public String getActionTimeEnd() {
		return actionTimeEnd;
	}

	public void setActionTimeEnd(String actionTimeEnd) {
		this.actionTimeEnd = actionTimeEnd;
	}

}