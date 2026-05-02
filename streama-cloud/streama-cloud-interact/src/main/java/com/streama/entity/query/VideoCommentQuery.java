package com.streama.entity.query;

import java.util.Date;

/**
 * @Description:评论查询对象
 * @author:孙将斌
 * @date:2026/03/11
 */
public class VideoCommentQuery extends BaseQuery {
	/**
	 * 评论ID
	 */
	private Integer commentId;

	/**
	 * 父级评论ID
	 */
	private Integer pCommentId;

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
	 * 回复内容
	 */
	private String content;

	private String contentFuzzy;

	/**
	 * 图片
	 */
	private String imgPath;

	private String imgPathFuzzy;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 回复人ID
	 */
	private String replyUserId;

	private String replyUserIdFuzzy;

	/**
	 * 未置顶 1:置顶
	 */
	private Integer topType;

	/**
	 * 发布时间
	 */
	private Date postTime;

	private String postTimeStart;

	private String postTimeEnd;

	/**
	 * 喜欢数量
	 */
	private Integer likeCount;

	/**
	 * 讨厌数量
	 */
	private Integer hateCount;

	private Boolean queryVideoInfo;

	public Boolean getQueryVideoInfo() {
		return queryVideoInfo;
	}

	public void setQueryVideoInfo(Boolean queryVideoInfo) {
		this.queryVideoInfo = queryVideoInfo;
	}

	public Boolean getLoadChildren() {
		return loadChildren;
	}

	public void setLoadChildren(Boolean loadChildren) {
		this.loadChildren = loadChildren;
	}

	private Boolean loadChildren;

	public Integer getCommentId() {
		return commentId;
	}

	public void setCommentId(Integer commentId) {
		this.commentId = commentId;
	}

	public Integer getPCommentId() {
		return pCommentId;
	}

	public void setPCommentId(Integer pCommentId) {
		this.pCommentId = pCommentId;
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

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getImgPath() {
		return imgPath;
	}

	public void setImgPath(String imgPath) {
		this.imgPath = imgPath;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getReplyUserId() {
		return replyUserId;
	}

	public void setReplyUserId(String replyUserId) {
		this.replyUserId = replyUserId;
	}

	public Integer getTopType() {
		return topType;
	}

	public void setTopType(Integer topType) {
		this.topType = topType;
	}

	public Date getPostTime() {
		return postTime;
	}

	public void setPostTime(Date postTime) {
		this.postTime = postTime;
	}

	public Integer getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(Integer likeCount) {
		this.likeCount = likeCount;
	}

	public Integer getHateCount() {
		return hateCount;
	}

	public void setHateCount(Integer hateCount) {
		this.hateCount = hateCount;
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

	public String getContentFuzzy() {
		return contentFuzzy;
	}

	public void setContentFuzzy(String contentFuzzy) {
		this.contentFuzzy = contentFuzzy;
	}

	public String getImgPathFuzzy() {
		return imgPathFuzzy;
	}

	public void setImgPathFuzzy(String imgPathFuzzy) {
		this.imgPathFuzzy = imgPathFuzzy;
	}

	public String getUserIdFuzzy() {
		return userIdFuzzy;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getReplyUserIdFuzzy() {
		return replyUserIdFuzzy;
	}

	public void setReplyUserIdFuzzy(String replyUserIdFuzzy) {
		this.replyUserIdFuzzy = replyUserIdFuzzy;
	}

	public String getPostTimeStart() {
		return postTimeStart;
	}

	public void setPostTimeStart(String postTimeStart) {
		this.postTimeStart = postTimeStart;
	}

	public String getPostTimeEnd() {
		return postTimeEnd;
	}

	public void setPostTimeEnd(String postTimeEnd) {
		this.postTimeEnd = postTimeEnd;
	}

}