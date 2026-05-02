package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.api.consumer.VideoClient;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.query.VideoCommentQuery;
import com.streama.entity.query.VideoDanmuQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.services.impl.VideoCommentServiceImpl;
import com.streama.services.impl.VideoDanmuServiceImpl;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ucenter")
@Validated
@Slf4j
@GlobalInterceptor(checkLogin = true)
public class CenterController extends ABaseController {

    @Resource
    private VideoClient videoClient;

    @Resource
    private VideoCommentServiceImpl videoCommentService;

    @Resource
    private VideoDanmuServiceImpl videoDanmuService;

    @RequestMapping("/loadComment")
    public ResponseVO loadComment(Integer pageNo, String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
        videoCommentQuery.setVideoId(videoId);
        videoCommentQuery.setVideoUserId(tokenUserInfoDto.getUserId());
        videoCommentQuery.setOrderBy("comment_id desc");
        videoCommentQuery.setPageNo(pageNo);
        videoCommentQuery.setQueryVideoInfo(true);
        PaginationResultVO resultVO = videoCommentService.findListByPage(videoCommentQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/delComment")
    public ResponseVO delComment(@NotNull Integer commentId) throws BusinessException {
        videoCommentService.deleteComment(commentId, getTokenUserInfoDto().getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadDanmu")
    public ResponseVO loadDanmu(Integer pageNo, String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
        videoDanmuQuery.setVideoId(videoId);
        videoDanmuQuery.setVideoUserId(tokenUserInfoDto.getUserId());
        videoDanmuQuery.setOrderBy("danmu_id desc");
        videoDanmuQuery.setPageNo(pageNo);
        videoDanmuQuery.setQueryVideoInfo(true);
        PaginationResultVO resultVO = videoDanmuService.findListByPage(videoDanmuQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/delDanmu")
    public ResponseVO delDanmu(@NotNull Integer danmuId) throws BusinessException {
        videoDanmuService.deleteDanmu(getTokenUserInfoDto().getUserId(), danmuId);
        return getSuccessResponseVO(null);
    }
}