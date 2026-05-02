package com.streama.entity.enums;

public enum SearchOrderTypeEnum {
    VIDEO_PLAY(0, "playCount", "视频播放量"),
    VIDEO_TIME(1, "createTime", "视频时间"),
    VIDEO_DANMU(2, "danmuCount", "弹幕量"),
    VIDEO_COLLECT(3, "collectCount", "视频收藏量");

    private Integer type;
    private String field;
    private String desc;

    SearchOrderTypeEnum(Integer type, String field, String desc) {
        this.type = type;
        this.field = field;
        this.desc = desc;
    }

    public static SearchOrderTypeEnum getByType(Integer type) {
        for (SearchOrderTypeEnum item : SearchOrderTypeEnum.values()) {
            if (item.type == type) {
                return item;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public String getField() {
        return field;
    }

    public String getDesc() {
        return desc;
    }
}
