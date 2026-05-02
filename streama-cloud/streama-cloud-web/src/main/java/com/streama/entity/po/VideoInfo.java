package com.streama.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.streama.entity.enums.DateTimePatternEnum;
import com.streama.utils.DateUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description:视频信息
 * @author:孙将斌
 * @date:2026/03/06
 */
public class VideoInfo implements Serializable {
	/**
	 * 视频ID
	 */
	private String videoId;

	/**
	 * 视频封面
	 */
	private String videoCover;

	/**
	 * 视频名称
	 */
	private String videoName;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 最后更新时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date lastUpdateTime;

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

	/**
	 * 标签
	 */
	private String tags;

	/**
	 * 简介
	 */
	private String introduction;

	/**
	 * 互动设置
	 */
	private String interaction;

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

	private String nickName;

	private String avatar;

	/**
	 * 最后播放时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date lastPlayTime;

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
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

	@Override
	public String toString() {
		return "视频ID:" + (videoId == null ? "空" : videoId) + ",视频封面:" + (videoCover == null ? "空" : videoCover) + ",视频名称:" + (videoName == null ? "空" : videoName) + ",用户ID:" + (userId == null ? "空" : userId) + ",创建时间:" + (createTime == null ? "空" : DateUtils.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",最后更新时间:" + (lastUpdateTime == null ? "空" : DateUtils.format(lastUpdateTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",父级分类ID:" + (pCategoryId == null ? "空" : pCategoryId) + ",分类ID:" + (categoryId == null ? "空" : categoryId) + ",0:自制作 1:转载:" + (postType == null ? "空" : postType) + ",原资源说明:" + (originInfo == null ? "空" : originInfo) + ",标签:" + (tags == null ? "空" : tags) + ",简介:" + (introduction == null ? "空" : introduction) + ",互动设置:" + (interaction == null ? "空" : interaction) + ",持续时间（秒）:" + (duration == null ? "空" : duration) + ",播放数量:" + (playCount == null ? "空" : playCount) + ",点赞数量:" + (likeCount == null ? "空" : likeCount) + ",弹幕数量:" + (danmuCount == null ? "空" : danmuCount) + ",评论数量:" + (commentCount == null ? "空" : commentCount) + ",投币数量:" + (coinCount == null ? "空" : coinCount) + ",收藏数量:" + (collectCount == null ? "空" : collectCount) + ",是否推荐 0:未推荐 1:已推荐:" + (recommendType == null ? "空" : recommendType) + ",最后播放时间:" + (lastPlayTime == null ? "空" : DateUtils.format(lastPlayTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}