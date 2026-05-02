package com.streama.services.impl;

import com.streama.component.AuditMqProducer;
import com.streama.entity.dto.AiAuditItemProgressMessage;
import com.streama.entity.dto.AuditRequestMessage;
import com.streama.entity.dto.AuditResultMessage;
import com.streama.entity.enums.AiAuditItemStatusEnum;
import com.streama.entity.enums.AiAuditSourceTypeEnum;
import com.streama.entity.enums.AiAuditTaskStatusEnum;
import com.streama.entity.enums.VideoFileUpdateTypeEnum;
import com.streama.entity.po.AiAuditTask;
import com.streama.entity.po.AiAuditTaskItem;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;
import com.streama.entity.query.VideoInfoFilePostQuery;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.mappers.AiAuditTaskItemMapper;
import com.streama.mappers.AiAuditTaskMapper;
import com.streama.mappers.VideoInfoFilePostMapper;
import com.streama.mappers.VideoInfoMapper;
import com.streama.mappers.VideoInfoPostMapper;
import com.streama.services.AiAuditTaskService;
import com.streama.utils.JsonUtils;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service("aiAuditTaskService")
@Slf4j
public class AiAuditTaskServiceImpl implements AiAuditTaskService {

    @Resource
    private AiAuditTaskMapper aiAuditTaskMapper;

    @Resource
    private AiAuditTaskItemMapper aiAuditTaskItemMapper;

    @Resource
    private VideoInfoPostMapper<VideoInfoPost, com.streama.entity.query.VideoInfoPostQuery> videoInfoPostMapper;

    @Resource
    private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;

    @Resource
    private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

