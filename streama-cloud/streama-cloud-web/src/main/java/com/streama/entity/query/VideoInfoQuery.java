package com.streama.entity.query;

import java.util.Date;

/**
 * @Description:视频信息查询对象
 * @author:孙将斌
 * @date:2026/03/06
 */
public class VideoInfoQuery extends BaseQuery {
	/**
	 * 视频ID
	 */
	private String videoId;

	private String videoIdFuzzy;

	/**
	 * 视频封面
	 */
	private String videoCover;

	private String videoCoverFuzzy;

	/**
	 * 视频名称
	 */
	private String videoName;

	private String videoNameFuzzy;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 创建时间
	 */
	private Date createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 最后更新时间
	 */
	private Date lastUpdateTime;

	private String lastUpdateTimeStart;

	private String lastUpdateTimeEnd;

	/**
	 * 父级分类ID
	 */
	private Integer pCategoryId;

	/**
	 * 分类ID
	 */
	private Integer categoryId;

	/**
	 * 0:自制作 1:转载
	 */
	private Integer postType;

	/**
	 * 原资源说明
	 */
	private String originInfo;

	private String originInfoFuzzy;

	/**
	 * 标签
	 */
	private String tags;

	private String tagsFuzzy;

	/**
	 * 简介
	 */
	private String introduction;

	private String introductionFuzzy;

	/**
	 * 互动设置
	 */
	private String interaction;

	private String interactionFuzzy;

	/**
	 * 持续时间（秒）
	 */
	private Integer duration;

	/**
	 * 播放数量
	 */
	private Integer playCount;

	/**
	 * 点赞数量
	 */
	private Integer likeCount;

	/**
	 * 弹幕数量
	 */
	private Integer danmuCount;

	/**
	 * 评论数量
	 */
	private Integer commentCount;

	/**
	 * 投币数量
	 */
	private Integer coinCount;

	/**
	 * 收藏数量
	 */
	private Integer collectCount;

	/**
	 * 是否推荐 0:未推荐 1:已推荐
	 */
	private Integer recommendType;

	/**
	 * 最后播放时间
	 */
	private Date lastPlayTime;

	private String lastPlayTimeStart;

	private String lastPlayTimeEnd;

	//是否查userinfo表
	private Boolean queryUserInfo;

	private Integer categoryIdOrPCategoryId;

	public Integer getCategoryIdOrPCategoryId() {
		return categoryIdOrPCategoryId;
	}

	public void setCategoryIdOrPCategoryId(Integer categoryIdOrPCategoryId) {
		this.categoryIdOrPCategoryId = categoryIdOrPCategoryId;
	}

	public Boolean getQueryUserInfo() {
		return queryUserInfo;
	}

	public void setQueryUserInfo(Boolean queryUserInfo) {
		this.queryUserInfo = queryUserInfo;
	}

