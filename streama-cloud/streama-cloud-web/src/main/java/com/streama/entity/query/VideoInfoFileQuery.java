package com.streama.entity.query;


/**
 * @Description:视频文件信息查询对象
 * @author:孙将斌
 * @date:2026/03/06
 */
public class VideoInfoFileQuery extends BaseQuery {
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
	 * 视频ID
	 */
	private String videoId;

	private String videoIdFuzzy;

	/**
	 * 文件名
	 */
	private String fileName;

	private String fileNameFuzzy;

	/**
	 * 文件索引
	 */
	private Integer fileIndex;

	/**
	 * 文件大小
	 */
	private Long fileSize;

	/**
	 * 文件路径
	 */
	private String filePath;

	private String filePathFuzzy;

	/**
	 * 持续时间（秒）
	 */
	private Integer duration;

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

	public String getVideoId() {
		return videoId;
	}

	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Integer getFileIndex() {
		return fileIndex;
	}

	public void setFileIndex(Integer fileIndex) {
		this.fileIndex = fileIndex;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
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

	public String getVideoIdFuzzy() {
		return videoIdFuzzy;
	}

	public void setVideoIdFuzzy(String videoIdFuzzy) {
		this.videoIdFuzzy = videoIdFuzzy;
	}

	public String getFileNameFuzzy() {
		return fileNameFuzzy;
	}

	public void setFileNameFuzzy(String fileNameFuzzy) {
		this.fileNameFuzzy = fileNameFuzzy;
	}

	public String getFilePathFuzzy() {
		return filePathFuzzy;
	}

	public void setFilePathFuzzy(String filePathFuzzy) {
		this.filePathFuzzy = filePathFuzzy;
	}

}