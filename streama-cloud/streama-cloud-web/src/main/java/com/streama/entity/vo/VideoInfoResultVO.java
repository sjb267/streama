package com.streama.entity.vo;

import com.streama.entity.po.VideoInfo;

import java.util.List;

public class VideoInfoResultVO {

    private VideoInfo videoInfo;
    private List videoInfoFiles;

    public VideoInfoResultVO() {

    }

    public VideoInfoResultVO(VideoInfo videoInfo, List videoInfoFiles) {
        this.videoInfo = videoInfo;
        this.videoInfoFiles = videoInfoFiles;
    }

    public List getVideoInfoFiles() {
        return videoInfoFiles;
    }

    public void setVideoInfoFiles(List videoInfoFiles) {
        this.videoInfoFiles = videoInfoFiles;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }
}
