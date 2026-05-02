package com.streama.entity.enums;

public enum VideoStatusEnum {
    STATUS0(0, "转码中"),
    STATUS1(1, "转码失败"),
    STATUS2(2, "待审核"),
    STATUS3(3, "审核成功"),
    STATUS4(4, "审核不通过");

    private Integer status;
    private String desc;

    VideoStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static VideoStatusEnum getVideoStatusEnum(Integer status) {
        for (VideoStatusEnum videoStatusEnum : VideoStatusEnum.values()) {
            if (videoStatusEnum.getStatus().equals(status)) {
                return videoStatusEnum;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
