package com.streama.entity.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class VideoAuditMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 5167750696008914944L;
    
    private String videoId;
    private String userId;
    private String videoName;
    private String videoCover;
    private String tags;
    private String introduction;
    private Integer pCategoryId;
    private Integer categoryId;
    private Integer postType;
    private Date createTime;
    private List<FileInfo> fileList;
    
    @Data
    public static class FileInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = -6584480201309306638L;
        private String fileId;
        private String filePath;
        private String fileName;
        private Integer fileIndex;
    }
}