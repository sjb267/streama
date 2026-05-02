package com.streama.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.streama.entity.enums.DateTimePatternEnum;
import com.streama.utils.DateUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description:
 * @author:孙将斌
 * @date:2026/03/13
 */
public class UserFocus implements Serializable {
	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 用户ID
	 */
	private String focusUserId;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date focusTime;

	private String otherNickName;

	private String otherUserId;

	private String otherPersonIntroduction;

	private String otherAvatar;

	private Integer focusType;

	public String getOtherNickName() {
		return otherNickName;
	}

	public void setOtherNickName(String otherNickName) {
		this.otherNickName = otherNickName;
	}

	public String getOtherUserId() {
		return otherUserId;
	}

	public void setOtherUserId(String otherUserId) {
		this.otherUserId = otherUserId;
	}

	public String getOtherPersonIntroduction() {
		return otherPersonIntroduction;
	}

	public void setOtherPersonIntroduction(String otherPersonIntroduction) {
		this.otherPersonIntroduction = otherPersonIntroduction;
	}

	public String getOtherAvatar() {
		return otherAvatar;
	}

	public void setOtherAvatar(String otherAvatar) {
		this.otherAvatar = otherAvatar;
	}

	public Integer getFocusType() {
		return focusType;
	}

	public void setFocusType(Integer focusType) {
		this.focusType = focusType;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getFocusUserId() {
		return focusUserId;
	}

	public void setFocusUserId(String focusUserId) {
		this.focusUserId = focusUserId;
	}

	public Date getFocusTime() {
		return focusTime;
	}

	public void setFocusTime(Date focusTime) {
		this.focusTime = focusTime;
	}

	@Override
	public String toString() {
		return "用户ID:" + (userId == null ? "空" : userId) + ",用户ID:" + (focusUserId == null ? "空" : focusUserId) + ",:" + (focusTime == null ? "空" : DateUtils.format(focusTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}