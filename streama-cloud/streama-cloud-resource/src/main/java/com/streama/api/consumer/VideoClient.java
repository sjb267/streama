package com.streama.api.consumer;

import com.streama.entity.constants.Constants;
import com.streama.entity.po.CategoryInfo;
import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.po.VideoInfoFilePost;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = Constants.SERVER_NAME_WEB)
public interface VideoClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoFileByFileId")
    VideoInfoFile getVideoInfoFileByFileId(@RequestParam String fileId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoFilePostByFileId")
    VideoInfoFilePost getVideoInfoFilePostByFileId(@RequestParam String fileId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/transferVideoFile4Db")
    void transferVideoFile4Db(@RequestParam String videoId,
                                       @RequestParam String uploadId,
                                       @RequestParam String userId,
                                       @RequestBody VideoInfoFilePost uploadFilePost);
}
