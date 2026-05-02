package com.streama.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.streama.entity.enums.DateTimePatternEnum;
import com.streama.utils.DateUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description:视频弹幕
 * @author:孙将斌
 * @date:2026/03/11
 */
public class VideoDanmu implements Serializable {
	/**
	 * 自增ID
	 */
	private Integer danmuId;

	/**
	 * 视频ID
	 */
	private String videoId;

	/**
	 * 唯一ID
	 */
	private String fileId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 发布时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date postTime;

	/**
	 * 内容
	 */
	private String text;

	/**
	 * 展示位置
	 */
	private Integer mode;

	/**
	 * 颜色
	 */
	private String color;

	/**
	 * 展示时间
	 */
	private Integer time;

	private String videoName;

	private String videoCover;

	private String nickName;

	public String getVideoName() {
		return videoName;
	}

	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}

	public String getVideoCover() {
		return videoCover;
	}

	public void setVideoCover(String videoCover) {
		this.videoCover = videoCover;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
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

	@Override
	public String toString() {
		return "自增ID:" + (danmuId == null ? "空" : danmuId) + ",视频ID:" + (videoId == null ? "空" : videoId) + ",唯一ID:" + (fileId == null ? "空" : fileId) + ",用户ID:" + (userId == null ? "空" : userId) + ",发布时间:" + (postTime == null ? "空" : DateUtils.format(postTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",内容:" + (text == null ? "空" : text) + ",展示位置:" + (mode == null ? "空" : mode) + ",颜色:" + (color == null ? "空" : color) + ",展示时间:" + (time == null ? "空" : time);
	}
}