package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.entity.vo.ResponseVO;
import com.streama.services.VideoInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ucenter")
@Validated
@Slf4j
@GlobalInterceptor(checkLogin = true)
public class CeneterInteractionController extends ABaseController {

    @Resource
    private VideoInfoService videoInfoService;

    //上传video信息和一群文件信息（可能包含多个视频）
    @RequestMapping("/loadAllVideo")
    public ResponseVO loadAllVideo() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId());
        videoInfoQuery.setOrderBy("create_time desc");
        List<VideoInfo> videoInfoList = videoInfoService.findListByParam(videoInfoQuery);
        return getSuccessResponseVO(videoInfoList);
    }
}