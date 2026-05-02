package com.streama.entity.vo;

import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;

import java.util.List;

public class VideoPostEditInfo {
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
