package com.streama.component;

import com.streama.entity.constants.Constants;
import com.streama.entity.dto.AuditResultMessage;
import com.streama.services.AiAuditTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditResultConsumer {

    @Resource
    private AiAuditTaskService aiAuditTaskService;

    @RabbitListener(queues = Constants.RABBIT_AUDIT_RESULT_QUEUE)
    public void consumeAuditResult(AuditResultMessage resultMessage) {
        try {
            aiAuditTaskService.handleAuditResult(resultMessage);
        } catch (Exception e) {
            log.error("处理AI审核结果失败", e);
            throw e;
        }
    }
}

