package com.streama.entity.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class AiAuditItemProgressMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = -7033159144080314361L;

    private String requestId;
    private String fileId;
    private Integer itemStatus;
    private String lastError;
    private Date updatedAt;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(Integer itemStatus) {
        this.itemStatus = itemStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
