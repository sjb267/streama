package com.streama.entity.constants;

public class Constants {

    public static final Integer ZERO = 0;

    public static final Integer ONE = 1;

    public static final Integer LENGTH_10 = 10;

    public static final Integer LENGTH_15 = 15;

    public static final Integer LENGTH_30 = 30;

    public static final Integer LENGTH_20 = 20;

    public static final Long MB_SIZE = 1024 * 1024L;
    /**
     * 密码正则表达式：8-18位，包含数字、字母和特殊字符
     */
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";

    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60000;

    public static final Integer REDIS_KEY_EXPIRES_ONE_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;

    public static final Integer TIME_SECONDS_DAY = REDIS_KEY_EXPIRES_ONE_DAY / 1000;

    public static final String FILE_FOLDER = "file/";

    public static final String FILE_COVER = "cover/";

    public static final String FILE_VIDEO = "video/";

    public static final String FILE_FOLDER_TEMP = "temp/";

    public static final String TOKEN_WEB = "token";

    public static final String TOKEN_ADMIN = "adminToken";

    public static final String REDIS_KEY_PREFIX = "streama:";

    public static final String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "checkcode:";

    public static final String REDIS_KEY_TOKEN_WEB = REDIS_KEY_PREFIX + "token:web:";

    public static final String REDIS_KEY_TOKEN_ADMIN = REDIS_KEY_PREFIX + "token:admin:";

    public static final String REDIS_KEY_CATEGORY_LIST = REDIS_KEY_PREFIX + "category:list:";

    public static final String REDIS_KEY_UPLOADING_FILE = REDIS_KEY_PREFIX + "uploading:";

    public static final String REDIS_KEY_SYS_SETTING = REDIS_KEY_PREFIX + "sysSetting:";

    public static final String REDIS_KEY_FILE_DEL = REDIS_KEY_PREFIX + "file:list:del:";

    public static final String REDIS_KEY_QUEUE_TRANSFER = REDIS_KEY_PREFIX + "queue:transfer:";

    public static final String REDIS_KEY_QUEUE_VIDEO_PLAY = REDIS_KEY_PREFIX + "queue:video:play:";

    public static final String IMAGE_THUMBNAIL_SUFFIX = "_thumbnail.jpg";

    public static final String TEMP_VIDEO_NAME = "/temp.mp4";

    public static final String VIDEO_CODE_HEVC = "hevc";

    public static final String VIDEO_CODE_TEMP_FILE_SUFFIX = "_temp";

    public static final String TS_NAME = "index.ts";

    public static final String M3U8_NAME = "index.m3u8";

    public static final String REDIS_KEY_VIDEO_SEARCH_COUNT = REDIS_KEY_PREFIX + "video:search:";

    public static final String INNER_API_PREFIX = "/innerApi";

    public static final String SERVER_NAME_ADMIN = "streama-cloud-admin";

    public static final String SERVER_NAME_WEB = "streama-cloud-web";

    public static final String SERVER_NAME_RESOURCE = "streama-cloud-resource";

    public static final String SERVER_NAME_INTERACT = "streama-cloud-interact";

    public static final String RABBIT_AUDIT_EXCHANGE = "streama.audit.exchange";

    public static final String RABBIT_AUDIT_REQUEST_QUEUE = "streama.audit.request.queue";

    public static final String RABBIT_AUDIT_REQUEST_ROUTING_KEY = "audit.video.request";

    public static final String RABBIT_AUDIT_REQUEST_DLX = "streama.audit.request.dlx";

    public static final String RABBIT_AUDIT_REQUEST_DLQ = "streama.audit.request.dlq";

    public static final String RABBIT_AUDIT_RESULT_QUEUE = "streama.audit.result.queue";

    public static final String RABBIT_AUDIT_RESULT_ROUTING_KEY = "audit.video.result";

    public static final String RABBIT_AUDIT_RESULT_DLX = "streama.audit.result.dlx";

    public static final String RABBIT_AUDIT_RESULT_DLQ = "streama.audit.result.dlq";
}
