package com.streama.task;

import com.streama.component.EsSearchComponent;
import com.streama.component.RedisComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.VideoPlayInfoDto;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.enums.SearchOrderTypeEnum;
import com.streama.services.VideoInfoPostService;
import com.streama.services.VideoInfoService;
import com.streama.utils.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class ExecuteQueueTask {
    private ExecutorService executorService = Executors.newFixedThreadPool(Constants.LENGTH_20);

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private EsSearchComponent esSearchComponent;

    @PostConstruct
    public void consumeVideoPlayQueue() {
        executorService.execute(() -> {
            while (true) {
                try {
                    VideoPlayInfoDto videoPlayInfoDto = redisComponent.getVideoPlayFromVideoPlayQueue();
                    if (videoPlayInfoDto == null) {
                        Thread.sleep(1500);
                        continue;
                    }
                    videoInfoService.addReadCount(videoPlayInfoDto.getVideoId());
                    //更新es播放数量
                    esSearchComponent.updateDocCount(videoPlayInfoDto.getVideoId(), SearchOrderTypeEnum.VIDEO_PLAY.getField(), 1);

                } catch (Exception e) {
                    log.error("获取视频播放队列文件消息失败", e);
                }
            }
        });
    }
}
