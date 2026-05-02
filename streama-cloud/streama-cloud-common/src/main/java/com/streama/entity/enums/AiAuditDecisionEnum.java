package com.streama.entity.enums;

public enum AiAuditDecisionEnum {
    PASS(1, "建议通过"),
    REJECT(2, "建议驳回"),
    MANUAL_REVIEW(3, "建议人工复核");

    private final Integer decision;
    private final String desc;

    AiAuditDecisionEnum(Integer decision, String desc) {
        this.decision = decision;
        this.desc = desc;
    }

    public Integer getDecision() {
        return decision;
    }

    public String getDesc() {
        return desc;
    }
}

