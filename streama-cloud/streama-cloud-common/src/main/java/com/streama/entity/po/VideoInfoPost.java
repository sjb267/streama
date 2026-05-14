package com.streama.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.streama.entity.enums.DateTimePatternEnum;
import com.streama.entity.enums.VideoStatusEnum;
import com.streama.utils.DateUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @Description:视频信息
 * @author:孙将斌
 * @date:2026/03/06
 */
public class VideoInfoPost extends VideoInfo implements Serializable {
	@Serial
	private static final long serialVersionUID = -5813324566338359371L;
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
	 * 0:转码中 1:转码失败 2:待审核 3:审核成功 4:审核失败
	 */
	private Integer status;

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

	private String statusName;

	/**
	 * AI建议 1:通过 2:驳回 3:人工复核
	 */
	private Integer aiDecision;

	/**
	 * AI风险等级 1:低 2:中 3:高
	 */
	private Integer aiRiskLevel;

	/**
	 * AI摘要
	 */
	private String aiSummary;

	public String getStatusName() {
		VideoStatusEnum videoStatusEnum = VideoStatusEnum.getVideoStatusEnum(status);
		return videoStatusEnum != null ? videoStatusEnum.getDesc() : "";
	}

	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	public Integer getAiDecision() {
		return aiDecision;
	}

	public void setAiDecision(Integer aiDecision) {
		this.aiDecision = aiDecision;
	}

	public Integer getAiRiskLevel() {
		return aiRiskLevel;
	}

	public void setAiRiskLevel(Integer aiRiskLevel) {
		this.aiRiskLevel = aiRiskLevel;
	}

	public String getAiSummary() {
		return aiSummary;
	}

	public void setAiSummary(String aiSummary) {
		this.aiSummary = aiSummary;
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

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
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

	@Override
	public String toString() {
		return "视频ID:" + (videoId == null ? "空" : videoId) + ",视频封面:" + (videoCover == null ? "空" : videoCover) + ",视频名称:" + (videoName == null ? "空" : videoName) + ",用户ID:" + (userId == null ? "空" : userId) + ",创建时间:" + (createTime == null ? "空" : DateUtils.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",最后更新时间:" + (lastUpdateTime == null ? "空" : DateUtils.format(lastUpdateTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",父级分类ID:" + (pCategoryId == null ? "空" : pCategoryId) + ",分类ID:" + (categoryId == null ? "空" : categoryId) + ",0:转码中 1:转码失败 2:待审核 3:审核成功 4:审核失败:" + (status == null ? "空" : status) + ",0:自制作 1:转载:" + (postType == null ? "空" : postType) + ",原资源说明:" + (originInfo == null ? "空" : originInfo) + ",标签:" + (tags == null ? "空" : tags) + ",简介:" + (introduction == null ? "空" : introduction) + ",互动设置:" + (interaction == null ? "空" : interaction) + ",持续时间（秒）:" + (duration == null ? "空" : duration);
	}
}
