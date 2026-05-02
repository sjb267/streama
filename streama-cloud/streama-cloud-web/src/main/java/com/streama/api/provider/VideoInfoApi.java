package com.streama.api.provider;

import com.streama.component.EsSearchComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.AdminVideoPostDetailDto;
import com.streama.entity.dto.AiAuditItemProgressMessage;
import com.streama.entity.enums.SearchOrderTypeEnum;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;
import com.streama.entity.po.AiAuditTask;
import com.streama.entity.po.AiAuditTaskItem;
import com.streama.entity.query.VideoInfoFilePostQuery;
import com.streama.entity.query.VideoInfoPostQuery;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.services.VideoInfoFileService;
import com.streama.services.VideoInfoFilePostService;
import com.streama.services.VideoInfoPostService;
import com.streama.services.VideoInfoService;
import com.streama.services.AiAuditTaskService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/video")
@Validated
public class VideoInfoApi {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    private AiAuditTaskService aiAuditTaskService;

    @RequestMapping("/getVideoInfoFileByFileId")
    public VideoInfoFile getVideoInfo(@NotEmpty String fileId) {
        return videoInfoFileService.getVideoInfoFileByFileId(fileId);
    }

    @RequestMapping("/getVideoInfoFilePostByFileId")
    public VideoInfoFilePost getVideoInfoFilePostByFileId(@NotEmpty String fileId) {
        return videoInfoFilePostService.getVideoInfoFilePostByFileId(fileId);
    }

    @RequestMapping("/getVideoInfoByVideoId")
    public VideoInfo getVideoInfoByVideoId(@NotEmpty String videoId) {
        return videoInfoService.getVideoInfoByVideoId(videoId);
    }

    @RequestMapping("/updateCountInfo")
    public void updateCountInfo(@NotEmpty String videoId, @NotEmpty String field, @NotNull Integer changeCount) {
        videoInfoService.updateCountInfo(videoId, field, changeCount);
    }

    @RequestMapping("/getVideoInfoPostByVideoId")
    public VideoInfoPost getVideoInfoPostByVideoId(@NotEmpty String videoId) {
        return videoInfoPostService.getVideoInfoPostByVideoId(videoId);
    }

    @RequestMapping("/updateDocCount")
    public void updateDocCount(@NotEmpty String videoId, SearchOrderTypeEnum searchOrderTypeEnum, Integer changeCount) throws BusinessException {
        esSearchComponent.updateDocCount(videoId, searchOrderTypeEnum.getField(), changeCount);
    }

    @RequestMapping("/admin/loadVideoList")
    public PaginationResultVO loadVideoPost(@RequestBody VideoInfoPostQuery videoInfoPostQuery) {
        videoInfoPostQuery.setOrderBy("v.last_update_time desc");
        videoInfoPostQuery.setQueryCountInfo(true);
        //查看所有的用户
        videoInfoPostQuery.setQueryUserInfo(true);
        videoInfoPostQuery.setQueryAiAuditInfo(true);
        return videoInfoPostService.findListByPage(videoInfoPostQuery);
    }

    @RequestMapping("/admin/auditVideo")
    public void auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason) throws BusinessException {
        videoInfoPostService.auditVideo(videoId, status, reason);
    }

    @RequestMapping("/getVideoCount")
    public Integer getVideoCount(@RequestBody VideoInfoQuery videoInfoQuery) {
        return videoInfoService.findCountByParam(videoInfoQuery);
    }

    @RequestMapping("/admin/getAiAuditItems")
    public List<AiAuditTaskItem> getAiAuditItems(@NotEmpty String videoId) {
        return aiAuditTaskService.getLatestAuditItems(videoId);
    }

    @RequestMapping("/admin/getAiAuditSummary")
    public AiAuditTask getAiAuditSummary(@NotEmpty String videoId) {
        return aiAuditTaskService.getLatestAuditTask(videoId);
    }

    @RequestMapping("/admin/getVideoPostDetail")
    public AdminVideoPostDetailDto getVideoPostDetail(@NotEmpty String videoId) {
        VideoInfoPostQuery postQuery = new VideoInfoPostQuery();
        postQuery.setVideoId(videoId);
        postQuery.setQueryCountInfo(true);
        postQuery.setQueryUserInfo(true);
        postQuery.setQueryAiAuditInfo(true);

        List<VideoInfoPost> postList = videoInfoPostService.findListByParam(postQuery);
        VideoInfoPost videoInfo = postList.isEmpty() ? null : postList.get(0);

        VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
        fileQuery.setVideoId(videoId);
        fileQuery.setOrderBy("file_index asc");

        AdminVideoPostDetailDto result = new AdminVideoPostDetailDto();
        result.setVideoInfo(videoInfo);
        result.setVideoInfoFilePosts(videoInfoFilePostService.findListByParam(fileQuery));
        return result;
    }

    @RequestMapping("/aiAuditItemProgress")
    public void aiAuditItemProgress(@RequestBody AiAuditItemProgressMessage progressMessage) {
        aiAuditTaskService.updateItemProgress(progressMessage);
    }

    @RequestMapping("/transferVideoFile4Db")
    public void transferVideFile4Db(@RequestParam String videoId,
                                    @RequestParam String uploadId,
                                    @RequestParam String userId,
                                    @RequestBody VideoInfoFilePost uploadFilePost) {
         videoInfoPostService.transferVideFile4Db(videoId, uploadId, userId, uploadFilePost);
    }
}
