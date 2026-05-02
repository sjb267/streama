package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;
import com.streama.entity.query.VideoInfoFilePostQuery;
import com.streama.entity.query.VideoInfoPostQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.entity.vo.VideoPostEditInfo;
import com.streama.entity.vo.VideoStatusCountInfoVO;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.enums.VideoStatusEnum;
import com.streama.exception.BusinessException;
import com.streama.services.VideoInfoFilePostService;
import com.streama.services.VideoInfoPostService;
import com.streama.services.VideoInfoService;
import com.streama.utils.JsonUtils;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CenterVideoPostController extends ABaseController {

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private VideoInfoService videoInfoService;

    //上传video信息和一群文件信息（可能包含多个视频）
    @RequestMapping("/postVideo")
    public ResponseVO postVideo(String videoId,
                                @NotEmpty String videoCover,
                                @NotEmpty @Size(max = 100) String videoName,
                                @NotNull Integer pCategoryId,
                                Integer categoryId,
                                @NotNull Integer postType,
                                @NotEmpty @Size(max = 300) String tags,
                                @Size(max = 2000) String introduction,
                                @Size(max = 3) String interaction,
                                @NotEmpty String uploadFileList) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        List<VideoInfoFilePost> filePostList = JsonUtils.convertJsonArray2List(uploadFileList, VideoInfoFilePost.class);

        VideoInfoPost videoInfoPost = new VideoInfoPost();
        videoInfoPost.setVideoId(videoId);
        videoInfoPost.setVideoCover(videoCover);
        videoInfoPost.setVideoName(videoName);
        videoInfoPost.setPCategoryId(pCategoryId);
        videoInfoPost.setCategoryId(categoryId);
        videoInfoPost.setPostType(postType);
        videoInfoPost.setTags(tags);
        videoInfoPost.setIntroduction(introduction);
        videoInfoPost.setInteraction(interaction);
        videoInfoPost.setUserId(tokenUserInfoDto.getUserId());

        videoInfoPostService.saveVideoInfo(videoInfoPost, filePostList);
        return getSuccessResponseVO(null);
    }

    //加载稿件管理
    @RequestMapping("/loadVideoPostList")
    public ResponseVO loadVideoPost(Integer status, Integer pageNo, String videoNameFuzzy) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoPostQuery videoInfoQuery = new VideoInfoPostQuery();
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId());
        videoInfoQuery.setOrderBy("v.create_time desc");
        videoInfoQuery.setPageNo(pageNo);
        if(status != null) {
            if(status == -1) {
                videoInfoQuery.setExcludeStatusList(new Integer[]{VideoStatusEnum.STATUS3.getStatus(), VideoStatusEnum.STATUS4.getStatus()});
            } else {
                videoInfoQuery.setStatus(status);
            }
        }
        videoInfoQuery.setVideoNameFuzzy(videoNameFuzzy);
        videoInfoQuery.setQueryCountInfo(true);
        PaginationResultVO resultVO = videoInfoPostService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    //加载稿件状态数量
    @RequestMapping("/getVideoCountInfo")
    public ResponseVO getVideoCountInfo() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoPostQuery videoInfoQuery = new VideoInfoPostQuery();
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId());
        videoInfoQuery.setStatus(VideoStatusEnum.STATUS3.getStatus());
        Integer auditPassCount = videoInfoPostService.findCountByParam(videoInfoQuery);

        videoInfoQuery.setStatus(VideoStatusEnum.STATUS4.getStatus());
        Integer auditFailCount = videoInfoPostService.findCountByParam(videoInfoQuery);

        videoInfoQuery.setStatus(null);
        videoInfoQuery.setExcludeStatusList(new Integer[]{VideoStatusEnum.STATUS3.getStatus(), VideoStatusEnum.STATUS4.getStatus()});
        Integer inProgress = videoInfoPostService.findCountByParam(videoInfoQuery);

        VideoStatusCountInfoVO videoStatusCountInfoVO = new VideoStatusCountInfoVO();
        videoStatusCountInfoVO.setAuditPassCount(auditPassCount);
        videoStatusCountInfoVO.setAuditFailCount(auditFailCount);
        videoStatusCountInfoVO.setInProgress(inProgress);
        return getSuccessResponseVO(videoStatusCountInfoVO);
    }

    @RequestMapping("/getVideoByVideoId")
    public ResponseVO getVideoByVideoId(@NotEmpty String videoId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoPost videoInfoPost = videoInfoPostService.getVideoInfoPostByVideoId(videoId);
        if(videoInfoPost == null || !videoInfoPost.getUserId().equals(tokenUserInfoDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
        videoInfoFilePostQuery.setVideoId(videoId);
        videoInfoFilePostQuery.setOrderBy("file_index asc");
        List<VideoInfoFilePost> videoInfoFilePostList = videoInfoFilePostService.findListByParam(videoInfoFilePostQuery);
        VideoPostEditInfo vo = new VideoPostEditInfo();
        vo.setVideoInfo(videoInfoPost);
        vo.setVideoInfoFilePosts(videoInfoFilePostList);
        return getSuccessResponseVO(vo);
    }

    @RequestMapping("/saveVideoInteraction")
    public ResponseVO saveVideoInteraction(@NotEmpty String videoId, String interaction) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoInfoService.changeInteraction(videoId, tokenUserInfoDto.getUserId(), interaction);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/deleteVideo")
    public ResponseVO deleteVideo(@NotEmpty String videoId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoInfoService.deleteVideo(videoId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
}