package com.streama.entity.dto;

import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class AdminVideoPostDetailDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3478986906690483618L;

    private VideoInfoPost videoInfo;
    private List<VideoInfoFilePost> videoInfoFilePosts;

    public VideoInfoPost getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfoPost videoInfo) {
        this.videoInfo = videoInfo;
    }

    public List<VideoInfoFilePost> getVideoInfoFilePosts() {
        return videoInfoFilePosts;
    }

    public void setVideoInfoFilePosts(List<VideoInfoFilePost> videoInfoFilePosts) {
        this.videoInfoFilePosts = videoInfoFilePosts;
    }
}
