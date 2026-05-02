package com.streama.api.consumer;

import com.streama.entity.constants.Constants;
import com.streama.entity.dto.AdminVideoPostDetailDto;
import com.streama.entity.po.AiAuditTask;
import com.streama.entity.query.VideoInfoPostQuery;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.entity.po.AiAuditTaskItem;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = Constants.SERVER_NAME_WEB)
public interface WebClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/loadVideoList")
    PaginationResultVO loadVideoList(@RequestBody VideoInfoPostQuery videoInfoPostQuery);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/auditVideo")
    void auditVideo(@RequestParam String videoId, @RequestParam Integer status, @RequestParam String reason);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoCount")
    Integer getVideoCount(@RequestBody VideoInfoQuery videoInfoQuery);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/getAiAuditItems")
    List<AiAuditTaskItem> getAiAuditItems(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/getAiAuditSummary")
    AiAuditTask getAiAuditSummary(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/getVideoPostDetail")
    AdminVideoPostDetailDto getVideoPostDetail(@RequestParam String videoId);

}