    @Resource
    private AuditMqProducer auditMqProducer;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void createAuditTaskAndSend(String videoId) {
        VideoInfoPost videoInfoPost = videoInfoPostMapper.selectByVideoId(videoId);
        if (videoInfoPost == null) {
            return;
        }

        Integer maxVersion = aiAuditTaskMapper.selectMaxAuditVersionByVideoId(videoId);
        Integer auditVersion = maxVersion == null ? 1 : maxVersion + 1;

        VideoInfo dbVideoInfo = videoInfoMapper.selectByVideoId(videoId);
        Integer sourceType = dbVideoInfo == null ? AiAuditSourceTypeEnum.NEW_VIDEO.getType() : AiAuditSourceTypeEnum.EDIT_VIDEO.getType();

        VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
        filePostQuery.setVideoId(videoId);
        if (AiAuditSourceTypeEnum.EDIT_VIDEO.getType().equals(sourceType)) {
            filePostQuery.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
        }
        filePostQuery.setOrderBy("file_index asc");
        List<VideoInfoFilePost> videoInfoFilePosts = videoInfoFilePostMapper.selectList(filePostQuery);

        Date now = new Date();
        AiAuditTask aiAuditTask = new AiAuditTask();
        aiAuditTask.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        aiAuditTask.setVideoId(videoId);
        aiAuditTask.setAuditVersion(auditVersion);
        aiAuditTask.setSourceType(sourceType);
        aiAuditTask.setTaskStatus(AiAuditTaskStatusEnum.PENDING.getStatus());
        aiAuditTask.setTriggerTime(now);
        aiAuditTask.setRetryCount(0);
        aiAuditTask.setCreatedAt(now);
        aiAuditTask.setUpdatedAt(now);
        aiAuditTaskMapper.insert(aiAuditTask);

        List<AiAuditTaskItem> aiAuditTaskItems = new ArrayList<>();
        for (VideoInfoFilePost filePost : videoInfoFilePosts) {
            AiAuditTaskItem item = new AiAuditTaskItem();
            item.setTaskId(aiAuditTask.getTaskId());
            item.setVideoId(videoId);
            item.setFileId(filePost.getFileId());
            item.setFileIndex(filePost.getFileIndex());
            item.setUploadId(filePost.getUploadId());
            item.setFileName(filePost.getFileName());
            item.setFilePath(filePost.getFilePath());
            item.setDuration(filePost.getDuration());
            item.setUpdateType(filePost.getUpdateType());
            item.setItemStatus(AiAuditItemStatusEnum.PENDING.getStatus());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            aiAuditTaskItems.add(item);
        }
        if (!aiAuditTaskItems.isEmpty()) {
            aiAuditTaskItemMapper.insertBatch(aiAuditTaskItems);
        } else {
            AiAuditTask taskUpdate = new AiAuditTask();
            taskUpdate.setTaskStatus(AiAuditTaskStatusEnum.CANCEL.getStatus());
            taskUpdate.setLastError("no_audit_items");
            taskUpdate.setUpdatedAt(new Date());
            aiAuditTaskMapper.updateByTaskId(taskUpdate, aiAuditTask.getTaskId());
            return;
        }

        AuditRequestMessage requestMessage = buildRequestMessage(aiAuditTask, videoInfoPost, aiAuditTaskItems);
        try {
            auditMqProducer.sendAuditRequest(requestMessage);
            AiAuditTask taskUpdate = new AiAuditTask();
            taskUpdate.setTaskStatus(AiAuditTaskStatusEnum.PROCESSING.getStatus());
            taskUpdate.setUpdatedAt(new Date());
            aiAuditTaskMapper.updateByTaskId(taskUpdate, aiAuditTask.getTaskId());
        } catch (Exception e) {
            log.error("发送AI审核请求失败, videoId:{}", videoId, e);
            AiAuditTask taskUpdate = new AiAuditTask();
            taskUpdate.setTaskStatus(AiAuditTaskStatusEnum.FAIL.getStatus());
            taskUpdate.setLastError(e.getMessage());
            taskUpdate.setUpdatedAt(new Date());
            aiAuditTaskMapper.updateByTaskId(taskUpdate, aiAuditTask.getTaskId());
        }
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void handleAuditResult(AuditResultMessage resultMessage) {
        if (resultMessage == null || StringTools.isEmpty(resultMessage.getRequestId())) {
            return;
        }
        AiAuditTask dbTask = aiAuditTaskMapper.selectByRequestId(resultMessage.getRequestId());
        if (dbTask == null) {
            return;
        }
        if (AiAuditTaskStatusEnum.FINISHED.getStatus().equals(dbTask.getTaskStatus())) {
            return;
        }

        Date now = new Date();
        List<AuditResultMessage.Item> resultItems = resultMessage.getItems();
        boolean hasFailedItem = false;
        String taskLastError = null;

        if (resultItems != null && !resultItems.isEmpty()) {
            for (AuditResultMessage.Item resultItem : resultItems) {
                if (StringTools.isEmpty(resultItem.getFileId())) {
                    continue;
                }
                Integer itemStatus = resultItem.getItemStatus() == null
                        ? AiAuditItemStatusEnum.FINISHED.getStatus()
                        : resultItem.getItemStatus();
                AiAuditTaskItem itemUpdate = new AiAuditTaskItem();
                itemUpdate.setItemStatus(itemStatus);
                itemUpdate.setItemDecision(resultItem.getItemDecision());
                itemUpdate.setRiskScore(resultItem.getRiskScore());
                itemUpdate.setRiskTagsJson(resultItem.getRiskTags() == null ? null : JsonUtils.convertObj2Json(resultItem.getRiskTags()));
                itemUpdate.setHitSegmentsJson(resultItem.getHitSegments() == null ? null : JsonUtils.convertObj2Json(resultItem.getHitSegments()));
                itemUpdate.setItemReason(resultItem.getItemReason());
                itemUpdate.setUpdatedAt(now);
                aiAuditTaskItemMapper.updateByTaskIdAndFileId(itemUpdate, dbTask.getTaskId(), resultItem.getFileId());

                if (AiAuditItemStatusEnum.FAIL.getStatus().equals(itemStatus)) {
                    hasFailedItem = true;
                    if (StringTools.isEmpty(taskLastError)) {
                        taskLastError = resultItem.getItemReason();
                    }
                }
            }
        }

        AiAuditTask taskUpdate = new AiAuditTask();
        taskUpdate.setTaskStatus(hasFailedItem ? AiAuditTaskStatusEnum.FAIL.getStatus() : AiAuditTaskStatusEnum.FINISHED.getStatus());
        taskUpdate.setAiDecision(resultMessage.getVideoDecision());
        taskUpdate.setAiRiskLevel(resultMessage.getVideoRiskLevel());
        taskUpdate.setAiSummary(resultMessage.getVideoSummary());
        taskUpdate.setModelName(resultMessage.getModelName());
        taskUpdate.setModelVersion(resultMessage.getModelVersion());
        taskUpdate.setCompletedAt(resultMessage.getCompletedAt() == null ? now : resultMessage.getCompletedAt());
        taskUpdate.setLastError(taskLastError);
        taskUpdate.setUpdatedAt(now);
        aiAuditTaskMapper.updateByTaskId(taskUpdate, dbTask.getTaskId());
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void updateItemProgress(AiAuditItemProgressMessage progressMessage) {
        if (progressMessage == null
                || StringTools.isEmpty(progressMessage.getRequestId())
                || StringTools.isEmpty(progressMessage.getFileId())
                || progressMessage.getItemStatus() == null) {
            return;
        }
        AiAuditTask dbTask = aiAuditTaskMapper.selectByRequestId(progressMessage.getRequestId());
        if (dbTask == null) {
            return;
        }

        Date now = progressMessage.getUpdatedAt() == null ? new Date() : progressMessage.getUpdatedAt();
        AiAuditTaskItem itemUpdate = new AiAuditTaskItem();
        itemUpdate.setItemStatus(progressMessage.getItemStatus());
        if (!StringTools.isEmpty(progressMessage.getLastError())) {
            itemUpdate.setItemReason(progressMessage.getLastError());
        }
        itemUpdate.setUpdatedAt(now);
        aiAuditTaskItemMapper.updateByTaskIdAndFileId(itemUpdate, dbTask.getTaskId(), progressMessage.getFileId());

        AiAuditTask taskUpdate = new AiAuditTask();
        if (AiAuditItemStatusEnum.FAIL.getStatus().equals(progressMessage.getItemStatus())) {
            taskUpdate.setTaskStatus(AiAuditTaskStatusEnum.FAIL.getStatus());
            taskUpdate.setLastError(progressMessage.getLastError());
        } else if (AiAuditItemStatusEnum.PROCESSING.getStatus().equals(progressMessage.getItemStatus())) {
            taskUpdate.setTaskStatus(AiAuditTaskStatusEnum.PROCESSING.getStatus());
        }
        taskUpdate.setUpdatedAt(now);
        aiAuditTaskMapper.updateByTaskId(taskUpdate, dbTask.getTaskId());
    }

    @Override
    public List<AiAuditTaskItem> getLatestAuditItems(String videoId) {
        AiAuditTask latestTask = aiAuditTaskMapper.selectLatestByVideoId(videoId);
        if (latestTask == null) {
            return new ArrayList<>();
        }
        return aiAuditTaskItemMapper.selectByTaskId(latestTask.getTaskId());
    }

    @Override
    public AiAuditTask getLatestAuditTask(String videoId) {
        return aiAuditTaskMapper.selectLatestByVideoId(videoId);
    }

    private AuditRequestMessage buildRequestMessage(AiAuditTask task, VideoInfoPost videoInfoPost, List<AiAuditTaskItem> taskItems) {
        AuditRequestMessage requestMessage = new AuditRequestMessage();
        requestMessage.setRequestId(task.getRequestId());
        requestMessage.setVideoId(task.getVideoId());
        requestMessage.setAuditVersion(task.getAuditVersion());
        requestMessage.setSourceType(task.getSourceType());
        requestMessage.setTriggerTime(task.getTriggerTime());

        AuditRequestMessage.VideoMeta videoMeta = new AuditRequestMessage.VideoMeta();
        videoMeta.setUserId(videoInfoPost.getUserId());
        videoMeta.setVideoName(videoInfoPost.getVideoName());
        videoMeta.setVideoCover(videoInfoPost.getVideoCover());
        videoMeta.setTags(videoInfoPost.getTags());
        videoMeta.setIntroduction(videoInfoPost.getIntroduction());
        videoMeta.setPCategoryId(videoInfoPost.getPCategoryId());
        videoMeta.setCategoryId(videoInfoPost.getCategoryId());
        videoMeta.setPostType(videoInfoPost.getPostType());
        requestMessage.setVideoMeta(videoMeta);

        List<AuditRequestMessage.Item> requestItems = new ArrayList<>();
        for (AiAuditTaskItem taskItem : taskItems) {
            AuditRequestMessage.Item item = new AuditRequestMessage.Item();
            item.setFileId(taskItem.getFileId());
            item.setFileIndex(taskItem.getFileIndex());
            item.setUploadId(taskItem.getUploadId());
            item.setFileName(taskItem.getFileName());
            item.setFilePath(taskItem.getFilePath());
            item.setDuration(taskItem.getDuration());
            item.setUpdateType(taskItem.getUpdateType());
            requestItems.add(item);
        }
        requestMessage.setItems(requestItems);
        return requestMessage;
    }
}
