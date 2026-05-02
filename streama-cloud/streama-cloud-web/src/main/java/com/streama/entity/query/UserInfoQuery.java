package com.streama.entity.query;

import java.util.Date;
import java.util.List;

/**
 * @Description:用户信息
查询对象
 * @author:孙将斌
 * @date:2026/03/09
 */
public class UserInfoQuery extends BaseQuery {
	/**
	 * 用户id
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 昵称
	 */
	private String nickName;

	private String nickNameFuzzy;

	/**
	 * 邮箱
	 */
	private String email;

	private String emailFuzzy;

	/**
	 * 密码
	 */
	private String password;

	private String passwordFuzzy;

	/**
	 * 0:女 1:男 2:未知
	 */
	private Integer sex;

	/**
	 * 出生日期
	 */
	private Date birthday;

	private String birthdayStart;

	private String birthdayEnd;

	/**
	 * 学校
	 */
	private String school;

	private String schoolFuzzy;

	/**
	 * 个人简介
	 */
	private String personIntroduction;

	private String personIntroductionFuzzy;

	/**
	 * 加入时间
	 */
	private Date joinTime;

	private String joinTimeStart;

	private String joinTimeEnd;

	/**
	 * 最后登录时间
	 */
	private Date lastLoginTime;

	private String lastLoginTimeStart;

	private String lastLoginTimeEnd;

	/**
	 * 最后登录IP
	 */
	private String lastLoginIp;

	private String lastLoginIpFuzzy;

	/**
	 * 0:禁用 1:正常
	 */
	private Integer status;

	/**
	 * 通知公告
	 */
	private String noticeInfo;

	private String noticeInfoFuzzy;

	/**
	 * 硬币总数量
	 */
	private Integer totalCoinCount;

	/**
	 * 当前硬币数
	 */
	private Integer currentCoinCount;

	/**
	 * 头像
	 */
	private String avatar;

	private String avatarFuzzy;

	private List<String> userIdList;

	public List<String> getUserIdList() {
		return userIdList;
	}

	public void setUserIdList(List<String> userIdList) {
		this.userIdList = userIdList;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getSex() {
		return sex;
	}

	public void setSex(Integer sex) {
		this.sex = sex;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getSchool() {
		return school;
	}

	public void setSchool(String school) {
		this.school = school;
	}

	public String getPersonIntroduction() {
		return personIntroduction;
	}

	public void setPersonIntroduction(String personIntroduction) {
		this.personIntroduction = personIntroduction;
	}

	public Date getJoinTime() {
		return joinTime;
	}

	public void setJoinTime(Date joinTime) {
		this.joinTime = joinTime;
	}

	public Date getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public String getLastLoginIp() {
		return lastLoginIp;
	}

	public void setLastLoginIp(String lastLoginIp) {
		this.lastLoginIp = lastLoginIp;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getNoticeInfo() {
		return noticeInfo;
	}

	public void setNoticeInfo(String noticeInfo) {
		this.noticeInfo = noticeInfo;
	}

	public Integer getTotalCoinCount() {
		return totalCoinCount;
	}

	public void setTotalCoinCount(Integer totalCoinCount) {
		this.totalCoinCount = totalCoinCount;
	}

	public Integer getCurrentCoinCount() {
		return currentCoinCount;
	}

	public void setCurrentCoinCount(Integer currentCoinCount) {
		this.currentCoinCount = currentCoinCount;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getUserIdFuzzy() {
		return userIdFuzzy;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getNickNameFuzzy() {
		return nickNameFuzzy;
	}

	public void setNickNameFuzzy(String nickNameFuzzy) {
		this.nickNameFuzzy = nickNameFuzzy;
	}

	public String getEmailFuzzy() {
		return emailFuzzy;
	}

	public void setEmailFuzzy(String emailFuzzy) {
		this.emailFuzzy = emailFuzzy;
	}

	public String getPasswordFuzzy() {
		return passwordFuzzy;
	}

	public void setPasswordFuzzy(String passwordFuzzy) {
		this.passwordFuzzy = passwordFuzzy;
	}

	public String getBirthdayStart() {
		return birthdayStart;
	}

	public void setBirthdayStart(String birthdayStart) {
		this.birthdayStart = birthdayStart;
	}

	public String getBirthdayEnd() {
		return birthdayEnd;
	}

	public void setBirthdayEnd(String birthdayEnd) {
		this.birthdayEnd = birthdayEnd;
	}

	public String getSchoolFuzzy() {
		return schoolFuzzy;
	}

	public void setSchoolFuzzy(String schoolFuzzy) {
		this.schoolFuzzy = schoolFuzzy;
	}

	public String getPersonIntroductionFuzzy() {
		return personIntroductionFuzzy;
	}

	public void setPersonIntroductionFuzzy(String personIntroductionFuzzy) {
		this.personIntroductionFuzzy = personIntroductionFuzzy;
	}

	public String getJoinTimeStart() {
		return joinTimeStart;
	}

	public void setJoinTimeStart(String joinTimeStart) {
		this.joinTimeStart = joinTimeStart;
	}

	public String getJoinTimeEnd() {
		return joinTimeEnd;
	}

	public void setJoinTimeEnd(String joinTimeEnd) {
		this.joinTimeEnd = joinTimeEnd;
	}

	public String getLastLoginTimeStart() {
		return lastLoginTimeStart;
	}

	public void setLastLoginTimeStart(String lastLoginTimeStart) {
		this.lastLoginTimeStart = lastLoginTimeStart;
	}

	public String getLastLoginTimeEnd() {
		return lastLoginTimeEnd;
	}

	public void setLastLoginTimeEnd(String lastLoginTimeEnd) {
		this.lastLoginTimeEnd = lastLoginTimeEnd;
	}

	public String getLastLoginIpFuzzy() {
		return lastLoginIpFuzzy;
	}

	public void setLastLoginIpFuzzy(String lastLoginIpFuzzy) {
		this.lastLoginIpFuzzy = lastLoginIpFuzzy;
	}

	public String getNoticeInfoFuzzy() {
		return noticeInfoFuzzy;
	}

	public void setNoticeInfoFuzzy(String noticeInfoFuzzy) {
		this.noticeInfoFuzzy = noticeInfoFuzzy;
	}

	public String getAvatarFuzzy() {
		return avatarFuzzy;
	}

	public void setAvatarFuzzy(String avatarFuzzy) {
		this.avatarFuzzy = avatarFuzzy;
	}

}