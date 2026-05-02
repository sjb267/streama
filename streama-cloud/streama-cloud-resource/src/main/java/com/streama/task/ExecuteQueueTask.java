package com.streama.task;

import com.streama.component.RedisComponent;
import com.streama.component.TransferFileComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.po.VideoInfoFilePost;
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
    private RedisComponent redisComponent;

    @Resource
    private TransferFileComponent transferFileComponent;

    @PostConstruct
    public void consumeTransferFileQueue() {
        executorService.execute(() -> {
            while (true) {
                try {
                    VideoInfoFilePost videoInfoFile = redisComponent.getFileFromTransferQueue();
                    if (videoInfoFile == null) {
                        Thread.sleep(1500);
                        continue;
                    }
                    transferFileComponent.transferVideoFile(videoInfoFile);
                } catch (Exception e) {
                    log.error("获取转码文件信息失败", e);
                }
            }
        });
    }
}
