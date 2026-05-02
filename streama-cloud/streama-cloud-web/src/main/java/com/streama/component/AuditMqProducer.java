package com.streama.component;

import com.streama.entity.constants.Constants;
import com.streama.entity.dto.AuditRequestMessage;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditMqProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendAuditRequest(AuditRequestMessage message) {
        rabbitTemplate.convertAndSend(Constants.RABBIT_AUDIT_EXCHANGE, Constants.RABBIT_AUDIT_REQUEST_ROUTING_KEY, message);
    }
}

