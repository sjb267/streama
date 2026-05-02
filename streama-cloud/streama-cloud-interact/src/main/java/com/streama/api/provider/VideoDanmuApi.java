package com.streama.api.provider;

import com.streama.entity.constants.Constants;
import com.streama.entity.query.VideoDanmuQuery;
import com.streama.services.VideoDanmuService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping((Constants.INNER_API_PREFIX + "/danmu"))
public class VideoDanmuApi {
    @Resource
    private VideoDanmuService videoDanmuService;

    @RequestMapping("/delDanmuByVideoId")
    public void delDanmuByVideoId(@NotEmpty String videoId) {
        VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
        videoDanmuQuery.setVideoId(videoId);
        videoDanmuService.deleteByParam(videoDanmuQuery);
    }
}
