package com.streama.entity.query;


/**
 * @Description:视频文件信息查询对象
 * @author:孙将斌
 * @date:2026/03/06
 */
public class VideoInfoFilePostQuery extends BaseQuery {
	/**
	 * 唯一ID
	 */
	private String fileId;

	private String fileIdFuzzy;

	/**
	 * 上传ID
	 */
	private String uploadId;

	private String uploadIdFuzzy;

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
	 * 文件索引
	 */
	private Integer fileIndex;

	/**
	 * 文件名
	 */
	private String fileName;

	private String fileNameFuzzy;

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
	 * 0:无更新 1:有更新
	 */
	private Integer updateType;

	/**
	 * 0:转码中 1:转码成功 2:转码失败
	 */
	private Integer transferResult;

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

	public String getUploadId() {
		return uploadId;
	}

	public void setUploadId(String uploadId) {
		this.uploadId = uploadId;
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

	public Integer getFileIndex() {
		return fileIndex;
	}

	public void setFileIndex(Integer fileIndex) {
		this.fileIndex = fileIndex;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
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

	public Integer getUpdateType() {
		return updateType;
	}

	public void setUpdateType(Integer updateType) {
		this.updateType = updateType;
	}

	public Integer getTransferResult() {
		return transferResult;
	}

	public void setTransferResult(Integer transferResult) {
		this.transferResult = transferResult;
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

	public String getUploadIdFuzzy() {
		return uploadIdFuzzy;
	}

	public void setUploadIdFuzzy(String uploadIdFuzzy) {
		this.uploadIdFuzzy = uploadIdFuzzy;
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