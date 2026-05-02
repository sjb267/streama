package com.streama.entity.enums;

public enum AiAuditSourceTypeEnum {
    NEW_VIDEO(1, "新增投稿"),
    EDIT_VIDEO(2, "编辑重审");

    private final Integer type;
    private final String desc;

    AiAuditSourceTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}

