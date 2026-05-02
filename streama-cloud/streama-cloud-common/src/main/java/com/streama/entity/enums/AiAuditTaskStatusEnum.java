package com.streama.entity.enums;

public enum AiAuditTaskStatusEnum {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    FINISHED(2, "完成"),
    FAIL(3, "失败"),
    CANCEL(4, "取消");

    private final Integer status;
    private final String desc;

    AiAuditTaskStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}

