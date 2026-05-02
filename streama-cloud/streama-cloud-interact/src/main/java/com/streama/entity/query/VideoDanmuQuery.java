package com.streama.entity.query;

import java.util.Date;

/**
 * @Description:视频弹幕查询对象
 * @author:孙将斌
 * @date:2026/03/11
 */
public class VideoDanmuQuery extends BaseQuery {
	/**
	 * 自增ID
	 */
	private Integer danmuId;

	/**
	 * 视频ID
	 */
	private String videoId;

	private String videoIdFuzzy;

	/**
	 * 唯一ID
	 */
	private String fileId;

	private String fileIdFuzzy;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 发布时间
	 */
	private Date postTime;

	private String postTimeStart;

	private String postTimeEnd;

	/**
	 * 内容
	 */
	private String text;

	private String textFuzzy;

	/**
	 * 展示位置
	 */
	private Integer mode;

	/**
	 * 颜色
	 */
	private String color;

	private String colorFuzzy;

	/**
	 * 展示时间
	 */
	private Integer time;

	private String videoUserId;

	private Boolean queryVideoInfo;

	public Boolean getQueryVideoInfo() {
		return queryVideoInfo;
	}

	public void setQueryVideoInfo(Boolean queryVideoInfo) {
		this.queryVideoInfo = queryVideoInfo;
	}

	public String getVideoUserId() {
		return videoUserId;
	}

	public void setVideoUserId(String videoUserId) {
		this.videoUserId = videoUserId;
	}

	public Integer getDanmuId() {
		return danmuId;
	}

	public void setDanmuId(Integer danmuId) {
		this.danmuId = danmuId;
	}

	public String getVideoId() {
		return videoId;
	}

	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getPostTime() {
		return postTime;
	}

	public void setPostTime(Date postTime) {
		this.postTime = postTime;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getMode() {
		return mode;
	}

	public void setMode(Integer mode) {
		this.mode = mode;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}

	public String getVideoIdFuzzy() {
		return videoIdFuzzy;
	}

	public void setVideoIdFuzzy(String videoIdFuzzy) {
		this.videoIdFuzzy = videoIdFuzzy;
	}

	public String getFileIdFuzzy() {
		return fileIdFuzzy;
	}

	public void setFileIdFuzzy(String fileIdFuzzy) {
		this.fileIdFuzzy = fileIdFuzzy;
	}

	public String getUserIdFuzzy() {
		return userIdFuzzy;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
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

	public String getTextFuzzy() {
		return textFuzzy;
	}

	public void setTextFuzzy(String textFuzzy) {
		this.textFuzzy = textFuzzy;
	}

	public String getColorFuzzy() {
		return colorFuzzy;
	}

	public void setColorFuzzy(String colorFuzzy) {
		this.colorFuzzy = colorFuzzy;
	}

}