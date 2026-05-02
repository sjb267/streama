package com.streama.controller;

import com.streama.api.consumer.WebClient;
import com.streama.entity.dto.AdminVideoPostDetailDto;
import com.streama.entity.po.AiAuditTask;
import com.streama.entity.po.AiAuditTaskItem;
import com.streama.entity.query.VideoInfoPostQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/videoInfo")
@Validated
public class VideoInfoController extends ABaseController {
    @Resource
    private WebClient webClient;

    @RequestMapping("/loadVideoList")
    public ResponseVO loadVideoPost(VideoInfoPostQuery videoInfoPostQuery) {
        return getSuccessResponseVO(webClient.loadVideoList(videoInfoPostQuery));
    }

    @RequestMapping("/auditVideo")
    public ResponseVO auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason) throws BusinessException {
        webClient.auditVideo(videoId, status, reason);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/getAiAuditItems")
    public ResponseVO getAiAuditItems(@NotEmpty String videoId) {
        List<AiAuditTaskItem> itemList = webClient.getAiAuditItems(videoId);
        return getSuccessResponseVO(itemList);
    }

    @RequestMapping("/getAiAuditSummary")
    public ResponseVO getAiAuditSummary(@NotEmpty String videoId) {
        AiAuditTask task = webClient.getAiAuditSummary(videoId);
        return getSuccessResponseVO(task);
    }

    @RequestMapping("/getVideoPostDetail")
    public ResponseVO getVideoPostDetail(@NotEmpty String videoId) {
        AdminVideoPostDetailDto detail = webClient.getVideoPostDetail(videoId);
        return getSuccessResponseVO(detail);
    }
}
