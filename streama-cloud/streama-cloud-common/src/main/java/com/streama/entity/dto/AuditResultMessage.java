package com.streama.entity.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuditResultMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 6128246611250033195L;

    private String requestId;
    private String videoId;
    private Integer auditVersion;
    private String modelName;
    private String modelVersion;
    private Date completedAt;
    private Integer videoDecision;
    private Integer videoRiskLevel;
    private String videoSummary;
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getVideoDecision() {
        return videoDecision;
    }

    public void setVideoDecision(Integer videoDecision) {
        this.videoDecision = videoDecision;
    }

    public Integer getVideoRiskLevel() {
        return videoRiskLevel;
    }

    public void setVideoRiskLevel(Integer videoRiskLevel) {
        this.videoRiskLevel = videoRiskLevel;
    }

    public String getVideoSummary() {
        return videoSummary;
    }

    public void setVideoSummary(String videoSummary) {
        this.videoSummary = videoSummary;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 6988999114483425742L;
        private String fileId;
        private Integer itemStatus;
        private Integer itemDecision;
        private BigDecimal riskScore;
        private Object riskTags;
        private Object hitSegments;
        private String itemReason;

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public Integer getItemDecision() {
            return itemDecision;
        }

        public void setItemDecision(Integer itemDecision) {
            this.itemDecision = itemDecision;
        }

        public Integer getItemStatus() {
            return itemStatus;
        }

        public void setItemStatus(Integer itemStatus) {
            this.itemStatus = itemStatus;
        }

        public BigDecimal getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(BigDecimal riskScore) {
            this.riskScore = riskScore;
        }

        public Object getRiskTags() {
            return riskTags;
        }

        public void setRiskTags(Object riskTags) {
            this.riskTags = riskTags;
        }

        public Object getHitSegments() {
            return hitSegments;
        }

        public void setHitSegments(Object hitSegments) {
            this.hitSegments = hitSegments;
        }

        public String getItemReason() {
            return itemReason;
        }

        public void setItemReason(String itemReason) {
            this.itemReason = itemReason;
        }
    }
}
