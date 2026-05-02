package com.streama.entity.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuditRequestMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = -6814419323522602064L;

    private String requestId;
    private String videoId;
    private Integer auditVersion;
    private Integer sourceType;
    private Date triggerTime;
    private VideoMeta videoMeta;
    private List<Item> items = new ArrayList<>();

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public Integer getAuditVersion() {
        return auditVersion;
    }

    public void setAuditVersion(Integer auditVersion) {
        this.auditVersion = auditVersion;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Date getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime) {
        this.triggerTime = triggerTime;
    }

    public VideoMeta getVideoMeta() {
        return videoMeta;
    }

    public void setVideoMeta(VideoMeta videoMeta) {
        this.videoMeta = videoMeta;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class VideoMeta implements Serializable {
        @Serial
        private static final long serialVersionUID = -5841114391363956198L;
        private String userId;
        private String videoName;
        private String videoCover;
        private String tags;
        private String introduction;
        private Integer pCategoryId;
        private Integer categoryId;
        private Integer postType;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

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
    }

    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 3484552838991944936L;
        private String fileId;
        private Integer fileIndex;
        private String uploadId;
        private String fileName;
        private String filePath;
        private Integer duration;
        private Integer updateType;

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public Integer getFileIndex() {
            return fileIndex;
        }

        public void setFileIndex(Integer fileIndex) {
            this.fileIndex = fileIndex;
        }

        public String getUploadId() {
            return uploadId;
        }

        public void setUploadId(String uploadId) {
            this.uploadId = uploadId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
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

        public Integer getUpdateType() {
            return updateType;
        }

        public void setUpdateType(Integer updateType) {
            this.updateType = updateType;
        }
    }
}

