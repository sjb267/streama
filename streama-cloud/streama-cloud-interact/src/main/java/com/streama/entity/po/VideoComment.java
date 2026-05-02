package com.streama.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.streama.entity.enums.DateTimePatternEnum;
import com.streama.utils.DateUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Description:评论
 * @author:孙将斌
 * @date:2026/03/11
 */
public class VideoComment implements Serializable {
	@Serial
	private static final long serialVersionUID = -1035437353519859234L;
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

	/**
	 * 视频用户ID
	 */
	private String videoUserId;

	/**
	 * 回复内容
	 */
	private String content;

	/**
	 * 图片
	 */
	private String imgPath;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 回复人ID
	 */
	private String replyUserId;

	/**
	 * 未置顶 1:置顶
	 */
	private Integer topType;

	/**
	 * 发布时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date postTime;

	/**
	 * 喜欢数量
	 */
	private Integer likeCount;

	/**
	 * 讨厌数量
	 */
	private Integer hateCount;

	private String avatar;

	private String nickName;

	private String replyAvatar;

	private String replyNickName;

	private String videoCover;

	private String videoName;

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

	private List<VideoComment> children;

	public List<VideoComment> getChildren() {
		return children;
	}

	public void setChildren(List<VideoComment> children) {
		this.children = children;
	}

	public String getReplyNickName() {
		return replyNickName;
	}

	public void setReplyNickName(String replyNickName) {
		this.replyNickName = replyNickName;
	}

	public String getReplyAvatar() {
		return replyAvatar;
	}

	public void setReplyAvatar(String replyAvatar) {
		this.replyAvatar = replyAvatar;
	}

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

	@Override
	public String toString() {
		return "评论ID:" + (commentId == null ? "空" : commentId) + ",父级评论ID:" + (pCommentId == null ? "空" : pCommentId) + ",视频ID:" + (videoId == null ? "空" : videoId) + ",视频用户ID:" + (videoUserId == null ? "空" : videoUserId) + ",回复内容:" + (content == null ? "空" : content) + ",图片:" + (imgPath == null ? "空" : imgPath) + ",用户ID:" + (userId == null ? "空" : userId) + ",回复人ID:" + (replyUserId == null ? "空" : replyUserId) + ",未置顶 1:置顶:" + (topType == null ? "空" : topType) + ",发布时间:" + (postTime == null ? "空" : DateUtils.format(postTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ",喜欢数量:" + (likeCount == null ? "空" : likeCount) + ",讨厌数量:" + (hateCount == null ? "空" : hateCount);
	}
}