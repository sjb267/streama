package com.streama.api.provider;

import com.streama.entity.constants.Constants;
import com.streama.entity.query.VideoCommentQuery;
import com.streama.services.VideoCommentService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/comment")
public class VideoCommentApi {
    @Resource
    private VideoCommentService videoCommentService;

    @RequestMapping("/delCommentByVideoId")
    public void delCommentByVideoId(@NotEmpty String videoId) {
        VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
        videoCommentQuery.setVideoId(videoId);
        videoCommentService.deleteByParam(videoCommentQuery);
    }
}