	public String getVideoId() {
		return videoId;
	}

	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}

	public String getVideoCover() {
		return videoCover;
	}

	public void setVideoCover(String videoCover) {
		this.videoCover = videoCover;
	}

	public String getVideoName() {
		return videoName;
	}

	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public Integer getPCategoryId() {
		return pCategoryId;
	}

	public void setPCategoryId(Integer pCategoryId) {
		this.pCategoryId = pCategoryId;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	public Integer getPostType() {
		return postType;
	}

	public void setPostType(Integer postType) {
		this.postType = postType;
	}

	public String getOriginInfo() {
		return originInfo;
	}

	public void setOriginInfo(String originInfo) {
		this.originInfo = originInfo;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getIntroduction() {
		return introduction;
	}

	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}

	public String getInteraction() {
		return interaction;
	}

	public void setInteraction(String interaction) {
		this.interaction = interaction;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public Integer getPlayCount() {
		return playCount;
	}

	public void setPlayCount(Integer playCount) {
		this.playCount = playCount;
	}

	public Integer getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(Integer likeCount) {
		this.likeCount = likeCount;
	}

	public Integer getDanmuCount() {
		return danmuCount;
	}

	public void setDanmuCount(Integer danmuCount) {
		this.danmuCount = danmuCount;
	}

	public Integer getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(Integer commentCount) {
		this.commentCount = commentCount;
	}

	public Integer getCoinCount() {
		return coinCount;
	}

	public void setCoinCount(Integer coinCount) {
		this.coinCount = coinCount;
	}

	public Integer getCollectCount() {
		return collectCount;
	}

	public void setCollectCount(Integer collectCount) {
		this.collectCount = collectCount;
	}

	public Integer getRecommendType() {
		return recommendType;
	}

	public void setRecommendType(Integer recommendType) {
		this.recommendType = recommendType;
	}

	public Date getLastPlayTime() {
		return lastPlayTime;
	}

	public void setLastPlayTime(Date lastPlayTime) {
		this.lastPlayTime = lastPlayTime;
	}

	public String getVideoIdFuzzy() {
		return videoIdFuzzy;
	}

	public void setVideoIdFuzzy(String videoIdFuzzy) {
		this.videoIdFuzzy = videoIdFuzzy;
	}

	public String getVideoCoverFuzzy() {
		return videoCoverFuzzy;
	}

	public void setVideoCoverFuzzy(String videoCoverFuzzy) {
		this.videoCoverFuzzy = videoCoverFuzzy;
	}

	public String getVideoNameFuzzy() {
		return videoNameFuzzy;
	}

	public void setVideoNameFuzzy(String videoNameFuzzy) {
		this.videoNameFuzzy = videoNameFuzzy;
	}

	public String getUserIdFuzzy() {
		return userIdFuzzy;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getCreateTimeStart() {
		return createTimeStart;
	}

	public void setCreateTimeStart(String createTimeStart) {
		this.createTimeStart = createTimeStart;
	}

	public String getCreateTimeEnd() {
		return createTimeEnd;
	}

	public void setCreateTimeEnd(String createTimeEnd) {
		this.createTimeEnd = createTimeEnd;
	}

	public String getLastUpdateTimeStart() {
		return lastUpdateTimeStart;
	}

	public void setLastUpdateTimeStart(String lastUpdateTimeStart) {
		this.lastUpdateTimeStart = lastUpdateTimeStart;
	}

	public String getLastUpdateTimeEnd() {
		return lastUpdateTimeEnd;
	}

	public void setLastUpdateTimeEnd(String lastUpdateTimeEnd) {
		this.lastUpdateTimeEnd = lastUpdateTimeEnd;
	}

	public String getOriginInfoFuzzy() {
		return originInfoFuzzy;
	}

	public void setOriginInfoFuzzy(String originInfoFuzzy) {
		this.originInfoFuzzy = originInfoFuzzy;
	}

	public String getTagsFuzzy() {
		return tagsFuzzy;
	}

	public void setTagsFuzzy(String tagsFuzzy) {
		this.tagsFuzzy = tagsFuzzy;
	}

	public String getIntroductionFuzzy() {
		return introductionFuzzy;
	}

	public void setIntroductionFuzzy(String introductionFuzzy) {
		this.introductionFuzzy = introductionFuzzy;
	}

	public String getInteractionFuzzy() {
		return interactionFuzzy;
	}

	public void setInteractionFuzzy(String interactionFuzzy) {
		this.interactionFuzzy = interactionFuzzy;
	}

	public String getLastPlayTimeStart() {
		return lastPlayTimeStart;
	}

	public void setLastPlayTimeStart(String lastPlayTimeStart) {
		this.lastPlayTimeStart = lastPlayTimeStart;
	}

	public String getLastPlayTimeEnd() {
		return lastPlayTimeEnd;
	}

	public void setLastPlayTimeEnd(String lastPlayTimeEnd) {
		this.lastPlayTimeEnd = lastPlayTimeEnd;
	}

}