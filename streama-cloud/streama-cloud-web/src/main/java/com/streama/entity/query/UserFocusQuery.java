package com.streama.entity.query;

import java.util.Date;

/**
 * @Description:查询对象
 * @author:孙将斌
 * @date:2026/03/13
 */
public class UserFocusQuery extends BaseQuery {
	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 用户ID
	 */
	private String focusUserId;

	private String focusUserIdFuzzy;

	/**
	 * 
	 */
	private Date focusTime;

	private String focusTimeStart;

	private String focusTimeEnd;

	private Integer queryType;

	public Integer getQueryType() {
		return queryType;
	}

	public void setQueryType(Integer queryType) {
		this.queryType = queryType;
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

	public String getUserIdFuzzy() {
		return userIdFuzzy;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getFocusUserIdFuzzy() {
		return focusUserIdFuzzy;
	}

	public void setFocusUserIdFuzzy(String focusUserIdFuzzy) {
		this.focusUserIdFuzzy = focusUserIdFuzzy;
	}

	public String getFocusTimeStart() {
		return focusTimeStart;
	}

	public void setFocusTimeStart(String focusTimeStart) {
		this.focusTimeStart = focusTimeStart;
	}

	public String getFocusTimeEnd() {
		return focusTimeEnd;
	}

	public void setFocusTimeEnd(String focusTimeEnd) {
		this.focusTimeEnd = focusTimeEnd;
	}

}