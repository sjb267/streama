package com.streama.services;

import com.streama.entity.dto.AiAuditItemProgressMessage;
import com.streama.entity.dto.AuditResultMessage;
import com.streama.entity.po.AiAuditTask;
import com.streama.entity.po.AiAuditTaskItem;

import java.util.List;

public interface AiAuditTaskService {

    void createAuditTaskAndSend(String videoId);

    void handleAuditResult(AuditResultMessage resultMessage);

    void updateItemProgress(AiAuditItemProgressMessage progressMessage);

    AiAuditTask getLatestAuditTask(String videoId);

    List<AiAuditTaskItem> getLatestAuditItems(String videoId);
}
